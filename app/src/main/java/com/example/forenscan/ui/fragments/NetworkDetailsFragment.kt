package com.example.forenscan.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.forenscan.R
import com.example.forenscan.data.models.NetworkClassification
import com.example.forenscan.data.models.WifiNetwork
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class NetworkDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_network_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Unpack Data
        val networkJson = arguments?.getString("network_data")
        val network = if (networkJson != null) {
            Gson().fromJson(networkJson, WifiNetwork::class.java)
        } else {
            null
        }

        if (network == null) {
            parentFragmentManager.popBackStack()
            return
        }

        setupUI(view, network)
    }

    private fun setupUI(view: View, network: WifiNetwork) {
        // --- Populate Basic Text Fields ---
        view.findViewById<TextView>(R.id.detail_ssid).text = network.ssid
        view.findViewById<TextView>(R.id.detail_bssid).text = network.macAddress
        view.findViewById<TextView>(R.id.detail_freq).text = network.frequency
        view.findViewById<TextView>(R.id.detail_security).text = network.encryption
        view.findViewById<TextView>(R.id.detail_signal_text).text = "${network.signalStrength} dBm"

        // Channel Calculation
        val freqInt = network.frequency.replace("MHz", "").trim().toIntOrNull() ?: 0
        val channel = if (freqInt > 2400 && freqInt < 2500) (freqInt - 2407) / 5 else 0
        view.findViewById<TextView>(R.id.detail_channel).text = if (channel > 0) "Ch $channel" else "N/A"

        // --- Signal Icon Color ---
        val signalIcon = view.findViewById<ImageView>(R.id.detail_signal_icon)
        val colorRes = when {
            network.signalStrength > -60 -> android.R.color.holo_green_dark
            network.signalStrength > -80 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        signalIcon.setColorFilter(ContextCompat.getColor(requireContext(), colorRes))

        // --- AUTOMATED ANALYSIS RESULT (Logic) ---
        val cardAnalysis = view.findViewById<MaterialCardView>(R.id.card_analysis_result)
        val imgIcon = view.findViewById<ImageView>(R.id.img_analysis_icon)
        val txtTitle = view.findViewById<TextView>(R.id.txt_analysis_title)
        val txtDesc = view.findViewById<TextView>(R.id.txt_analysis_desc)

        when (network.classification) {
            NetworkClassification.EVIL_TWIN -> {
                // THREAT STYLING (Red)
                cardAnalysis.setCardBackgroundColor(Color.parseColor("#FEE2E2")) // Light Red
                imgIcon.setImageResource(R.drawable.ic_warning)
                imgIcon.setColorFilter(Color.parseColor("#B91C1C")) // Dark Red
                txtTitle.text = "THREAT DETECTED: EVIL TWIN"
                txtTitle.setTextColor(Color.parseColor("#B91C1C"))
                txtDesc.text = "The application has identified this network as a malicious clone. Do not connect."
                txtDesc.setTextColor(Color.parseColor("#991B1B"))
            }
            NetworkClassification.SUSPICIOUS -> {
                // SUSPICIOUS STYLING (Orange)
                cardAnalysis.setCardBackgroundColor(Color.parseColor("#FFEDD5")) // Light Orange
                imgIcon.setImageResource(R.drawable.ic_warning)
                imgIcon.setColorFilter(Color.parseColor("#C2410C")) // Dark Orange
                txtTitle.text = "SUSPICIOUS ACTIVITY"
                txtTitle.setTextColor(Color.parseColor("#C2410C"))
                txtDesc.text = "Anomalies detected in signal strength or encryption. Proceed with caution."
                txtDesc.setTextColor(Color.parseColor("#9A3412"))
            }
            else -> {
                // SAFE STYLING (Green) - Default in XML, but explicit here is safer
                cardAnalysis.setCardBackgroundColor(Color.parseColor("#DCFCE7")) // Light Green
                imgIcon.setImageResource(R.drawable.ic_check_circle) // Ensure you have this icon or use ic_wifi
                imgIcon.setColorFilter(Color.parseColor("#15803D")) // Dark Green
                txtTitle.text = "SAFE NETWORK"
                txtTitle.setTextColor(Color.parseColor("#15803D"))
                txtDesc.text = "System verification complete. No anomalies detected."
                txtDesc.setTextColor(Color.parseColor("#14532D"))
            }
        }

        // --- Close Button ---
        view.findViewById<MaterialButton>(R.id.btn_close_details).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}