package com.example.forenscan.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.data.ActivityItem
import com.example.forenscan.databinding.ItemActivityEventBinding
import android.graphics.Color
import android.view.View
import com.example.forenscan.R
import androidx.core.graphics.toColorInt

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
            timeText.text = item.timestamp

            timelineLine.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE

            if (item.severity != null) {
                alertStatusChip.text = item.severity
                alertStatusChip.visibility = View.VISIBLE
            } else {
                alertStatusChip.visibility = View.GONE
            }

            when {
                item.severity == "Critical" -> {
                    iconView.setImageResource(R.drawable.ic_warning)
                    iconView.setColorFilter("#F44336".toColorInt())
                }
                item.title.contains("Protection") -> {
                    iconView.setImageResource(R.drawable.ic_shield)
                    iconView.setColorFilter("#4CAF50".toColorInt())
                }
                item.title.contains("Scan") -> {
                    iconView.setImageResource(R.drawable.ic_activity)
                    iconView.setColorFilter("#2196F3".toColorInt())
                }
                item.title.contains("WiFi") || item.title.contains("Connected") -> {
                    iconView.setImageResource(R.drawable.ic_wifi)
                    iconView.setColorFilter("#4CAF50".toColorInt())
                }
                else -> {
                    iconView.setImageResource(R.drawable.ic_activity)
                    iconView.setColorFilter("#666666".toColorInt())
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ActivityViewHolder(val binding: ItemActivityEventBinding) : RecyclerView.ViewHolder(binding.root)
}