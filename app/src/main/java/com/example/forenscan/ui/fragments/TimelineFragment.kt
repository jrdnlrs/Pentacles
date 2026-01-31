package com.example.forenscan.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forenscan.R
import com.example.forenscan.data.models.ActivityType
import com.example.forenscan.ui.adapters.TimelineAdapter
import com.example.forenscan.ui.adapters.TimelineEvent
import com.example.forenscan.ui.viewmodel.FSViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineFragment : Fragment() {

    // 1. Connect to the Shared ViewModel
    private val viewModel: FSViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_timeline, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Setup the RecyclerView (List of Events)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_timeline)
        recycler.layoutManager = LinearLayoutManager(context)

        // 3. Observe REAL Data from Database
        viewModel.history.observe(viewLifecycleOwner) { activities ->
            // Convert database items to UI items
            val timelineEvents = activities.map { item ->
                TimelineEvent(
                    title = item.title,
                    description = item.description,
                    timestamp = formatTimestamp(item.timestamp),
                    ssid = "System", // Default tag, you can adjust this if needed
                    isCritical = item.type == ActivityType.THREAT_DETECTED,
                    techDetails = listOf("Event ID: ${item.id.take(8)}", "Type: ${item.type}")
                )
            }

            // If the list is empty, you might want to show a "No Activity" text here later
            // For now, just set the adapter
            recycler.adapter = TimelineAdapter(timelineEvents)
        }

        // 4. Setup the Export Buttons
        setupExportButtons(view)
    }

    private fun setupExportButtons(view: View) {
        val btnJson = view.findViewById<Button>(R.id.btn_export_json)
        val btnCsv = view.findViewById<Button>(R.id.btn_export_csv)

        // --- JSON BUTTON CLICK ---
        btnJson.setOnClickListener {
            btnJson.isEnabled = false
            Toast.makeText(context, "Generating JSON Log...", Toast.LENGTH_SHORT).show()
            viewModel.exportTimeline("JSON")
            btnJson.postDelayed({ btnJson.isEnabled = true }, 1000)
        }

        // --- CSV BUTTON CLICK ---
        btnCsv.setOnClickListener {
            btnCsv.isEnabled = false
            Toast.makeText(context, "Generating CSV Report...", Toast.LENGTH_SHORT).show()
            viewModel.exportTimeline("CSV")
            btnCsv.postDelayed({ btnCsv.isEnabled = true }, 1000)
        }

        // --- OBSERVE EXPORT RESULT (FIXED) ---
        // We use 'it' to refer to the URI passed by the LiveData
        viewModel.timelineExportStatus.observe(viewLifecycleOwner) { fileUri ->
            if (fileUri != null) {
                // Open the Android Share Sheet
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/*" // Supports both CSV and JSON
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Save Forensic Log To..."))

                // Reset the status so it doesn't trigger again
                viewModel.timelineExportStatus.postValue(null)
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}