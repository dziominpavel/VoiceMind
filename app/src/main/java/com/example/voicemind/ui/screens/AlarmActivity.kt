package com.example.voicemind.ui.screens

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.notification.AlarmSoundPlayer
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.VoiceMindTheme
import com.example.voicemind.viewmodel.VoiceMindViewModel

class AlarmActivity : ComponentActivity() {

    private val viewModel: VoiceMindViewModel by viewModels()

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
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY) ?: ""
        val fireAt = intent.getLongExtra(EXTRA_REMINDER_FIRE_AT, 0L)

        AlarmSoundPlayer.stop(this)

        setContent {
            VoiceMindTheme {
                AlarmScreen(
                    body = body,
                    fireAt = fireAt,
                    onDone = {
                        AlarmSoundPlayer.stop(this)
                        viewModel.completeReminder(reminderId)
                        finish()
                    },
                    onSnooze = {
                        AlarmSoundPlayer.stop(this)
                        viewModel.snoozeReminder(reminderId, 10)
                        finish()
                    },
                    onCancel = {
                        AlarmSoundPlayer.stop(this)
                        viewModel.cancelReminder(reminderId)
                        finish()
                    },
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AlarmSoundPlayer.stop(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundPlayer.stop(this)
    }

    override fun onBackPressed() {
        AlarmSoundPlayer.stop(this)
        viewModel.cancelReminder(intent.getLongExtra(EXTRA_REMINDER_ID, -1L))
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_BODY = "reminder_body"
        const val EXTRA_REMINDER_FIRE_AT = "reminder_fire_at"
    }
}

@Composable
fun AlarmScreen(
    body: String,
    fireAt: Long,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
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
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.saveButtonHeight),
        ) {
            Text(
                text = stringResource(R.string.alarm_screen_snooze),
                style = MaterialTheme.typography.titleMedium,
            )
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
