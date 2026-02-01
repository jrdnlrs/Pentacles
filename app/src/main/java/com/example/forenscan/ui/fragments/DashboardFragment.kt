package com.example.forenscan.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forenscan.R
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.ActivityType
import com.example.forenscan.data.models.ThreatAlert
import com.example.forenscan.databinding.CardMetricBinding
import com.example.forenscan.databinding.FragmentDashboardBinding
import com.example.forenscan.ui.adapters.ActivityAdapter
import com.example.forenscan.ui.viewmodel.FSViewModel
import java.util.UUID
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    // --- View Binding Setup ---
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // --- ViewModel (The Brain) ---
    private val viewModel: FSViewModel by activityViewModels()

    // --- State Variables ---
    private var isSystemStatusExpanded = false

    // ============================================
    // NEW: PERMISSION LAUNCHER (Handles the Pop-Up)
    // ============================================
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if Location was granted
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (locationGranted) {
                // User clicked "Allow" -> Start the scan
                Toast.makeText(context, "Permission Granted. Starting Scan...", Toast.LENGTH_SHORT).show()
                startRealScan()
            } else {
                // User clicked "Deny" -> Show explanation
                Toast.makeText(context, "Location permission is required to scan Wi-Fi.", Toast.LENGTH_LONG).show()
                stopScanVisuals()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initial UI Setup
        setupStaticMetricCards()
        setupTopNavigation()
        setupScanButton()
        setupSystemStatusCard()

        // This checks if the RFC_EvilTwin.tflite model is active
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isMLModelAvailable()) {
                // Update button to show ML is ready
                binding.scanButton.text = "Start ML Security Scan"
            }
        }

        // 2. Connect to Database (Live Updates)
        setupObservers()

        // 3. Setup Recent Activity
        setupRecentActivity()

        // 4. Default Tab
        updateTabStyles(isNetworks = false)

        // 5. NEW: Check permissions silently on startup (optional UX improvement)
        checkAndRequestPermissions(onlyCheck = true)
    }

    // ============================================
    // NEW: PERMISSION CHECKER LOGIC
    // ============================================
    private fun checkAndRequestPermissions(onlyCheck: Boolean = false): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        // 1. Check Location (Required for Wi-Fi Scan)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // 2. Check Notifications (Android 13+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // We are missing permissions
            if (!onlyCheck) {
                // If this wasn't just a check, actually launch the pop-up
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
            return false
        }

        // We have all permissions
        return true
    }

    // ============================================
    // DATA OBSERVERS
    // ============================================
    private fun setupObservers() {
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            binding.cardNetworks.metricValueText.text = networks.size.toString()
            binding.cardNetworks.metricSubtext.text = "Last scan: Just now"

            val connectedNet = networks.find { it.isConnected }
            if (connectedNet != null) {
                binding.cardConnections.metricValueText.text = "Active"
                binding.cardConnections.metricSubtext.text = connectedNet.ssid
                binding.cardConnections.metricIcon.setColorFilter(Color.parseColor("#4CAF50"))
            } else {
                binding.cardConnections.metricValueText.text = "None"
                binding.cardConnections.metricSubtext.text = "Not Connected"
                binding.cardConnections.metricIcon.setColorFilter(Color.GRAY)
            }
        }

        viewModel.threats.observe(viewLifecycleOwner) { threats ->
            binding.cardThreats.metricValueText.text = threats.size.toString()
            updateSecurityStatus(threats)
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.cardUptime.metricTitleText.text = "Total Scans"
                binding.cardUptime.metricValueText.text = stats.totalScans.toString()
                binding.cardUptime.metricSubtext.text = "Since install"
            }
        }
    }

    // ============================================
    // UI LOGIC & ANIMATIONS
    // ============================================

    private fun setupStaticMetricCards() {
        fun bindCardStyle(includeBinding: CardMetricBinding, iconRes: Int, accentColorRes: Int, title: String, hueDrawableRes: Int) {
            includeBinding.apply {
                metricIcon.setImageResource(iconRes)
                metricIcon.setColorFilter(ContextCompat.getColor(requireContext(), accentColorRes))
                metricTitleText.text = title
                metricValueText.text = "-"
                cardContentContainer.setBackgroundResource(hueDrawableRes)
            }
        }

        bindCardStyle(binding.cardNetworks, R.drawable.ic_wifi, R.color.networks_accent, getString(R.string.networks), R.drawable.bg_card_networks_hue)
        bindCardStyle(binding.cardThreats, R.drawable.ic_shield, R.color.threats_accent, getString(R.string.threats), R.drawable.bg_card_threats_hue)
        bindCardStyle(binding.cardConnections, R.drawable.ic_activity, R.color.connections_accent, getString(R.string.connections), R.drawable.bg_card_connections_hue)
        bindCardStyle(binding.cardUptime, R.drawable.ic_clock, R.color.uptime_accent, getString(R.string.uptime), R.drawable.bg_card_uptime_hue)
    }

    private fun setupScanButton() {
        binding.scanButton.setOnClickListener {
            // Animation
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            // UPDATED: Check Permission BEFORE Scanning
            if (checkAndRequestPermissions(onlyCheck = false)) {
                startRealScan()
            }
            // If false, the pop-up handles the rest
        }
    }

    private fun startRealScan() {
        // 1. Visual Feedback
        binding.scanButton.text = "Scanning..."
        binding.scanButton.isEnabled = false
        binding.scanButton.setIconTintResource(android.R.color.transparent)
        binding.scanProgress.visibility = View.VISIBLE

        // 2. Call the Service via ViewModel
        viewModel.startScanningService()
        Toast.makeText(context, "Background Scan Started", Toast.LENGTH_SHORT).show()

        // 3. Reset Button Visuals
        binding.scanButton.postDelayed({
            stopScanVisuals()
        }, 3000)
    }

//    private fun stopScanVisuals() { // previous stopScanVisuals
//        if (_binding == null) return
//        binding.scanButton.isEnabled = true
//        binding.scanButton.setIconTintResource(android.R.color.white)
//        binding.scanButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_scan)
//        binding.scanButton.text = "Start Security Scan"
//        binding.scanProgress.visibility = View.GONE
//    }

    private fun stopScanVisuals() {
        if (_binding == null) return
        binding.scanButton.isEnabled = true
        binding.scanButton.setIconTintResource(android.R.color.white)
        binding.scanButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_scan)

        // Check ML status again to restore the correct text
        viewLifecycleOwner.lifecycleScope.launch {
            binding.scanButton.text = if (viewModel.isMLModelAvailable()) "Start ML Security Scan" else "Start Security Scan"
        }

        binding.scanProgress.visibility = View.GONE
    }

    private fun updateSecurityStatus(threats: List<ThreatAlert>) {
        binding.statusCardContainer.visibility = View.VISIBLE

        if (threats.isNotEmpty()) {
            binding.apply {
                statusCardBg.setBackgroundResource(R.drawable.bg_card_threats_hue)
                statusIcon.setImageResource(R.drawable.ic_warning)
                statusIcon.setColorFilter(Color.parseColor("#F44336"))
                threatLevelText.text = "THREAT DETECTED"
                threatDescriptionText.text = "${threats.size} Evil Twin attack(s) detected! Disconnect immediately."
            }
        } else {
            binding.apply {
                statusCardBg.setBackgroundResource(R.drawable.bg_card_networks_hue)
                statusIcon.setImageResource(R.drawable.ic_shield)
                statusIcon.setColorFilter(Color.parseColor("#4CAF50"))
                threatLevelText.text = "SYSTEM SECURE"
                threatDescriptionText.text = "No threats detected. Your connection is safe."
            }
        }
    }

    private fun setupSystemStatusCard() {
        binding.systemStatusHeader.setOnClickListener {
            toggleSystemStatusDetails()
        }
    }

    // REPLACE your old toggleSystemStatusDetails with this:
    private fun toggleSystemStatusDetails() {
        isSystemStatusExpanded = !isSystemStatusExpanded

        if (isSystemStatusExpanded) {
            // 1. Show the details
            binding.systemStatusDetails.visibility = View.VISIBLE

            // 2. Rotate arrow (Animation)
            binding.systemStatusArrow.animate().rotation(180f).setDuration(300).start()

            // 3. FETCH REAL DATA from ViewModel
            // (Ensure you added getNetworkStatus() to FSViewModel as discussed earlier!)
            val status = viewModel.getNetworkStatus()

            // 4. Update the UI with real values
            binding.tvSysIp.text = status["IP Address"]
            binding.tvSysGateway.text = status["Gateway"]
            binding.tvSysBand.text = status["Band"]
            binding.tvSysSpeed.text = status["Speed"]

        } else {
            // Collapse the card
            binding.systemStatusDetails.visibility = View.GONE
            binding.systemStatusArrow.animate().rotation(0f).setDuration(300).start()
        }
    }

    private fun setupRecentActivity() {
        val now = System.currentTimeMillis()
        val activityList = listOf(
            ActivityItem(UUID.randomUUID().toString(), "Protection Enabled", "Real-time monitoring activated", now, ActivityType.PROTECTION_ENABLED),
            ActivityItem(UUID.randomUUID().toString(), "System Ready", "Database initialized", now, ActivityType.SYSTEM_STATUS_CHANGE)
        )

        binding.recentActivityRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ActivityAdapter(activityList)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTopNavigation() {
        binding.btnNetworksTab.setOnClickListener {
            binding.overviewScrollView.visibility = View.GONE
            binding.networksFragmentContainer.visibility = View.VISIBLE
            childFragmentManager.beginTransaction()
                .replace(R.id.networks_fragment_container, NetworksFragment())
                .commit()
            updateTabStyles(isNetworks = true)
        }

        binding.btnOverview.setOnClickListener {
            binding.networksFragmentContainer.visibility = View.GONE
            binding.overviewScrollView.visibility = View.VISIBLE
            updateTabStyles(isNetworks = false)
        }
    }

    private fun updateTabStyles(isNetworks: Boolean) {
        if (isNetworks) {
            binding.btnNetworksTab.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.btnNetworksTab.setTextColor(Color.BLACK)
            binding.btnOverview.setBackgroundResource(android.R.color.transparent)
            binding.btnOverview.setTextColor(Color.parseColor("#666666"))
        } else {
            binding.btnOverview.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.btnOverview.setTextColor(Color.BLACK)
            binding.btnNetworksTab.setBackgroundResource(android.R.color.transparent)
            binding.btnNetworksTab.setTextColor(Color.parseColor("#666666"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}