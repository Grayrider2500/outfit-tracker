package com.dressed.app.data.picker

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted storage for picker AI: master toggle, selected provider, and per-provider API keys.
 */
class AiPickerPreferencesStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    var isReasoningEnabled: Boolean
        get() {
            if (!prefs.contains(KEY_REASONING)) return true
            return prefs.getBoolean(KEY_REASONING, true)
        }
        set(value) {
            prefs.edit().putBoolean(KEY_REASONING, value).apply()
        }

    var selectedProvider: PickerAIProvider
        get() = PickerAIProvider.fromStorageKey(prefs.getString(KEY_PROVIDER, null))
        set(value) {
            prefs.edit().putString(KEY_PROVIDER, value.storageKey).apply()
        }

    fun getStoredApiKey(provider: PickerAIProvider): String? =
        prefs.getString(apiKeyPrefKey(provider), null)?.trim()?.takeUnless { it.isEmpty() }

    fun setStoredApiKey(provider: PickerAIProvider, key: String?) {
        prefs.edit().apply {
            val k = key?.trim().orEmpty()
            if (k.isEmpty()) remove(apiKeyPrefKey(provider)) else putString(apiKeyPrefKey(provider), k)
            apply()
        }
    }

    fun hasStoredKey(provider: PickerAIProvider): Boolean = !getStoredApiKey(provider).isNullOrEmpty()

    private fun apiKeyPrefKey(provider: PickerAIProvider): String = KEY_API_PREFIX + provider.storageKey

    companion object {
        private const val PREFS_FILE = "ai_picker_secure_prefs"
        private const val KEY_REASONING = "reasoning_enabled"
        private const val KEY_PROVIDER = "selected_provider"
        private const val KEY_API_PREFIX = "api_key_"

        private fun createPrefs(context: Context): SharedPreferences {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
