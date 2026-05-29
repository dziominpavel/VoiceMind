package com.example.voicemind.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    suspend fun updateAll(context: Context) = withContext(Dispatchers.IO) {
        val manager = GlanceAppWidgetManager(context)
        MicWidget().let { widget ->
            manager.getGlanceIds(widget.javaClass).forEach { id ->
                widget.update(context, id)
            }
        }
        ReminderListWidget().let { widget ->
            manager.getGlanceIds(widget.javaClass).forEach { id ->
                widget.update(context, id)
            }
        }
    }
}
