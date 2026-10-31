package com.example.voicemind.data.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.voicemind.R
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelsAndroidTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        NotificationChannels.createAll(context)
    }

    @After
    fun tearDown() {
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(SECOND_NOTIFICATION_ID)
    }

    @Test
    fun createAll_keepsBothVibrateChannelNotifications() {
        postNotification(NOTIFICATION_ID, "first")
        postNotification(SECOND_NOTIFICATION_ID, "second")
        assertTrue(awaitActiveNotification(NOTIFICATION_ID))
        assertTrue(awaitActiveNotification(SECOND_NOTIFICATION_ID))

        NotificationChannels.createAll(context)

        assertTrue(awaitActiveNotification(NOTIFICATION_ID))
        assertTrue(awaitActiveNotification(SECOND_NOTIFICATION_ID))
    }

    private fun postNotification(id: Int, text: String) {
        manager.notify(
            id,
            NotificationCompat.Builder(context, NotificationChannels.VIBRATE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VoiceMind")
                .setContentText(text)
                .build(),
        )
    }

    private fun awaitActiveNotification(id: Int): Boolean {
        val deadline = System.currentTimeMillis() + 2_000L
        while (System.currentTimeMillis() < deadline) {
            if (manager.activeNotifications.any { it.id == id }) {
                return true
            }
            Thread.sleep(50)
        }
        return false
    }

    companion object {
        private const val NOTIFICATION_ID = 424242
        private const val SECOND_NOTIFICATION_ID = 424243
    }
}
