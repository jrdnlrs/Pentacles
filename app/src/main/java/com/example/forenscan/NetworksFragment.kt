package com.example.forenscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forenscan.databinding.FragmentNetworksBinding

class NetworksFragment : Fragment() {
    private var _binding: FragmentNetworksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNetworksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNetworksList()

        binding.btnRefresh.setOnClickListener {
            startScan()
        }
    }

    private fun startScan() {
        binding.btnRefresh.isEnabled = false
        binding.btnRefresh.text = "Scanning..."
        binding.scanProgress.visibility = View.VISIBLE
        binding.scanStatusText.text = "Detecting Evil Twins..."
        binding.root.postDelayed({
            stopScan()
        }, 3000)
    }

    private fun stopScan() {
        if (_binding == null) return
        binding.btnRefresh.isEnabled = true
        binding.btnRefresh.text = "Refresh"
        binding.scanProgress.visibility = View.GONE
        binding.scanStatusText.text = "Found 5 networks • 1 threats detected"
        setupNetworksList()
    }

    private fun setupNetworksList() {
        val wifiList = listOf(
            WifiNetwork("SM_M...", "00:1A:2B:3C:4D:5E", "WPA2", "2.4GHz", 85, NetworkClassification.SAFE, true),
            WifiNetwork("SM_M...", "A1:B2:C3:D4:E5:F6", "WPA2", "2.4GHz", 82, NetworkClassification.EVIL_TWIN, false),
            WifiNetwork("Barangay_Fr...", "12:34:56:78:9A:BC", "Open", "2.4GHz", 65, NetworkClassification.SUSPICIOUS, false),
            WifiNetwork("Jollibee_Guest", "BB:CC:DD:EE:FF:00", "WPA2", "5GHz", 70, NetworkClassification.SAFE, false),
            WifiNetwork("PLDT_HOME_WIFI", "11:22:33:44:55:66", "WPA3", "5GHz", 45, NetworkClassification.SAFE, false)
        )

        binding.networksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.networksRecyclerView.adapter = WifiNetworkAdapter(wifiList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}