package com.lukasosstudios.localnotes.model

import com.lukasosstudios.localnotes.R

/**
 * The five customizable note colors. Each maps to a background / border / accent
 * triad defined in colors.xml (with dark-mode equivalents in values-night).
 */
enum class NoteColor(
    val id: String,
    val backgroundRes: Int,
    val borderRes: Int,
    val accentRes: Int,
    val labelRes: Int
) {
    CREAM("cream", R.color.note_cream_bg, R.color.note_cream_border, R.color.note_cream_accent, R.string.color_cream),
    BLUE("blue", R.color.note_blue_bg, R.color.note_blue_border, R.color.note_blue_accent, R.string.color_blue),
    SAGE("sage", R.color.note_sage_bg, R.color.note_sage_border, R.color.note_sage_accent, R.string.color_sage),
    ROSE("rose", R.color.note_rose_bg, R.color.note_rose_border, R.color.note_rose_accent, R.string.color_rose),
    LAVENDER("lavender", R.color.note_lavender_bg, R.color.note_lavender_border, R.color.note_lavender_accent, R.string.color_lavender);

    companion object {
        fun fromId(id: String?): NoteColor = values().find { it.id == id } ?: CREAM
    }
}
