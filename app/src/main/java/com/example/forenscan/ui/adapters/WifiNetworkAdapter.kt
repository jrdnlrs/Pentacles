package com.example.forenscan.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.NetworkClassification
import com.example.forenscan.data.models.WifiNetwork
import com.example.forenscan.databinding.ItemNetworkConnectionBinding

class WifiNetworkAdapter(private val networks: List<WifiNetwork>) :
    RecyclerView.Adapter<WifiNetworkAdapter.WifiViewHolder>() {

    class WifiViewHolder(val binding: ItemNetworkConnectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val binding = ItemNetworkConnectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WifiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        val network = networks[position]
        holder.binding.apply {
            networkName.text = network.ssid
            detailSsid.text = network.ssid
            macAddress.text = network.macAddress
            encryption.text = network.encryption
            frequencyText.text = network.frequency
            signalStrengthText.text = "${network.signalStrength}%"

            connectedBadge.visibility = if (network.isConnected) View.VISIBLE else View.GONE

            // Specific styling for hues and icons
            when (network.classification) {
                NetworkClassification.SAFE -> {
                    cardBackground.setBackgroundResource(R.drawable.bg_safe_hue)
                    statusBadge.text = "Safe"
                    statusBadge.setBackgroundResource(R.drawable.bg_badge_safe)
                    statusBadge.setTextColor(Color.parseColor("#4CAF50"))
                    statusIconSmall.setImageResource(R.drawable.ic_check_circle)
                    statusIconSmall.setColorFilter(Color.parseColor("#4CAF50"))
                    wifiIcon.setColorFilter(Color.parseColor("#4CAF50"))
                    alertCard.visibility = View.GONE
                    recommendationCard.visibility = View.GONE
                }
                NetworkClassification.SUSPICIOUS -> {
                    cardBackground.setBackgroundResource(R.drawable.bg_card_suspicious_hue)
                    statusBadge.text = "Suspicious"
                    statusBadge.setBackgroundResource(R.drawable.bg_badge_suspicious)
                    statusBadge.setTextColor(Color.parseColor("#FF9800"))
                    statusIconSmall.setImageResource(R.drawable.ic_warning)
                    statusIconSmall.setColorFilter(Color.parseColor("#FF9800"))
                    wifiIcon.setColorFilter(Color.parseColor("#FF9800"))
                    alertCard.visibility = View.GONE
                    recommendationCard.visibility = View.GONE
                }
                NetworkClassification.EVIL_TWIN -> {
                    cardBackground.setBackgroundResource(R.drawable.bg_badge_evil)
                    statusBadge.text = "Evil Twin Suspected"
                    statusBadge.setBackgroundResource(R.drawable.badge_danger)
                    statusBadge.setTextColor(Color.parseColor("#F44336"))
                    statusIconSmall.setImageResource(R.drawable.ic_cancel)
                    statusIconSmall.setColorFilter(Color.parseColor("#F44336"))
                    wifiIcon.setColorFilter(Color.parseColor("#F44336"))
                    alertCard.visibility = View.VISIBLE
                    recommendationCard.visibility = View.VISIBLE
                    recommendationText.text = "Recommendation: Do not connect to this network. Report to system administrator if this is a legitimate business network."
                }
            }

            mainContent.setOnClickListener {
                val isVisible = detailsSection.visibility == View.VISIBLE
                detailsSection.visibility = if (isVisible) View.GONE else View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = networks.size
}