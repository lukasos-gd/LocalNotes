package com.lukasosstudios.localnotes.ui.security

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.ui.common.TranslatedActivity
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ActivityLockBinding
import com.lukasosstudios.localnotes.databinding.IncludeNumericKeypadBinding
import com.lukasosstudios.localnotes.ui.common.NumericKeypadBinder
import com.lukasosstudios.localnotes.util.AppLockState
import com.lukasosstudios.localnotes.util.HapticUtils
import com.lukasosstudios.localnotes.util.PinManager
import java.util.concurrent.TimeUnit

class LockActivity : TranslatedActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var pinManager: PinManager
    private val enteredPin = StringBuilder()
    private var lockoutTimer: CountDownTimer? = null

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

        applyTranslatedStrings()
    }

    private fun applyTranslatedStrings() {
        binding.lockScreenTitleText.text = getString(R.string.lock_screen_title)
        binding.lockScreenSubtitleText.text = getString(R.string.lock_screen_subtitle)
        binding.unlockButtonLabel.text = getString(R.string.unlock_button)
        binding.usePinLabel.text = getString(R.string.use_pin_instead)
        binding.enterPinTitleText.text = getString(R.string.enter_pin_title)
        binding.useFingerprintLabel.text = getString(R.string.use_fingerprint_instead)
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

    override fun onStop() {
        super.onStop()
        lockoutTimer?.cancel()
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
        refreshLockoutState()
    }

    private fun promptBiometric() {
        if (BiometricManager.from(this).canAuthenticate(allowedAuthenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                HapticUtils.confirm(this@LockActivity)
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
        if (enteredPin.isEmpty() || pinManager.isLockedOut()) return
        if (pinManager.verifyPin(enteredPin.toString())) {
            HapticUtils.confirm(this)
            unlockAndFinish()
        } else {
            HapticUtils.error(this)
            enteredPin.clear()
            renderPinDots()
            refreshLockoutState()
        }
    }

    /** Shows either the normal "wrong PIN, N attempts left" message, or a live lockout countdown. */
    private fun refreshLockoutState() {
        lockoutTimer?.cancel()

        if (pinManager.isLockedOut()) {
            setKeypadEnabled(false)
            binding.pinErrorText.visibility = View.VISIBLE
            lockoutTimer = object : CountDownTimer(pinManager.lockoutRemainingMillis(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.pinErrorText.text = getString(R.string.pin_locked_out, formatDuration(millisUntilFinished))
                }

                override fun onFinish() {
                    setKeypadEnabled(true)
                    hideError()
                }
            }.start()
        } else if (pinManager.attemptsRemaining() < PinManager.MAX_ATTEMPTS) {
            setKeypadEnabled(true)
            binding.pinErrorText.text = getString(R.string.pin_incorrect, pinManager.attemptsRemaining())
            binding.pinErrorText.visibility = View.VISIBLE
        } else {
            setKeypadEnabled(true)
        }
    }

    private fun setKeypadEnabled(enabled: Boolean) {
        val keys: IncludeNumericKeypadBinding = binding.keypad
        listOf(
            keys.key0, keys.key1, keys.key2, keys.key3, keys.key4,
            keys.key5, keys.key6, keys.key7, keys.key8, keys.key9,
            keys.keyBackspace, keys.keyDone
        ).forEach { it.isEnabled = enabled; it.alpha = if (enabled) 1f else 0.35f }
    }

    private fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
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
