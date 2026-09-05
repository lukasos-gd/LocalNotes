package com.lukasosstudios.localnotes.ui.editor

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.data.NoteRepository
import com.lukasosstudios.localnotes.databinding.ActivityNoteEditorBinding
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteColor

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repository: NoteRepository

    private var existing: Note? = null
    private var selectedColor: NoteColor = NoteColor.CREAM
    private val swatchViews = mutableMapOf<NoteColor, android.widget.FrameLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(this)

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        existing = fileName?.let { name -> repository.listNotes().find { it.fileName == name } }

        val isNew = existing == null
        binding.topTitle.text = getString(if (isNew) R.string.editor_title_new else R.string.editor_title_edit)
        binding.titleInput.setText(existing?.title ?: "")
        binding.bodyInput.setText(existing?.body ?: "")
        selectedColor = existing?.color ?: NoteColor.CREAM

        buildSwatches()
        applyColor()

        binding.backButton.setOnClickListener { save() }
        binding.saveButton.setOnClickListener { save() }
    }

    private fun buildSwatches() {
        binding.swatchRow.removeAllViews()
        swatchViews.clear()
        val sizePx = (49 * resources.displayMetrics.density).toInt()
        val marginPx = (12 * resources.displayMetrics.density).toInt()

        NoteColor.values().forEach { color ->
            val frame = android.widget.FrameLayout(this)
            val params = android.widget.LinearLayout.LayoutParams(sizePx, sizePx)
            params.marginEnd = marginPx
            frame.layoutParams = params

            val bg = ContextCompat.getDrawable(this, R.drawable.bg_swatch)?.mutate() as GradientDrawable
            bg.setColor(ContextCompat.getColor(this, color.backgroundRes))
            frame.background = bg
            frame.contentDescription = getString(color.labelRes)

            val checkIcon = android.widget.ImageView(this)
            val iconSize = (18 * resources.displayMetrics.density).toInt()
            val iconParams = android.widget.FrameLayout.LayoutParams(iconSize, iconSize)
            iconParams.gravity = android.view.Gravity.CENTER
            checkIcon.layoutParams = iconParams
            checkIcon.setImageResource(R.drawable.ic_check)
            checkIcon.tag = "check"
            frame.addView(checkIcon)

            frame.setOnClickListener {
                selectedColor = color
                applyColor()
            }

            binding.swatchRow.addView(frame)
            swatchViews[color] = frame
        }
    }

    private fun applyColor() {
        val cardBg = ContextCompat.getDrawable(this, R.drawable.bg_paper_card)?.mutate() as GradientDrawable
        cardBg.setColor(ContextCompat.getColor(this, selectedColor.backgroundRes))
        cardBg.setStroke((1 * resources.displayMetrics.density).toInt(), ContextCompat.getColor(this, selectedColor.borderRes))
        binding.paperCard.background = cardBg

        val accentColor = ContextCompat.getColor(this, selectedColor.accentRes)
        binding.paperAccent.setBackgroundColor(accentColor)
        binding.titleInput.setHighlightColor(accentColor)
        binding.bodyInput.setHighlightColor(accentColor)

        swatchViews.forEach { (color, frame) ->
            val isSelected = color == selectedColor
            val bg = frame.background as? GradientDrawable
            val borderColor = if (isSelected) {
                ContextCompat.getColor(this, color.accentRes)
            } else {
                ContextCompat.getColor(this, color.borderRes)
            }
            bg?.setStroke((2 * resources.displayMetrics.density).toInt(), borderColor)
            val checkIcon = frame.findViewWithTag<android.widget.ImageView>("check")
            checkIcon?.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.INVISIBLE
            checkIcon?.setColorFilter(ContextCompat.getColor(this, color.accentRes))
        }
    }

    private fun save() {
        val title = binding.titleInput.text.toString().trim()
        val body = binding.bodyInput.text.toString()

        if (title.isBlank() && body.isBlank()) {
            finish()
            return
        }

        val now = System.currentTimeMillis()
        val note = existing?.copy(
            title = title,
            body = body,
            color = selectedColor,
            updatedAt = now
        ) ?: Note(
            fileName = repository.newFileName(),
            title = title,
            body = body,
            color = selectedColor,
            isPinned = false,
            isArchived = false,
            isDeleted = false,
            createdAt = now,
            updatedAt = now
        )
        repository.saveNote(note)
        finish()
    }

    override fun onBackPressed() {
        save()
    }

    companion object {
        const val EXTRA_FILE_NAME = "extra_file_name"
    }
}
