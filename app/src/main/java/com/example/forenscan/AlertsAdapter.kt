package com.example.forenscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Data Model
data class AlertItem(
    val title: String,
    val description: String,
    val type: String,     // e.g. "Evil Twin"
    val severity: String, // e.g. "CRITICAL"
    val ssid: String,
    val location: String,
    val timeAgo: String,
    val macAddress: String,
    val recommendedAction: String
)

// 2. Adapter Class
class AlertsAdapter(private val alerts: List<AlertItem>) :
    RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.text_alert_title)
        val description: TextView = view.findViewById(R.id.text_description)
        val badgeType: TextView = view.findViewById(R.id.badge_type)
        val badgeSeverity: TextView = view.findViewById(R.id.badge_severity)
        val ssid: TextView = view.findViewById(R.id.text_ssid)
        val location: TextView = view.findViewById(R.id.text_location)
        val time: TextView = view.findViewById(R.id.text_time)
        val mac: TextView = view.findViewById(R.id.text_mac)
        val action: TextView = view.findViewById(R.id.text_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_threat_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.title.text = alert.title
        holder.description.text = alert.description
        holder.badgeType.text = alert.type
        holder.badgeSeverity.text = alert.severity
        holder.ssid.text = alert.ssid
        holder.location.text = alert.location
        holder.time.text = alert.timeAgo
        holder.mac.text = alert.macAddress
        holder.action.text = alert.recommendedAction

        // Logic to change colors based on Severity (Optional but recommended)
        if (alert.severity == "CRITICAL") {
            holder.badgeSeverity.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            holder.badgeSeverity.setBackgroundResource(R.drawable.badge_critical_bg)
        } else {
            // Add logic for Warning/Medium here
        }
    }

    override fun getItemCount() = alerts.size
}