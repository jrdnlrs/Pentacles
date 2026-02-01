package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.example.forenscan.R
import com.example.forenscan.data.models.WifiNetwork
import com.example.forenscan.databinding.FragmentNetworksBinding
import com.example.forenscan.ui.adapters.WifiNetworkAdapter
import com.example.forenscan.ui.viewmodel.FSViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson

class NetworksFragment : Fragment() {

    private var _binding: FragmentNetworksBinding? = null
    private val binding get() = _binding!!

    // Connect to the shared ViewModel to get live data
    private val viewModel: FSViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Layout Manager
        binding.networksRecyclerView.layoutManager = LinearLayoutManager(context)

        // 2. Setup Refresh Button & Progress Bar
        setupRefreshLogic() // <--- Add this function call

        // 3. Observe Live Data
        viewModel.networks.observe(viewLifecycleOwner) { networks ->
            // Update the "Found X networks" text
            val threatCount = networks.count { it.classification.name != "SAFE" }
            binding.scanStatusText.text = "Found ${networks.size} networks • $threatCount threats detected"

            // Hide progress bar when data arrives
            binding.scanProgress.visibility = View.GONE
            binding.btnRefresh.isEnabled = true

            val adapter = WifiNetworkAdapter(networks) { network ->
                openNetworkDetails(network)
            }
            binding.networksRecyclerView.adapter = adapter
        }
    }

    private fun setupRefreshLogic() {
        binding.btnRefresh.setOnClickListener {
            // Visual Feedback
            binding.btnRefresh.isEnabled = false // Prevent double clicking
            binding.scanProgress.visibility = View.VISIBLE
            binding.scanStatusText.text = "Scanning nearby networks..."

            // Trigger Scan via ViewModel
            viewModel.startScanningService()

            // Show toast
            Toast.makeText(context, "Scanning...", Toast.LENGTH_SHORT).show()

            // FIX: Use binding.root.postDelayed and check if binding is null to avoid crashes
            binding.root.postDelayed({
                // Check if _binding is NOT null (meaning the view still exists)
                if (_binding != null) {
                    binding.btnRefresh.isEnabled = true
                    binding.scanProgress.visibility = View.GONE
                }
            }, 3000)
        }
    }

    // --- NAVIGATION LOGIC ---
    private fun openNetworkDetails(network: WifiNetwork) {
        // 1. Convert the Network object to a JSON string so we can pass it
        val gson = Gson()
        val networkJson = gson.toJson(network)

        // 2. Create the Details Fragment and attach the data
        val detailsFragment = NetworkDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("network_data", networkJson)
            }
        }

        // 3. Perform the screen transition
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.networks_fragment_container, detailsFragment)
            .addToBackStack(null) // This lets the user press "Back" to return to the list
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}