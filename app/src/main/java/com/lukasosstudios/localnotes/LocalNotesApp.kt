package com.lukasosstudios.localnotes

import android.app.Application
import com.lukasosstudios.localnotes.data.SettingsRepository
import com.lukasosstudios.localnotes.util.ThemeUtils

class LocalNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the cached theme immediately so there's no flash of the wrong
        // appearance before the first activity reads storage.
        val settings = SettingsRepository(this)
        ThemeUtils.apply(settings.theme)
    }
}
