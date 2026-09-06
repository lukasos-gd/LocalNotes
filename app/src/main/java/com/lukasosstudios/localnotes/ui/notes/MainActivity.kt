package com.lukasosstudios.localnotes.ui.notes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.data.NoteRepository
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.databinding.ActivityMainBinding
import com.lukasosstudios.localnotes.databinding.ItemFilterPillBinding
import com.lukasosstudios.localnotes.model.Note
import com.lukasosstudios.localnotes.model.NoteFilter
import com.lukasosstudios.localnotes.model.SortMode
import com.lukasosstudios.localnotes.ui.calculator.CalculatorActivity
import com.lukasosstudios.localnotes.ui.common.ConfirmDialog
import com.lukasosstudios.localnotes.ui.editor.NoteEditorActivity
import com.lukasosstudios.localnotes.ui.settings.SettingsActivity
import com.lukasosstudios.localnotes.util.AppLock

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: NoteRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var adapter: NotesAdapter

    private var allNotes: List<Note> = emptyList()
    private var currentFilter: NoteFilter = NoteFilter.ALL
    private var searchQuery: String = ""

    private val selectedFileNames = mutableSetOf<String>()
    private var selectionMode = false
    private var pendingScrollToTop = false

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(this)
        settingsRepository = SettingsRepository(this)

        adapter = NotesAdapter(
            onOpen = { note -> openEditor(note.fileName) },
            onTogglePin = { note -> mutate(note.copy(isPinned = !note.isPinned)) },
            onToggleArchive = { note -> mutate(note.copy(isArchived = !note.isArchived)) },
            onTrash = { note -> confirmTrash(note) },
            onRestore = { note -> mutate(note.copy(isDeleted = false)) },
            onDeleteForever = { note -> confirmDeleteForever(note) },
            onLongPress = { note -> toggleSelection(note) },
            isSelected = { note -> selectedFileNames.contains(note.fileName) },
            isSelectionMode = { selectionMode }
        )
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = adapter

        binding.calculatorButton.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.newNoteButton.setOnClickListener { openEditor(null) }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                binding.clearSearchButton.visibility = if (searchQuery.isEmpty()) View.GONE else View.VISIBLE
                render()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.clearSearchButton.setOnClickListener { binding.searchInput.setText("") }

        binding.emptyTrashButton.setOnClickListener { emptyTrash() }

        binding.selectionCancelButton.setOnClickListener { exitSelectionMode() }
        binding.selectionPinButton.setOnClickListener { bulkPrimaryAction() }
        binding.selectionArchiveButton.setOnClickListener { bulkArchive() }
        binding.selectionTrashButton.setOnClickListener { bulkTrashOrDeleteForever() }

        buildFilterPills()
    }

    override fun onResume() {
        super.onResume()
        if (AppLock.guard(this, settingsRepository, lockLauncher)) return

        if (!repository.hasPermission()) {
            promptForStoragePermission()
        } else {
            settingsRepository.loadFromFile(repository)
            reload()
        }
    }

    private fun promptForStoragePermission() {
        ConfirmDialog.show(
            activity = this,
            title = getString(R.string.storage_not_granted),
            message = getString(R.string.storage_permission_rationale),
            icon = R.drawable.ic_folder,
            positiveLabel = getString(R.string.storage_not_granted),
            negativeLabel = null,
            cancelable = false
        ) { requestStoragePermission() }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_LEGACY_STORAGE
            )
        }
    }

    private fun reload() {
        allNotes = repository.listNotes()
        // Drop selections for notes that no longer exist/qualify.
        selectedFileNames.retainAll(allNotes.map { it.fileName }.toSet())
        if (selectedFileNames.isEmpty()) selectionMode = false
        render()
    }

    private fun mutate(updated: Note) {
        val patched = updated.copy(updatedAt = System.currentTimeMillis())
        repository.saveNote(patched)
        reload()
    }

    private fun confirmDeleteForever(note: Note) {
        ConfirmDialog.show(
            activity = this,
            title = getString(R.string.delete_forever_title),
            message = getString(R.string.delete_forever_message),
            icon = R.drawable.ic_trash,
            destructive = true,
            positiveLabel = getString(R.string.delete)
        ) {
            repository.deleteForever(note)
            reload()
        }
    }

    private fun confirmTrash(note: Note) {
        ConfirmDialog.show(
            activity = this,
            title = getString(R.string.trash_confirm_title),
            message = getString(R.string.trash_confirm_message),
            icon = R.drawable.ic_trash,
            positiveLabel = getString(R.string.trash_action)
        ) {
            mutate(note.copy(isDeleted = true, isArchived = false))
        }
    }

    private fun emptyTrash() {
        allNotes.filter { it.isDeleted }.forEach { repository.deleteForever(it) }
        reload()
    }

    // ---- Multi-select -------------------------------------------------

    private fun toggleSelection(note: Note) {
        if (selectedFileNames.contains(note.fileName)) {
            selectedFileNames.remove(note.fileName)
        } else {
            selectedFileNames.add(note.fileName)
        }
        selectionMode = selectedFileNames.isNotEmpty()
        renderSelectionBar()
        adapter.refreshSelectionState()
    }

    private fun exitSelectionMode() {
        selectedFileNames.clear()
        selectionMode = false
        renderSelectionBar()
        adapter.refreshSelectionState()
    }

    private fun renderSelectionBar() {
        binding.selectionBar.visibility = if (selectionMode) View.VISIBLE else View.GONE
        binding.brandHeaderRow.visibility = if (selectionMode) View.GONE else View.VISIBLE
        binding.selectionCountText.text = getString(R.string.selection_count, selectedFileNames.size)

        val isTrash = currentFilter == NoteFilter.TRASH
        binding.selectionArchiveButton.visibility = if (isTrash) View.GONE else View.VISIBLE
        binding.selectionPinButton.setImageResource(if (isTrash) R.drawable.ic_restore else R.drawable.ic_pin_outline)
        binding.selectionPinButton.contentDescription =
            getString(if (isTrash) R.string.cd_bulk_restore else R.string.cd_bulk_pin)
        binding.selectionTrashButton.contentDescription =
            getString(if (isTrash) R.string.cd_bulk_delete_forever else R.string.cd_bulk_trash)
    }

    private fun selectedNotes(): List<Note> = allNotes.filter { selectedFileNames.contains(it.fileName) }

    private fun bulkPrimaryAction() {
        if (currentFilter == NoteFilter.TRASH) {
            selectedNotes().forEach { repository.saveNote(it.copy(isDeleted = false, updatedAt = System.currentTimeMillis())) }
        } else {
            val shouldPin = selectedNotes().any { !it.isPinned }
            selectedNotes().forEach { repository.saveNote(it.copy(isPinned = shouldPin, updatedAt = System.currentTimeMillis())) }
        }
        exitSelectionMode()
        reload()
    }

    private fun bulkArchive() {
        val shouldArchive = selectedNotes().any { !it.isArchived }
        selectedNotes().forEach {
            repository.saveNote(it.copy(isArchived = shouldArchive, isPinned = false, updatedAt = System.currentTimeMillis()))
        }
        exitSelectionMode()
        reload()
    }

    private fun bulkTrashOrDeleteForever() {
        val count = selectedFileNames.size
        if (currentFilter == NoteFilter.TRASH) {
            ConfirmDialog.show(
                activity = this,
                title = getString(R.string.bulk_delete_forever_title, count),
                message = getString(R.string.bulk_delete_forever_message),
                icon = R.drawable.ic_trash,
                destructive = true,
                positiveLabel = getString(R.string.delete)
            ) {
                selectedNotes().forEach { repository.deleteForever(it) }
                exitSelectionMode()
                reload()
            }
        } else {
            ConfirmDialog.show(
                activity = this,
                title = getString(R.string.bulk_trash_confirm_title, count),
                message = getString(R.string.bulk_trash_confirm_message),
                icon = R.drawable.ic_trash,
                positiveLabel = getString(R.string.trash_action)
            ) {
                selectedNotes().forEach {
                    repository.saveNote(it.copy(isDeleted = true, isArchived = false, isPinned = false, updatedAt = System.currentTimeMillis()))
                }
                exitSelectionMode()
                reload()
            }
        }
    }

    // ---- Rendering ------------------------------------------------------

    private fun visibleForFilter(filter: NoteFilter): List<Note> = allNotes.filter { note ->
        when (filter) {
            NoteFilter.PINNED -> note.isPinned && !note.isDeleted && !note.isArchived
            NoteFilter.ARCHIVED -> note.isArchived && !note.isDeleted
            NoteFilter.TRASH -> note.isDeleted
            NoteFilter.ALL -> !note.isDeleted && !note.isArchived
        }
    }

    private fun render() {
        val sort = settingsRepository.sort
        val filtered = visibleForFilter(currentFilter)
            .filter { "${it.title} ${it.body}".contains(searchQuery, ignoreCase = true) }
        val sorted = when (sort) {
            SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortMode.CREATED -> filtered.sortedByDescending { it.createdAt }
            SortMode.UPDATED -> filtered.sortedByDescending { it.updatedAt }
        }

        adapter.submit(sorted, currentFilter)
        renderSelectionBar()

        if (pendingScrollToTop) {
            pendingScrollToTop = false
            if (sort == SortMode.UPDATED && sorted.isNotEmpty()) {
                binding.notesRecyclerView.scrollToPosition(0)
            }
        }

        val liveCount = allNotes.count { !it.isDeleted }
        val noteWord = if (liveCount == 1) "note" else "notes"
        binding.summaryTitle.text = "$liveCount $noteWord"

        binding.sectionTitle.text = when (currentFilter) {
            NoteFilter.ALL -> getString(R.string.section_your_notes)
            NoteFilter.PINNED -> getString(R.string.filter_pinned)
            NoteFilter.ARCHIVED -> getString(R.string.filter_archived)
            NoteFilter.TRASH -> getString(R.string.filter_trash)
        }
        binding.emptyTrashButton.visibility =
            if (currentFilter == NoteFilter.TRASH && allNotes.any { it.isDeleted }) View.VISIBLE else View.GONE

        val isEmpty = sorted.isEmpty()
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.notesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            binding.emptyIcon.setImageResource(if (currentFilter == NoteFilter.TRASH) R.drawable.ic_trash else R.drawable.ic_edit)
            binding.emptyTitle.text = when {
                searchQuery.isNotEmpty() -> getString(R.string.empty_title_search)
                currentFilter == NoteFilter.TRASH -> getString(R.string.empty_title_trash)
                else -> getString(R.string.empty_title_default)
            }
            binding.emptyCopy.text = if (searchQuery.isNotEmpty()) {
                getString(R.string.empty_copy_search)
            } else {
                getString(R.string.empty_copy_default)
            }
        }

        buildFilterPills()
    }

    private fun buildFilterPills() {
        binding.filterRow.removeAllViews()
        val pinnedCount = allNotes.count { it.isPinned && !it.isDeleted && !it.isArchived }
        val archivedCount = allNotes.count { it.isArchived && !it.isDeleted }
        val allCount = allNotes.count { !it.isDeleted && !it.isArchived }
        val trashCount = allNotes.count { it.isDeleted }

        val entries = listOf(
            Triple(NoteFilter.ALL, getString(R.string.filter_all), allCount),
            Triple(NoteFilter.PINNED, getString(R.string.filter_pinned), pinnedCount),
            Triple(NoteFilter.ARCHIVED, getString(R.string.filter_archived), archivedCount),
            Triple(NoteFilter.TRASH, getString(R.string.filter_trash), trashCount)
        )

        entries.forEach { (filter, label, count) ->
            val pillBinding = ItemFilterPillBinding.inflate(LayoutInflater.from(this), binding.filterRow, false)
            pillBinding.pillLabel.text = label
            pillBinding.pillCount.text = if (count > 0) count.toString() else ""
            pillBinding.pillCount.visibility = if (count > 0) View.VISIBLE else View.GONE

            val selected = filter == currentFilter
            pillBinding.pillRoot.setBackgroundResource(
                if (selected) R.drawable.bg_filter_pill_selected else R.drawable.bg_filter_pill
            )
            val labelColor = if (selected) R.color.primaryForeground else R.color.secondaryForeground
            val countColor = if (selected) R.color.primaryForeground else R.color.mutedForeground
            pillBinding.pillLabel.setTextColor(ContextCompat.getColor(this, labelColor))
            pillBinding.pillCount.setTextColor(ContextCompat.getColor(this, countColor))

            pillBinding.pillRoot.setOnClickListener {
                currentFilter = filter
                exitSelectionMode()
                render()
            }
            binding.filterRow.addView(pillBinding.root)
        }
    }

    private fun openEditor(fileName: String?) {
        pendingScrollToTop = true
        val intent = Intent(this, NoteEditorActivity::class.java)
        if (fileName != null) intent.putExtra(NoteEditorActivity.EXTRA_FILE_NAME, fileName)
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_LEGACY_STORAGE = 1001
    }
}
