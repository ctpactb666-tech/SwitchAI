package com.wstxda.switchai.wakeword

import android.content.Context
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.File

class SherpaSpeakerEmbeddingEngine(
    context: Context,
) : SpeakerEmbeddingEngine {

    private val modelFile = SpeakerModelInstaller.ensureInstalled(context)

    private val extractor: SpeakerEmbeddingExtractor? by lazy {
        if (!modelFile.isFile) {
            null
        } else {
            runCatching {
                SpeakerEmbeddingExtractor(
                    assetManager = null,
                    config = SpeakerEmbeddingExtractorConfig(
                        model = modelFile.absolutePath,
                        numThreads = 2,
                        debug = false,
                        provider = "cpu",
                    ),
                )
            }.getOrNull()
        }
    }

    override val isReady: Boolean
        get() = modelFile.isFile && extractor != null

    override fun compute(samples: FloatArray, sampleRate: Int): FloatArray? {
        if (samples.isEmpty() || sampleRate <= 0) return null
        val activeExtractor = extractor ?: return null

        return runCatching {
            val stream = activeExtractor.createStream()
            try {
                stream.acceptWaveform(samples, sampleRate)
                stream.inputFinished()
                if (!activeExtractor.isReady(stream)) return@runCatching null
                activeExtractor.compute(stream)
            } finally {
                stream.release()
            }
        }.getOrNull()
    }

    companion object {
        const val MODEL_FILE_NAME = "3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx"
    }
}
