package com.wstxda.switchai.wakeword

import java.util.UUID

data class WakePhrase(
    val phrase: String,
    val assistantKey: String,
    val languageTag: String = "und",
    val enabled: Boolean = true,
    /** "Точное совпадение": the whole utterance must equal the phrase. */
    val exactMatch: Boolean = false,
    /** "Игнорировать в музыке": skip while media is playing (avoids false triggers from songs). */
    val ignoreWhenMusic: Boolean = true,
    /** "Работать в офлайн режиме": stored preference, recognition is on-device anyway. */
    val offline: Boolean = false,
    val id: String = UUID.randomUUID().toString(),
) {
    /** Several variants can be entered separated by ";" — e.g. "Слушай, Алиса; Эй, Алиса". */
    val variants: List<String>
        get() = phrase.split(';').map { it.trim() }.filter { it.isNotEmpty() }

    /** First variant — used for display in lists. */
    val displayText: String
        get() = variants.firstOrNull() ?: phrase
}
