package com.lukasosstudios.localnotes.data

import android.content.Context
import com.lukasosstudios.localnotes.model.SortMode
import com.lukasosstudios.localnotes.model.ThemeMode
import java.util.Properties

/**
 * Settings are cached in SharedPreferences for instant startup (so the theme
 * can be applied before storage permission is even checked), and mirrored to
 * Android/media/settings.properties whenever they change.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("local_notes_prefs", Context.MODE_PRIVATE)

    var theme: ThemeMode
        get() = ThemeMode.fromId(prefs.getString(KEY_THEME, null))
        set(value) {
            prefs.edit().putString(KEY_THEME, value.id).apply()
        }

    var sort: SortMode
        get() = SortMode.fromId(prefs.getString(KEY_SORT, null))
        set(value) {
            prefs.edit().putString(KEY_SORT, value.id).apply()
        }

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()
        }

    /** Reads Android/media/settings.properties (if present) into the cache. */
    fun loadFromFile(repository: NoteRepository) {
        val file = repository.settingsFile()
        if (!file.exists()) return
        try {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            theme = ThemeMode.fromId(props.getProperty("theme"))
            sort = SortMode.fromId(props.getProperty("sort"))
            appLockEnabled = props.getProperty("app_lock") == "true"
        } catch (_: Exception) {
            // Keep whatever is already cached
        }
    }

    /** Writes the current settings out to Android/media/settings.properties. */
    fun writeToFile(repository: NoteRepository, notesCount: Int) {
        if (!repository.hasPermission()) return
        repository.ensureDirs()
        val sb = StringBuilder()
        sb.append("theme=").append(theme.id).append('\n')
        sb.append("sort=").append(sort.id).append('\n')
        sb.append("app_lock=").append(appLockEnabled).append('\n')
        sb.append("notes_count=").append(notesCount).append('\n')
        sb.append("storage_format=plain-text\n")
        try {
            repository.settingsFile().writeText(sb.toString())
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_SORT = "sort"
        private const val KEY_APP_LOCK = "app_lock"
    }
}
