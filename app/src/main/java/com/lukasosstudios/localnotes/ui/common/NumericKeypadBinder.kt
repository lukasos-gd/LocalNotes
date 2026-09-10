package com.lukasosstudios.localnotes.ui.common

import com.lukasosstudios.localnotes.databinding.IncludeNumericKeypadBinding
import com.lukasosstudios.localnotes.util.HapticUtils

object NumericKeypadBinder {
    fun bind(
        keypad: IncludeNumericKeypadBinding,
        onDigit: (String) -> Unit,
        onBackspace: () -> Unit,
        onDone: () -> Unit
    ) {
        val context = keypad.root.context
        val digitKeys = listOf(
            keypad.key0 to "0", keypad.key1 to "1", keypad.key2 to "2",
            keypad.key3 to "3", keypad.key4 to "4", keypad.key5 to "5",
            keypad.key6 to "6", keypad.key7 to "7", keypad.key8 to "8", keypad.key9 to "9"
        )
        digitKeys.forEach { (view, digit) ->
            view.setOnClickListener {
                HapticUtils.tick(context)
                onDigit(digit)
            }
        }
        keypad.keyBackspace.setOnClickListener {
            HapticUtils.tick(context)
            onBackspace()
        }
        keypad.keyDone.setOnClickListener {
            HapticUtils.tick(context)
            onDone()
        }
    }
}
