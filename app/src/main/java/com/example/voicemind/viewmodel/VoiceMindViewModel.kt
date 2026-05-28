package com.example.voicemind.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.parse.ReminderParser
import com.example.voicemind.data.parse.isVoiceParseSuccessful
import com.example.voicemind.data.speech.SpeechInputController
import com.example.voicemind.util.ReminderPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class VoiceMindViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository.getInstance(application)
    private val settings = SettingsRepository.getInstance(application)
    private val parser = ReminderParser()

    private var speechController: SpeechInputController? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _listeningState = MutableStateFlow(ListeningState.Idle)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    /** Подтверждение после успешного голосового разбора. */
    private val _pendingConfirm = MutableStateFlow<PendingReminderConfirm?>(null)
    val pendingConfirm: StateFlow<PendingReminderConfirm?> = _pendingConfirm.asStateFlow()

    /** Ручной ввод / правка / fallback после голоса. */
    private val _manualDraft = MutableStateFlow<ManualReminderDraft?>(null)
    val manualDraft: StateFlow<ManualReminderDraft?> = _manualDraft.asStateFlow()

    private val _detailReminder = MutableStateFlow<Reminder?>(null)
    val detailReminder: StateFlow<Reminder?> = _detailReminder.asStateFlow()

    private val _listTab = MutableStateFlow(ReminderListTab.Upcoming)
    val listTab: StateFlow<ReminderListTab> = _listTab.asStateFlow()

    val upcomingReminders = repository.observeUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyReminders = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextReminder = upcomingReminders
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val defaultDeliveryMode = settings.defaultDeliveryMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeliveryMode.NOTIFICATION)

    val confirmBeforeSchedule = settings.confirmBeforeSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _fallbackToSystemSpeech = MutableStateFlow(false)
    val fallbackToSystemSpeech: StateFlow<Boolean> = _fallbackToSystemSpeech.asStateFlow()

    fun consumeFallbackToSystemSpeech() {
        _fallbackToSystemSpeech.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    /** UI «слушаю» без встроенного SpeechRecognizer (системный диалог). */
    fun beginExternalVoiceSession() {
        _listeningState.value = ListeningState.Listening
    }

    fun dismissConfirm() {
        _pendingConfirm.value = null
    }

    fun dismissManual() {
        _manualDraft.value = null
    }

    fun dismissDetail() {
        _detailReminder.value = null
    }

    fun setListTab(tab: ReminderListTab) {
        _listTab.value = tab
    }

    fun setDefaultDeliveryMode(mode: DeliveryMode) {
        viewModelScope.launch {
            settings.setDefaultDeliveryMode(mode)
        }
    }

    fun setConfirmBeforeSchedule(enabled: Boolean) {
        viewModelScope.launch {
            settings.setConfirmBeforeSchedule(enabled)
        }
    }

    /** Ручной режим: пустая форма с датой/временем по умолчанию. */
    fun openManualCreate() {
        _manualDraft.value = ManualReminderDraft(
            fireAtMillis = defaultFireAtMillis(),
            deliveryMode = defaultDeliveryMode.value,
        )
    }

    fun startListening() {
        Log.d(TAG, "startListening")
        if (_listeningState.value == ListeningState.Listening) {
            stopListening()
            return
        }
        _listeningState.value = ListeningState.Listening
        speechController = SpeechInputController(
            context = getApplication(),
            onResult = { onSpeechResult(it) },
            onError = { msg ->
                _errorMessage.value = msg
                _listeningState.value = ListeningState.Idle
                if (msg != "Нет разрешения на микрофон") {
                    _fallbackToSystemSpeech.value = true
                }
            },
        ).also { it.startListening() }
    }

    /** Результат из встроенного SpeechRecognizer или системного диалога распознавания. */
    fun onSpeechResult(text: String) {
        Log.d(TAG, "onSpeechResult: $text")
        _listeningState.value = ListeningState.Processing
        handleVoicePhrase(text)
        _listeningState.value = ListeningState.Idle
    }

    fun onSpeechSessionCancelled() {
        stopListening()
        _listeningState.value = ListeningState.Idle
    }

    fun stopListening() {
        speechController?.stopListening()
        speechController = null
        if (_listeningState.value == ListeningState.Listening) {
            _listeningState.value = ListeningState.Idle
        }
    }

    fun updatePending(
        body: String,
        fireAtMillis: Long?,
        deliveryMode: DeliveryMode,
    ) {
        val pending = _pendingConfirm.value ?: return
        _pendingConfirm.value = pending.copy(
            body = body,
            fireAtMillis = fireAtMillis,
            deliveryMode = deliveryMode,
        )
    }

    /** Сохранение после успешного голосового разбора (экран подтверждения). */
    fun confirmVoiceReminder() {
        val pending = _pendingConfirm.value ?: return
        saveReminder(
            body = pending.body,
            fireAtMillis = pending.fireAtMillis,
            deliveryMode = pending.deliveryMode,
            rawPhrase = pending.rawPhrase,
            editingId = null,
        ) { _pendingConfirm.value = null }
    }

    /** Сохранение из ручной формы. */
    fun saveManualReminder(
        body: String,
        fireAtMillis: Long?,
        deliveryMode: DeliveryMode,
    ) {
        val draft = _manualDraft.value ?: return
        saveReminder(
            body = body,
            fireAtMillis = fireAtMillis,
            deliveryMode = deliveryMode,
            rawPhrase = draft.rawPhrase,
            editingId = draft.editingReminderId,
        ) { _manualDraft.value = null }
    }

    fun cancelReminder(id: Long) {
        safeDb(getString(R.string.error_cancel_failed)) {
            repository.cancelReminder(id)
        }
    }

    fun openReminderForEdit(id: Long) {
        safeDb(getString(R.string.error_save_failed)) {
            val reminder = repository.getById(id) ?: return@safeDb
            if (reminder.status != ReminderStatus.SCHEDULED.name &&
                reminder.status != ReminderStatus.SNOOZED.name
            ) {
                return@safeDb
            }
            _manualDraft.value = manualDraftFromReminder(reminder)
        }
    }

    fun openReminderDetail(reminder: Reminder) {
        _detailReminder.value = reminder
    }

    private fun handleVoicePhrase(phrase: String) {
        Log.d(TAG, "handleVoicePhrase: $phrase")
        val trimmed = phrase.trim()
        if (trimmed.isEmpty()) {
            _errorMessage.value = getString(R.string.error_empty_phrase)
            return
        }
        val result = parser.parse(trimmed)
        if (result.isVoiceParseSuccessful()) {
            val pending = PendingReminderConfirm(
                rawPhrase = trimmed,
                body = result.body.trim(),
                fireAtMillis = result.fireAt?.toEpochMilli(),
                deliveryMode = defaultDeliveryMode.value,
                confidence = result.confidence,
                warnings = result.warnings,
            )
            if (!confirmBeforeSchedule.value && canAutoSave(pending)) {
                _pendingConfirm.value = pending
                confirmVoiceReminder()
            } else {
                _pendingConfirm.value = pending
            }
        } else {
            _manualDraft.value = ManualReminderDraft(
                body = result.body.ifBlank { trimmed },
                fireAtMillis = result.fireAt?.toEpochMilli() ?: defaultFireAtMillis(),
                deliveryMode = defaultDeliveryMode.value,
                rawPhrase = trimmed,
                fromVoiceParseFailure = true,
            )
        }
    }

    private fun saveReminder(
        body: String,
        fireAtMillis: Long?,
        deliveryMode: DeliveryMode,
        rawPhrase: String?,
        editingId: Long?,
        onSuccess: () -> Unit,
    ) {
        val fireAt = fireAtMillis
        if (fireAt == null) {
            _errorMessage.value = getString(R.string.error_no_time)
            return
        }
        if (body.isBlank()) {
            _errorMessage.value = getString(R.string.error_empty_body)
            return
        }
        if (fireAt <= System.currentTimeMillis()) {
            _errorMessage.value = getString(R.string.error_past_time)
            return
        }
        if (!ReminderPermissions.hasPostNotifications(getApplication())) {
            _errorMessage.value = getString(R.string.error_notifications_permission)
        } else if (!ReminderPermissions.canScheduleExactAlarms(getApplication())) {
            _errorMessage.value = getString(R.string.error_exact_alarm)
        }
        safeDb(getString(R.string.error_save_failed)) {
            if (editingId != null) {
                val existing = repository.getById(editingId) ?: return@safeDb
                repository.updateAndSchedule(
                    existing.copy(
                        fireAt = fireAt,
                        body = body.trim(),
                        rawPhrase = rawPhrase ?: existing.rawPhrase,
                        deliveryMode = deliveryMode.name,
                    ),
                )
            } else {
                val now = System.currentTimeMillis()
                repository.insertAndSchedule(
                    Reminder(
                        clientId = UUID.randomUUID().toString(),
                        fireAt = fireAt,
                        body = body.trim(),
                        rawPhrase = rawPhrase,
                        deliveryMode = deliveryMode.name,
                        status = ReminderStatus.SCHEDULED.name,
                        createdAt = now,
                        alarmRequestCode = 0,
                    ),
                )
            }
            onSuccess()
        }
    }

    private fun manualDraftFromReminder(reminder: Reminder): ManualReminderDraft {
        val mode = runCatching { DeliveryMode.valueOf(reminder.deliveryMode) }
            .getOrDefault(DeliveryMode.NOTIFICATION)
        return ManualReminderDraft(
            body = reminder.body,
            fireAtMillis = reminder.fireAt,
            deliveryMode = mode,
            rawPhrase = reminder.rawPhrase,
            editingReminderId = reminder.id,
        )
    }

    private fun defaultFireAtMillis(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone)
            .plusDays(1)
            .atTime(LocalTime.of(9, 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun canAutoSave(pending: PendingReminderConfirm): Boolean {
        val fireAt = pending.fireAtMillis ?: return false
        return pending.body.isNotBlank() && fireAt > System.currentTimeMillis()
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private fun safeDb(errorFallback: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: errorFallback
                Log.e(TAG, errorFallback, e)
            }
        }
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    companion object {
        private const val TAG = "VoiceMindViewModel"
    }
}
