package com.wstxda.switchai.activity

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wstxda.switchai.R
import com.wstxda.switchai.databinding.ActivityMainBinding
import com.wstxda.switchai.service.UpdaterService

class MainActivity : BaseActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_container) as NavHostFragment
        val navController = navHost.navController

        applySystemInsets()
        setupBottomNavigation(binding.bottomNavigation, navController)

        UpdaterService.checkForUpdatesAuto(lifecycleScope, this, supportFragmentManager)
    }

    private fun applySystemInsets() {
        val baseBottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostContainer.setPadding(
                bars.left,
                bars.top,
                bars.right,
                0,
            )
            binding.bottomNavContainer.layoutParams =
                (binding.bottomNavContainer.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_side_margin)
                    rightMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_side_margin)
                    bottomMargin = baseBottomMargin + bars.bottom
                }
            insets
        }
    }

    private fun setupBottomNavigation(
        bottomNavigation: BottomNavigationView,
        navController: NavController,
    ) {
        bottomNavigation.setOnItemSelectedListener { item ->
            val current = navController.currentDestination?.id
            if (current != item.itemId) {
                navController.navigate(item.itemId)
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.assistantsFragment,
                R.id.commandsFragment,
                R.id.settingsFragment -> {
                    bottomNavigation.visibility = android.view.View.VISIBLE
                    bottomNavigation.menu.findItem(destination.id)?.isChecked = true
                }
                else -> bottomNavigation.visibility = android.view.View.VISIBLE
            }
        }
    }
}
