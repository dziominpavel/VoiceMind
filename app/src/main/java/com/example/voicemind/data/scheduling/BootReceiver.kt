package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.ui.widget.WidgetRefreshWorker
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
                val repo = ReminderRepository.getInstance(context)
                repo.rescheduleAll()
                repo.fireOverdue()
                WidgetRefreshWorker.schedule(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
