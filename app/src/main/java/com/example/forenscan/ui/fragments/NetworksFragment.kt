package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forenscan.R
import com.example.forenscan.data.models.WifiNetwork
import com.example.forenscan.databinding.FragmentNetworksBinding
import com.example.forenscan.ui.adapters.WifiNetworkAdapter
import com.example.forenscan.ui.viewmodel.FSViewModel
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

        // 2. Observe Live Data from ViewModel
        viewModel.networks.observe(viewLifecycleOwner) { networks ->

            // 3. Initialize Adapter with Click Logic
            // We pass the list 'networks' AND the code to run when clicked
            val adapter = WifiNetworkAdapter(networks) { network ->
                openNetworkDetails(network)
            }

            binding.networksRecyclerView.adapter = adapter
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