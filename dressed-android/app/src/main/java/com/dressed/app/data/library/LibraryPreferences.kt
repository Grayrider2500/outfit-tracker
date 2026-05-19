package com.dressed.app.data.library

import android.content.Context

/** SharedPreferences for borrowable library sharing (sharer name + explainer). */
class LibraryPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var sharerDisplayName: String
        get() = prefs.getString(KEY_SHARER, null)?.trim().orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SHARER, value.trim()).apply()
        }

    /** Mirrors spec key `library_explainer_seen`: when true, skip the first-use explainer. */
    var libraryExplainerSeen: Boolean
        get() = prefs.getBoolean(KEY_EXPLAINER_SEEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_EXPLAINER_SEEN, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "dressed_library_prefs"
        private const val KEY_SHARER = "library_sharer_name"
        /** Same logical name as iOS `library_explainer_seen`. */
        private const val KEY_EXPLAINER_SEEN = "library_explainer_seen"
    }
}
