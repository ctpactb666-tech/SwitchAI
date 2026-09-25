package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.ui.catalog.AssistantInfo
import com.wstxda.switchai.ui.catalog.AssistantUiPreferences
import com.wstxda.switchai.wakeword.WakeAssistantLauncher

class AssistantsFragment : Fragment(R.layout.fragment_assistants) {

    private data class Row(val info: AssistantInfo, val installed: Boolean)

    private val adapter = AssistantAdapter(
        onOpen = { row ->
            findNavController().navigate(
                R.id.assistantSettingsFragment,
                bundleOf(AssistantSettingsFragment.ARG_KEY to row.info.key),
            )
        },
        onLaunch = { row -> WakeAssistantLauncher.launch(requireContext(), row.info.key) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<RecyclerView>(R.id.assistantList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AssistantsFragment.adapter
        }
        view.findViewById<View>(R.id.assistantsListen).setOnClickListener { NeonViews.openListening(this) }
        view.findViewById<ChipGroup>(R.id.filterGroup).setOnCheckedStateChangeListener { _, _ -> render(view) }
        view.findViewById<View>(R.id.assistantsHidden).setOnClickListener {
            AssistantUiPreferences(requireContext()).clearHidden()
            render(view)
        }
    }

    override fun onResume() {
        super.onResume()
        // Installed state, priority and hidden flags may change while we are away.
        view?.let(::render)
    }

    private fun render(view: View) {
        val context = requireContext()
        val all = AssistantCatalog.visible(context).map {
            Row(it, AssistantCatalog.isInstalled(context, it.packageName))
        }
        val rows = when (view.findViewById<ChipGroup>(R.id.filterGroup).checkedChipId) {
            R.id.filterPopular -> all.filter { it.info.popular }
            R.id.filterInstalled -> all.filter { it.installed }
            // "Все": installed first, then the rest, keeping the priority / popular order.
            else -> all.sortedByDescending { it.installed }
        }
        adapter.submitList(rows)
        view.findViewById<View>(R.id.assistantsEmpty).visibility =
            if (rows.isEmpty()) View.VISIBLE else View.GONE

        val hiddenCount = AssistantUiPreferences(context).hidden().size
        view.findViewById<TextView>(R.id.assistantsHidden).apply {
            visibility = if (hiddenCount > 0) View.VISIBLE else View.GONE
            text = getString(R.string.neon_assistants_hidden, hiddenCount)
        }
    }

    private class AssistantAdapter(
        private val onOpen: (Row) -> Unit,
        private val onLaunch: (Row) -> Unit,
    ) : ListAdapter<Row, AssistantAdapter.Holder>(Diff) {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val tile: FrameLayout = view.findViewById(R.id.assistantTile)
            val icon: ImageView = view.findViewById(R.id.assistantIcon)
            val name: TextView = view.findViewById(R.id.assistantName)
            val vendor: TextView = view.findViewById(R.id.assistantVendor)
            val statusIcon: ImageView = view.findViewById(R.id.assistantStatusIcon)
            val status: TextView = view.findViewById(R.id.assistantStatus)
            val launch: View = view.findViewById(R.id.assistantQuickLaunch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_assistant_neon, parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = getItem(position)
            val context = holder.itemView.context
            NeonViews.bindAssistantTile(holder.tile, holder.icon, row.info)
            holder.name.text = row.info.name
            holder.vendor.text = row.info.vendor

            val color = ContextCompat.getColor(
                context, if (row.installed) R.color.neon_teal else R.color.neon_text_muted
            )
            holder.status.setText(if (row.installed) R.string.neon_installed else R.string.neon_not_installed)
            holder.status.setTextColor(color)
            holder.statusIcon.visibility = if (row.installed) View.VISIBLE else View.GONE

            holder.launch.contentDescription = context.getString(R.string.neon_launch_assistant, row.info.name)
            holder.launch.setOnClickListener { onLaunch(row) }
            holder.itemView.setOnClickListener { onOpen(row) }
        }

        private object Diff : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(oldItem: Row, newItem: Row) = oldItem.info.key == newItem.info.key
            override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
        }
    }
}
