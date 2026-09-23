package com.wstxda.switchai.wakeword

import android.content.Context

/**
 * Adapter boundary for sherpa-onnx speaker embeddings.
 *
 * The native sherpa-onnx runtime/model is intentionally kept behind this
 * interface so the rest of SwitchAI does not depend directly on JNI details.
 */
class SherpaSpeakerEmbeddingEngine(
    private val context: Context,
) : SpeakerEmbeddingEngine {

    override val isReady: Boolean
        get() = false

    override fun compute(samples: FloatArray, sampleRate: Int): FloatArray? {
        // Native sherpa-onnx binding/model hookup lands here.
        // Until both JNI and model assets are present, fail closed.
        return null
    }
}
