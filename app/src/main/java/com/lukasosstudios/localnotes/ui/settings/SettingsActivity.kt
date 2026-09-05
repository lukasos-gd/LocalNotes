package com.lukasosstudios.localnotes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.data.NoteRepository
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.databinding.ActivitySettingsBinding
import com.lukasosstudios.localnotes.databinding.ItemOptionRowBinding
import com.lukasosstudios.localnotes.model.SortMode
import com.lukasosstudios.localnotes.model.ThemeMode
import com.lukasosstudios.localnotes.util.ThemeUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: NoteRepository
    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(this)
        settings = SettingsRepository(this)
        settings.loadFromFile(repository)

        binding.backButton.setOnClickListener { finish() }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionText.text = "v${pInfo.versionName}"
        } catch (_: Exception) {
        }

        buildThemeOptions()
        buildSortOptions()
        renderStorageStatus()

        binding.storageActionButton.setOnClickListener { requestStoragePermission() }
    }

    override fun onResume() {
        super.onResume()
        renderStorageStatus()
    }

    private fun buildThemeOptions() {
        binding.themeGroup.removeAllViews()
        val entries = listOf(
            Triple(ThemeMode.SYSTEM, getString(R.string.theme_system), R.drawable.ic_phone),
            Triple(ThemeMode.LIGHT, getString(R.string.theme_light), R.drawable.ic_sun),
            Triple(ThemeMode.DARK, getString(R.string.theme_dark), R.drawable.ic_moon)
        )
        entries.forEach { (mode, label, icon) ->
            val row = ItemOptionRowBinding.inflate(LayoutInflater.from(this), binding.themeGroup, false)
            row.optionIcon.setImageResource(icon)
            row.optionLabel.text = label
            val selected = settings.theme == mode
            styleOptionRow(row, selected)
            row.optionRoot.setOnClickListener {
                settings.theme = mode
                settings.writeToFile(repository, repository.listNotes().size)
                ThemeUtils.apply(mode)
                buildThemeOptions()
            }
            binding.themeGroup.addView(row.root)
        }
    }

    private fun buildSortOptions() {
        binding.sortGroup.removeAllViews()
        val entries = listOf(
            Triple(SortMode.UPDATED, getString(R.string.sort_updated), R.drawable.ic_clock),
            Triple(SortMode.CREATED, getString(R.string.sort_created), R.drawable.ic_calendar),
            Triple(SortMode.TITLE, getString(R.string.sort_title), R.drawable.ic_type)
        )
        entries.forEach { (mode, label, icon) ->
            val row = ItemOptionRowBinding.inflate(LayoutInflater.from(this), binding.sortGroup, false)
            row.optionIcon.setImageResource(icon)
            row.optionLabel.text = label
            val selected = settings.sort == mode
            styleOptionRow(row, selected)
            row.optionRoot.setOnClickListener {
                settings.sort = mode
                settings.writeToFile(repository, repository.listNotes().size)
                buildSortOptions()
            }
            binding.sortGroup.addView(row.root)
        }
    }

    private fun styleOptionRow(row: ItemOptionRowBinding, selected: Boolean) {
        row.optionRoot.setBackgroundResource(
            if (selected) R.drawable.bg_option_row_selected else R.drawable.bg_option_row
        )
        row.optionCheck.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        val tint = if (selected) R.color.tint else R.color.mutedForeground
        row.optionIcon.setColorFilter(ContextCompat.getColor(this, tint))
    }

    private fun renderStorageStatus() {
        val granted = repository.hasPermission()
        binding.storageStatusIcon.visibility = if (granted) View.VISIBLE else View.GONE
        binding.storageActionButton.visibility = if (granted) View.GONE else View.VISIBLE
        binding.storageActionLabel.text = getString(R.string.storage_not_granted)
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
                1001
            )
        }
    }
}
