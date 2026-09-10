package com.lukasosstudios.localnotes.util

import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan

/**
 * A small markdown renderer covering the syntax that's actually useful in a
 * notes app: headings, bold, italic, strikethrough, inline code, checklists,
 * bullet lists, numbered lists, and blockquotes. The underlying text on disk
 * is always plain markdown -- this only ever adds/removes *display* spans
 * (live editing), or builds a fully separate rendered copy (list preview), or
 * flips a couple of literal characters in the Editable (checklist toggling).
 */
object MarkdownLite {

    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALIC_REGEX = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    private val STRIKETHROUGH_REGEX = Regex("~~(.+?)~~")
    private val INLINE_CODE_REGEX = Regex("`([^`\\n]+)`")
    private val CHECKLIST_REGEX = Regex("^(\\s*)-\\s\\[( |x|X)\\]\\s?(.*)$")
    private val HEADING_REGEX = Regex("^(#{1,3})\\s+(.+)$")
    private val BULLET_REGEX = Regex("^(\\s*)[-*]\\s+(?!\\[[ xX]\\])(.+)$")
    private val NUMBERED_REGEX = Regex("^(\\s*)(\\d+)\\.\\s+(.+)$")
    private val BLOCKQUOTE_REGEX = Regex("^>\\s?(.*)$")

    private const val CODE_BG = 0x22808080 // subtle, theme-agnostic
    private const val QUOTE_COLOR = 0x99808080.toInt()

    /**
     * Applies live styling spans onto an editable body as the user types.
     * Markers ("**", "#", "> ", "- [ ]", etc.) stay visible and editable;
     * only the visual style changes.
     */
    fun styleEditable(editable: Editable) {
        clearOwnedSpans(editable)

        BOLD_REGEX.findAll(editable).forEach { spanRange(editable, it.range, StyleSpan(Typeface.BOLD)) }
        ITALIC_REGEX.findAll(editable).forEach { spanRange(editable, it.range, StyleSpan(Typeface.ITALIC)) }
        STRIKETHROUGH_REGEX.findAll(editable).forEach { spanRange(editable, it.range, StrikethroughSpan()) }
        INLINE_CODE_REGEX.findAll(editable).forEach { match ->
            spanRange(editable, match.range, TypefaceSpan("monospace"))
            spanRange(editable, match.range, BackgroundColorSpan(CODE_BG))
        }

        forEachLine(editable.toString()) { line, lineStart, lineEnd ->
            val heading = HEADING_REGEX.find(line)
            val checklist = CHECKLIST_REGEX.find(line)
            val quote = BLOCKQUOTE_REGEX.find(line)

            when {
                heading != null -> {
                    val size = when (heading.groupValues[1].length) {
                        1 -> 1.35f
                        2 -> 1.2f
                        else -> 1.08f
                    }
                    editable.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, OWNED_FLAG)
                    editable.setSpan(RelativeSizeSpan(size), lineStart, lineEnd, OWNED_FLAG)
                }
                checklist != null && checklist.groupValues[2].lowercase() == "x" -> {
                    editable.setSpan(StrikethroughSpan(), lineStart, lineEnd, OWNED_FLAG)
                }
                quote != null -> {
                    editable.setSpan(StyleSpan(Typeface.ITALIC), lineStart, lineEnd, OWNED_FLAG)
                    editable.setSpan(ForegroundColorSpan(QUOTE_COLOR), lineStart, lineEnd, OWNED_FLAG)
                }
            }
        }
    }

    /**
     * Builds a fully rendered, read-only preview (for the notes list card):
     * markers are replaced with their visual equivalent (headings sized,
     * bullets become "•", checklists become ☐/☑, blockquotes indented).
     */
    fun renderPreview(raw: String): CharSequence {
        val builder = SpannableStringBuilder()

        raw.split("\n").forEachIndexed { index, line ->
            if (index > 0) builder.append("\n")
            val start = builder.length

            val heading = HEADING_REGEX.find(line)
            val checklist = CHECKLIST_REGEX.find(line)
            val bullet = BULLET_REGEX.find(line)
            val numbered = NUMBERED_REGEX.find(line)
            val quote = BLOCKQUOTE_REGEX.find(line)

            when {
                heading != null -> {
                    builder.append(heading.groupValues[2])
                    val size = when (heading.groupValues[1].length) {
                        1 -> 1.25f
                        2 -> 1.12f
                        else -> 1.05f
                    }
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(size), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                checklist != null -> {
                    val checked = checklist.groupValues[2].lowercase() == "x"
                    val glyph = if (checked) "\u2611 " else "\u2610 "
                    builder.append(glyph).append(checklist.groupValues[3])
                    if (checked) {
                        builder.setSpan(StrikethroughSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                bullet != null -> {
                    builder.append("\u2022 ").append(bullet.groupValues[2])
                }
                numbered != null -> {
                    val marker = "${numbered.groupValues[2]}. "
                    builder.append(marker).append(numbered.groupValues[3])
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, start + marker.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                quote != null -> {
                    builder.append(quote.groupValues[1])
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(QUOTE_COLOR), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> builder.append(line)
            }
        }

        BOLD_REGEX.findAll(builder).toList().forEach { spanRange(builder, it.range, StyleSpan(Typeface.BOLD)) }
        ITALIC_REGEX.findAll(builder).toList().forEach { spanRange(builder, it.range, StyleSpan(Typeface.ITALIC)) }
        STRIKETHROUGH_REGEX.findAll(builder).toList().forEach { spanRange(builder, it.range, StrikethroughSpan()) }
        INLINE_CODE_REGEX.findAll(builder).toList().forEach { match ->
            spanRange(builder, match.range, TypefaceSpan("monospace"))
            spanRange(builder, match.range, BackgroundColorSpan(CODE_BG))
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

    // ---- internals --------------------------------------------------------

    private fun spanRange(text: Editable, range: IntRange, span: Any) {
        text.setSpan(span, range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun forEachLine(text: String, action: (line: String, start: Int, end: Int) -> Unit) {
        var lineStart = 0
        for (line in text.split("\n")) {
            val lineEnd = lineStart + line.length
            action(line, lineStart, lineEnd)
            lineStart = lineEnd + 1
        }
    }

    private fun clearOwnedSpans(editable: Editable) {
        editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StrikethroughSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, BackgroundColorSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, TypefaceSpan::class.java).forEach { editable.removeSpan(it) }
    }

    private const val OWNED_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
}
