package com.wstxda.switchai.wakeword

interface SpeakerEmbeddingEngine {
    val isReady: Boolean
    fun compute(samples: FloatArray, sampleRate: Int): FloatArray?
}
