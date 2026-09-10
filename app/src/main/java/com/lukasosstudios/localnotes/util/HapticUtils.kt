package com.lukasosstudios.localnotes.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Some OEM skins (MIUI in particular) gate View.performHapticFeedback()
 * behind their own "touch vibration" toggle, which is often off by default.
 * Going straight to the Vibrator service sidesteps that -- but a raw,
 * generic one-shot pulse can *still* be barely perceptible on some hardware
 * if it's short or the amplitude the HAL picks for "default" is weak. Where
 * available (API 29+), this uses Android's *predefined* effects instead --
 * these are calibrated per-device by the OEM against the actual haptic
 * actuator, so they come through far more reliably than a hand-picked
 * duration/amplitude pair ever can.
 */
object HapticUtils {

    fun tick(context: Context) = perform(context, Kind.TICK)

    fun confirm(context: Context) = perform(context, Kind.CONFIRM)

    fun error(context: Context) = perform(context, Kind.ERROR)

    private enum class Kind { TICK, CONFIRM, ERROR }

    private fun perform(context: Context, kind: Kind) {
        try {
            val vibrator = vibratorFor(context)
            if (!vibrator.hasVibrator()) return

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val effect = when (kind) {
                        Kind.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        Kind.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        Kind.ERROR -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    }
                    vibrator.vibrate(effect)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // No OEM-calibrated effects available -- go long and at full
                    // amplitude rather than trusting "default" to be strong enough.
                    when (kind) {
                        Kind.TICK -> vibrator.vibrate(VibrationEffect.createOneShot(30, 255))
                        Kind.CONFIRM -> vibrator.vibrate(VibrationEffect.createOneShot(60, 255))
                        Kind.ERROR -> vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 50, 80, 50), intArrayOf(0, 255, 0, 255), -1)
                        )
                    }
                }

                else -> {
                    @Suppress("DEPRECATION")
                    when (kind) {
                        Kind.TICK -> vibrator.vibrate(30)
                        Kind.CONFIRM -> vibrator.vibrate(60)
                        Kind.ERROR -> vibrator.vibrate(longArrayOf(0, 50, 80, 50), -1)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun vibratorFor(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
