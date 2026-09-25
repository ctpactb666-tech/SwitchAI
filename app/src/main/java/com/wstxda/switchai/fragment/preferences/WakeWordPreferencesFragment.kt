package com.wstxda.switchai.fragment.preferences

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wstxda.switchai.R
import com.wstxda.switchai.fragment.BasePreferenceFragment
import com.wstxda.switchai.wakeword.OwnerVoiceEnrollment
import com.wstxda.switchai.wakeword.OwnerVoiceEnrollmentSession
import com.wstxda.switchai.wakeword.OwnerVoiceProfileStore
import com.wstxda.switchai.wakeword.OwnerVoiceVerifier
import com.wstxda.switchai.wakeword.SpeakerModelInstaller
import com.wstxda.switchai.wakeword.WakeAudioSession
import com.wstxda.switchai.wakeword.WakePhrase
import com.wstxda.switchai.wakeword.WakePhraseStore
import com.wstxda.switchai.wakeword.WakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class WakeWordPreferencesFragment : BasePreferenceFragment() {

    override val preferencesResId: Int get() = R.xml.preferences_wake_word

    private var pendingPermissionAction: (() -> Unit)? = null
    private var activeAudioSession: WakeAudioSession? = null

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (micGranted) {
            pendingPermissionAction?.invoke()
        } else {
            findPreference<SwitchPreferenceCompat>(KEY_WAKE_WORD_ENABLED)?.isChecked = false
            Toast.makeText(
                requireContext(),
                "Для голосовой активации нужен доступ к микрофону",
                Toast.LENGTH_LONG,
            ).show()
        }
        pendingPermissionAction = null
    }

    override fun setupListeners() {
        findPreference<SwitchPreferenceCompat>(KEY_WAKE_WORD_ENABLED)
            ?.setOnPreferenceChangeListener { preference, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    if (hasMicrophonePermission()) {
                        startWakeWordService()
                        true
                    } else {
                        requestVoicePermissions {
                            (preference as? SwitchPreferenceCompat)?.isChecked = true
                            startWakeWordService()
                        }
                        false
                    }
                } else {
                    requireContext().stopService(
                        Intent(requireContext(), WakeWordService::class.java)
                    )
                    true
                }
            }

        findPreference<Preference>(KEY_MANAGE_PHRASES)
            ?.setOnPreferenceClickListener {
                showPhraseManager()
                true
            }

        findPreference<Preference>(KEY_OWNER_ENROLL)
            ?.setOnPreferenceClickListener {
                runWithMicrophonePermission { startOwnerEnrollment() }
                true
            }

        findPreference<Preference>(KEY_OWNER_TEST)
            ?.setOnPreferenceClickListener {
                runWithMicrophonePermission { startOwnerVoiceTest() }
                true
            }
    }

    override fun onDestroyView() {
        activeAudioSession?.stop()
        activeAudioSession = null
        super.onDestroyView()
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun runWithMicrophonePermission(action: () -> Unit) {
        if (hasMicrophonePermission()) {
            action()
        } else {
            requestVoicePermissions(action)
        }
    }

    private fun requestVoicePermissions(onGranted: () -> Unit) {
        pendingPermissionAction = onGranted
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun startWakeWordService() {
        val intent = Intent(requireContext(), WakeWordService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun showPhraseManager() {
        val store = WakePhraseStore(requireContext())
        val phrases = store.load()

        val labels = if (phrases.isEmpty()) {
            arrayOf("Пока нет фраз. Нажмите «Добавить»")
        } else {
            phrases.map { phrase ->
                "«${phrase.phrase}» → ${assistantName(phrase.assistantKey)}"
            }.toTypedArray()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Фразы активации")
            .setItems(labels) { _, which ->
                if (phrases.isNotEmpty()) {
                    confirmDeletePhrase(phrases[which])
                }
            }
            .setPositiveButton("Добавить") { _, _ -> showAddPhraseDialog() }
            .setNegativeButton("Закрыть", null)
            .create()
        dialog.show()
    }

    private fun showAddPhraseDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Например: Слушай Gemini"
            setSingleLine(false)
            minLines = 2
            setPadding(48, 16, 48, 0)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Новая фраза")
            .setMessage("Введите любую фразу, по которой должен запускаться ассистент.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Далее") { _, _ ->
                val phrase = input.text?.toString()?.trim().orEmpty()
                if (phrase.isNotEmpty()) {
                    showAssistantPicker(phrase)
                } else {
                    Toast.makeText(requireContext(), "Введите фразу", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showAssistantPicker(phrase: String) {
        val names = arrayOf("ChatGPT", "Gemini", "Claude", "Алиса")
        val keys = arrayOf(
            "chatgpt_assistant",
            "gemini_assistant",
            "claude_assistant",
            "alice_assistant",
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите ассистента")
            .setItems(names) { _, which ->
                val store = WakePhraseStore(requireContext())
                val updated = store.load().toMutableList().apply {
                    add(
                        WakePhrase(
                            phrase = phrase,
                            assistantKey = keys[which],
                            languageTag = Locale.getDefault().toLanguageTag(),
                            enabled = true,
                        )
                    )
                }
                store.save(updated)
                Toast.makeText(
                    requireContext(),
                    "Фраза «$phrase» сохранена",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDeletePhrase(phrase: WakePhrase) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить фразу?")
            .setMessage("«${phrase.phrase}»")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                val store = WakePhraseStore(requireContext())
                store.save(store.load().filterNot { it == phrase })
                Toast.makeText(requireContext(), "Фраза удалена", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun assistantName(key: String): String = when (key) {
        "chatgpt_assistant" -> "ChatGPT"
        "gemini_assistant" -> "Gemini"
        "claude_assistant" -> "Claude"
        "alice_assistant" -> "Алиса"
        else -> key
    }

    private fun startOwnerEnrollment() {
        runCatching { SpeakerModelInstaller.ensureInstalled(requireContext()) }
            .onFailure {
                showError("Не удалось подготовить локальную модель распознавания голоса.")
                return
            }

        OwnerVoiceProfileStore(requireContext()).clear()
        val session = OwnerVoiceEnrollmentSession(requireContext())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Обучение голоса")
            .setMessage(
                "Нужно записать 10 коротких фраз. Каждая запись займёт около 4 секунд. " +
                    "Говорите обычным голосом в тихом месте."
            )
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Начать") { _, _ ->
                showEnrollmentStep(session, 0)
            }
            .show()
    }

    private fun showEnrollmentStep(session: OwnerVoiceEnrollmentSession, index: Int) {
        if (!isAdded || index !in OwnerVoiceEnrollment.russianPrompts.indices) return
        val prompt = OwnerVoiceEnrollment.russianPrompts[index]

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Фраза ${index + 1} из ${OwnerVoiceEnrollment.REQUIRED_SAMPLES}")
            .setMessage("После нажатия «Записать» скажите:\n\n“$prompt”")
            .setNegativeButton("Отмена") { _, _ ->
                activeAudioSession?.stop()
                activeAudioSession = null
                session.reset()
            }
            .setPositiveButton("Записать") { _, _ ->
                Toast.makeText(requireContext(), "Говорите…", Toast.LENGTH_SHORT).show()
                captureSpeakerEmbedding { embedding ->
                    if (!isAdded) return@captureSpeakerEmbedding
                    if (embedding == null) {
                        showError("Не удалось распознать запись. Попробуйте ещё раз.")
                        showEnrollmentStep(session, index)
                        return@captureSpeakerEmbedding
                    }

                    when (session.addSample(embedding)) {
                        is OwnerVoiceEnrollmentSession.AddResult.Accepted ->
                            showEnrollmentStep(session, index + 1)

                        OwnerVoiceEnrollmentSession.AddResult.Complete -> {
                            if (session.finish()) {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Голос обучен")
                                    .setMessage(
                                        "Все 10 образцов сохранены локально. " +
                                            "Теперь можно выполнить контрольную проверку."
                                    )
                                    .setPositiveButton("Готово", null)
                                    .show()
                            } else {
                                showError("Не удалось сохранить голосовой профиль.")
                            }
                        }

                        else -> {
                            showError("Запись не принята. Повторите эту фразу.")
                            showEnrollmentStep(session, index)
                        }
                    }
                }
            }
            .show()
    }

    private fun startOwnerVoiceTest() {
        val verifier = OwnerVoiceVerifier(requireContext())
        if (!verifier.hasProfile()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Сначала обучите голос")
                .setMessage("Для проверки нужен профиль из 10 записанных фраз.")
                .setPositiveButton("Понятно", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Проверка голоса")
            .setMessage("Нажмите «Проверить» и произнесите любую короткую фразу обычным голосом.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Проверить") { _, _ ->
                Toast.makeText(requireContext(), "Говорите…", Toast.LENGTH_SHORT).show()
                captureSpeakerEmbedding { embedding ->
                    if (!isAdded) return@captureSpeakerEmbedding
                    when (val result = verifier.verify(embedding)) {
                        is OwnerVoiceVerifier.Verification.Verified ->
                            showVoiceTestResult(
                                true,
                                "Владелец подтверждён",
                                result.score,
                            )

                        is OwnerVoiceVerifier.Verification.Rejected ->
                            showVoiceTestResult(
                                false,
                                "Голос не совпадает",
                                result.score,
                            )

                        OwnerVoiceVerifier.Verification.NoProfile ->
                            showError("Голосовой профиль не найден.")

                        OwnerVoiceVerifier.Verification.NoCandidate ->
                            showError("Не удалось получить образец голоса.")
                    }
                }
            }
            .show()
    }

    private fun captureSpeakerEmbedding(onResult: (FloatArray?) -> Unit) {
        activeAudioSession?.stop()
        val audioSession = WakeAudioSession(requireContext())
        activeAudioSession = audioSession

        if (!audioSession.start()) {
            activeAudioSession = null
            onResult(null)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(RECORDING_DURATION_MS)
            val embedding = withContext(Dispatchers.Default) {
                audioSession.currentSpeakerEmbedding()
            }
            audioSession.stop()
            if (activeAudioSession === audioSession) {
                activeAudioSession = null
            }
            onResult(embedding)
        }
    }

    private fun showVoiceTestResult(matched: Boolean, title: String, score: Float) {
        val scoreText = String.format(Locale.getDefault(), "%.2f", score)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(
                if (matched) {
                    "Совпадение подтверждено. Оценка: $scoreText"
                } else {
                    "Проверка не пройдена. Оценка: $scoreText"
                }
            )
            .setPositiveButton("Готово", null)
            .show()
    }

    private fun showError(message: String) {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Не удалось выполнить действие")
            .setMessage(message)
            .setPositiveButton("Понятно", null)
            .show()
    }

    companion object {
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_MANAGE_PHRASES = "wake_word_phrases_manage"
        private const val KEY_OWNER_ENROLL = "owner_voice_enroll"
        private const val KEY_OWNER_TEST = "owner_voice_test"
        private const val RECORDING_DURATION_MS = 4000L
    }
}
