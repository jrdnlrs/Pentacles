package com.example.forenscan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forenscan.databinding.FragmentDashboardBinding
import com.example.forenscan.databinding.CardMetricBinding
import com.example.forenscan.data.ActivityItem
import com.example.forenscan.ui.ActivityAdapter
import android.graphics.Color
import androidx.core.graphics.toColorInt

class DashboardFragment : Fragment() {

    // --- View Binding Setup ---
    private var _binding: FragmentDashboardBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // --- State Variables ---
    private var isSystemStatusExpanded = false
    private var isScanning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initial Data & Customization Setup
        setupMetricCards()

        // 2. Scan Button Logic
        setupScanButton()

        // 3. System Status
        setupSystemStatusCard()

        // 4. Recent Activity RecyclerView
        setupRecentActivity()

        // 5. Initial Top Nav State
        setupTopNavigation()

        updateTabStyles(isNetworks = false)

        setupTopNavigation()
    }

    // --- Metric Card Setup (Data, Icons, Hue) ---
    private fun setupMetricCards() {

        fun bindCardViews(includeBinding: CardMetricBinding, iconRes: Int, accentColorRes: Int, value: String, title: String, subtext: String, hueDrawableRes: Int) {
            includeBinding.apply {
                metricIcon.setImageResource(iconRes)
                metricIcon.setColorFilter(ContextCompat.getColor(requireContext(), accentColorRes))
                metricValueText.text = value
                metricTitleText.text = title
                metricSubtext.text = subtext
                cardContentContainer.setBackgroundResource(hueDrawableRes)
            }
        }

        // 1. Networks Card
        bindCardViews(
            binding.cardNetworks,
            R.drawable.ic_wifi,
            R.color.networks_accent,
            "23",
            getString(R.string.networks),
            getString(R.string.last_scan),
            R.drawable.bg_card_networks_hue
        )

        // 2. Threats Card
        bindCardViews(
            binding.cardThreats,
            R.drawable.ic_shield, //
            R.color.threats_accent,
            "2",
            getString(R.string.threats),
            getString(R.string.active_alerts),
            R.drawable.bg_card_threats_hue
        )

        // 3. Connections Card
        bindCardViews(
            binding.cardConnections,
            R.drawable.ic_activity, //
            R.color.connections_accent,
            "1",
            getString(R.string.connections),
            getString(R.string.currently_active),
            R.drawable.bg_card_connections_hue
        )

        // 4. Uptime Card
        bindCardViews(
            binding.cardUptime,
            R.drawable.ic_clock,
            R.color.uptime_accent,
            getString(R.string.uptime_value),
            getString(R.string.uptime),
            getString(R.string.protection_active),
            R.drawable.bg_card_uptime_hue
        )
    }

    // --- Scan Button Logic ---
    private fun setupScanButton() {
        binding.scanButton.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            startScan()
        }
    }

    private fun startScan() {
        isScanning = true
        binding.scanButton.text = "Scanning..."
        binding.scanButton.isEnabled = false
        binding.scanButton.setIconTintResource(android.R.color.transparent)
        binding.scanProgress.visibility = View.VISIBLE
        binding.scanButton.postDelayed({
            stopScan()
        }, 3000)
    }

    private fun stopScan() {
        isScanning = false
        binding.scanButton.isEnabled = true
        binding.scanButton.setIconTintResource(android.R.color.white)
        binding.scanButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_scan)
        binding.scanButton.text = "Start Security Scan"
        binding.scanProgress.visibility = View.GONE

        // For testing/UI updates:
        val results = listOf("SECURE", "CAUTION", "THREAT")
        updateSecurityStatus(results.random())
    }


    private fun updateSecurityStatus(state: String) {
        binding.statusCardContainer.visibility = View.VISIBLE

        when (state) {
            "SECURE" -> {
                binding.apply {
                    statusCardBg.setBackgroundResource(R.drawable.bg_card_networks_hue)
                    statusIcon.setImageResource(R.drawable.ic_shield)
                    statusIcon.setColorFilter("#4CAF50".toColorInt())
                    threatLevelText.text = "SECURE"
                    threatDescriptionText.text = "No threats detected. Your connection is safe."
                }
            }
            "CAUTION" -> {
                binding.apply {
                    statusCardBg.setBackgroundResource(R.drawable.bg_card_connections_hue)
                    statusIcon.setImageResource(R.drawable.ic_caution)
                    statusIcon.setColorFilter("#FF9800".toColorInt())
                    threatLevelText.text = "CAUTION"
                    threatDescriptionText.text = "Suspicious network activity. Monitor closely."
                }
            }
            "THREAT" -> {
                binding.apply {
                    statusCardBg.setBackgroundResource(R.drawable.bg_card_threats_hue)
                    statusIcon.setImageResource(R.drawable.ic_warning)
                    statusIcon.setColorFilter("#F44336".toColorInt())
                    threatLevelText.text = "THREAT DETECTED"
                    threatDescriptionText.text = "Evil Twin attack detected! Disconnect immediately."
                }
            }
        }
    }

    // --- Collapsible System Status ---
    private fun setupSystemStatusCard() {
        binding.systemStatusHeader.setOnClickListener {
            toggleSystemStatusDetails()
        }
    }

    private fun toggleSystemStatusDetails() {
        isSystemStatusExpanded = !isSystemStatusExpanded


        binding.systemStatusDetails.visibility = if (isSystemStatusExpanded) View.VISIBLE else View.GONE

        val rotation = if (isSystemStatusExpanded) 180f else 0f
        binding.systemStatusArrow.animate()
            .rotation(rotation)
            .setDuration(300)
            .start()
    }

    // --- Recent Activity Setup ---
    private fun setupRecentActivity() {
        val activityList = listOf(
            ActivityItem("Evil Twin Detected", "Suspicious network \"SM_Mall_WIFI\" found", "Critical", "2 min ago"),
            ActivityItem("Network Scan Completed", "23 networks analyzed, 2 threats found", null, "5 min ago"),
            ActivityItem("Connected to WiFi", "Successfully connected to secure network", null, "15 min ago"),
            ActivityItem("Protection Enabled", "Real-time monitoring activated", null, "1 hr ago")
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
            binding.btnOverview.setTextColor("#666666".toColorInt())
        } else {
            binding.btnOverview.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.btnOverview.setTextColor(Color.BLACK)

            binding.btnNetworksTab.setBackgroundResource(android.R.color.transparent)
            binding.btnNetworksTab.setTextColor("#666666".toColorInt())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Essential to prevent memory leaks in Fragments using View Binding
        _binding = null
    }
}
