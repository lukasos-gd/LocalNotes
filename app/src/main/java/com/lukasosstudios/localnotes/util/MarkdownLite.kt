package com.lukasosstudios.localnotes.util

import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan

/**
 * A deliberately small markdown renderer: **bold**, *italic*, and
 * "- [ ] " / "- [x] " checklist lines. The underlying text on disk is always
 * plain markdown -- this only ever adds/removes *display* spans, or (for
 * checklist toggling) flips three literal characters in the Editable.
 */
object MarkdownLite {

    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALIC_REGEX = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    private val CHECKLIST_REGEX = Regex("^(\\s*)-\\s\\[( |x|X)\\]\\s?(.*)$")

    /**
     * Applies live styling spans onto an editable body as the user types.
     * Markers ("**", "*", "- [ ]") stay visible and editable; only the
     * visual style (bold/italic/strike) changes.
     */
    fun styleEditable(editable: Editable) {
        // Clear only the spans we own so re-styling is idempotent.
        editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StrikethroughSpan::class.java).forEach { editable.removeSpan(it) }

        BOLD_REGEX.findAll(editable).forEach { match ->
            editable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        ITALIC_REGEX.findAll(editable).forEach { match ->
            editable.setSpan(StyleSpan(Typeface.ITALIC), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        var lineStart = 0
        val text = editable.toString()
        for (line in text.split("\n")) {
            val lineEnd = lineStart + line.length
            val match = CHECKLIST_REGEX.find(line)
            if (match != null && match.groupValues[2].lowercase() == "x") {
                editable.setSpan(StrikethroughSpan(), lineStart, lineEnd, CHECKLIST_SPAN_FLAG)
            }
            lineStart = lineEnd + 1
        }
    }

    /**
     * Builds a read-only rendered preview (for the notes list card): bold and
     * italic styled, checklist markers swapped for ☐ / ☑ glyphs.
     */
    fun renderPreview(raw: String): CharSequence {
        val builder = SpannableStringBuilder()
        raw.split("\n").forEachIndexed { index, line ->
            if (index > 0) builder.append("\n")
            val match = CHECKLIST_REGEX.find(line)
            if (match != null) {
                val checked = match.groupValues[2].lowercase() == "x"
                val rest = match.groupValues[3]
                val glyph = if (checked) "\u2611 " else "\u2610 "
                val start = builder.length
                builder.append(glyph).append(rest)
                if (checked) {
                    builder.setSpan(StrikethroughSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                builder.append(line)
            }
        }

        BOLD_REGEX.findAll(builder).toList().forEach { match ->
            builder.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        ITALIC_REGEX.findAll(builder).toList().forEach { match ->
            builder.setSpan(StyleSpan(Typeface.ITALIC), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    /**
     * If [offset] falls within a checklist line's "[ ]"/"[x]" marker (or the
     * few characters around it), toggles it in place and returns true.
     */
    fun toggleChecklistAt(editable: Editable, offset: Int): Boolean {
        val text = editable.toString()
        val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEndRaw = text.indexOf('\n', lineStart)
        val lineEnd = if (lineEndRaw == -1) text.length else lineEndRaw
        val line = text.substring(lineStart, lineEnd)
        val match = CHECKLIST_REGEX.find(line) ?: return false

        // Only toggle when the tap landed near the checkbox itself, not the note text.
        val markerLocalStart = line.indexOf('[')
        val markerLocalEnd = line.indexOf(']')
        if (markerLocalStart == -1 || markerLocalEnd == -1) return false
        val tapLocal = offset - lineStart
        if (tapLocal < markerLocalStart || tapLocal > markerLocalEnd + 1) return false

        val checked = match.groupValues[2].lowercase() == "x"
        val markerAbsolute = lineStart + markerLocalStart + 1
        editable.replace(markerAbsolute, markerAbsolute + 1, if (checked) " " else "x")
        return true
    }

    private const val CHECKLIST_SPAN_FLAG = 0x10000
}
