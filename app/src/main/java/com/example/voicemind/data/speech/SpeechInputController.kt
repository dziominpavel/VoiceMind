package com.example.voicemind.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechInputController(
    context: Context,
    private val onPartial: (String) -> Unit = {},
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Распознавание речи недоступно на устройстве")
            return
        }
        stopListening()
        val sr = SpeechRecognizer.createSpeechRecognizer(appContext).also { recognizer = it }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.text()?.let { onPartial(it) }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.text()
                if (text.isNullOrBlank()) {
                    onError("Не удалось распознать речь")
                } else {
                    onResult(text)
                }
                stopListening()
            }
            override fun onError(error: Int) {
                onError(errorMessage(error))
                stopListening()
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        sr.startListening(recognizerIntent())
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "stopListening", e)
        }
        recognizer = null
    }

    private fun recognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun Bundle.text(): String? =
        getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Ошибка записи звука"
        SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента распознавания"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон"
        SpeechRecognizer.ERROR_NETWORK -> "Нужна сеть для распознавания на этом устройстве"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
        SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана, попробуйте ещё раз"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание занято, подождите"
        SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера распознавания"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Нет речи, попробуйте ещё раз"
        else -> "Ошибка распознавания ($code)"
    }

    companion object {
        private const val TAG = "SpeechInputController"
    }
}
