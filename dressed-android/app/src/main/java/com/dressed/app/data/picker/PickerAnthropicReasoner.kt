package com.dressed.app.data.picker

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

private val jsonParser = Json { ignoreUnknownKeys = true }

/**
 * Phase 3: optional Claude (Anthropic) copy for picker reasons. Rule-based [WardrobePickerEngine] is unchanged;
 * this layer only rewrites [PickerSuggestion.reason] when the API succeeds.
 */
object PickerAnthropicReasoner {

    suspend fun enrichReasons(
        apiKey: String,
        suggestions: List<WardrobePickerEngine.PickerSuggestion>,
        occasionLabel: String,
        weatherLabels: List<String>,
        moodLabels: List<String>,
        nowEpochMs: Long,
    ): List<WardrobePickerEngine.PickerSuggestion> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || suggestions.isEmpty()) return@withContext suggestions

        val userContent = buildUserPrompt(suggestions, occasionLabel, weatherLabels, moodLabels, nowEpochMs)
        val bodyObj = buildJsonObject {
            put("model", JsonPrimitive(CLAUDE_MODEL))
            put("max_tokens", JsonPrimitive(1024))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userContent))
                        },
                    )
                },
            )
        }
        val body = bodyObj.toString()

        val text = runCatching { postAnthropic(apiKey, body) }.getOrNull() ?: return@withContext suggestions
        val reasons = runCatching { parseReasonsJson(text, suggestions.size) }.getOrNull()
            ?: return@withContext suggestions

        suggestions.mapIndexed { i, s ->
            val r = reasons.getOrNull(i)?.trim().orEmpty()
            if (r.isNotEmpty()) s.copy(reason = r) else s
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

    private fun postAnthropic(apiKey: String, jsonBody: String): String {
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
