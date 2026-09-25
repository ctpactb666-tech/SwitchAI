package com.wstxda.switchai.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.catalog.AssistantInfo

/**
 * Small helpers shared by the neon screens, so every screen wires its
 * top bar and rows the same way.
 */
object NeonViews {

    /** Back arrow + title + round action (defaults to opening the listening screen). */
    fun setupTopBar(
        fragment: Fragment,
        root: View,
        title: String,
        @DrawableRes actionIcon: Int = R.drawable.ic_wave_bars,
        onAction: (() -> Unit)? = null,
    ) {
        root.findViewById<View>(R.id.topBack).setOnClickListener {
            fragment.findNavController().navigateUp()
        }
        root.findViewById<TextView>(R.id.topTitle).text = title
        root.findViewById<ImageButton>(R.id.topAction).apply {
            setImageResource(actionIcon)
            setOnClickListener {
                if (onAction != null) onAction() else openListening(fragment)
            }
        }
    }

    fun openListening(fragment: Fragment) {
        val nav = fragment.findNavController()
        if (nav.currentDestination?.id != R.id.listeningFragment) {
            nav.navigate(R.id.listeningFragment)
        }
    }

    /** Binds an included item_neon_switch_row. The whole row is clickable. */
    fun bindSwitchRow(
        row: View,
        @DrawableRes icon: Int,
        title: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ): MaterialSwitch {
        row.findViewById<ImageView>(R.id.rowIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.rowTitle).text = title
        val switch = row.findViewById<MaterialSwitch>(R.id.rowSwitch)
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checked
        switch.setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        row.setOnClickListener { if (switch.isEnabled) switch.toggle() }
        switch.contentDescription = title
        return switch
    }

    /** Binds an included item_neon_nav_row. */
    fun bindNavRow(
        row: View,
        @DrawableRes icon: Int,
        title: String,
        subtitle: String? = null,
        onClick: () -> Unit,
    ) {
        row.findViewById<ImageView>(R.id.rowIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.rowTitle).text = title
        row.findViewById<TextView>(R.id.rowSubtitle).apply {
            text = subtitle
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        row.setOnClickListener { onClick() }
    }

    /** Colored brand tile with a white assistant icon. */
    fun bindAssistantTile(tile: FrameLayout, icon: ImageView, info: AssistantInfo) {
        tile.setBackgroundResource(info.tileRes)
        icon.setImageResource(info.iconRes)
    }
}
