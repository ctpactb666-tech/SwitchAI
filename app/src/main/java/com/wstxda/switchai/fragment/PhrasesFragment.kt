package com.wstxda.switchai.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.ui.catalog.AssistantInfo
import com.wstxda.switchai.wakeword.WakePhrase
import com.wstxda.switchai.wakeword.WakePhraseStore

class PhrasesFragment : Fragment(R.layout.fragment_phrases) {

    private val store by lazy { WakePhraseStore(requireContext()) }
    private lateinit var adapter: PhraseAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val topBar = view.findViewById<View>(R.id.topBar)
        NeonViews.setupTopBar(this, topBar, getString(R.string.neon_phrases_title))
        topBar.findViewById<View>(R.id.topExtra).apply {
            visibility = View.VISIBLE
            setOnClickListener { findNavController().navigate(R.id.addPhraseFragment) }
        }

        val assistants = AssistantCatalog.all(requireContext()).associateBy { it.key }
        adapter = PhraseAdapter(
            assistants = assistants,
            onOpen = { phrase ->
                findNavController().navigate(
                    R.id.addPhraseFragment,
                    bundleOf(AddPhraseFragment.ARG_PHRASE_ID to phrase.id),
                )
            },
            onToggle = { phrase, enabled -> store.setEnabled(phrase.id, enabled) },
            onDelete = { phrase -> confirmDelete(phrase) },
            onStartDrag = { holder -> touchHelper.startDrag(holder) },
        )

        val list = view.findViewById<RecyclerView>(R.id.phraseList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                adapter.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            // Dragging starts only from the handle, not by long-press on the whole card.
            override fun isLongPressDragEnabled() = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1f
                store.save(adapter.items)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.85f
            }
        })
        touchHelper.attachToRecyclerView(list)
    }

    override fun onResume() {
        super.onResume()
        view?.let(::render)
    }

    private fun render(view: View) {
        val phrases = store.load()
        adapter.setItems(phrases)
        view.findViewById<View>(R.id.phrasesEmpty).visibility =
            if (phrases.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(phrase: WakePhrase) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.neon_phrase_delete_title)
            .setMessage(getString(R.string.neon_quoted, phrase.displayText))
            .setNegativeButton(R.string.neon_cancel, null)
            .setPositiveButton(R.string.neon_delete) { _, _ ->
                store.delete(phrase.id)
                view?.let(::render)
            }
            .show()
    }

    private class PhraseAdapter(
        private val assistants: Map<String, AssistantInfo>,
        private val onOpen: (WakePhrase) -> Unit,
        private val onToggle: (WakePhrase, Boolean) -> Unit,
        private val onDelete: (WakePhrase) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    ) : RecyclerView.Adapter<PhraseAdapter.Holder>() {

        val items = mutableListOf<WakePhrase>()

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val drag: View = view.findViewById(R.id.phraseDrag)
            val text: TextView = view.findViewById(R.id.phraseText)
            val tile: FrameLayout = view.findViewById(R.id.phraseAssistantTile)
            val icon: ImageView = view.findViewById(R.id.phraseAssistantIcon)
            val assistant: TextView = view.findViewById(R.id.phraseAssistant)
            val enabled: MaterialSwitch = view.findViewById(R.id.phraseEnabled)
        }

        @SuppressLint("NotifyDataSetChanged") // whole list is reloaded from storage
        fun setItems(newItems: List<WakePhrase>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun move(from: Int, to: Int) {
            if (from !in items.indices || to !in items.indices) return
            items.add(to, items.removeAt(from))
            notifyItemMoved(from, to)
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_phrase_neon, parent, false)
        )

        @SuppressLint("ClickableViewAccessibility") // handle only starts a drag; click is on the card
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val phrase = items[position]
            val context = holder.itemView.context
            holder.text.text = context.getString(R.string.neon_quoted, phrase.displayText)

            val info = assistants[phrase.assistantKey]
            if (info != null) {
                NeonViews.bindAssistantTile(holder.tile, holder.icon, info)
                holder.assistant.text = info.name
            } else {
                holder.tile.setBackgroundResource(R.drawable.bg_icon_default)
                holder.icon.setImageResource(R.drawable.ic_assistant_default)
                holder.assistant.text = phrase.assistantKey
            }

            holder.enabled.setOnCheckedChangeListener(null)
            holder.enabled.isChecked = phrase.enabled
            holder.enabled.contentDescription = phrase.displayText
            holder.enabled.setOnCheckedChangeListener { _, checked ->
                val index = holder.bindingAdapterPosition
                if (index in items.indices) {
                    items[index] = items[index].copy(enabled = checked)
                    onToggle(items[index], checked)
                }
            }

            holder.itemView.setOnClickListener {
                holder.bindingAdapterPosition.takeIf { it in items.indices }?.let { onOpen(items[it]) }
            }
            holder.itemView.setOnLongClickListener {
                holder.bindingAdapterPosition.takeIf { it in items.indices }?.let { onDelete(items[it]) }
                true
            }
            holder.drag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
                false
            }
        }
    }
}
