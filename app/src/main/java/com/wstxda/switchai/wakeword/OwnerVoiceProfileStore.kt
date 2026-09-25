package com.wstxda.switchai.wakeword

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.json.JSONArray
import kotlin.math.sqrt

class OwnerVoiceProfileStore(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun save(samples: List<FloatArray>): Boolean {
        if (samples.size < OwnerVoiceEnrollment.REQUIRED_SAMPLES) return false
        val dim = samples.firstOrNull()?.size ?: return false
        if (dim == 0 || samples.any { it.size != dim }) return false

        val mean = FloatArray(dim)
        samples.forEach { sample ->
            for (i in 0 until dim) mean[i] += sample[i]
        }
        for (i in mean.indices) mean[i] /= samples.size.toFloat()

        val normalized = normalize(mean)
        val array = JSONArray()
        normalized.forEach { array.put(it.toDouble()) }

        prefs.edit {
            putString(KEY_PROFILE, array.toString())
            putInt(KEY_DIM, dim)
            putBoolean(KEY_READY, true)
        }
        return true
    }

    fun load(): FloatArray? {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            FloatArray(array.length()) { index -> array.getDouble(index).toFloat() }
        }.getOrNull()
    }

    fun clear() {
        prefs.edit {
            remove(KEY_PROFILE)
            remove(KEY_DIM)
            putBoolean(KEY_READY, false)
        }
    }

    fun isReady(): Boolean = prefs.getBoolean(KEY_READY, false) && load() != null

    companion object {
        private const val KEY_PROFILE = "owner_voice_embedding"
        private const val KEY_DIM = "owner_voice_embedding_dim"
        private const val KEY_READY = "owner_voice_profile_ready"

        fun normalize(vector: FloatArray): FloatArray {
            val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
            if (norm <= 0f) return vector.copyOf()
            return FloatArray(vector.size) { index -> vector[index] / norm }
        }

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size || a.isEmpty()) return -1f
            val na = normalize(a)
            val nb = normalize(b)
            var dot = 0f
            for (i in na.indices) dot += na[i] * nb[i]
            return dot
        }
    }
}
