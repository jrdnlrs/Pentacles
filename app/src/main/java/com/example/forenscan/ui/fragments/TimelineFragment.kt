package com.example.forenscan.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.ActivityType
import com.example.forenscan.ui.adapters.TimelineAdapter
import com.example.forenscan.ui.adapters.TimelineEvent
import com.example.forenscan.ui.viewmodel.FSViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineFragment : Fragment() {

    private val viewModel: FSViewModel by activityViewModels()

    // Data Holders
    private var allEvents: List<ActivityItem> = emptyList()       // The Master List from DB
    private var currentVisibleList: List<ActivityItem> = emptyList() // The Filtered List for UI/Export

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_timeline)
        recycler.layoutManager = LinearLayoutManager(context)
        val spinner = view.findViewById<Spinner>(R.id.spinner_filter)
        val statusText = view.findViewById<TextView>(R.id.tv_status)

        // 1. SETUP SPINNER (The Dropdown)
        val filterOptions = arrayOf("All Events", "Evil Twin Only")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
        spinner.adapter = spinnerAdapter

        // 2. SETUP SPINNER LISTENER
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyFilter(position, recycler, statusText)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 3. OBSERVE REAL DATA
        viewModel.history.observe(viewLifecycleOwner) { activities ->
            allEvents = activities // Save master list
            // Re-apply whatever filter is currently selected
            applyFilter(spinner.selectedItemPosition, recycler, statusText)
        }

        // 4. SETUP EXPORT BUTTONS
        setupExportButtons(view)
    }

    // --- FILTER ENGINE ---
    private fun applyFilter(position: Int, recycler: RecyclerView, statusText: TextView) {
        // Filter the master list based on spinner position
        currentVisibleList = if (position == 1) {
            // Index 1 = "Evil Twin Only"
            allEvents.filter { it.type == ActivityType.THREAT_DETECTED }
        } else {
            // Index 0 = "All Events"
            allEvents
        }

        // Update Status Text
        statusText.text = "Showing ${currentVisibleList.size} events • Filter: ${if (position == 1) "Critical" else "All"}"

        // Convert to UI Items for Adapter
        val uiEvents = currentVisibleList.map { item ->
            TimelineEvent(
                title = item.title,
                description = item.description,
                timestamp = formatTimestamp(item.timestamp),
                ssid = "System",
                isCritical = item.type == ActivityType.THREAT_DETECTED,
                techDetails = listOf("Event ID: ${item.id.take(8)}", "Type: ${item.type}")
            )
        }
        recycler.adapter = TimelineAdapter(uiEvents)
    }

    // --- EXPORT LOGIC ---
    private fun setupExportButtons(view: View) {
        val btnJson = view.findViewById<Button>(R.id.btn_export_json)
        val btnCsv = view.findViewById<Button>(R.id.btn_export_csv)

        // Click Listeners pass 'currentVisibleList' so we only export what we see
        btnJson.setOnClickListener {
            if (currentVisibleList.isNotEmpty()) {
                Toast.makeText(context, "Exporting Filtered JSON...", Toast.LENGTH_SHORT).show()
                viewModel.exportTimeline("JSON", currentVisibleList) // Passes the filtered list
            } else {
                Toast.makeText(context, "No events to export.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCsv.setOnClickListener {
            if (currentVisibleList.isNotEmpty()) {
                Toast.makeText(context, "Exporting Filtered CSV...", Toast.LENGTH_SHORT).show()
                viewModel.exportTimeline("CSV", currentVisibleList) // Passes the filtered list
            } else {
                Toast.makeText(context, "No events to export.", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe Export Result (Launch Share Sheet)
        viewModel.timelineExportStatus.observe(viewLifecycleOwner) { fileUri ->
            if (fileUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/*"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Save Forensic Log To..."))
                viewModel.timelineExportStatus.postValue(null)
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}