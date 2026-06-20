package com.example.voicemind.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.notification.AlarmSoundPlayer
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.VoiceMindTheme
import com.example.voicemind.viewmodel.VoiceMindViewModel

class AlarmActivity : ComponentActivity() {

    private val viewModel: VoiceMindViewModel by viewModels()

    /** Текущий reminderId — обновляется в onCreate и onNewIntent. */
    private var reminderIdState by mutableStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        reminderIdState = reminderId

        // Extras из fullScreenIntent (screen-off путь) — используются как placeholder
        // до загрузки данных через ViewModel.
        val initialBody = intent.getStringExtra(EXTRA_REMINDER_BODY)
        val initialFireAt = intent.getLongExtra(EXTRA_REMINDER_FIRE_AT, 0L)

        setContent {
            VoiceMindTheme {
                AlarmActivityContent(
                    reminderId = reminderIdState,
                    initialBody = initialBody,
                    initialFireAt = initialFireAt,
                    viewModel = viewModel,
                    onDone = { id ->
                        AlarmSoundPlayer.stop(this)
                        viewModel.completeReminder(id)
                        finish()
                    },
                    onSnooze = { id, minutes ->
                        AlarmSoundPlayer.stop(this)
                        viewModel.snoozeReminder(id, minutes)
                        finish()
                    },
                    onCancel = { id ->
                        AlarmSoundPlayer.stop(this)
                        viewModel.cancelReminder(id)
                        finish()
                    },
                    onFinish = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Если активность завершается (после нажатия Готово/Отменить/Отложить),
        // игнорируем новый intent — звук и статус обработает корутина ресивера и notification.
        if (isFinishing) return
        val newId = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: return
        if (newId < 0) return
        reminderIdState = newId
        // Звук перезапускается корутиной ReminderAlarmReceiver (AlarmSoundPlayer.play
        // вызывает stop() внутри, затем стартует с новыми параметрами).
    }

    override fun onPause() {
        super.onPause()
        AlarmSoundPlayer.stop(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundPlayer.stop(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlarmSoundPlayer.stop(this)
        viewModel.cancelReminder(reminderIdState)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_BODY = "reminder_body"
        const val EXTRA_REMINDER_FIRE_AT = "reminder_fire_at"
    }
}

/**
 * Контент AlarmActivity. Загружает [Reminder] через [viewModel] по [reminderId].
 *
 * - Пока данные грузятся и нет extras (прямой startActivity из ресивера): ничего не показываем.
 * - Пока данные грузятся, но есть extras (fullScreenIntent из notification): показываем placeholder.
 * - После загрузки: если deliveryMode ALARM/VIBRATE → полноэкранный UI; если NOTIFICATION/SILENT → finish().
 */
@Composable
private fun AlarmActivityContent(
    reminderId: Long,
    initialBody: String?,
    initialFireAt: Long,
    viewModel: VoiceMindViewModel,
    onDone: (Long) -> Unit,
    onSnooze: (Long, Int) -> Unit,
    onCancel: (Long) -> Unit,
    onFinish: () -> Unit,
) {
    var reminder by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(reminderId) {
        if (reminderId >= 0) {
            reminder = viewModel.loadReminderForAlarm(reminderId)
        }
    }

    val loaded = reminder
    if (loaded == null) {
        // Загрузка ещё идёт — если есть extras из fullScreenIntent, показываем их как placeholder.
        if (initialBody != null && initialFireAt > 0) {
            AlarmScreen(
                body = initialBody,
                fireAt = initialFireAt,
                onDone = { onDone(reminderId) },
                onSnooze = { m -> onSnooze(reminderId, m) },
                onCancel = { onCancel(reminderId) },
            )
        }
        // Иначе — пустой экран (тёмный фон темы), ждём загрузку.
        return
    }

    // Проверяем deliveryMode напоминания.
    val mode = runCatching { DeliveryMode.valueOf(loaded.deliveryMode) }
        .getOrNull() ?: DeliveryMode.NOTIFICATION
    if (mode == DeliveryMode.NOTIFICATION || mode == DeliveryMode.SILENT) {
        // Для NOTIFICATION/SILENT полноэкранный UI не показываем — полагаемся на notification.
        LaunchedEffect(Unit) {
            onFinish()
        }
        return
    }

    AlarmScreen(
        body = loaded.body,
        fireAt = loaded.fireAt,
        onDone = { onDone(loaded.id) },
        onSnooze = { m -> onSnooze(loaded.id, m) },
        onCancel = { onCancel(loaded.id) },
    )
}

@Composable
fun AlarmScreen(
    body: String,
    fireAt: Long,
    onDone: () -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = FormatUtils.formatFireAt(fireAt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.xxxl))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.alarmPrimaryButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.alarm_screen_done),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.alarm_screen_snooze_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            listOf(5, 10, 15, 60).forEach { minutes ->
                OutlinedButton(
                    onClick = { onSnooze(minutes) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = when (minutes) {
                            5 -> stringResource(R.string.alarm_screen_snooze_5)
                            10 -> stringResource(R.string.alarm_screen_snooze_10)
                            15 -> stringResource(R.string.alarm_screen_snooze_15)
                            60 -> stringResource(R.string.alarm_screen_snooze_1h)
                            else -> "+$minutes"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.saveButtonHeight),
        ) {
            Text(
                text = stringResource(R.string.alarm_screen_cancel),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
