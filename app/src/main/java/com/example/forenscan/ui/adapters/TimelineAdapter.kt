package com.example.forenscan.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.google.android.material.card.MaterialCardView

// 1. Data Model
data class TimelineEvent(
    val title: String,
    val description: String,
    val timestamp: String,
    val ssid: String,
    val isCritical: Boolean,
    val techDetails: List<String>
)

// 2. Adapter
class TimelineAdapter(private val events: List<TimelineEvent>) :
    RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {

    class TimelineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.text_event_title)
        val description: TextView = view.findViewById(R.id.text_event_desc)
        val timestamp: TextView = view.findViewById(R.id.text_timestamp)
        val ssid: TextView = view.findViewById(R.id.text_ssid)
        val badge: TextView = view.findViewById(R.id.badge_status)
        val techRow1: TextView = view.findViewById(R.id.text_tech_row_1)
        val techRow2: TextView = view.findViewById(R.id.text_tech_row_2)
        val techRow3: TextView = view.findViewById(R.id.text_tech_row_3)

        // Styling Targets
        val line: View = view.findViewById(R.id.timeline_line)
        val icon: ImageView = view.findViewById(R.id.timeline_icon_bg)
        val card: MaterialCardView = view.findViewById(R.id.card_event)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_event, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val event = events[position]

        holder.title.text = event.title
        holder.description.text = event.description
        holder.timestamp.text = event.timestamp
        holder.ssid.text = event.ssid

        // Populate Tech Details safely
        holder.techRow1.text = if (event.techDetails.isNotEmpty()) event.techDetails[0] else ""
        holder.techRow2.text = if (event.techDetails.size > 1) event.techDetails[1] else ""
        holder.techRow3.text = if (event.techDetails.size > 2) event.techDetails[2] else ""

        // === STYLING LOGIC ===
        if (event.isCritical) {
            // CRITICAL (Red)
            holder.badge.text = "CRITICAL"
            holder.badge.setTextColor(Color.WHITE)
            holder.badge.setBackgroundResource(R.drawable.badge_critical_bg)

            holder.line.setBackgroundColor(Color.parseColor("#EF4444")) // Red Line
            holder.icon.setColorFilter(Color.parseColor("#EF4444"))     // Red Dot
            holder.card.setCardBackgroundColor(Color.parseColor("#FEF2F2")) // Light Red Card
            holder.card.strokeColor = Color.parseColor("#FECACA")
        } else {
            // INFO (Blue/Neutral)
            holder.badge.text = "INFO"
            holder.badge.setTextColor(Color.parseColor("#475569"))
            holder.badge.setBackgroundResource(R.drawable.badge_neutral_bg)

            holder.line.setBackgroundColor(Color.parseColor("#E2E8F0")) // Gray Line
            holder.icon.clearColorFilter() // Default Gray Dot
            holder.card.setCardBackgroundColor(Color.parseColor("#F8FAFC")) // White/Gray Card
            holder.card.strokeColor = Color.parseColor("#E2E8F0")
        }
    }

    override fun getItemCount() = events.size
}