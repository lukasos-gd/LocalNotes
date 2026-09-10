package com.lukasosstudios.localnotes.util

/**
 * Tracks whether the app is currently "unlocked" for this process lifetime.
 * Reset to false by [com.lukasosstudios.localnotes.LocalNotesApp] whenever the
 * whole app (not just one activity) leaves the foreground. This is
 * intentionally in-memory only -- there is nothing to persist, and a fresh
 * process should always start locked if app lock is enabled.
 */
object AppLockState {
    @Volatile
    var unlocked: Boolean = true
}
