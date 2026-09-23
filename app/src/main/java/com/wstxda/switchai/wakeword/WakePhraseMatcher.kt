package com.wstxda.switchai.wakeword

import java.text.Normalizer
import java.util.Locale

object WakePhraseMatcher {
    fun match(recognizedText: String, phrases: List<WakePhrase>): WakePhrase? {
        val spoken = normalize(recognizedText)
        if (spoken.isBlank()) return null

        return phrases
            .asSequence()
            .filter { it.enabled && it.phrase.isNotBlank() }
            .sortedByDescending { it.phrase.length }
            .firstOrNull { normalize(it.phrase) == spoken }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
