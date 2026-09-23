package com.wstxda.switchai.wakeword

import android.content.Context

class WakeWordCoordinator(context: Context) {

    private val phraseStore = WakePhraseStore(context)
    private val verifier = OwnerVoiceVerifier(context)
    private val security = OwnerVoiceSecurity(context)
    private val appContext = context.applicationContext

    fun onRecognizedText(text: String): Result {
        if (security.isLocked) return Result.Locked

        val phrase = WakePhraseMatcher.match(text, phraseStore.load())
            ?: return Result.NoWakePhrase

        return when (verifier.verifyLatestUtterance()) {
            OwnerVoiceVerifier.Verification.Verified -> {
                security.onOwnerVerified()
                if (WakeAssistantLauncher.launch(appContext, phrase)) {
                    Result.Launched(phrase)
                } else {
                    Result.AssistantUnavailable
                }
            }

            OwnerVoiceVerifier.Verification.Rejected -> {
                when (val securityResult = security.onOwnerRejected()) {
                    OwnerVoiceSecurity.Result.Locked -> Result.Locked
                    is OwnerVoiceSecurity.Result.Rejected ->
                        Result.OwnerRejected(securityResult.attempts)
                }
            }

            OwnerVoiceVerifier.Verification.NoProfile -> Result.OwnerProfileMissing
            OwnerVoiceVerifier.Verification.ModelRequired -> Result.OwnerModelNotReady
        }
    }

    sealed interface Result {
        data object NoWakePhrase : Result
        data class Launched(val phrase: WakePhrase) : Result
        data object AssistantUnavailable : Result
        data class OwnerRejected(val attempts: Int) : Result
        data object OwnerProfileMissing : Result
        data object OwnerModelNotReady : Result
        data object Locked : Result
    }
}
