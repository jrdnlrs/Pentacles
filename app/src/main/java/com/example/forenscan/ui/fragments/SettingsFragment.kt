package com.example.forenscan.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.forenscan.R
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Preferences (Mode Private so other apps can't read them)
        sharedPref = requireContext().getSharedPreferences("FS_PREFS", Context.MODE_PRIVATE)

        // 2. Setup All Controls
        setupDarkMode(view)
        setupScanIntervalSpinner(view)
        setupExportPermission(view)

        // 3. Setup General Switches (Just saving their state for now)
        setupGenericSwitch(view, R.id.real_time_monitoring_switch, "PREF_REALTIME")
        setupGenericSwitch(view, R.id.background_scanning_switch, "PREF_BACKGROUND")
        setupGenericSwitch(view, R.id.threat_notifications_switch, "PREF_NOTIF_THREATS")
        setupGenericSwitch(view, R.id.auto_connect_switch, "PREF_AUTO_BLOCK")
        setupGenericSwitch(view, R.id.data_collections_switch, "PREF_DATA_COLLECT")
        setupGenericSwitch(view, R.id.Location_services_switch, "PREF_LOCATION")
        setupGenericSwitch(view, R.id.data_retention_switch, "PREF_RETENTION")
    }

    // --- 1. DARK MODE LOGIC ---
    private fun setupDarkMode(view: View) {
        val switch = view.findViewById<SwitchMaterial>(R.id.dark_mode_switch)

        // Load Saved State
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        switch.isChecked = isDarkMode

        switch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("DARK_MODE", isChecked).apply()

            // Switch Theme Instantly
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    // --- 2. SCAN INTERVAL (SPINNER) ---
    private fun setupScanIntervalSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.background_scan_spinner)

        // Use your Custom Layout (spinner_item.xml)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.scan_intervals,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Load saved position (Default to index 1: Balanced)
        val savedIndex = sharedPref.getInt("SCAN_INTERVAL_INDEX", 1)
        spinner.setSelection(savedIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Convert index to Milliseconds
                val intervalMillis = when (position) {
                    0 -> 5000L      // Fast (5s)
                    1 -> 30000L     // Balanced (30s)
                    2 -> 60000L     // Battery (1m)
                    else -> 30000L
                }

                // Save for the Service to read
                sharedPref.edit()
                    .putLong("SCAN_INTERVAL", intervalMillis)
                    .putInt("SCAN_INTERVAL_INDEX", position)
                    .apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // --- 3. EXPORT PERMISSION ---
    private fun setupExportPermission(view: View) {
        // This switch controls if the buttons on the Timeline Fragment are active
        val switch = view.findViewById<SwitchMaterial>(R.id.report_export_switch)

        switch.isChecked = sharedPref.getBoolean("ALLOW_EXPORT", true)

        switch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("ALLOW_EXPORT", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(context, "Forensic Export $status", Toast.LENGTH_SHORT).show()
        }
    }

    // --- HELPER FOR OTHER SWITCHES ---
    private fun setupGenericSwitch(view: View, viewId: Int, prefKey: String) {
        val switch = view.findViewById<SwitchMaterial>(viewId)
        // Load saved state (default to true)
        switch.isChecked = sharedPref.getBoolean(prefKey, true)

        switch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(prefKey, isChecked).apply()
        }
    }
}