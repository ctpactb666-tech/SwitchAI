package com.wstxda.switchai.wakeword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Real state of [WakeWordService], observed by the UI so that
 * "Активно" is only shown when the service is really listening.
 */
object WakeWordState {

    enum class Status {
        STOPPED,
        LISTENING,
        /** On-device speech recognition is not available (Android < 12 or no offline model). */
        UNAVAILABLE,
        /** Too many failed owner-voice checks; unlock the phone to reset. */
        LOCKED,
    }

    private val _status = MutableLiveData(Status.STOPPED)
    val status: LiveData<Status> = _status

    val current: Status get() = _status.value ?: Status.STOPPED

    fun set(status: Status) {
        _status.postValue(status)
    }
}
