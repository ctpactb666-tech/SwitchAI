package com.wstxda.switchai.activity

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentContainerView
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
        setupBottomNavigation(binding.bottomNavigation, navController)

        UpdaterService.checkForUpdatesAuto(lifecycleScope, this, supportFragmentManager)
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
