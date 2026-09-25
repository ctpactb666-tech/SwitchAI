package com.wstxda.switchai.wakeword

data class AudioFrame(
    val samples: FloatArray,
    val sampleRate: Int,
    val timestampMs: Long = System.currentTimeMillis(),
)
