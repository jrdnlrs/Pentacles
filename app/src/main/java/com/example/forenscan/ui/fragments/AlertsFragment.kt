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
import com.example.forenscan.data.models.ThreatSeverity
import com.example.forenscan.ui.adapters.AlertItem
import com.example.forenscan.ui.adapters.AlertsAdapter
import com.example.forenscan.ui.viewmodel.FSViewModel
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

        // 1. Bind Views (Matching your fragment_alerts.xml IDs)
        val recyclerView = view.findViewById<RecyclerView>(R.id.alerts_recycler_view)
        val emptyStateCard = view.findViewById<CardView>(R.id.empty_state_card)
        val activeBadge = view.findViewById<TextView>(R.id.active_alerts_badge)

        // Stats Counters
        val critCountText = view.findViewById<TextView>(R.id.critical_count)
        val highCountText = view.findViewById<TextView>(R.id.high_count)
        val medCountText = view.findViewById<TextView>(R.id.medium_count)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // 2. Observe Real Threats from Database
        viewModel.threats.observe(viewLifecycleOwner) { threats ->

            // --- A. UPDATE COUNTERS & BADGE ---
            val critCount = threats.count { it.severity == ThreatSeverity.CRITICAL }
            val highCount = threats.count { it.severity == ThreatSeverity.HIGH }
            val medCount = threats.count { it.severity == ThreatSeverity.MEDIUM }

            critCountText.text = critCount.toString()
            highCountText.text = highCount.toString()
            medCountText.text = medCount.toString()

            activeBadge.text = "${threats.size} Active"

            // --- B. HANDLE LIST vs EMPTY STATE ---
            if (threats.isNotEmpty()) {
                // Hide Empty State, Show List
                emptyStateCard.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Convert Database Object -> UI Object
                val uiAlerts = threats.map { threat ->
                    AlertItem(
                        title = threat.title,
                        description = threat.description,
                        type = "Evil Twin",
                        severity = threat.severity.name,
                        ssid = threat.networkName,
                        location = "Detected Locally",
                        timeAgo = formatTimestamp(threat.timestamp),
                        macAddress = threat.macAddress,
                        recommendedAction = threat.recommendedAction
                    )
                }
                recyclerView.adapter = AlertsAdapter(uiAlerts)
            } else {
                // Show Empty State, Hide List
                emptyStateCard.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                // Clear adapter just in case
                recyclerView.adapter = AlertsAdapter(emptyList())
            }
        }

        return view
    }

    private fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}