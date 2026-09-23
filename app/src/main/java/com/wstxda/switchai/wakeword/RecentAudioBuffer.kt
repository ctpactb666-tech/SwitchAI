package com.wstxda.switchai.wakeword

import java.util.ArrayDeque

class RecentAudioBuffer(
    private val maxSamples: Int,
) {
    private val chunks = ArrayDeque<FloatArray>()
    private var totalSamples = 0

    @Synchronized
    fun append(samples: FloatArray) {
        if (samples.isEmpty()) return
        chunks.addLast(samples.copyOf())
        totalSamples += samples.size
        trim()
    }

    @Synchronized
    fun snapshot(): FloatArray {
        val out = FloatArray(totalSamples)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(out, destinationOffset = offset)
            offset += chunk.size
        }
        return out
    }

    @Synchronized
    fun clear() {
        chunks.clear()
        totalSamples = 0
    }

    private fun trim() {
        while (totalSamples > maxSamples && chunks.isNotEmpty()) {
            val first = chunks.removeFirst()
            totalSamples -= first.size
        }
    }
}
