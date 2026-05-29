package com.example.voicemind.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus

class ToggleReminderStatusAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[reminderIdKey] ?: return
        val currentStatus = parameters[currentStatusKey] ?: return
        val repo = ReminderRepository.getInstance(context)
        val reminder = repo.getById(id) ?: return

        if (
            currentStatus == ReminderStatus.SCHEDULED.name ||
            currentStatus == ReminderStatus.SNOOZED.name
        ) {
            repo.dismissReminder(id)
        } else {
            val now = System.currentTimeMillis()
            val newFireAt = (reminder.fireAt.takeIf { it > now } ?: (now + 3_600_000L))
            repo.updateAndSchedule(
                reminder.copy(
                    fireAt = newFireAt,
                    status = ReminderStatus.SCHEDULED.name,
                    snoozeCount = 0,
                ),
            )
        }
        WidgetUpdater.updateAll(context)
    }

    companion object {
        val reminderIdKey = ActionParameters.Key<Long>("reminder_id")
        val currentStatusKey = ActionParameters.Key<String>("current_status")
    }
}
