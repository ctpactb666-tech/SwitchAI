package com.wstxda.switchai.wakeword

import android.content.Context

class OwnerVoiceEnrollmentSession(context: Context) {
    private val samples = mutableListOf<FloatArray>()
    private val profileStore = OwnerVoiceProfileStore(context)

    val completedSamples: Int
        get() = samples.size

    val requiredSamples: Int
        get() = OwnerVoiceEnrollment.REQUIRED_SAMPLES

    val isComplete: Boolean
        get() = completedSamples >= requiredSamples

    fun addSample(embedding: FloatArray): AddResult {
        if (embedding.isEmpty()) return AddResult.Invalid

        val normalized = OwnerVoiceProfileStore.normalize(embedding)
        samples += normalized
        return if (isComplete) AddResult.Complete else AddResult.Accepted(
            completedSamples,
            requiredSamples,
        )
    }

    fun finish(): Boolean = isComplete && profileStore.save(samples)

    fun reset() {
        samples.clear()
    }

    sealed interface AddResult {
        data class Accepted(val completed: Int, val required: Int) : AddResult
        data object Complete : AddResult
        data object TooSimilar : AddResult
        data object Invalid : AddResult
    }

}
