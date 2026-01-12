package com.example.forenscan.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.ActivityType
import com.example.forenscan.databinding.ItemActivityEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityAdapter(private val items: List<ActivityItem>) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {
            titleText.text = item.title
            descriptionText.text = item.description

            // FIXED: Format the Long timestamp to a String
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = sdf.format(Date(item.timestamp))

            // Handle timeline visibility
            timelineLine.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE

            // FIXED: Use 'item.type' instead of 'item.severity'
            when (item.type) {
                ActivityType.THREAT_DETECTED -> {
                    // Critical / Alert styling
                    alertStatusChip.text = "ALERT"
                    alertStatusChip.visibility = View.VISIBLE

                    iconView.setImageResource(R.drawable.ic_warning)
                    iconView.setColorFilter("#F44336".toColorInt()) // Red
                }
                ActivityType.PROTECTION_ENABLED -> {
                    alertStatusChip.visibility = View.GONE

                    iconView.setImageResource(R.drawable.ic_shield)
                    iconView.setColorFilter("#4CAF50".toColorInt()) // Green
                }
                ActivityType.WIFI_CONNECTED -> {
                    alertStatusChip.visibility = View.GONE

                    iconView.setImageResource(R.drawable.ic_wifi)
                    iconView.setColorFilter("#4CAF50".toColorInt()) // Green
                }
                ActivityType.SCAN_COMPLETED -> {
                    alertStatusChip.visibility = View.GONE

                    iconView.setImageResource(R.drawable.ic_activity) // Or scan icon
                    iconView.setColorFilter("#2196F3".toColorInt()) // Blue
                }
                else -> {
                    // System Status Change / Default
                    alertStatusChip.visibility = View.GONE

                    iconView.setImageResource(R.drawable.ic_activity)
                    iconView.setColorFilter("#666666".toColorInt()) // Grey
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ActivityViewHolder(val binding: ItemActivityEventBinding) : RecyclerView.ViewHolder(binding.root)
}