package com.lukasosstudios.localnotes.ui.editor

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.data.NoteRepository
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.databinding.ActivityNoteEditorBinding
import com.lukasosstudios.localnotes.databinding.DialogCustomColorBinding
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteColor
import com.lukasosstudios.localnotes.model.NoteColorResolver
import com.lukasosstudios.localnotes.util.AppLock
import com.lukasosstudios.localnotes.util.ColorUtils
import com.lukasosstudios.localnotes.util.MarkdownLite

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repository: NoteRepository
    private lateinit var settingsRepository: SettingsRepository

    private var existing: Note? = null
    private var selectedColor: NoteColor = NoteColor.CREAM
    private var customColorHex: String? = null
    private val swatchViews = mutableMapOf<NoteColor, android.widget.FrameLayout>()
    private var isApplyingMarkdownStyles = false

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(this)
        settingsRepository = SettingsRepository(this)

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        existing = fileName?.let { name -> repository.listNotes().find { it.fileName == name } }

        val isNew = existing == null
        binding.topTitle.text = getString(if (isNew) R.string.editor_title_new else R.string.editor_title_edit)
        binding.titleInput.setText(existing?.title ?: "")
        binding.bodyInput.setText(existing?.body ?: "")
        selectedColor = existing?.color ?: NoteColor.CREAM
        customColorHex = existing?.customColorHex

        buildSwatches()
        applyColor()
        MarkdownLite.styleEditable(binding.bodyInput.text)

        binding.bodyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isApplyingMarkdownStyles || s == null) return
                isApplyingMarkdownStyles = true
                MarkdownLite.styleEditable(s)
                isApplyingMarkdownStyles = false
            }
        })

        binding.bodyInput.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = view as android.widget.EditText
                val offset = editText.getOffsetForPosition(event.x, event.y)
                MarkdownLite.toggleChecklistAt(editText.text, offset)
            }
            false
        }

        binding.backButton.setOnClickListener { save() }
        binding.saveButton.setOnClickListener { save() }

        if (isNew) {
            binding.titleInput.requestFocus()
            binding.titleInput.post {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.titleInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLock.guard(this, settingsRepository, lockLauncher)
    }

    private fun buildSwatches() {
        binding.swatchRow.removeAllViews()
        swatchViews.clear()
        val sizePx = (49 * resources.displayMetrics.density).toInt()
        val marginPx = (12 * resources.displayMetrics.density).toInt()

        NoteColor.PRESETS.forEach { color ->
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

        // Trailing "custom" swatch: shows the current custom color if one was
        // picked, otherwise a neutral "+" prompt to open the picker.
        val customFrame = android.widget.FrameLayout(this)
        val customParams = android.widget.LinearLayout.LayoutParams(sizePx, sizePx)
        customFrame.layoutParams = customParams
        customFrame.contentDescription = getString(R.string.color_custom)

        val customBg = ContextCompat.getDrawable(this, R.drawable.bg_swatch)?.mutate() as GradientDrawable
        customBg.setColor(
            if (!customColorHex.isNullOrBlank()) ColorUtils.parseHexOrFallback(customColorHex, ContextCompat.getColor(this, R.color.surfaceStrong))
            else ContextCompat.getColor(this, R.color.surfaceStrong)
        )
        customFrame.background = customBg

        val plusIcon = android.widget.ImageView(this)
        val plusSize = (18 * resources.displayMetrics.density).toInt()
        val plusParams = android.widget.FrameLayout.LayoutParams(plusSize, plusSize)
        plusParams.gravity = android.view.Gravity.CENTER
        plusIcon.layoutParams = plusParams
        plusIcon.setImageResource(R.drawable.ic_add)
        plusIcon.tag = "check"
        plusIcon.setColorFilter(ContextCompat.getColor(this, R.color.mutedForeground))
        customFrame.addView(plusIcon)

        customFrame.setOnClickListener { showCustomColorDialog() }

        binding.swatchRow.addView(customFrame)
        swatchViews[NoteColor.CUSTOM] = customFrame
    }

    private fun showCustomColorDialog() {
        val dialogBinding = DialogCustomColorBinding.inflate(layoutInflater)
        val startColor = ColorUtils.parseHexOrFallback(customColorHex, ContextCompat.getColor(this, R.color.note_blue_bg))

        fun currentPickerColor(): Int = Color.rgb(
            dialogBinding.redSeek.progress, dialogBinding.greenSeek.progress, dialogBinding.blueSeek.progress
        )

        fun refreshPreview() {
            val bg = dialogBinding.colorPreview.background.mutate() as? GradientDrawable
            bg?.setColor(currentPickerColor())
        }

        dialogBinding.redSeek.progress = Color.red(startColor)
        dialogBinding.greenSeek.progress = Color.green(startColor)
        dialogBinding.blueSeek.progress = Color.blue(startColor)
        dialogBinding.hexInput.setText(ColorUtils.toHex(startColor))
        refreshPreview()

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                dialogBinding.hexInput.setText(ColorUtils.toHex(currentPickerColor()))
                refreshPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        dialogBinding.redSeek.setOnSeekBarChangeListener(seekListener)
        dialogBinding.greenSeek.setOnSeekBarChangeListener(seekListener)
        dialogBinding.blueSeek.setOnSeekBarChangeListener(seekListener)

        dialogBinding.hexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                if (ColorUtils.isValidHex(text)) {
                    val parsed = ColorUtils.parseHexOrFallback(text, startColor)
                    dialogBinding.redSeek.progress = Color.red(parsed)
                    dialogBinding.greenSeek.progress = Color.green(parsed)
                    dialogBinding.blueSeek.progress = Color.blue(parsed)
                    refreshPreview()
                }
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.useColorButton.setOnClickListener {
            customColorHex = ColorUtils.toHex(currentPickerColor())
            selectedColor = NoteColor.CUSTOM
            buildSwatches()
            applyColor()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyColor() {
        val bgColor = NoteColorResolver.background(this, selectedColor, customColorHex)
        val borderColor = NoteColorResolver.border(this, selectedColor, customColorHex)
        val accentColor = NoteColorResolver.accent(this, selectedColor, customColorHex)

        val cardBg = ContextCompat.getDrawable(this, R.drawable.bg_paper_card)?.mutate() as GradientDrawable
        cardBg.setColor(bgColor)
        cardBg.setStroke((1 * resources.displayMetrics.density).toInt(), borderColor)
        binding.paperCard.background = cardBg

        binding.paperAccent.setBackgroundColor(accentColor)
        binding.titleInput.setHighlightColor(accentColor)
        binding.bodyInput.setHighlightColor(accentColor)

        swatchViews.forEach { (color, frame) ->
            val isSelected = color == selectedColor
            val bg = frame.background as? GradientDrawable
            val frameBorderColor = if (isSelected) {
                accentColor
            } else if (color.isCustom) {
                ContextCompat.getColor(this, R.color.border)
            } else {
                ContextCompat.getColor(this, color.borderRes)
            }
            bg?.setStroke((2 * resources.displayMetrics.density).toInt(), frameBorderColor)

            val markIcon = frame.findViewWithTag<android.widget.ImageView>("check")
            if (color.isCustom) {
                // The "+" icon always shows; just recolor it against the selected state.
                markIcon?.setImageResource(if (isSelected && !customColorHex.isNullOrBlank()) R.drawable.ic_check else R.drawable.ic_add)
                markIcon?.visibility = android.view.View.VISIBLE
                markIcon?.setColorFilter(
                    if (isSelected) accentColor else ContextCompat.getColor(this, R.color.mutedForeground)
                )
            } else {
                markIcon?.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.INVISIBLE
                markIcon?.setColorFilter(accentColor)
            }
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
            customColorHex = customColorHex,
            updatedAt = now
        ) ?: Note(
            fileName = repository.newFileName(),
            title = title,
            body = body,
            color = selectedColor,
            customColorHex = customColorHex,
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
