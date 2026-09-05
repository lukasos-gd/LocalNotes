package com.lukasosstudios.localnotes.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zips/unzips the whole Android/media/<package>/ folder (notes + settings)
 * so the person can move everything to a new phone or keep an off-device
 * copy, without needing any account or cloud service.
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
            }
            zipFile
        } catch (e: Exception) {
            zipFile.delete()
            null
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
