package com.example.voicemind

import android.app.Application
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.notification.NotificationChannels
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
        }
    }
}
