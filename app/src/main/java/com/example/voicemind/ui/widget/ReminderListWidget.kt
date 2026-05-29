package com.example.voicemind.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.ColorFilter
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionParametersOf
import androidx.glance.text.TextDecoration
import com.example.voicemind.MainActivity
import com.example.voicemind.R
import com.example.voicemind.ui.widget.WidgetActions
import com.example.voicemind.data.AppDatabase
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus

class ReminderListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 40.dp),
            DpSize(250.dp, 180.dp),
            DpSize(320.dp, 320.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getInstance(context).reminderDao()
        val upcoming = dao.getAllScheduled().take(5)
        val history = if (upcoming.size < 5) {
            dao.getRecentHistory(5 - upcoming.size)
        } else emptyList()
        val allReminders = upcoming + history

        provideContent {
            val maxItems = when {
                LocalSize.current.width >= 320.dp && LocalSize.current.height >= 320.dp -> 5
                LocalSize.current.height >= 180.dp -> 3
                else -> 1
            }
            val reminders = allReminders.take(maxItems)

            GlanceTheme(colors = VoiceMindWidgetTheme.colors) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    Header(context)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (reminders.isEmpty()) {
                        EmptyState(context)
                    } else {
                        ReminderList(context, reminders)
                    }
                }
            }
        }
    }

    @Composable
    private fun Header(context: Context) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_list_title),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onBackground,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_mic),
                contentDescription = context.getString(R.string.home_mic_start),
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                action = WidgetActions.ACTION_START_VOICE
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    ),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
        }
    }

    @Composable
    private fun EmptyState(context: Context) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.list_empty),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }

    @Composable
    private fun ReminderList(context: Context, reminders: List<Reminder>) {
        LazyColumn {
            items(reminders) { reminder ->
                ReminderItem(context, reminder = reminder)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }

    @Composable
    private fun ReminderItem(context: Context, reminder: Reminder) {
        val isDone = reminder.status != ReminderStatus.SCHEDULED.name &&
            reminder.status != ReminderStatus.SNOOZED.name
        val textColor = if (isDone) {
            GlanceTheme.colors.onSurfaceVariant
        } else {
            GlanceTheme.colors.onSurface
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reminder.body,
                style = TextStyle(
                    color = textColor,
                    fontSize = 13.sp,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Image(
                provider = ImageProvider(
                    if (isDone) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                ),
                contentDescription = "Toggle done",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(
                        actionRunCallback<ToggleReminderStatusAction>(
                            actionParametersOf(
                                ToggleReminderStatusAction.reminderIdKey to reminder.id,
                                ToggleReminderStatusAction.currentStatusKey to reminder.status,
                            )
                        )
                    ),
            )
        }
    }
}
