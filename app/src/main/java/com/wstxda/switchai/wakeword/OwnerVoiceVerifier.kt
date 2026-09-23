package com.wstxda.switchai.wakeword

import android.content.Context

class OwnerVoiceVerifier(context: Context) {

    private val profileStore = OwnerVoiceProfileStore(context)

    fun hasProfile(): Boolean = profileStore.isReady()

    fun verify(candidateEmbedding: FloatArray?): Verification {
        val profile = profileStore.load() ?: return Verification.NoProfile
        val candidate = candidateEmbedding ?: return Verification.NoCandidate

        val score = OwnerVoiceProfileStore.cosineSimilarity(profile, candidate)
        return if (score >= DEFAULT_THRESHOLD) {
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
        // Initial threshold. It will be exposed as an advanced setting after
        // device testing with the selected speaker-embedding model.
        const val DEFAULT_THRESHOLD = 0.72f
    }
}
