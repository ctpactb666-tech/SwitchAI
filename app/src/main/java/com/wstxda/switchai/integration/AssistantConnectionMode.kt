package com.wstxda.switchai.integration

enum class AssistantConnectionMode(val value: String) {
    AUTO("auto"),
    APP("app"),
    EMBEDDED("embedded");

    companion object {
        fun from(value: String?): AssistantConnectionMode =
            entries.firstOrNull { it.value == value } ?: AUTO
    }
}
