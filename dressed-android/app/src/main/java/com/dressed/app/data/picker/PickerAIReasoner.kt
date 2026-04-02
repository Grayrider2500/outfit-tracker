package com.dressed.app.data.picker

import com.dressed.app.BuildConfig
import com.dressed.app.data.model.WardrobeCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

private const val DAY_MS = 86400000L
private const val ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val CLAUDE_MODEL = "claude-3-5-haiku-20241022"
private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
private const val OPENAI_MODEL = "gpt-4o-mini"

private val jsonParser = Json { ignoreUnknownKeys = true }

enum class PickerAiBannerState {
    /** Release / non-debug policy: no cloud calls. */
    FEATURE_DISABLED_IN_BUILD,

    NEEDS_KEY,
    KEY_SAVED_REASONING_OFF,
    READY,
}

/**
 * Unified BYOK picker reasoning: Anthropic, OpenAI, or Grok placeholder (rule-based only).
 */
object PickerAIReasoner {

    fun resolvedApiKey(provider: PickerAIProvider, store: AiPickerPreferencesStore): String {
        store.getStoredApiKey(provider)?.takeIf { it.isNotBlank() }?.let { return it }
        return when (provider) {
            PickerAIProvider.ANTHROPIC -> BuildConfig.ANTHROPIC_API_KEY
            PickerAIProvider.OPENAI -> BuildConfig.OPENAI_API_KEY
            PickerAIProvider.GROK -> ""
        }
    }

    fun resolveBannerState(store: AiPickerPreferencesStore): Pair<PickerAiBannerState, PickerAIProvider> {
        if (!BuildConfig.ENABLE_AI_REASONING) {
            return PickerAiBannerState.FEATURE_DISABLED_IN_BUILD to store.selectedProvider
        }
        val p = store.selectedProvider
        if (p == PickerAIProvider.GROK) {
            return PickerAiBannerState.NEEDS_KEY to p
        }
        val hasCred = resolvedApiKey(p, store).isNotBlank()
        if (!hasCred) return PickerAiBannerState.NEEDS_KEY to p
        return if (store.isReasoningEnabled) {
            PickerAiBannerState.READY to p
        } else {
            PickerAiBannerState.KEY_SAVED_REASONING_OFF to p
        }
    }

    /** Returns null on success, or an error message. */
    suspend fun validateApiKey(provider: PickerAIProvider, apiKey: String): String? = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return@withContext "Enter an API key to test."
        when (provider) {
            PickerAIProvider.GROK ->
                return@withContext "Grok / xAI isn’t available in this build yet — choose Anthropic or OpenAI."
            PickerAIProvider.ANTHROPIC -> validateAnthropic(trimmed)
            PickerAIProvider.OPENAI -> validateOpenAI(trimmed)
        }
    }

    suspend fun enrichReasons(
        store: AiPickerPreferencesStore,
        suggestions: List<WardrobePickerEngine.PickerSuggestion>,
        occasionLabel: String,
        weatherLabels: List<String>,
        moodLabels: List<String>,
        nowEpochMs: Long,
    ): List<WardrobePickerEngine.PickerSuggestion> = withContext(Dispatchers.IO) {
        if (!BuildConfig.ENABLE_AI_REASONING || !store.isReasoningEnabled) return@withContext suggestions
        val provider = store.selectedProvider
        if (provider == PickerAIProvider.GROK) return@withContext suggestions

        val apiKey = resolvedApiKey(provider, store)
        if (apiKey.isBlank() || suggestions.isEmpty()) return@withContext suggestions

        val userContent = buildUserPrompt(suggestions, occasionLabel, weatherLabels, moodLabels, nowEpochMs)
        val text = when (provider) {
            PickerAIProvider.ANTHROPIC -> runCatching { completeAnthropic(apiKey, userContent) }.getOrNull()
            PickerAIProvider.OPENAI -> runCatching { completeOpenAI(apiKey, userContent) }.getOrNull()
            PickerAIProvider.GROK -> null
        } ?: return@withContext suggestions

        val reasons = runCatching { parseReasonsJson(text, suggestions.size) }.getOrNull()
            ?: return@withContext suggestions

        suggestions.mapIndexed { i, s ->
            val r = reasons.getOrNull(i)?.trim().orEmpty()
            if (r.isNotEmpty()) s.copy(reason = r) else s
        }
    }

    private fun validateAnthropic(apiKey: String): String? {
        val bodyObj = buildJsonObject {
            put("model", JsonPrimitive(CLAUDE_MODEL))
            put("max_tokens", JsonPrimitive(8))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive("Reply with the single word ok."))
                        },
                    )
                },
            )
        }
        return postAnthropicRaw(apiKey, bodyObj.toString())
    }

    private fun validateOpenAI(apiKey: String): String? {
        val bodyObj = buildJsonObject {
            put("model", JsonPrimitive(OPENAI_MODEL))
            put("max_tokens", JsonPrimitive(16))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive("Reply with the single word ok."))
                        },
                    )
                },
            )
        }
        return postOpenAiRaw(apiKey, bodyObj.toString())
    }

    private fun completeAnthropic(apiKey: String, userText: String): String {
        val bodyObj = buildJsonObject {
            put("model", JsonPrimitive(CLAUDE_MODEL))
            put("max_tokens", JsonPrimitive(1024))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userText))
                        },
                    )
                },
            )
        }
        val conn = (URL(ANTHROPIC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
        }
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(bodyObj.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Anthropic HTTP $code")

        val root = jsonParser.parseToJsonElement(responseText).jsonObject
        val content = root["content"]?.jsonArray ?: error("no content")
        return buildString {
            for (el in content) {
                val o = el.jsonObject
                if (o["type"]?.jsonPrimitive?.content == "text") {
                    append(o["text"]?.jsonPrimitive?.content.orEmpty())
                }
            }
        }
    }

    /** @return null if HTTP success, else error string */
    private fun postAnthropicRaw(apiKey: String, jsonBody: String): String? {
        val conn = (URL(ANTHROPIC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
        }
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        return when (code) {
            in 200..299 -> null
            401, 403 -> "Key rejected (check that it is valid and active)."
            else -> {
                val short = if (responseText.length > 120) responseText.take(120) + "…" else responseText
                if (short.isEmpty()) "Request failed (HTTP $code)." else "HTTP $code: $short"
            }
        }
    }

    private fun completeOpenAI(apiKey: String, userText: String): String {
        val bodyObj = buildJsonObject {
            put("model", JsonPrimitive(OPENAI_MODEL))
            put("max_tokens", JsonPrimitive(1024))
            put(
                "response_format",
                buildJsonObject { put("type", JsonPrimitive("json_object")) },
            )
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userText))
                        },
                    )
                },
            )
        }
        val conn = (URL(OPENAI_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(bodyObj.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("OpenAI HTTP $code")

        val root = jsonParser.parseToJsonElement(responseText).jsonObject
        val choices = root["choices"]?.jsonArray ?: error("no choices")
        val first = choices.firstOrNull()?.jsonObject ?: error("empty choices")
        val message = first["message"]?.jsonObject ?: error("no message")
        return message["content"]?.jsonPrimitive?.content ?: error("no content")
    }

    private fun postOpenAiRaw(apiKey: String, jsonBody: String): String? {
        val conn = (URL(OPENAI_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(jsonBody) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        return when (code) {
            in 200..299 -> null
            401, 403 -> "Key rejected (check that it is valid and active)."
            else -> {
                val short = if (responseText.length > 120) responseText.take(120) + "…" else responseText
                if (short.isEmpty()) "Request failed (HTTP $code)." else "HTTP $code: $short"
            }
        }
    }

    private fun buildUserPrompt(
        suggestions: List<WardrobePickerEngine.PickerSuggestion>,
        occasionLabel: String,
        weatherLabels: List<String>,
        moodLabels: List<String>,
        nowEpochMs: Long,
    ): String {
        val outfits = buildJsonArray {
            suggestions.forEachIndexed { idx, s ->
                add(
                    buildJsonObject {
                        put("index", JsonPrimitive(idx))
                        put("title", JsonPrimitive(s.title))
                        put("rule_based_hint", JsonPrimitive(s.reason))
                        put(
                            "pieces",
                            buildJsonArray {
                                s.items.forEach { it ->
                                    val days = it.lastWornAtEpochMs?.let { lw ->
                                        ((nowEpochMs - lw) / DAY_MS).toInt()
                                    }
                                    add(
                                        buildJsonObject {
                                            put("name", JsonPrimitive(it.name))
                                            put("category", JsonPrimitive(WardrobeCategories.label(it.category)))
                                            put("color", JsonPrimitive(it.colorName))
                                            put("worn_count", JsonPrimitive(it.wornCount))
                                            if (days != null) {
                                                put("days_since_last_worn", JsonPrimitive(days))
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }
        val w = if (weatherLabels.isEmpty()) "none" else weatherLabels.joinToString(", ")
        val m = if (moodLabels.isEmpty()) "none" else moodLabels.joinToString(", ")
        return buildString {
            appendLine("You are a friendly wardrobe assistant inside a mobile app.")
            appendLine("The app already chose these outfits using deterministic rules and scoring.")
            appendLine("For each outfit, write ONE short explanation (1–2 sentences), warm and specific.")
            appendLine("Mention concrete pieces/colors when natural, and occasion/weather/mood if helpful.")
            appendLine("Do not contradict the outfit composition.")
            appendLine()
            appendLine("Return ONLY valid JSON with this exact shape, no markdown fences:")
            appendLine("""{"reasons":["...","..."]}""")
            appendLine("Use exactly ${suggestions.size} strings in the same order as the outfits below.")
            appendLine()
            appendLine("Occasion: $occasionLabel")
            appendLine("Weather tags: $w")
            appendLine("Mood tags: $m")
            appendLine()
            appendLine("Outfits JSON:")
            appendLine(outfits.toString())
        }
    }

    private fun parseReasonsJson(modelText: String, expected: Int): List<String>? {
        var t = modelText.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```").trim()
            if (t.endsWith("```")) t = t.removeSuffix("```").trim()
        }
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val slice = t.substring(start, end + 1)
        val obj = jsonParser.parseToJsonElement(slice).jsonObject
        val arr = obj["reasons"]?.jsonArray ?: return null
        val list = arr.map { it.jsonPrimitive.content }
        if (list.size != expected) return null
        return list
    }
}
