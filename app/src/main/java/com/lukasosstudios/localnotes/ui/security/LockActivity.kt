package com.lukasosstudios.localnotes.ui.security

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ActivityLockBinding
import com.lukasosstudios.localnotes.util.AppLockState

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding

    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.unlockButton.setOnClickListener { promptUnlock() }
    }

    override fun onStart() {
        super.onStart()
        promptUnlock()
    }

    private fun promptUnlock() {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(allowedAuthenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No usable screen lock on this device at all -- can't enforce it, let the user through.
            unlockAndFinish()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlockAndFinish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User backed out or hit a hard error -- leave them on the lock screen.
            }

            override fun onAuthenticationFailed() {
                // A single failed attempt -- biometric prompt keeps its own retry UI.
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun unlockAndFinish() {
        AppLockState.unlocked = true
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
