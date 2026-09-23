package com.wstxda.switchai.wakeword

import android.content.Context
import androidx.preference.PreferenceManager

class OwnerVoiceVerifier(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun hasProfile(): Boolean =
        prefs.getBoolean(KEY_PROFILE_READY, false)

    fun markProfileReady(ready: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_READY, ready).apply()
    }

    /**
     * Speaker embedding verification is intentionally fail-closed.
     * Until the local speaker model is connected and enrollment has produced
     * a valid profile, no wake phrase is allowed to launch an assistant.
     */
    fun verifyLatestUtterance(): Verification =
        if (hasProfile()) Verification.ModelRequired else Verification.NoProfile

    sealed interface Verification {
        data object Verified : Verification
        data object Rejected : Verification
        data object NoProfile : Verification
        data object ModelRequired : Verification
    }

    companion object {
        private const val KEY_PROFILE_READY = "owner_voice_profile_ready"
    }
}
