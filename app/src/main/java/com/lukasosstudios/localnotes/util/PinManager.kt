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
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashHex = prefs.getString(KEY_HASH, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val candidateHash = hash(pin, salt).joinToString("") { "%02x".format(it) }
        return candidateHash == storedHashHex
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
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
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
    }
}
