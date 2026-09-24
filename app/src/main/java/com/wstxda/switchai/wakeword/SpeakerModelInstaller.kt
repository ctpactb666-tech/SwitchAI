package com.wstxda.switchai.wakeword

import android.content.Context
import java.io.File

object SpeakerModelInstaller {
    private const val ASSET_PATH = "models/3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx"

    fun ensureInstalled(context: Context): File {
        val target = File(context.filesDir, "models/${SherpaSpeakerEmbeddingEngine.MODEL_FILE_NAME}")
        if (target.isFile && target.length() > 0L) return target

        target.parentFile?.mkdirs()
        context.assets.open(ASSET_PATH).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return target
    }
}
