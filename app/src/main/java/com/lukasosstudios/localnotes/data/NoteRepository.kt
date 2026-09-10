package com.lukasosstudios.localnotes.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteColor
import java.io.File

/**
 * Reads and writes notes as individual plain-text files under
 * Android/media/notes/ on shared external storage. Each file is the single
 * source of truth for that note -- there is no hidden database.
 */
class NoteRepository(private val context: Context) {

    fun storageRoot(): File =
        File(Environment.getExternalStorageDirectory(), "Android/media/${context.packageName}")

    fun notesDir(): File = File(storageRoot(), "notes")

    fun settingsFile(): File = File(storageRoot(), "settings.properties")

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun ensureDirs() {
        if (!notesDir().exists()) notesDir().mkdirs()
    }

    fun listNotes(): List<Note> {
        if (!hasPermission()) return emptyList()
        ensureDirs()
        val files = notesDir().listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return emptyList()
        return files.mapNotNull { parseNoteFile(it) }
    }

    private fun parseNoteFile(file: File): Note? {
        return try {
            val text = file.readText(Charsets.UTF_8)
            val lines = text.split("\n")
            val headerEnd = lines.indexOfFirst { it.trim() == HEADER_MARKER }
            if (headerEnd == -1) return null

            val props = HashMap<String, String>()
            for (i in 0 until headerEnd) {
                val line = lines[i]
                val idx = line.indexOf('=')
                if (idx > 0) props[line.substring(0, idx).trim()] = line.substring(idx + 1)
            }

            val contentLines = lines.subList(headerEnd + 1, lines.size)
            val title = contentLines.firstOrNull().orEmpty()
            val body = if (contentLines.size > 1) contentLines.drop(1).joinToString("\n") else ""

            Note(
                fileName = file.name,
                title = title,
                body = body,
                color = NoteColor.fromId(props["color"]),
                customColorHex = props["customColor"],
                isPinned = props["pinned"] == "true",
                isArchived = props["archived"] == "true",
                isDeleted = props["deleted"] == "true",
                createdAt = props["created"]?.toLongOrNull() ?: file.lastModified(),
                updatedAt = props["updated"]?.toLongOrNull() ?: file.lastModified()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveNote(note: Note) {
        if (!hasPermission()) return
        ensureDirs()
        val file = File(notesDir(), note.fileName)
        val sb = StringBuilder()
        sb.append("color=").append(note.color.id).append('\n')
        if (note.color.isCustom && !note.customColorHex.isNullOrBlank()) {
            sb.append("customColor=").append(note.customColorHex).append('\n')
        }
        sb.append("pinned=").append(note.isPinned).append('\n')
        sb.append("archived=").append(note.isArchived).append('\n')
        sb.append("deleted=").append(note.isDeleted).append('\n')
        sb.append("created=").append(note.createdAt).append('\n')
        sb.append("updated=").append(note.updatedAt).append('\n')
        sb.append(HEADER_MARKER).append('\n')
        sb.append(note.title).append('\n')
        sb.append(note.body)
        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    fun deleteForever(note: Note) {
        File(notesDir(), note.fileName).delete()
    }

    fun newFileName(): String {
        val suffix = (1000..9999).random()
        return "${System.currentTimeMillis()}-$suffix.txt"
    }

    companion object {
        private const val HEADER_MARKER = "===="
    }
}
