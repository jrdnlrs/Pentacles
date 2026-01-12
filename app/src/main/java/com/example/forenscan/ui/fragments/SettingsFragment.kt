package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.forenscan.R

// --- FIX: CONSTANTS MUST BE AT THE TOP LEVEL (OUTSIDE THE CLASS) ---
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SettingsFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find the Spinner in your layout by its ID
        val spinner: android.widget.Spinner = view.findViewById(R.id.background_scan_spinner)

        // 2. Create an Adapter using your custom "spinner_item.xml" layout
        // This connects the data (@array/scan_intervals) to the look (spinner_item)
        val adapter = android.widget.ArrayAdapter.createFromResource(
            requireContext(),
            R.array.scan_intervals,
            R.layout.spinner_item
        )

        // 3. Set the layout for the dropdown list (when you click it)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 4. Apply the adapter to the spinner
        spinner.adapter = adapter
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}