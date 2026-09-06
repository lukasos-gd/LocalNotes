package com.lukasosstudios.localnotes.ui.security

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ActivityLockBinding
import com.lukasosstudios.localnotes.ui.common.NumericKeypadBinder
import com.lukasosstudios.localnotes.util.AppLockState
import com.lukasosstudios.localnotes.util.PinManager

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var pinManager: PinManager
    private val enteredPin = StringBuilder()

    // Biometric only -- the fallback is our own PIN screen, not the system credential.
    private val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pinManager = PinManager(this)

        binding.unlockButton.setOnClickListener { promptBiometric() }
        binding.usePinButton.setOnClickListener { showPinSection() }
        binding.useFingerprintButton.setOnClickListener { showBiometricSection() }

        NumericKeypadBinder.bind(
            keypad = binding.keypad,
            onDigit = { digit ->
                if (enteredPin.length < MAX_PIN_LENGTH) {
                    enteredPin.append(digit)
                    renderPinDots()
                    hideError()
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin.deleteCharAt(enteredPin.length - 1)
                    renderPinDots()
                }
            },
            onDone = { verifyEnteredPin() }
        )
    }

    override fun onStart() {
        super.onStart()
        val canUseBiometric = BiometricManager.from(this).canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS

        if (!canUseBiometric && !pinManager.hasPin()) {
            // Nothing we can actually verify against -- don't lock the person out.
            unlockAndFinish()
            return
        }

        if (canUseBiometric) {
            showBiometricSection()
        } else {
            showPinSection()
        }
    }

    private fun showBiometricSection() {
        binding.biometricSection.visibility = View.VISIBLE
        binding.pinSection.visibility = View.GONE
        binding.usePinButton.visibility = if (pinManager.hasPin()) View.VISIBLE else View.GONE
        enteredPin.clear()
        renderPinDots()
        hideError()
        promptBiometric()
    }

    private fun showPinSection() {
        binding.biometricSection.visibility = View.GONE
        binding.pinSection.visibility = View.VISIBLE
        val canUseBiometric = BiometricManager.from(this).canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
        binding.useFingerprintButton.visibility = if (canUseBiometric) View.VISIBLE else View.GONE
        enteredPin.clear()
        renderPinDots()
        hideError()
    }

    private fun promptBiometric() {
        if (BiometricManager.from(this).canAuthenticate(allowedAuthenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
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
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun verifyEnteredPin() {
        if (enteredPin.isEmpty()) return
        if (pinManager.verifyPin(enteredPin.toString())) {
            unlockAndFinish()
        } else {
            showError()
            enteredPin.clear()
            renderPinDots()
        }
    }

    private fun renderPinDots() {
        binding.pinDotsRow.removeAllViews()
        val dotSize = (12 * resources.displayMetrics.density).toInt()
        val margin = (5 * resources.displayMetrics.density).toInt()
        repeat(enteredPin.length) {
            val dot = View(this)
            dot.setBackgroundResource(R.drawable.bg_pin_dot)
            val params = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            params.marginStart = margin
            params.marginEnd = margin
            dot.layoutParams = params
            binding.pinDotsRow.addView(dot)
        }
    }

    private fun showError() {
        binding.pinErrorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.pinErrorText.visibility = View.INVISIBLE
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

    companion object {
        private const val MAX_PIN_LENGTH = 8
    }
}
