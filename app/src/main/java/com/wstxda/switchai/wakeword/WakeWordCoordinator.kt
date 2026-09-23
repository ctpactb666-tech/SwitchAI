package com.wstxda.switchai.wakeword

import android.content.Context

class WakeWordCoordinator(context: Context) {

    private val phraseStore = WakePhraseStore(context)
    private val verifier = OwnerVoiceVerifier(context)
    private val security = OwnerVoiceSecurity(context)
    private val appContext = context.applicationContext

    fun onRecognizedText(
        text: String,
        speakerEmbedding: FloatArray? = null,
    ): Result {
        if (security.isLocked) return Result.Locked

        val phrase = WakePhraseMatcher.match(text, phraseStore.load())
            ?: return Result.NoWakePhrase

        return when (val verification = verifier.verify(speakerEmbedding)) {
            is OwnerVoiceVerifier.Verification.Verified -> {
                security.onOwnerVerified()
                if (WakeAssistantLauncher.launch(appContext, phrase)) {
                    Result.Launched(phrase, verification.score)
                } else {
                    Result.AssistantUnavailable
                }
            }

            is OwnerVoiceVerifier.Verification.Rejected -> {
                when (val securityResult = security.onOwnerRejected()) {
                    OwnerVoiceSecurity.Result.Locked -> Result.Locked
                    is OwnerVoiceSecurity.Result.Rejected ->
                        Result.OwnerRejected(securityResult.attempts, verification.score)
                }
            }

            OwnerVoiceVerifier.Verification.NoProfile -> Result.OwnerProfileMissing
            OwnerVoiceVerifier.Verification.NoCandidate -> Result.OwnerSampleMissing
        }
    }

    sealed interface Result {
        data object NoWakePhrase : Result
        data class Launched(val phrase: WakePhrase, val ownerScore: Float) : Result
        data object AssistantUnavailable : Result
        data class OwnerRejected(val attempts: Int, val ownerScore: Float) : Result
        data object OwnerProfileMissing : Result
        data object OwnerSampleMissing : Result
        data object Locked : Result
    }
}
