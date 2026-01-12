package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.ui.adapters.TimelineAdapter
import com.example.forenscan.ui.adapters.TimelineEvent

class TimelineFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timeline, container, false)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_timeline)
        recycler.layoutManager = LinearLayoutManager(context)

        // Mock Data based on your screenshots
        val events = listOf(
            TimelineEvent(
                title = "Evil Twin Attack Detected",
                description = "Duplicate SSID detected with different MAC address",
                timestamp = "2024-01-15 14:32:15",
                ssid = "SM_Mall_WiFi",
                isCritical = true, // RED
                techDetails = listOf(
                    "Original MAC: 00:1A:2B:3C:4D:5E",
                    "Suspicious MAC: 00:1A:2B:3C:4D:5F",
                    "Encryption: WPA2"
                )
            ),
            TimelineEvent(
                title = "Connected to WiFi Network",
                description = "Successfully connected to legitimate network",
                timestamp = "2024-01-15 14:30:45",
                ssid = "SM_Mall_WiFi",
                isCritical = false, // BLUE
                techDetails = listOf(
                    "DHCP Server: 192.168.1.1",
                    "DNS: 8.8.8.8, 8.8.4.4",
                    "Gateway: 192.168.1.1"
                )
            ),
            TimelineEvent(
                title = "Network Scan Initiated",
                description = "Started scanning for available networks",
                timestamp = "2024-01-15 14:28:22",
                ssid = "N/A",
                isCritical = false, // BLUE
                techDetails = listOf(
                    "Scan Type: Active",
                    "Networks Found: 12",
                    "Duration: 15 seconds"
                )
            )
        )

        recycler.adapter = TimelineAdapter(events)
        return view
    }
}