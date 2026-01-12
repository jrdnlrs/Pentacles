package com.example.forenscan

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.forenscan.ui.fragments.DashboardFragment
import com.example.forenscan.ui.fragments.LearnFragment
//import com.example.forenscan.ui.fragments.SettingsFragment
import com.example.forenscan.ui.fragments.ThreatFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 1. Default Load
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        // 2. Navigation Logic
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    replaceFragment(DashboardFragment())
                    true
                }
                R.id.navigation_alerts -> {
                    // CRITICAL CHANGE: Load ThreatsFragment (The Toggle Page)
                    replaceFragment(ThreatFragment())
                    true
                }
                R.id.navigation_learn -> {
                    // Point to the correct fragment
                    replaceFragment(LearnFragment())
                    true
                }
                R.id.navigation_settings -> {
                    // Point to the correct fragment
                   replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // 4. Helper function to swap the fragments
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}