package com.lukasosstudios.localnotes.model

import android.content.Context
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.util.ColorUtils

/**
 * Ten curated note colors, plus a CUSTOM slot backed by a user-picked hex
 * value (stored per-note as customColorHex). Presets carry proper light/dark
 * resource pairs; custom colors are a single fixed color the user chose, with
 * border/accent tones derived from it at runtime.
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
    LAVENDER("lavender", R.color.note_lavender_bg, R.color.note_lavender_border, R.color.note_lavender_accent, R.string.color_lavender),
    PEACH("peach", R.color.note_peach_bg, R.color.note_peach_border, R.color.note_peach_accent, R.string.color_peach),
    MINT("mint", R.color.note_mint_bg, R.color.note_mint_border, R.color.note_mint_accent, R.string.color_mint),
    SLATE("slate", R.color.note_slate_bg, R.color.note_slate_border, R.color.note_slate_accent, R.string.color_slate),
    SUN("sun", R.color.note_sun_bg, R.color.note_sun_border, R.color.note_sun_accent, R.string.color_sun),
    CORAL("coral", R.color.note_coral_bg, R.color.note_coral_border, R.color.note_coral_accent, R.string.color_coral),

    /** backgroundRes/borderRes/accentRes are unused for CUSTOM -- resolve via NoteColorResolver. */
    CUSTOM("custom", 0, 0, 0, R.string.color_custom);

    val isCustom: Boolean get() = this == CUSTOM

    companion object {
        val PRESETS: List<NoteColor> = values().filter { it != CUSTOM }

        fun fromId(id: String?): NoteColor = values().find { it.id == id } ?: CREAM
    }
}

/** Resolves the actual background/border/accent ARGB ints for a note, handling CUSTOM. */
object NoteColorResolver {
    fun background(context: Context, color: NoteColor, customHex: String?): Int =
        if (color.isCustom) {
            ColorUtils.parseHexOrFallback(customHex, ContextCompat.getColor(context, R.color.note_cream_bg))
        } else {
            ContextCompat.getColor(context, color.backgroundRes)
        }

    fun border(context: Context, color: NoteColor, customHex: String?): Int =
        if (color.isCustom) {
            ColorUtils.darken(background(context, color, customHex), 0.16f)
        } else {
            ContextCompat.getColor(context, color.borderRes)
        }

    fun accent(context: Context, color: NoteColor, customHex: String?): Int =
        if (color.isCustom) {
            ColorUtils.darken(background(context, color, customHex), 0.42f)
        } else {
            ContextCompat.getColor(context, color.accentRes)
        }
}
