package com.lukasosstudios.localnotes.ui.notes

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ItemNoteBinding
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteFilter
import com.lukasosstudios.localnotes.util.DateUtils

class NotesAdapter(
    private val onOpen: (Note) -> Unit,
    private val onTogglePin: (Note) -> Unit,
    private val onToggleArchive: (Note) -> Unit,
    private val onTrash: (Note) -> Unit,
    private val onRestore: (Note) -> Unit,
    private val onDeleteForever: (Note) -> Unit
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

            val bg = ContextCompat.getDrawable(context, R.drawable.bg_note_card_fill)?.mutate() as? GradientDrawable
            bg?.setColor(ContextCompat.getColor(context, note.color.backgroundRes))
            bg?.setStroke(1.dp(context), ContextCompat.getColor(context, note.color.borderRes))
            binding.noteCardRoot.background = bg

            binding.noteAccent.setBackgroundColor(ContextCompat.getColor(context, note.color.accentRes))

            binding.noteTitle.text = note.title.ifBlank { context.getString(R.string.untitled_note) }
            binding.noteBody.text = note.body.ifBlank { context.getString(R.string.no_text_yet) }
            binding.noteDate.text = DateUtils.formatRelative(note.updatedAt)

            binding.notePinIcon.visibility = if (note.isPinned) android.view.View.VISIBLE else android.view.View.GONE
            val accentColor = ContextCompat.getColor(context, note.color.accentRes)
            binding.notePinIcon.setColorFilter(accentColor)

            val isTrash = filter == NoteFilter.TRASH
            binding.pinAction.visibility = if (isTrash) android.view.View.GONE else android.view.View.VISIBLE
            binding.archiveAction.visibility = if (isTrash) android.view.View.GONE else android.view.View.VISIBLE
            binding.trashAction.visibility = if (isTrash) android.view.View.GONE else android.view.View.VISIBLE
            binding.restoreAction.visibility = if (isTrash) android.view.View.VISIBLE else android.view.View.GONE
            binding.deleteForeverAction.visibility = if (isTrash) android.view.View.VISIBLE else android.view.View.GONE

            binding.pinAction.setImageResource(if (note.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline)
            binding.pinAction.setColorFilter(
                if (note.isPinned) accentColor else ContextCompat.getColor(context, R.color.mutedForeground)
            )
            binding.archiveAction.setImageResource(if (note.isArchived) R.drawable.ic_inbox else R.drawable.ic_archive)

            binding.root.setOnClickListener { onOpen(note) }
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
