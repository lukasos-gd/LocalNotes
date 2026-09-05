package com.lukasosstudios.localnotes.model

/**
 * A single note. Each note is persisted as its own plain-text file inside
 * Android/media/notes/, named [fileName]. [fileName] doubles as the note's
 * stable identity across app restarts.
 */
data class Note(
    val fileName: String,
    val title: String,
    val body: String,
    val color: NoteColor,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank()
}

enum class NoteFilter { ALL, PINNED, ARCHIVED, TRASH }

enum class SortMode(val id: String) {
    UPDATED("updated"), CREATED("created"), TITLE("title");

    companion object {
        fun fromId(id: String?): SortMode = values().find { it.id == id } ?: UPDATED
    }
}

enum class ThemeMode(val id: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromId(id: String?): ThemeMode = values().find { it.id == id } ?: SYSTEM
    }
}
