package com.lukasosstudios.localnotes.util

import android.graphics.Color

object ColorUtils {

    /** Parses "#RRGGBB" / "#AARRGGBB", returns [fallback] if invalid or null. */
    fun parseHexOrFallback(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }

    /** Returns true if [hex] is a well-formed #RGB/#RRGGBB color string. */
    fun isValidHex(hex: String): Boolean = try {
        Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        true
    } catch (e: IllegalArgumentException) {
        false
    }

    /** Darkens [color] by [amount] (0f..1f) in HSV space, keeping hue/saturation. */
    fun darken(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    fun toHex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)
}
