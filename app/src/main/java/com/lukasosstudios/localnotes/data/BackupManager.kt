package com.lukasosstudios.localnotes.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zips/unzips the whole Android/media/<package>/ folder (notes + settings)
 * so the person can move everything to a new phone or keep an off-device
 * copy, without needing any account or cloud service. Every export also
 * includes a manifest.json describing exactly what's inside -- readable in
 * any text editor, same philosophy as the notes themselves.
 */
class BackupManager(private val context: Context, private val repository: NoteRepository) {

    fun backupsDir(): File = File(repository.storageRoot(), "backups")

    /** Creates a fresh backup zip and returns it, or null on failure. */
    fun export(): File? {
        if (!repository.hasPermission()) return null
        repository.ensureDirs()
        val dir = backupsDir()
        if (!dir.exists()) dir.mkdirs()

        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(System.currentTimeMillis())
        val zipFile = File(dir, "LocalNotes-backup-$stamp.zip")

        return try {
            val notes = repository.listNotes()
            ZipOutputStream(zipFile.outputStream()).use { zip ->
                val notesDir = repository.notesDir()
                notesDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }?.forEach { noteFile ->
                    zip.putNextEntry(ZipEntry("notes/${noteFile.name}"))
                    noteFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                val settingsFile = repository.settingsFile()
                if (settingsFile.exists()) {
                    zip.putNextEntry(ZipEntry("settings.properties"))
                    settingsFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(buildManifest(notes).toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            zipFile
        } catch (e: Exception) {
            zipFile.delete()
            null
        }
    }

    private fun buildManifest(notes: List<com.lukasosstudios.localnotes.model.Note>): JSONObject {
        val settings = SettingsRepository(context)
        settings.loadFromFile(repository)

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var versionName = "unknown"
        var versionCode = 0L
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = info.versionName ?: "unknown"
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (_: Exception) {
        }

        val notesArray = JSONArray()
        notes.forEach { note ->
            notesArray.put(
                JSONObject().apply {
                    put("file", note.fileName)
                    put("title", note.title)
                    put("color", note.color.id)
                    put("custom_color", note.customColorHex ?: JSONObject.NULL)
                    put("pinned", note.isPinned)
                    put("archived", note.isArchived)
                    put("deleted", note.isDeleted)
                    put("created_at", note.createdAt)
                    put("updated_at", note.updatedAt)
                }
            )
        }

        return JSONObject().apply {
            put("app", "Local Notes")
            put("package", context.packageName)
            put("app_version_name", versionName)
            put("app_version_code", versionCode)
            put("manifest_version", 1)
            put("exported_at_millis", System.currentTimeMillis())
            put("exported_at_iso", isoFormat.format(System.currentTimeMillis()))
            put(
                "counts",
                JSONObject().apply {
                    put("total", notes.size)
                    put("active", notes.count { !it.isDeleted && !it.isArchived })
                    put("pinned", notes.count { it.isPinned && !it.isDeleted && !it.isArchived })
                    put("archived", notes.count { it.isArchived && !it.isDeleted })
                    put("trashed", notes.count { it.isDeleted })
                }
            )
            put(
                "settings",
                JSONObject().apply {
                    put("theme", settings.theme.id)
                    put("sort", settings.sort.id)
                    put("app_lock_enabled", settings.appLockEnabled)
                }
            )
            put("notes", notesArray)
        }
    }

    /** Extracts a backup zip (picked via SAF) into the live notes/settings folder. */
    fun import(uri: Uri): Boolean {
        if (!repository.hasPermission()) return false
        repository.ensureDirs()

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    var importedAny = false
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && (name.endsWith(".txt") || name.endsWith("settings.properties"))) {
                            val safeName = File(name).name // strip any path traversal
                            val destDir = if (name.startsWith("notes/")) repository.notesDir() else repository.storageRoot()
                            if (!destDir.exists()) destDir.mkdirs()
                            val destFile = File(destDir, safeName)
                            destFile.outputStream().use { out -> zip.copyTo(out) }
                            importedAny = true
                        }
                        // manifest.json is informational only -- intentionally not re-imported,
                        // it's regenerated fresh on every export.
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    importedAny
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
