package com.example.voicemind.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

object SpeechRecognition {

    fun isAvailable(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return false
        return recognizerIntent().resolveActivity(appContext.packageManager) != null
    }

    fun recognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // longer silence timeout so the user can finish longer phrases
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_500)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500)
    }

    fun createRecognizer(context: Context): SpeechRecognizer {
        val appContext = context.applicationContext
        // Prefer the standard (cloud / OEM) recognizer; on-device can be
        // non-functional on devices without GMS (e.g. Huawei).
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }
                .getOrElse {
                    runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext) }
                        .getOrElse { SpeechRecognizer.createSpeechRecognizer(appContext) }
                }
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
    }
}
