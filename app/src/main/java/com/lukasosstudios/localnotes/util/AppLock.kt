package com.lukasosstudios.localnotes.util

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.ui.security.LockActivity

object AppLock {
    /**
     * Call at the top of onResume(). Returns true if a lock screen was
     * launched (caller should skip the rest of its resume logic for this
     * pass -- it will run again once the lock screen finishes).
     */
    fun guard(
        activity: Activity,
        settings: SettingsRepository,
        launcher: ActivityResultLauncher<Intent>
    ): Boolean {
        if (settings.appLockEnabled && !AppLockState.unlocked) {
            launcher.launch(Intent(activity, LockActivity::class.java))
            return true
        }
        return false
    }
}
