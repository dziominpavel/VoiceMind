package com.example.voicemind.ui.theme

import android.app.Activity
import android.content.Context
import android.view.HapticFeedbackConstants

enum class HapticType {
    Light,      // card tap, chip tap, toggle switch
    Medium,     // start/stop listening, save
    Heavy,      // delete, cancel alarm
    Success,    // save complete, mark done, snooze scheduled
    Toggle,     // switch on/off
}

object NeoWaveHaptics {
    fun perform(context: Context, type: HapticType) {
        val view = (context as? Activity)?.window?.decorView?.rootView ?: return
        val feedback = when (type) {
            HapticType.Light -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticType.Medium -> HapticFeedbackConstants.CONFIRM
            HapticType.Heavy -> HapticFeedbackConstants.REJECT
            HapticType.Success -> HapticFeedbackConstants.CONFIRM
            HapticType.Toggle -> HapticFeedbackConstants.TOGGLE_ON
        }
        view.performHapticFeedback(feedback)
    }
}
