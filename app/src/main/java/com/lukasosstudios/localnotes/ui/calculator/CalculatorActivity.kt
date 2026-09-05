package com.lukasosstudios.localnotes.ui.calculator

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.ActivityCalculatorBinding
import kotlin.math.roundToLong

class CalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorBinding
    private var expression = ""

    private data class Key(val label: String, val kind: Kind, val icon: Int = 0)
    private enum class Kind { CLEAR, PAREN_OPEN, PAREN_CLOSE, OPERATOR, NUMBER, DOT, BACKSPACE, EQUALS }

    private val keys = listOf(
        Key("C", Kind.CLEAR), Key("(", Kind.PAREN_OPEN), Key(")", Kind.PAREN_CLOSE), Key("÷", Kind.OPERATOR),
        Key("7", Kind.NUMBER), Key("8", Kind.NUMBER), Key("9", Kind.NUMBER), Key("×", Kind.OPERATOR),
        Key("4", Kind.NUMBER), Key("5", Kind.NUMBER), Key("6", Kind.NUMBER), Key("−", Kind.OPERATOR),
        Key("1", Kind.NUMBER), Key("2", Kind.NUMBER), Key("3", Kind.NUMBER), Key("+", Kind.OPERATOR),
        Key("", Kind.BACKSPACE, R.drawable.ic_backspace), Key("0", Kind.NUMBER), Key(".", Kind.DOT), Key("=", Kind.EQUALS)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        buildKeypad()
    }

    private fun buildKeypad() {
        binding.keypad.removeAllViews()
        val density = resources.displayMetrics.density
        val gapPx = (10 * density).toInt()

        keys.forEach { key ->
            val cell = GridLayout.LayoutParams()
            cell.width = 0
            cell.height = (68 * density).toInt()
            cell.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            cell.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            cell.setMargins(gapPx / 2, gapPx / 2, gapPx / 2, gapPx / 2)

            val bgRes = when (key.kind) {
                Kind.EQUALS -> R.drawable.bg_calc_key_equals
                Kind.OPERATOR, Kind.PAREN_OPEN, Kind.PAREN_CLOSE, Kind.CLEAR, Kind.BACKSPACE -> R.drawable.bg_calc_key_operator
                else -> R.drawable.bg_calc_key_number
            }

            val textColorRes = when (key.kind) {
                Kind.EQUALS -> R.color.primaryForeground
                Kind.OPERATOR, Kind.PAREN_OPEN, Kind.PAREN_CLOSE, Kind.CLEAR -> R.color.secondaryForeground
                else -> R.color.foreground
            }

            if (key.kind == Kind.BACKSPACE) {
                val button = ImageButton(this)
                button.layoutParams = cell
                button.setBackgroundResource(bgRes)
                button.setImageResource(key.icon)
                button.setColorFilter(ContextCompat.getColor(this, R.color.foreground))
                button.setOnClickListener { onKeyPress(key) }
                binding.keypad.addView(button)
            } else {
                val button = TextView(this)
                button.layoutParams = cell
                button.setBackgroundResource(bgRes)
                button.text = key.label
                button.gravity = Gravity.CENTER
                button.textSize = 23f
                button.setTypeface(button.typeface, android.graphics.Typeface.BOLD)
                button.setTextColor(ContextCompat.getColor(this, textColorRes))
                button.setOnClickListener { onKeyPress(key) }
                binding.keypad.addView(button)
            }
        }
    }

    private fun onKeyPress(key: Key) {
        when (key.kind) {
            Kind.CLEAR -> {
                expression = ""
                updateDisplay("0")
            }
            Kind.BACKSPACE -> {
                if (expression.isNotEmpty()) expression = expression.dropLast(1)
                updateDisplay(expression.ifEmpty { "0" })
            }
            Kind.EQUALS -> {
                val result = evaluate(expression)
                updateDisplay(result)
                expression = if (result == ERROR) "" else result
            }
            else -> {
                expression += key.label
                updateDisplay(expression)
            }
        }
    }

    private fun updateDisplay(result: String) {
        binding.resultText.text = result
        binding.expressionText.text = expression.ifEmpty { getString(R.string.calculator_ready) }
    }

    private fun evaluate(expr: String): String {
        if (expr.isBlank()) return "0"
        val normalized = expr.replace('×', '*').replace('÷', '/').replace('−', '-')
        if (!normalized.matches(Regex("^[0-9+\\-*/%.()\\s]+$"))) return ERROR

        return try {
            val tokens = tokenize(normalized)
            val result = evalTokens(tokens)
            if (result.isNaN() || result.isInfinite()) ERROR else formatResult(result)
        } catch (e: Exception) {
            ERROR
        }
    }

    private fun tokenize(expr: String): List<String> {
        val regex = Regex("(\\d*\\.?\\d+)|[()+\\-*/%]")
        return regex.findAll(expr).map { it.value }.toList()
    }

    private fun evalTokens(tokens: List<String>): Double {
        val values = ArrayDeque<Double>()
        val operators = ArrayDeque<String>()
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "%" to 2)

        fun apply() {
            val operator = operators.removeLast()
            val right = values.removeLast()
            val left = values.removeLast()
            values.addLast(
                when (operator) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> if (right == 0.0) Double.NaN else left / right
                    "%" -> left % right
                    else -> throw IllegalStateException("bad op")
                }
            )
        }

        for (token in tokens) {
            val number = token.toDoubleOrNull()
            when {
                number != null -> values.addLast(number)
                token == "(" -> operators.addLast(token)
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") apply()
                    if (operators.isNotEmpty()) operators.removeLast()
                }
                else -> {
                    while (operators.isNotEmpty() && operators.last() != "(" &&
                        (precedence[operators.last()] ?: 0) >= (precedence[token] ?: 0)
                    ) apply()
                    operators.addLast(token)
                }
            }
        }
        while (operators.isNotEmpty()) apply()
        return values.lastOrNull() ?: Double.NaN
    }

    private fun formatResult(value: Double): String {
        val rounded = (value * 100000000.0).roundToLong() / 100000000.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    companion object {
        private const val ERROR = "Error"
    }
}
