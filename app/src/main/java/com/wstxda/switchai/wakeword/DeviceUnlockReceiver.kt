package com.wstxda.switchai.wakeword

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeviceUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT ||
            intent.action == Intent.ACTION_USER_UNLOCKED
        ) {
            OwnerVoiceSecurity(context).resetAfterDeviceUnlock()
        }
    }
}
