package com.example.voicemind

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.notification.NotificationChannels
import com.example.voicemind.ui.widget.ScreenOnReceiver
import com.example.voicemind.ui.widget.WidgetRefreshWorker
import com.example.voicemind.ui.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoiceMindApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        appScope.launch {
            ReminderRepository.getInstance(this@VoiceMindApplication).rescheduleAll()
            WidgetUpdater.updateAll(this@VoiceMindApplication)
        }
        WidgetRefreshWorker.schedule(this)
        registerReceiver(ScreenOnReceiver(), IntentFilter(Intent.ACTION_SCREEN_ON))
    }
}
