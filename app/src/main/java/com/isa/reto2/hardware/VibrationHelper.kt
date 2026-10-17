package com.isa.reto2.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper para gestionar el acceso de hardware a la vibración del teléfono.
 * Compatible con versiones recientes (Android 12/API 31+ con VibratorManager)
 * y versiones anteriores mediante Vibrator.
 */
class VibrationHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Verifica si el dispositivo cuenta con un motor de vibración.
     */
    fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() == true
    }

    /**
     * Vibración corta para feedback táctil suave (e.g. click de botón).
     */
    fun vibrateClick(milliseconds: Long = 50) {
        vibrateOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    /**
     * Vibración de confirmación o éxito (doble pulso).
     */
    fun vibrateSuccess() {
        val timings = longArrayOf(0, 80, 60, 100)
        val amplitudes = intArrayOf(0, 150, 0, 255)
        vibrateWaveform(timings, amplitudes, -1)
    }

    /**
     * Vibración de advertencia o error (3 pulsos rápidos).
     */
    fun vibrateError() {
        val timings = longArrayOf(0, 100, 50, 100, 50, 150)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        vibrateWaveform(timings, amplitudes, -1)
    }

    /**
     * Vibración de disparo único con duración y amplitud personalizada.
     */
    fun vibrateOneShot(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
        v.vibrate(effect)
    }

    /**
     * Vibración con patrón personalizado (Waveform).
     */
    fun vibrateWaveform(timings: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(timings, amplitudes, repeat)
        v.vibrate(effect)
    }

    /**
     * Cancela la vibración activa.
     */
    fun cancel() {
        vibrator?.cancel()
    }
}
