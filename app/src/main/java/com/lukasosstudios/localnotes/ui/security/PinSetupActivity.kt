package com.lukasosstudios.localnotes.ui.security

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ActivityPinSetupBinding
import com.lukasosstudios.localnotes.ui.common.NumericKeypadBinder
import com.lukasosstudios.localnotes.util.PinManager

class PinSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinSetupBinding
    private lateinit var pinManager: PinManager

    private val entered = StringBuilder()
    private var firstEntry: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pinManager = PinManager(this)

        binding.backButton.setOnClickListener { finish() }

        NumericKeypadBinder.bind(
            keypad = binding.keypad,
            onDigit = { digit ->
                if (entered.length < MAX_PIN_LENGTH) {
                    entered.append(digit)
                    renderDots()
                    hideError()
                }
            },
            onBackspace = {
                if (entered.isNotEmpty()) {
                    entered.deleteCharAt(entered.length - 1)
                    renderDots()
                }
            },
            onDone = { submitStep() }
        )
    }

    private fun submitStep() {
        if (entered.length < MIN_PIN_LENGTH) {
            showError(getString(R.string.pin_setup_too_short))
            return
        }

        val first = firstEntry
        if (first == null) {
            firstEntry = entered.toString()
            entered.clear()
            renderDots()
            binding.stepTitle.text = getString(R.string.pin_setup_confirm)
        } else {
            if (entered.toString() == first) {
                pinManager.setPin(first)
                setResult(RESULT_OK)
                finish()
            } else {
                showError(getString(R.string.pin_setup_mismatch))
                firstEntry = null
                entered.clear()
                renderDots()
                binding.stepTitle.text = getString(R.string.pin_setup_enter_new)
            }
        }
    }

    private fun renderDots() {
        binding.pinDotsRow.removeAllViews()
        val dotSize = (12 * resources.displayMetrics.density).toInt()
        val margin = (5 * resources.displayMetrics.density).toInt()
        repeat(entered.length) {
            val dot = View(this)
            dot.setBackgroundResource(R.drawable.bg_pin_dot)
            val params = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            params.marginStart = margin
            params.marginEnd = margin
            dot.layoutParams = params
            binding.pinDotsRow.addView(dot)
        }
    }

    private fun showError(message: String) {
        binding.pinErrorText.text = message
        binding.pinErrorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.pinErrorText.visibility = View.INVISIBLE
    }

    companion object {
        private const val MIN_PIN_LENGTH = 4
        private const val MAX_PIN_LENGTH = 8
    }
}
