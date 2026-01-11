package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.forenscan.R
import com.google.android.material.button.MaterialButtonToggleGroup

class ThreatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout (fragment_threats.xml)
        return inflater.inflate(R.layout.fragment_threats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_group)

        // 1. Default Load: Show AlertsFragment when this page opens
        // Note: No import needed for AlertsFragment because we are in the same package!
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.threats_container, AlertsFragment())
                .commit()
        }

        // 2. Toggle Logic: Switch between Alerts and Timeline
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selectedFragment = when (checkedId) {
                    R.id.btn_alerts -> AlertsFragment()
                    R.id.btn_timeline -> TimelineFragment()
                    else -> AlertsFragment()
                }

                // Swap the fragment inside the container
                childFragmentManager.beginTransaction()
                    .replace(R.id.threats_container, selectedFragment)
                    .commit()
            }
        }
    }
}