package com.lukasosstudios.localnotes.ui.notes

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ItemNoteBinding
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteColorResolver
import com.lukasosstudios.localnotes.model.NoteFilter
import com.lukasosstudios.localnotes.util.DateUtils
import com.lukasosstudios.localnotes.util.MarkdownLite

class NotesAdapter(
    private val onOpen: (Note) -> Unit,
    private val onTogglePin: (Note) -> Unit,
    private val onToggleArchive: (Note) -> Unit,
    private val onTrash: (Note) -> Unit,
    private val onRestore: (Note) -> Unit,
    private val onDeleteForever: (Note) -> Unit,
    private val onLongPress: (Note) -> Unit,
    private val isSelected: (Note) -> Boolean,
    private val isSelectionMode: () -> Boolean
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val items = mutableListOf<Note>()
    var currentFilter: NoteFilter = NoteFilter.ALL
        private set

    fun submit(notes: List<Note>, filter: NoteFilter) {
        currentFilter = filter
        val diff = DiffUtil.calculateDiff(NoteDiffCallback(items, notes))
        items.clear()
        items.addAll(notes)
        diff.dispatchUpdatesTo(this)
    }

    /** Re-binds all visible rows without recomputing the note list (selection state changed). */
    fun refreshSelectionState() {
        notifyItemRangeChanged(0, items.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(items[position], currentFilter)
    }

    override fun getItemCount(): Int = items.size

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note, filter: NoteFilter) {
            val context = binding.root.context

            val bgColor = NoteColorResolver.background(context, note.color, note.customColorHex)
            val borderColor = NoteColorResolver.border(context, note.color, note.customColorHex)
            val accentColor = NoteColorResolver.accent(context, note.color, note.customColorHex)

            val bg = ContextCompat.getDrawable(context, R.drawable.bg_note_card_fill)?.mutate() as? GradientDrawable
            bg?.setColor(bgColor)
            bg?.setStroke(1.dp(context), borderColor)
            binding.noteCardRoot.background = bg

            binding.noteAccent.setBackgroundColor(accentColor)

            binding.noteTitle.text = note.title.ifBlank { context.getString(R.string.untitled_note) }
            binding.noteBody.text = if (note.body.isBlank()) {
                context.getString(R.string.no_text_yet)
            } else {
                MarkdownLite.renderPreview(note.body)
            }
            binding.noteDate.text = DateUtils.formatRelative(note.updatedAt)

            binding.notePinIcon.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.notePinIcon.setColorFilter(accentColor)

            val selectionMode = isSelectionMode()
            val selected = isSelected(note)

            val isTrash = filter == NoteFilter.TRASH
            val actionsVisible = !selectionMode
            binding.pinAction.visibility = if (!actionsVisible || isTrash) View.GONE else View.VISIBLE
            binding.archiveAction.visibility = if (!actionsVisible || isTrash) View.GONE else View.VISIBLE
            binding.trashAction.visibility = if (!actionsVisible || isTrash) View.GONE else View.VISIBLE
            binding.restoreAction.visibility = if (!actionsVisible || !isTrash) View.GONE else View.VISIBLE
            binding.deleteForeverAction.visibility = if (!actionsVisible || !isTrash) View.GONE else View.VISIBLE

            binding.pinAction.setImageResource(if (note.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline)
            binding.pinAction.setColorFilter(
                if (note.isPinned) accentColor else ContextCompat.getColor(context, R.color.mutedForeground)
            )
            binding.archiveAction.setImageResource(if (note.isArchived) R.drawable.ic_inbox else R.drawable.ic_archive)

            binding.selectionCheck.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.selectionCheck.setImageResource(
                if (selected) R.drawable.ic_check_circle_filled else R.drawable.ic_circle_outline
            )
            binding.noteCardRoot.alpha = if (selectionMode && !selected) 0.6f else 1f

            binding.root.setOnClickListener {
                if (isSelectionMode()) onLongPress(note) else onOpen(note)
            }
            binding.root.setOnLongClickListener {
                onLongPress(note)
                true
            }
            binding.pinAction.setOnClickListener { onTogglePin(note) }
            binding.archiveAction.setOnClickListener { onToggleArchive(note) }
            binding.trashAction.setOnClickListener { onTrash(note) }
            binding.restoreAction.setOnClickListener { onRestore(note) }
            binding.deleteForeverAction.setOnClickListener { onDeleteForever(note) }
        }
    }

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private class NoteDiffCallback(
        private val old: List<Note>,
        private val new: List<Note>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].fileName == new[newItemPosition].fileName

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}
