package com.lukasosstudios.localnotes.util

import android.content.Context
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores a custom app-level PIN as a salted PBKDF2 hash -- never the PIN
 * itself. This PIN is only ever used as a fallback when biometric
 * authentication isn't available or fails; the primary unlock method is
 * still the device's fingerprint/face sensor.
 *
 * Failed attempts are rate-limited (5 tries per 3-minute window) to make
 * brute-forcing the PIN impractical, tracked persistently so restarting the
 * app can't be used to reset the counter.
 */
class PinManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, salt.joinToString("") { "%02x".format(it) })
            .putString(KEY_HASH, hash.joinToString("") { "%02x".format(it) })
            .apply()
        resetAttempts()
    }

    fun verifyPin(pin: String): Boolean {
        if (isLockedOut()) return false
        val saltHex = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashHex = prefs.getString(KEY_HASH, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val candidateHash = hash(pin, salt).joinToString("") { "%02x".format(it) }
        val matches = candidateHash == storedHashHex
        if (matches) resetAttempts() else registerFailedAttempt()
        return matches
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
        resetAttempts()
    }

    // ---- Rate limiting: 5 attempts per rolling 3-minute window -------------

    fun isLockedOut(): Boolean {
        val count = prefs.getInt(KEY_ATTEMPT_COUNT, 0)
        if (count < MAX_ATTEMPTS) return false
        val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
        return System.currentTimeMillis() - windowStart < WINDOW_MILLIS
    }

    /** Milliseconds remaining until the lockout window clears. 0 if not locked out. */
    fun lockoutRemainingMillis(): Long {
        if (!isLockedOut()) return 0L
        val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
        return (WINDOW_MILLIS - (System.currentTimeMillis() - windowStart)).coerceAtLeast(0L)
    }

    fun attemptsRemaining(): Int {
        val count = prefs.getInt(KEY_ATTEMPT_COUNT, 0)
        val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
        if (System.currentTimeMillis() - windowStart >= WINDOW_MILLIS) return MAX_ATTEMPTS
        return (MAX_ATTEMPTS - count).coerceAtLeast(0)
    }

    private fun registerFailedAttempt() {
        val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
        val now = System.currentTimeMillis()
        if (now - windowStart >= WINDOW_MILLIS) {
            // Previous window (if any) has fully elapsed -- start a fresh one.
            prefs.edit().putLong(KEY_WINDOW_START, now).putInt(KEY_ATTEMPT_COUNT, 1).apply()
        } else {
            val count = prefs.getInt(KEY_ATTEMPT_COUNT, 0) + 1
            prefs.edit().putInt(KEY_ATTEMPT_COUNT, count).apply()
        }
    }

    private fun resetAttempts() {
        prefs.edit().remove(KEY_ATTEMPT_COUNT).remove(KEY_WINDOW_START).apply()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "local_notes_pin"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_ATTEMPT_COUNT = "attempt_count"
        private const val KEY_WINDOW_START = "window_start"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        const val MAX_ATTEMPTS = 5
        const val WINDOW_MILLIS = 3 * 60 * 1000L
    }
}
