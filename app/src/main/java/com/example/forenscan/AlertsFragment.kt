package com.example.forenscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlertsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        // 2. Find the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.alerts_recycler_view)

        // 3. Create Mock Data (This matches your AlertItem class in AlertsAdapter.kt)
        val mockData = listOf(
            AlertItem(
                title = "Evil Twin Attack Detected",
                description = "Duplicate SSID 'SM_Mall_WiFi' detected with suspicious MAC address",
                type = "Evil Twin",
                severity = "CRITICAL",
                ssid = "SM_Mall_WiFi",
                location = "SM North EDSA, Quezon City",
                timeAgo = "2 minutes ago",
                macAddress = "00:1A:2B:3C:4D:5F",
                recommendedAction = "Disconnect immediately and connect only to the legitimate network"
            ),
            AlertItem(
                title = "Suspicious Network Activity",
                description = "Unusual DNS requests detected from connected device",
                type = "Suspicious Activity",
                severity = "HIGH",
                ssid = "UP_Guest_Network",
                location = "University of the Philippines Diliman",
                timeAgo = "1 hour ago",
                macAddress = "00:3C:4D:5E:6F:7A",
                recommendedAction = "Monitor network traffic and consider disconnecting"
            ),
            AlertItem(
                title = "Weak Encryption Detected",
                description = "Network using outdated WEP encryption protocol",
                type = "Weak Encryption",
                severity = "MEDIUM",
                ssid = "Barangay_Free_WiFi",
                location = "Barangay Commonwealth, QC",
                timeAgo = "15 minutes ago",
                macAddress = "00:2B:3C:4D:5E:6F",
                recommendedAction = "Avoid transmitting sensitive data on this network"
            )
        )

        // 4. Set up the RecyclerView with the Adapter
        // Note: No import needed for AlertsAdapter because it's in the same package!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = AlertsAdapter(mockData)

        return view
    }
}