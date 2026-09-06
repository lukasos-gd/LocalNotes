package com.lukasosstudios.localnotes.ui.common

import com.lukasosstudios.localnotes.databinding.IncludeNumericKeypadBinding

object NumericKeypadBinder {
    fun bind(
        keypad: IncludeNumericKeypadBinding,
        onDigit: (String) -> Unit,
        onBackspace: () -> Unit,
        onDone: () -> Unit
    ) {
        val digitKeys = listOf(
            keypad.key0 to "0", keypad.key1 to "1", keypad.key2 to "2",
            keypad.key3 to "3", keypad.key4 to "4", keypad.key5 to "5",
            keypad.key6 to "6", keypad.key7 to "7", keypad.key8 to "8", keypad.key9 to "9"
        )
        digitKeys.forEach { (view, digit) -> view.setOnClickListener { onDigit(digit) } }
        keypad.keyBackspace.setOnClickListener { onBackspace() }
        keypad.keyDone.setOnClickListener { onDone() }
    }
}
