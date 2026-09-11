package com.lukasosstudios.localnotes.util

import android.content.Context
import android.os.Environment
import com.lukasosstudios.localnotes.R
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Properties

data class LanguageOption(
    val id: String,
    val displayName: String,
    val isBuiltIn: Boolean
)

object TranslationManager {

    @Volatile private var overrides: Map<String, String> = emptyMap()
    @Volatile private var loadedId: String? = null

    private val BUILT_IN = mapOf(
        "fr" to R.raw.lang_fr,
        "de" to R.raw.lang_de,
        "es" to R.raw.lang_es
    )

    fun currentOverrides(): Map<String, String> = overrides

    fun translationsDir(context: Context): File =
        File(Environment.getExternalStorageDirectory(), "Android/media/${context.packageName}/translations")

    fun builtInOptions(): List<LanguageOption> = listOf(
        LanguageOption("en", "English", true),
        LanguageOption("fr", "Français", true),
        LanguageOption("de", "Deutsch", true),
        LanguageOption("es", "Español", true)
    )

    fun customOptions(context: Context): List<LanguageOption> {
        val dir = translationsDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) } ?: return emptyList()
        return files.map { file ->
            val name = displayNameForFile(file) ?: file.name.removeSuffix(EXTENSION)
            LanguageOption(file.name, name, false)
        }.sortedBy { it.displayName }
    }

    fun activeId(context: Context): String = prefs(context).getString(KEY_ACTIVE, "en") ?: "en"

    fun setActive(context: Context, id: String) {
        prefs(context).edit().putString(KEY_ACTIVE, id).apply()
        reload(context)
    }

    fun ensureLoaded(context: Context) {
        val active = activeId(context)
        if (active == loadedId) return
        reload(context)
    }

    fun reload(context: Context) {
        val active = activeId(context)
        loadedId = active
        overrides = when {
            active == "en" -> emptyMap()
            BUILT_IN.containsKey(active) -> loadProperties(context.resources.openRawResource(BUILT_IN.getValue(active)))
            active.endsWith(EXTENSION) -> {
                try {
                    loadProperties(File(translationsDir(context), active).inputStream())
                } catch (e: Exception) {
                    emptyMap()
                }
            }
            else -> emptyMap()
        }
    }

    private val ALL_STRINGS: List<Pair<String, Int>> = listOf(
        "eyebrow_home" to R.string.eyebrow_home,
        "summary_label" to R.string.summary_label,
        "notes_count_one" to R.string.notes_count_one,
        "notes_count_other" to R.string.notes_count_other,
        "search_hint" to R.string.search_hint,
        "filter_all" to R.string.filter_all,
        "filter_pinned" to R.string.filter_pinned,
        "filter_archived" to R.string.filter_archived,
        "filter_trash" to R.string.filter_trash,
        "section_your_notes" to R.string.section_your_notes,
        "empty_trash" to R.string.empty_trash,
        "new_note" to R.string.new_note,
        "untitled_note" to R.string.untitled_note,
        "no_text_yet" to R.string.no_text_yet,
        "empty_title_search" to R.string.empty_title_search,
        "empty_title_trash" to R.string.empty_title_trash,
        "empty_title_default" to R.string.empty_title_default,
        "empty_copy_search" to R.string.empty_copy_search,
        "empty_copy_default" to R.string.empty_copy_default,
        "empty_title_needs_permission" to R.string.empty_title_needs_permission,
        "empty_copy_needs_permission" to R.string.empty_copy_needs_permission,
        "cd_calculator" to R.string.cd_calculator,
        "cd_settings" to R.string.cd_settings,
        "cd_pin" to R.string.cd_pin,
        "cd_unpin" to R.string.cd_unpin,
        "cd_archive" to R.string.cd_archive,
        "cd_unarchive" to R.string.cd_unarchive,
        "cd_trash" to R.string.cd_trash,
        "cd_restore" to R.string.cd_restore,
        "cd_delete_forever" to R.string.cd_delete_forever,
        "editor_title_new" to R.string.editor_title_new,
        "editor_title_edit" to R.string.editor_title_edit,
        "save" to R.string.save,
        "title_hint" to R.string.title_hint,
        "body_hint" to R.string.body_hint,
        "saved_locally_hint" to R.string.saved_locally_hint,
        "note_color_label" to R.string.note_color_label,
        "privacy_tip" to R.string.privacy_tip,
        "calculator_title" to R.string.calculator_title,
        "calculator_ready" to R.string.calculator_ready,
        "calculator_helper" to R.string.calculator_helper,
        "settings_title" to R.string.settings_title,
        "settings_eyebrow" to R.string.settings_eyebrow,
        "settings_heading" to R.string.settings_heading,
        "settings_subtitle" to R.string.settings_subtitle,
        "section_appearance" to R.string.section_appearance,
        "theme_system" to R.string.theme_system,
        "theme_light" to R.string.theme_light,
        "theme_dark" to R.string.theme_dark,
        "section_sort" to R.string.section_sort,
        "sort_updated" to R.string.sort_updated,
        "sort_created" to R.string.sort_created,
        "sort_title" to R.string.sort_title,
        "section_storage" to R.string.section_storage,
        "storage_title" to R.string.storage_title,
        "storage_subtitle" to R.string.storage_subtitle,
        "storage_granted" to R.string.storage_granted,
        "storage_not_granted" to R.string.storage_not_granted,
        "about_title" to R.string.about_title,
        "permissions_link" to R.string.permissions_link,
        "storage_permission_rationale" to R.string.storage_permission_rationale,
        "color_cream" to R.string.color_cream,
        "color_blue" to R.string.color_blue,
        "color_sage" to R.string.color_sage,
        "color_rose" to R.string.color_rose,
        "color_lavender" to R.string.color_lavender,
        "color_peach" to R.string.color_peach,
        "color_mint" to R.string.color_mint,
        "color_slate" to R.string.color_slate,
        "color_sun" to R.string.color_sun,
        "color_ocean" to R.string.color_ocean,
        "color_plum" to R.string.color_plum,
        "color_custom" to R.string.color_custom,
        "delete_forever_title" to R.string.delete_forever_title,
        "delete_forever_message" to R.string.delete_forever_message,
        "delete" to R.string.delete,
        "cancel" to R.string.cancel,
        "trash_confirm_title" to R.string.trash_confirm_title,
        "trash_confirm_message" to R.string.trash_confirm_message,
        "trash_action" to R.string.trash_action,
        "bulk_trash_confirm_title" to R.string.bulk_trash_confirm_title,
        "bulk_trash_confirm_message" to R.string.bulk_trash_confirm_message,
        "empty_trash_confirm_title" to R.string.empty_trash_confirm_title,
        "empty_trash_confirm_message" to R.string.empty_trash_confirm_message,
        "selection_count" to R.string.selection_count,
        "cd_select_cancel" to R.string.cd_select_cancel,
        "cd_bulk_pin" to R.string.cd_bulk_pin,
        "cd_bulk_archive" to R.string.cd_bulk_archive,
        "cd_bulk_trash" to R.string.cd_bulk_trash,
        "cd_bulk_restore" to R.string.cd_bulk_restore,
        "cd_bulk_delete_forever" to R.string.cd_bulk_delete_forever,
        "bulk_delete_forever_title" to R.string.bulk_delete_forever_title,
        "bulk_delete_forever_message" to R.string.bulk_delete_forever_message,
        "custom_color_title" to R.string.custom_color_title,
        "custom_color_hex_hint" to R.string.custom_color_hex_hint,
        "use_color" to R.string.use_color,
        "section_backup" to R.string.section_backup,
        "backup_export_title" to R.string.backup_export_title,
        "backup_export_subtitle" to R.string.backup_export_subtitle,
        "backup_import_title" to R.string.backup_import_title,
        "backup_import_subtitle" to R.string.backup_import_subtitle,
        "backup_export_success" to R.string.backup_export_success,
        "backup_export_failed" to R.string.backup_export_failed,
        "backup_import_success" to R.string.backup_import_success,
        "backup_import_failed" to R.string.backup_import_failed,
        "backup_import_confirm_title" to R.string.backup_import_confirm_title,
        "backup_import_confirm_message" to R.string.backup_import_confirm_message,
        "import_action" to R.string.import_action,
        "section_security" to R.string.section_security,
        "app_lock_title" to R.string.app_lock_title,
        "app_lock_subtitle" to R.string.app_lock_subtitle,
        "app_lock_unavailable" to R.string.app_lock_unavailable,
        "lock_screen_title" to R.string.lock_screen_title,
        "lock_screen_subtitle" to R.string.lock_screen_subtitle,
        "unlock_button" to R.string.unlock_button,
        "lock_prompt_title" to R.string.lock_prompt_title,
        "use_pin_instead" to R.string.use_pin_instead,
        "use_fingerprint_instead" to R.string.use_fingerprint_instead,
        "enter_pin_title" to R.string.enter_pin_title,
        "pin_incorrect" to R.string.pin_incorrect,
        "pin_locked_out" to R.string.pin_locked_out,
        "pin_setup_title" to R.string.pin_setup_title,
        "pin_setup_enter_new" to R.string.pin_setup_enter_new,
        "pin_setup_confirm" to R.string.pin_setup_confirm,
        "pin_setup_mismatch" to R.string.pin_setup_mismatch,
        "pin_setup_too_short" to R.string.pin_setup_too_short,
        "pin_row_title" to R.string.pin_row_title,
        "pin_row_subtitle_set" to R.string.pin_row_subtitle_set,
        "pin_row_subtitle_unset" to R.string.pin_row_subtitle_unset,
        "pin_set_badge" to R.string.pin_set_badge,
        "pin_not_set_badge" to R.string.pin_not_set_badge,
        "remove_pin_title" to R.string.remove_pin_title,
        "remove_pin_message" to R.string.remove_pin_message,
        "remove_action" to R.string.remove_action,
        "section_language" to R.string.section_language,
        "export_template_title" to R.string.export_template_title,
        "export_template_subtitle" to R.string.export_template_subtitle,
        "export_template_success" to R.string.export_template_success,
        "export_template_failed" to R.string.export_template_failed,
        "language_changed_restart" to R.string.language_changed_restart
    )

    fun exportTemplate(context: Context): File? {
        val appContext = context.applicationContext
        val dir = translationsDir(context)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "my.lang")

        return try {
            val sb = StringBuilder()
            sb.append("__name=My Language\n\n")

            ALL_STRINGS.forEach { (name, resId) ->
                try {
                    val defaultValue = appContext.getString(resId).replace("\n", "\\n")
                    sb.append(name).append('=').append(defaultValue).append('\n')
                } catch (e: Exception) {
                }
            }

            file.writeText(sb.toString(), Charsets.UTF_8)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun displayNameForFile(file: File): String? {
        return try {
            val props = Properties()
            file.inputStream().use { props.load(InputStreamReader(it, Charsets.UTF_8)) }
            props.getProperty(DISPLAY_NAME_KEY)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadProperties(stream: InputStream): Map<String, String> {
        return stream.use {
            val props = Properties()
            props.load(InputStreamReader(it, Charsets.UTF_8))
            props.entries
                .associate { entry -> (entry.key as String) to (entry.value as String) }
                .filterKeys { key -> key != DISPLAY_NAME_KEY }
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("local_notes_prefs", Context.MODE_PRIVATE)

    private const val KEY_ACTIVE = "active_translation"
    private const val DISPLAY_NAME_KEY = "__name"
    const val EXTENSION = ".lang"
}
