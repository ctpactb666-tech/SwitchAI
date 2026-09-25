package com.wstxda.switchai.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.wstxda.switchai.R
import com.wstxda.switchai.databinding.ActivityMainBinding
import com.wstxda.switchai.service.UpdaterService
import com.wstxda.switchai.wakeword.WakeWordSettings

class MainActivity : BaseActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var systemBars: Insets = Insets.NONE
    private var navVisible = true

    /** Tabs of the bottom navigation. */
    private val topLevel = setOf(
        R.id.homeFragment,
        R.id.assistantsFragment,
        R.id.commandsFragment,
        R.id.settingsFragment,
    )

    /** Full-screen destinations from the mockup: no bottom navigation. */
    private val fullScreen = setOf(
        R.id.listeningFragment,
        R.id.addPhraseFragment,
        R.id.assistantSettingsFragment,
    )

    /** Sub-screens → tab that should stay highlighted. */
    private val parentTab = mapOf(
        R.id.assistantSettingsFragment to R.id.assistantsFragment,
        R.id.selectorPreferencesFragment to R.id.settingsFragment,
        R.id.voiceInputPreferencesFragment to R.id.settingsFragment,
        R.id.wakeWordPreferencesFragment to R.id.commandsFragment,
        R.id.accessibilityPreferencesFragment to R.id.settingsFragment,
        R.id.shortcutsPreferencesFragment to R.id.settingsFragment,
        R.id.appearancePreferencesFragment to R.id.settingsFragment,
        R.id.aboutPreferencesFragment to R.id.settingsFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_container) as NavHostFragment
        val navController = navHost.navController

        applySystemInsets()
        setupBottomNavigation(navController)

        UpdaterService.checkForUpdatesAuto(lifecycleScope, this, supportFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        // Restores background listening after a reboot / app update if the user enabled it.
        WakeWordSettings.syncService(this)
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            updateContentPadding()
            insets
        }
    }

    private fun updateContentPadding() {
        val navReservedSpace = if (navVisible) {
            resources.getDimensionPixelSize(R.dimen.bottom_nav_height) +
                resources.getDimensionPixelSize(R.dimen.bottom_nav_margin) +
                systemBars.bottom
        } else {
            systemBars.bottom
        }

        binding.navHostContainer.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            navReservedSpace,
        )
        binding.bottomNavContainer.layoutParams =
            (binding.bottomNavContainer.layoutParams as ViewGroup.MarginLayoutParams).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_side_margin) + systemBars.left
                rightMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_side_margin) + systemBars.right
                bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin) + systemBars.bottom
            }
    }

    private fun setupBottomNavigation(navController: NavController) {
        val bottomNavigation = binding.bottomNavigation

        bottomNavigation.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(navController.graph.startDestinationId, false, true)
                    .build()
                runCatching { navController.navigate(item.itemId, null, options) }
            }
            true
        }

        // Tapping the active tab again returns to its first screen.
        bottomNavigation.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showNav = destination.id !in fullScreen
            if (showNav != navVisible) {
                navVisible = showNav
                binding.bottomNavContainer.visibility = if (showNav) View.VISIBLE else View.GONE
                updateContentPadding()
            }

            val tab = if (destination.id in topLevel) destination.id else parentTab[destination.id]
            tab?.let { bottomNavigation.menu.findItem(it)?.isChecked = true }
        }
    }
}
