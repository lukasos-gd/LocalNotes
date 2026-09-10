package com.lukasosstudios.localnotes

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.util.AppLockState
import com.lukasosstudios.localnotes.util.ThemeUtils

class LocalNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val settings = SettingsRepository(this)

        // Apply the cached theme immediately so there's no flash of the wrong
        // appearance before the first activity reads storage.
        ThemeUtils.apply(settings.theme)

        // A fresh process should start locked if app lock is on. Once the
        // whole app (not just one of our activities) actually leaves the
        // foreground, re-arm the lock so the next resume asks again.
        AppLockState.unlocked = !settings.appLockEnabled
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (SettingsRepository(this@LocalNotesApp).appLockEnabled) {
                    AppLockState.unlocked = false
                }
            }
        })
    }
}
