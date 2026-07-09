package com.example.voicemind.data.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

object AlarmSoundPlayer {

    private var ringtone: Ringtone? = null
    private var previousVolume: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private const val AUTO_STOP_MS = 60_000L
    private val VIBRATE_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
    private const val TAG = "AlarmSoundPlayer"

    fun play(
        context: Context,
        customUriString: String? = null,
        volumePercent: Int = 100,
        withVibration: Boolean = true,
    ) {
        stop(context)
        try {
            val alarmUri = customUriString?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (volumePercent in 1..100 && audioManager != null) {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetVolume = (maxVolume * volumePercent / 100f).toInt().coerceIn(0, maxVolume)
                previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
            }

            ringtone = RingtoneManager.getRingtone(context, alarmUri).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    volume = 1.0f
                }
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }

            if (withVibration) {
                startVibration(context)
            }
            scheduleAutoStop(context)
        } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
            stop(context)
        }
    }

    fun playVibrationOnly(context: Context) {
        stop(context)
        try {
            startVibration(context)
            scheduleAutoStop(context)
        } catch (e: Exception) {
            Log.e(TAG, "playVibrationOnly failed", e)
            stop(context)
        }
    }

    private fun startVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(
                    VibrationEffect.createWaveform(VIBRATE_PATTERN, 0),
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(VIBRATE_PATTERN, 0)
            }
        }
    }

    private fun scheduleAutoStop(context: Context) {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = Runnable { stop(context.applicationContext) }
        handler.postDelayed(stopRunnable!!, AUTO_STOP_MS)
    }

    fun stop(context: Context) {
        try {
            stopRunnable?.let { handler.removeCallbacks(it) }
            stopRunnable = null

            ringtone?.stop()
            ringtone = null

            previousVolume?.let { saved ->
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0)
                previousVolume = null
            }

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(Vibrator::class.java)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "stop failed", e)
            ringtone = null
            previousVolume = null
            stopRunnable = null
        }
    }
}
