package com.wstxda.switchai.wakeword

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class OwnerVoiceSecurity(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    val isLocked: Boolean
        get() = prefs.getBoolean(KEY_LOCKED, false)

    fun onOwnerVerified() {
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putBoolean(KEY_LOCKED, false)
        }
    }

    fun onOwnerRejected(): Result {
        if (isLocked) return Result.Locked

        val next = failedAttempts + 1
        val locked = next >= MAX_FAILED_ATTEMPTS
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, next)
            putBoolean(KEY_LOCKED, locked)
        }

        return if (locked) Result.Locked else Result.Rejected(next)
    }

    fun resetAfterDeviceUnlock() {
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putBoolean(KEY_LOCKED, false)
        }
    }

    sealed interface Result {
        data class Rejected(val attempts: Int) : Result
        data object Locked : Result
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 3
        private const val KEY_FAILED_ATTEMPTS = "owner_voice_failed_attempts"
        private const val KEY_LOCKED = "owner_voice_locked"
    }
}
