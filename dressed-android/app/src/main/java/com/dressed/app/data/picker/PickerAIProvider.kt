package com.dressed.app.data.picker

/** BYOK cloud provider for wardrobe picker explanations. */
enum class PickerAIProvider(val storageKey: String) {
    ANTHROPIC("anthropic"),
    OPENAI("openai"),
    GROK("grok"),
    ;

    val displayName: String
        get() = when (this) {
            ANTHROPIC -> "Anthropic (Claude)"
            OPENAI -> "OpenAI"
            GROK -> "Grok / xAI"
        }

    val shortBannerLabel: String
        get() = when (this) {
            ANTHROPIC -> "Claude"
            OPENAI -> "OpenAI"
            GROK -> "Grok"
        }

    companion object {
        fun fromStorageKey(raw: String?): PickerAIProvider {
            if (raw == null) return ANTHROPIC
            return entries.find { it.storageKey == raw } ?: ANTHROPIC
        }
    }
}
