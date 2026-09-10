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

    fun exportTemplate(context: Context): File? {
        val appContext = context.applicationContext
        val dir = translationsDir(context)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "test.txt")

        return try {
            val sb = StringBuilder()
            sb.append("Local Notes translation reference\n")
            sb.append("Copy the lines below into a new file ending in .lang inside this same folder,\n")
            sb.append("edit the text after each =, then pick it from Settings -> Language.\n")
            sb.append("__name=My Language\n\n")

            val fields = R.string::class.java.fields.sortedBy { it.name }
            fields.forEach { field ->
                try {
                    val resId = field.getInt(null)
                    val defaultValue = appContext.getString(resId).replace("\n", "\\n")
                    sb.append(field.name).append('=').append(defaultValue).append('\n')
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
