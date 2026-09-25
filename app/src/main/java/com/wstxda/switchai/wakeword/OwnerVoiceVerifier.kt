package com.wstxda.switchai.wakeword

import android.content.Context

class OwnerVoiceVerifier(private val context: Context) {

    private val profileStore = OwnerVoiceProfileStore(context)

    fun hasProfile(): Boolean = profileStore.isReady()

    fun verify(candidateEmbedding: FloatArray?): Verification {
        val profile = profileStore.load() ?: return Verification.NoProfile
        val candidate = candidateEmbedding ?: return Verification.NoCandidate

        val score = OwnerVoiceProfileStore.cosineSimilarity(profile, candidate)
        return if (score >= WakeWordSettings.ownerThreshold(context)) {
            Verification.Verified(score)
        } else {
            Verification.Rejected(score)
        }
    }

    sealed interface Verification {
        data class Verified(val score: Float) : Verification
        data class Rejected(val score: Float) : Verification
        data object NoProfile : Verification
        data object NoCandidate : Verification
    }

    companion object {
        // Fallback value; the real threshold comes from the "Чувствительность" slider.
        const val DEFAULT_THRESHOLD = 0.75f
    }
}
