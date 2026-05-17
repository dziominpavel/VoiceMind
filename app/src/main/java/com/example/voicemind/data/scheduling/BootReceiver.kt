package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicemind.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderRepository.getInstance(context).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
