package com.example.voicemind.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.example.voicemind.MainActivity
import com.example.voicemind.R
import com.example.voicemind.data.AppDatabase
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus

class ReminderListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 40.dp),
            DpSize(320.dp, 40.dp),
            DpSize(400.dp, 56.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getInstance(context).reminderDao()
        val cutoff = System.currentTimeMillis() - 30 * 60_000L
        val recentDoneAll = dao.getWidgetRecentDone(cutoff = cutoff, limit = 5)
        val upcomingAll = dao.getWidgetUpcoming(limit = 5)

        provideContent {
            val size = LocalSize.current
            val maxItems = when {
                size.height >= 56.dp -> 4
                size.height >= 40.dp -> 3
                else -> 2
            }

            val recentDone = recentDoneAll.take(maxItems)
            val upcoming = upcomingAll.take(maxItems)

            val items = when {
                upcoming.isNotEmpty() && recentDone.isNotEmpty() ->
                    (listOf(recentDone.first()) + upcoming).take(maxItems)
                upcoming.isNotEmpty() -> upcoming.take(maxItems)
                else -> recentDone.take(maxItems)
            }

            GlanceTheme(colors = VoiceMindWidgetTheme.colors) {
                if (items.isEmpty()) {
                    EmptyState(context)
                } else {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            Column(
                                verticalAlignment = Alignment.Top,
                            ) {
                                items.forEach { reminder ->
                                    ReminderItem(context, reminder = reminder)
                                    if (reminder != items.last()) {
                                        Spacer(modifier = GlanceModifier.height(2.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(48.dp)
                                .background(GlanceTheme.colors.primary)
                                .cornerRadius(12.dp)
                                .clickable(
                                    actionStartActivity(
                                        Intent(context, MainActivity::class.java).apply {
                                            action = WidgetActions.ACTION_START_VOICE
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_mic),
                                contentDescription = context.getString(R.string.home_mic_start),
                                modifier = GlanceModifier.size(24.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState(context: Context) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                action = WidgetActions.ACTION_OPEN_LIST
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = context.getString(R.string.list_empty),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                action = WidgetActions.ACTION_START_VOICE
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_mic),
                    contentDescription = context.getString(R.string.home_mic_start),
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                )
            }
        }
    }

    @Composable
    private fun ReminderItem(context: Context, reminder: Reminder) {
        val isDone = reminder.status != ReminderStatus.PENDING.name
        val textColor = if (isDone) {
            GlanceTheme.colors.onSurfaceVariant
        } else {
            GlanceTheme.colors.onSurface
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reminder.body,
                style = TextStyle(
                    color = textColor,
                    fontSize = 11.sp,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                action = WidgetActions.ACTION_OPEN_REMINDER
                                putExtra(WidgetActions.EXTRA_REMINDER_ID, reminder.id)
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    ),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = FormatUtils.formatRelativeFireAt(reminder.fireAt),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Image(
                provider = ImageProvider(
                    if (isDone) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                ),
                contentDescription = "Toggle done",
                modifier = GlanceModifier
                    .size(22.dp)
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
