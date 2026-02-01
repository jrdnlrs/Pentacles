package com.example.forenscan.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.ActivityType
import com.example.forenscan.data.models.viewmodel.FSViewModel
import com.example.forenscan.ui.adapters.TimelineAdapter
import com.example.forenscan.ui.adapters.TimelineEvent
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineFragment : Fragment() {

    // 1. Connect to the Shared ViewModel located in data.models
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

        // 1. Bind UI elements
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_timeline)
        val exportMenu = view.findViewById<TextInputLayout>(R.id.menu_export)
        val dropdownText = view.findViewById<AutoCompleteTextView>(R.id.export_dropdown)
        val exportHint = view.findViewById<TextView>(R.id.tv_export_hint)

        recycler.layoutManager = LinearLayoutManager(context)
        val spinner = view.findViewById<Spinner>(R.id.spinner_filter)
        val statusText = view.findViewById<TextView>(R.id.tv_status)

        // 2. Observe REAL Data from Database for forensic analysis
        viewModel.history.observe(viewLifecycleOwner) { activities ->
            val timelineEvents = activities.map { item ->
                TimelineEvent(
                    title = item.title,
                    description = item.description,
                    timestamp = formatTimestamp(item.timestamp),
                    ssid = "System",
                    isCritical = item.type == ActivityType.THREAT_DETECTED,
                    techDetails = listOf("Event ID: ${item.id.take(8)}", "Type: ${item.type}")
                )
            }
            recycler.adapter = TimelineAdapter(timelineEvents)
        }

        // 3. Observe the "Export Enabled" toggle state from FSViewModel
        viewModel.isExportEnabled.observe(viewLifecycleOwner) { isEnabled ->
            // Controls the "initial look" and lock based on Settings Fragment
            exportMenu.isEnabled = isEnabled

            if (isEnabled) {
                // Feature Enabled: Hide hint and use active colors
                exportHint.visibility = View.GONE
                dropdownText.setTextColor(Color.parseColor("#475569"))
            } else {
                // Feature Disabled: Show hint and use disabled gray
                exportHint.visibility = View.VISIBLE
                dropdownText.setTextColor(Color.parseColor("#CBD5E1"))
                dropdownText.setText("Export to:", false)
            }
        }

        // 4. Setup the "Export to:" Dropdown Menu (JSON, CSV, PDF)
        setupExportDropdown(view)

        // 5. Observe the Export result to trigger the Share Sheet
        observeExportStatus()
    }

    private fun setupExportDropdown(view: View) {
        val dropdownText = view.findViewById<AutoCompleteTextView>(R.id.export_dropdown)

        // Define forensic export options
        val exportOptions = arrayOf("JSON", "CSV", "PDF")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, exportOptions)
        dropdownText.setAdapter(adapter)

        dropdownText.setOnItemClickListener { _, _, position, _ ->
            val selectedFormat = exportOptions[position]
            Toast.makeText(context, "Generating $selectedFormat Report...", Toast.LENGTH_SHORT).show()

            // Trigger the ViewModel export logic
            viewModel.exportTimeline(selectedFormat)

            // Reset dropdown text to "Export to:" after selection
            dropdownText.post {
                dropdownText.setText("Export to:", false)
            }
        }
    }

    private fun observeExportStatus() {
        viewModel.timelineExportStatus.observe(viewLifecycleOwner) { fileUri ->
            if (fileUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    // Handles both text-based logs and PDF documents
                    type = if (fileUri.toString().contains("pdf", ignoreCase = true)) {
                        "application/pdf"
                    } else {
                        "text/*"
                    }
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Save Forensic Log To..."))

                // Reset status to prevent triggering the share sheet again on rotation
                viewModel.timelineExportStatus.postValue(null)
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}