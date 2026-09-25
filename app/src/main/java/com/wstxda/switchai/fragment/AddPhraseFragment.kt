package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.ui.catalog.AssistantInfo
import com.wstxda.switchai.wakeword.WakePhrase
import com.wstxda.switchai.wakeword.WakePhraseStore

class AddPhraseFragment : Fragment(R.layout.fragment_add_phrase) {

    private val store by lazy { WakePhraseStore(requireContext()) }

    /** Phrase being edited, or null when adding a new one. */
    private var editing: WakePhrase? = null

    private lateinit var assistants: List<AssistantInfo>
    private lateinit var selectedAssistant: AssistantInfo
    private var languageTag = LANG_AUTO
    private var exactMatch = false
    private var ignoreMusic = true
    private var offline = false

    private val languages by lazy {
        listOf(
            LANG_AUTO to getString(R.string.neon_language_auto),
            "ru-RU" to getString(R.string.neon_language_ru),
            "en-US" to getString(R.string.neon_language_en),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        editing = arguments?.getString(ARG_PHRASE_ID)?.let { store.find(it) }

        // Installed assistants first — those are the ones that can actually be launched.
        assistants = AssistantCatalog.visible(context)
            .ifEmpty { AssistantCatalog.all(context) }
            .sortedByDescending { AssistantCatalog.isInstalled(context, it.packageName) }

        val current = editing
        selectedAssistant = current?.let { phrase -> AssistantCatalog.find(context, phrase.assistantKey) }
            ?: assistants.first()
        languageTag = current?.languageTag ?: LANG_AUTO
        exactMatch = current?.exactMatch ?: false
        ignoreMusic = current?.ignoreWhenMusic ?: true
        offline = current?.offline ?: false

        NeonViews.setupTopBar(
            this,
            view.findViewById(R.id.topBar),
            getString(if (current == null) R.string.neon_add_phrase_title else R.string.neon_edit_phrase_title),
        )

        val input = view.findViewById<EditText>(R.id.phraseInput)
        if (savedInstanceState == null) input.setText(current?.phrase.orEmpty())
        val clear = view.findViewById<View>(R.id.phraseClear)
        clear.visibility = if (input.text.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        clear.setOnClickListener { input.setText("") }
        input.doAfterTextChanged {
            clear.visibility = if (it.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            showHelper(view, error = false)
        }

        view.findViewById<View>(R.id.assistantPicker).setOnClickListener { pickAssistant(view) }
        view.findViewById<View>(R.id.languagePicker).setOnClickListener { pickLanguage(view) }
        renderAssistant(view)
        renderLanguage(view)

        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowExact), R.drawable.ic_smartphone,
            getString(R.string.neon_exact_match), exactMatch,
        ) { exactMatch = it }
        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowIgnoreMusic), R.drawable.ic_music_note,
            getString(R.string.neon_ignore_music), ignoreMusic,
        ) { ignoreMusic = it }
        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowOffline), R.drawable.ic_offline,
            getString(R.string.neon_offline), offline,
        ) { offline = it }

        view.findViewById<View>(R.id.deletePhrase).apply {
            visibility = if (current != null) View.VISIBLE else View.GONE
            setOnClickListener { current?.let(::confirmDelete) }
        }

        view.findViewById<View>(R.id.savePhrase).setOnClickListener { save(view) }
    }

    private fun save(view: View) {
        val text = view.findViewById<EditText>(R.id.phraseInput).text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || text.split(';').all { it.isBlank() }) {
            showHelper(view, error = true)
            return
        }

        val base = editing ?: WakePhrase(phrase = text, assistantKey = selectedAssistant.key)
        store.upsert(
            base.copy(
                phrase = text,
                assistantKey = selectedAssistant.key,
                languageTag = languageTag,
                exactMatch = exactMatch,
                ignoreWhenMusic = ignoreMusic,
                offline = offline,
            )
        )
        Toast.makeText(requireContext(), R.string.neon_phrase_saved, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun showHelper(view: View, error: Boolean) {
        view.findViewById<TextView>(R.id.phraseHelper).apply {
            setText(if (error) R.string.neon_phrase_empty_error else R.string.neon_phrase_helper)
            setTextColor(
                ContextCompat.getColor(
                    requireContext(), if (error) R.color.neon_danger else R.color.neon_text_muted
                )
            )
        }
    }

    private fun renderAssistant(view: View) {
        NeonViews.bindAssistantTile(
            view.findViewById<FrameLayout>(R.id.pickerTile),
            view.findViewById<ImageView>(R.id.pickerIcon),
            selectedAssistant,
        )
        view.findViewById<TextView>(R.id.pickerName).text = selectedAssistant.name
        view.findViewById<TextView>(R.id.pickerVendor).text = selectedAssistant.vendor
    }

    private fun renderLanguage(view: View) {
        view.findViewById<TextView>(R.id.languageName).text =
            languages.firstOrNull { it.first == languageTag }?.second
                ?: languageTag // a tag saved by the old settings screen, e.g. "ru-RU"
    }

    private fun pickAssistant(view: View) {
        val adapter = object : ArrayAdapter<AssistantInfo>(
            requireContext(), R.layout.item_assistant_picker, assistants
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_assistant_picker, parent, false)
                val info = getItem(position)!!
                NeonViews.bindAssistantTile(
                    row.findViewById(R.id.pickerTile), row.findViewById(R.id.pickerIcon), info
                )
                row.findViewById<TextView>(R.id.pickerName).text = info.name
                row.findViewById<TextView>(R.id.pickerVendor).text = info.vendor
                return row
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.neon_choose_assistant)
            .setAdapter(adapter) { _, which ->
                selectedAssistant = assistants[which]
                renderAssistant(view)
            }
            .setNegativeButton(R.string.neon_cancel, null)
            .show()
    }

    private fun pickLanguage(view: View) {
        val labels = languages.map { it.second }.toTypedArray()
        val checked = languages.indexOfFirst { it.first == languageTag }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.neon_language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                languageTag = languages[which].first
                renderLanguage(view)
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmDelete(phrase: WakePhrase) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.neon_phrase_delete_title)
            .setMessage(getString(R.string.neon_quoted, phrase.displayText))
            .setNegativeButton(R.string.neon_cancel, null)
            .setPositiveButton(R.string.neon_delete) { _, _ ->
                store.delete(phrase.id)
                findNavController().navigateUp()
            }
            .show()
    }

    companion object {
        const val ARG_PHRASE_ID = "phrase_id"
        private const val LANG_AUTO = "und"
    }
}
