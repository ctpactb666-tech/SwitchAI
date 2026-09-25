package com.wstxda.switchai.wakeword

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import androidx.preference.PreferenceManager
import com.wstxda.switchai.ui.catalog.AssistantUiPreferences

class WakeWordCoordinator(context: Context) {

    private val appContext = context.applicationContext
    private val phraseStore = WakePhraseStore(appContext)
    private val verifier = OwnerVoiceVerifier(appContext)
    private val security = OwnerVoiceSecurity(appContext)
    private val assistantPrefs = AssistantUiPreferences(appContext)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

    /** Partial and final results of one utterance must not launch the assistant twice. */
    private var lastLaunchAt = 0L

    fun onRecognizedText(
        text: String,
        speakerEmbedding: FloatArray? = null,
    ): Result {
        if (security.isLocked) return Result.Locked

        if (SystemClock.elapsedRealtime() - lastLaunchAt < DEBOUNCE_MS) return Result.Skipped

        // "Работать при заблокированном экране"
        if (!prefs.getBoolean(WakeWordSettings.KEY_LOCKED_SCREEN, true) && isDeviceLocked()) {
            return Result.Skipped
        }

        // "Использовать голосовой ввод" on the assistant settings screen
        val phrases = phraseStore.load().filter { assistantPrefs.wakeEnabled(it.assistantKey) }
        val phrase = WakePhraseMatcher.match(text, phrases) ?: return Result.NoWakePhrase

        // "Игнорировать в музыке"
        if (phrase.ignoreWhenMusic && isMusicPlaying()) return Result.Skipped

        if (!prefs.getBoolean(WakeWordSettings.KEY_OWNER_ONLY, true)) {
            return launch(phrase, ownerScore = null)
        }

        return when (val verification = verifier.verify(speakerEmbedding)) {
            is OwnerVoiceVerifier.Verification.Verified -> {
                security.onOwnerVerified()
                launch(phrase, verification.score)
            }

            is OwnerVoiceVerifier.Verification.Rejected -> {
                when (val securityResult = security.onOwnerRejected()) {
                    OwnerVoiceSecurity.Result.Locked -> Result.Locked
                    is OwnerVoiceSecurity.Result.Rejected ->
                        Result.OwnerRejected(securityResult.attempts, verification.score)
                }
            }

            // Voice was never trained: there is nothing to compare with, so the
            // owner check cannot apply. Previously this silently blocked every launch.
            OwnerVoiceVerifier.Verification.NoProfile -> launch(phrase, ownerScore = null)

            OwnerVoiceVerifier.Verification.NoCandidate -> Result.OwnerSampleMissing
        }
    }

    private fun launch(phrase: WakePhrase, ownerScore: Float?): Result {
        lastLaunchAt = SystemClock.elapsedRealtime()
        WakeFeedback.onPhraseRecognized(appContext)

        // "Автоматически начинать разговор": off → only a tap-to-start notification.
        if (!assistantPrefs.autoConversation(phrase.assistantKey)) {
            WakeLaunchNotifier.showTapToStart(appContext, phrase)
            return Result.Launched(phrase, ownerScore)
        }

        val started = WakeAssistantLauncher.launch(appContext, phrase)
        WakeLaunchNotifier.showConversation(appContext, phrase)
        return if (started) Result.Launched(phrase, ownerScore) else Result.AssistantUnavailable
    }

    private fun isDeviceLocked(): Boolean =
        appContext.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    private fun isMusicPlaying(): Boolean =
        appContext.getSystemService(AudioManager::class.java)?.isMusicActive == true

    sealed interface Result {
        data object NoWakePhrase : Result
        data object Skipped : Result
        data class Launched(val phrase: WakePhrase, val ownerScore: Float?) : Result
        data object AssistantUnavailable : Result
        data class OwnerRejected(val attempts: Int, val ownerScore: Float) : Result
        data object OwnerProfileMissing : Result
        data object OwnerSampleMissing : Result
        data object Locked : Result
    }

    companion object {
        private const val DEBOUNCE_MS = 4_000L
    }
}
