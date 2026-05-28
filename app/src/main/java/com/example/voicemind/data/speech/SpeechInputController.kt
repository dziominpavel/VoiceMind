package com.example.voicemind.data.speech

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechInputController(
    context: Context,
    private val onPartial: (String) -> Unit = {},
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var timeoutRunnable: Runnable? = null

    fun startListening() {
        if (!SpeechRecognition.isAvailable(appContext)) {
            onError("Распознавание речи недоступно на устройстве")
            return
        }
        mainHandler.post {
            releaseRecognizer()
            val sr = try {
                SpeechRecognition.createRecognizer(appContext).also { recognizer = it }
            } catch (e: Exception) {
                Log.e(TAG, "createRecognizer failed", e)
                onError("Не удалось создать распознаватель")
                return@post
            }
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech")
                }
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech")
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    Log.d(TAG, "onPartialResults: ${partialResults?.text()}")
                    partialResults?.text()?.let { onPartial(it) }
                }
                override fun onResults(results: Bundle?) {
                    Log.d(TAG, "onResults: ${results?.text()}")
                    cancelTimeout()
                    val text = results?.text()
                    if (text.isNullOrBlank()) {
                        onError("Не удалось распознать речь")
                    } else {
                        onResult(text)
                    }
                    stopListening()
                }
                override fun onError(error: Int) {
                    Log.w(TAG, "onError code=$error")
                    cancelTimeout()
                    onError(errorMessage(error))
                    stopListening()
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            try {
                sr.startListening(SpeechRecognition.recognizerIntent())
                scheduleTimeout()
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                onError("Ошибка запуска распознавания")
                releaseRecognizer()
            }
        }
    }

    fun stopListening() {
        cancelTimeout()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            releaseRecognizer()
        } else {
            mainHandler.post { releaseRecognizer() }
        }
    }

    private fun releaseRecognizer() {
        cancelTimeout()
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "releaseRecognizer", e)
        }
        recognizer = null
    }

    private fun Bundle.text(): String? =
        getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()

    private fun scheduleTimeout() {
        cancelTimeout()
        val runnable = Runnable {
            Log.w(TAG, "timeout: no result from SpeechRecognizer")
            onError("Таймаут распознавания — устройство не ответило")
            stopListening()
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, 10_000)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

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
