package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.ThreatAlert
import com.example.forenscan.data.models.ThreatSeverity
import com.example.forenscan.ui.adapters.AlertItem
import com.example.forenscan.ui.adapters.AlertsAdapter
import com.example.forenscan.ui.viewmodel.FSViewModel
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertsFragment : Fragment() {

    private val viewModel: FSViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        // 1. Bind UI Elements
        val recyclerView = view.findViewById<RecyclerView>(R.id.alerts_recycler_view)
        val emptyStateCard = view.findViewById<CardView>(R.id.empty_state_card)
        val activeBadge = view.findViewById<TextView>(R.id.active_alerts_badge)
        val chipGroup = view.findViewById<ChipGroup>(R.id.filter_chip_group)

        val critCountText = view.findViewById<TextView>(R.id.critical_count)
        val highCountText = view.findViewById<TextView>(R.id.high_count)
        val medCountText = view.findViewById<TextView>(R.id.medium_count)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // 2. Observe Live Data
        viewModel.threats.observe(viewLifecycleOwner) { allThreats ->

            // --- A. Update Counters (Only count ACTIVE threats) ---
            val activeThreats = allThreats.filter { !it.isResolved }

            critCountText.text = activeThreats.count { it.severity == ThreatSeverity.CRITICAL }.toString()
            highCountText.text = activeThreats.count { it.severity == ThreatSeverity.HIGH }.toString()
            medCountText.text = activeThreats.count { it.severity == ThreatSeverity.MEDIUM }.toString()
            activeBadge.text = "${activeThreats.size} Active"

            // --- B. Filter List based on Chip Selection ---
            val checkedId = chipGroup.checkedChipId
            val filteredList = filterThreats(checkedId, allThreats)

            updateList(recyclerView, emptyStateCard, filteredList)
        }

        // 3. Handle Chip Filtering (Active vs Resolved)
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.threats.value?.let { allThreats ->
                val filteredList = filterThreats(checkedId, allThreats)
                updateList(recyclerView, emptyStateCard, filteredList)
            }
        }

        return view
    }

    private fun updateList(recycler: RecyclerView, emptyState: View, threats: List<ThreatAlert>) {
        if (threats.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recycler.visibility = View.VISIBLE

            // Convert DB Objects to UI Objects
            val uiAlerts = threats.map { threat ->
                AlertItem(
                    id = threat.id,
                    title = threat.title,
                    description = threat.description,
                    type = "Evil Twin", // Or derive from threat.type if available
                    severity = threat.severity.name,
                    ssid = threat.networkName,
                    location = "Detected Locally",
                    timeAgo = formatTimestamp(threat.timestamp),
                    macAddress = threat.macAddress,
                    recommendedAction = threat.recommendedAction,
                    isResolved = threat.isResolved
                )
            }

            // Initialize Adapter with the Click Listener
            val adapter = AlertsAdapter(uiAlerts) { threatId ->
                // This code runs when "Resolve" is clicked
                viewModel.resolveThreat(threatId)
            }
            recycler.adapter = adapter
        }
    }

    private fun filterThreats(checkedId: Int, allThreats: List<ThreatAlert>): List<ThreatAlert> {
        return when (checkedId) {
            R.id.chip_active -> allThreats.filter { !it.isResolved }
            R.id.chip_resolved -> allThreats.filter { it.isResolved }
            else -> allThreats // R.id.chip_all
        }
    }

    private fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}