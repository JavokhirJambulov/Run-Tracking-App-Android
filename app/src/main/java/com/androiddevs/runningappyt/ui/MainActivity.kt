package com.androiddevs.runningappyt.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.ActivityMainBinding
import com.androiddevs.runningappyt.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.androiddevs.runningappyt.ui.fragments.SettingsFragment
import com.androiddevs.runningappyt.ui.fragments.StatisticsFragment
import com.androiddevs.runningappyt.ui.fragments.TrackingFragment
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navigateToTrackingFragmentIfNeeded(intent)
        setSupportActionBar(mainBinding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)

        mainBinding.bottomNavigationView.setupWithNavController(navController)
        mainBinding.bottomNavigationView.setOnItemReselectedListener{/**/}
        navController.addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id) {
                    R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                        mainBinding.bottomNavigationView.visibility = View.VISIBLE
                    else -> mainBinding.bottomNavigationView.visibility = View.GONE
                }
            }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            mainBinding.navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }
}
