package com.lukasosstudios.localnotes.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.data.BackupManager
import com.lukasosstudios.localnotes.data.NoteRepository
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.databinding.ActivitySettingsBinding
import com.lukasosstudios.localnotes.databinding.ItemOptionRowBinding
import com.lukasosstudios.localnotes.model.SortMode
import com.lukasosstudios.localnotes.model.ThemeMode
import com.lukasosstudios.localnotes.ui.common.ConfirmDialog
import com.lukasosstudios.localnotes.ui.security.LockActivity
import com.lukasosstudios.localnotes.ui.security.PinSetupActivity
import com.lukasosstudios.localnotes.util.AppLock
import com.lukasosstudios.localnotes.util.AppLockState
import com.lukasosstudios.localnotes.util.PinManager
import com.lukasosstudios.localnotes.util.ThemeUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: NoteRepository
    private lateinit var settings: SettingsRepository
    private lateinit var backupManager: BackupManager
    private lateinit var pinManager: PinManager

    private var pendingImportUri: Uri? = null

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) finish()
    }

    private val verifyToDisableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            settings.appLockEnabled = false
            settings.writeToFile(repository, repository.listNotes().size)
        }
        renderAppLockSwitch()
    }

    private val pinSetupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            renderPinRow()
        }
    }

    private val importPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(this)
        settings = SettingsRepository(this)
        backupManager = BackupManager(this, repository)
        pinManager = PinManager(this)
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
        renderAppLockSwitch()
        renderPinRow()

        binding.storageActionButton.setOnClickListener { requestStoragePermission() }
        binding.exportRow.setOnClickListener { exportBackup() }
        binding.importRow.setOnClickListener { importPicker.launch(arrayOf("application/zip", "application/octet-stream")) }
        binding.pinRow.setOnClickListener { pinSetupLauncher.launch(Intent(this, PinSetupActivity::class.java)) }
        binding.pinRemoveButton.setOnClickListener { confirmRemovePin() }
    }

    override fun onResume() {
        super.onResume()
        if (AppLock.guard(this, settings, lockLauncher)) return
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

    // ---- App lock --------------------------------------------------------

    private fun renderAppLockSwitch() {
        binding.appLockSwitch.setOnCheckedChangeListener(null)
        binding.appLockSwitch.isChecked = settings.appLockEnabled
        binding.appLockSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                val manager = BiometricManager.from(this)
                if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
                    Toast.makeText(this, R.string.app_lock_unavailable, Toast.LENGTH_LONG).show()
                    renderAppLockSwitch()
                    return@setOnCheckedChangeListener
                }
                settings.appLockEnabled = true
                settings.writeToFile(repository, repository.listNotes().size)
                AppLockState.unlocked = true
            } else {
                // Disabling protection requires proving it's really you first --
                // revert the switch now and only apply the change if verification succeeds.
                renderAppLockSwitch()
                verifyToDisableLauncher.launch(Intent(this, LockActivity::class.java))
            }
        }
    }

    // ---- App PIN -----------------------------------------------------------

    private fun renderPinRow() {
        val hasPin = pinManager.hasPin()
        binding.pinRowBadge.text = getString(if (hasPin) R.string.pin_set_badge else R.string.pin_not_set_badge)
        binding.pinRowSubtitle.text = getString(if (hasPin) R.string.pin_row_subtitle_set else R.string.pin_row_subtitle_unset)
        binding.pinRemoveButton.visibility = if (hasPin) View.VISIBLE else View.GONE
    }

    private fun confirmRemovePin() {
        ConfirmDialog.show(
            activity = this,
            title = getString(R.string.remove_pin_title),
            message = getString(R.string.remove_pin_message),
            icon = R.drawable.ic_keypad,
            destructive = true,
            positiveLabel = getString(R.string.remove_action)
        ) {
            pinManager.clearPin()
            renderPinRow()
        }
    }

    // ---- Backup & restore --------------------------------------------------

    private fun exportBackup() {
        val zip = backupManager.export()
        if (zip == null) {
            Toast.makeText(this, R.string.backup_export_failed, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, R.string.backup_export_success, Toast.LENGTH_SHORT).show()
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", zip)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.backup_export_title)))
        } catch (_: Exception) {
        }
    }

    private fun confirmImport(uri: Uri) {
        pendingImportUri = uri
        ConfirmDialog.show(
            activity = this,
            title = getString(R.string.backup_import_confirm_title),
            message = getString(R.string.backup_import_confirm_message),
            icon = R.drawable.ic_import,
            positiveLabel = getString(R.string.import_action)
        ) {
            val success = pendingImportUri?.let { backupManager.import(it) } ?: false
            Toast.makeText(
                this,
                if (success) R.string.backup_import_success else R.string.backup_import_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
