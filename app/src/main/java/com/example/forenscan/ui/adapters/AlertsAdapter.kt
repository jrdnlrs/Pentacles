package com.example.forenscan.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.google.android.material.button.MaterialButton

// 1. Data Model
data class AlertItem(
    val id: String,          // Added ID to track specific threats
    val title: String,
    val description: String,
    val type: String,
    val severity: String,
    val ssid: String,
    val location: String,
    val timeAgo: String,
    val macAddress: String,
    val recommendedAction: String,
    val isResolved: Boolean  // Added to hide button if already fixed
)

// 2. Adapter Class
class AlertsAdapter(
    private val alerts: List<AlertItem>,
    private val onResolveClick: (String) -> Unit // Callback for the button
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs must match 'item_threat_alert.xml' exactly
        val title: TextView = view.findViewById(R.id.text_alert_title)
        val description: TextView = view.findViewById(R.id.text_description)
        val badgeSeverity: TextView = view.findViewById(R.id.badge_severity)

        // Technical Details
        val ssid: TextView = view.findViewById(R.id.text_ssid)
        val location: TextView = view.findViewById(R.id.text_location)
        val time: TextView = view.findViewById(R.id.text_time)
        val mac: TextView = view.findViewById(R.id.text_mac)

        // Visuals & Actions
        val action: TextView = view.findViewById(R.id.text_action)
        val alertIcon: ImageView = view.findViewById(R.id.img_alert_icon)
        val btnResolve: MaterialButton = view.findViewById(R.id.btn_resolve)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_threat_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        val context = holder.itemView.context

        // Bind Text
        holder.title.text = alert.title
        holder.description.text = alert.description
        holder.badgeSeverity.text = alert.severity
        holder.ssid.text = alert.ssid
        holder.location.text = alert.location
        holder.time.text = alert.timeAgo
        holder.mac.text = alert.macAddress
        holder.action.text = alert.recommendedAction

        // Dynamic Styling (Red/Orange/Yellow)
        when (alert.severity) {
            "CRITICAL" -> {
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_critical_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.red_700))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.red_600))
            }
            "HIGH" -> {
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_high_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.orange_700))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.orange_600))
            }
            "MEDIUM" -> {
                holder.badgeSeverity.setBackgroundResource(R.drawable.badge_neutral_bg)
                holder.badgeSeverity.setTextColor(ContextCompat.getColor(context, R.color.slate_600))
                holder.alertIcon.setColorFilter(ContextCompat.getColor(context, R.color.yellow_600))
            }
        }

        // Logic: Show/Hide Resolve Button
        if (alert.isResolved) {
            holder.btnResolve.visibility = View.GONE
        } else {
            holder.btnResolve.visibility = View.VISIBLE
            holder.btnResolve.setOnClickListener {
                onResolveClick(alert.id)
            }
        }
    }

    override fun getItemCount() = alerts.size
}