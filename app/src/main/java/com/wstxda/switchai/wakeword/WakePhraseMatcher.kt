package com.wstxda.switchai.wakeword

import java.text.Normalizer
import java.util.Locale

object WakePhraseMatcher {

    fun match(recognizedText: String, phrases: List<WakePhrase>): WakePhrase? {
        val spoken = normalize(recognizedText)
        if (spoken.isBlank()) return null

        // Longest variants first, so "Слушай, Gemini" wins over "Gemini".
        val candidates = phrases
            .filter { it.enabled }
            .flatMap { phrase -> phrase.variants.map { variant -> phrase to normalize(variant) } }
            .filter { (_, variant) -> variant.isNotBlank() }
            .sortedByDescending { (_, variant) -> variant.length }

        return candidates.firstOrNull { (phrase, variant) ->
            if (phrase.exactMatch) {
                spoken == variant
            } else {
                // Whole words only: "слушай алиса открой" matches "слушай алиса".
                " $spoken ".contains(" $variant ")
            }
        }?.first
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
