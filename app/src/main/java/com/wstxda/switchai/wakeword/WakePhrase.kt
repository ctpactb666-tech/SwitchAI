package com.wstxda.switchai.wakeword

data class WakePhrase(
    val phrase: String,
    val assistantKey: String,
    val languageTag: String = "und",
    val enabled: Boolean = true,
)
