package com.example.forenscan.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R

// 1. Data Model
data class AlertItem(
    val title: String,
    val description: String,
    val type: String,     // e.g. "Evil Twin"
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM"
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
        // Added this to control the icon color
        val alertIcon: ImageView = view.findViewById(R.id.img_alert_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_threat_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        val context = holder.itemView.context

        // Bind Text Data
        holder.title.text = alert.title
        holder.description.text = alert.description
        holder.badgeType.text = alert.type
        holder.badgeSeverity.text = alert.severity
        holder.ssid.text = alert.ssid
        holder.location.text = alert.location
        holder.time.text = alert.timeAgo
        holder.mac.text = alert.macAddress
        holder.action.text = alert.recommendedAction

        // Dynamic Styling based on Severity
        when (alert.severity) {
            "CRITICAL" -> {
                // Red Theme
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_critical_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.red_700))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.red_600))
            }
            "HIGH" -> {
                // Orange Theme
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_high_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.orange_700))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.orange_600))
            }
            "MEDIUM" -> {
                // Yellow/Neutral Theme
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_neutral_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.slate_600))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.yellow_600))
            }
        }
    }

    override fun getItemCount() = alerts.size
}