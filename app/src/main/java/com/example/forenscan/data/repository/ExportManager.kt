package com.example.forenscan.data.repository

import android.content.Context
import com.example.forenscan.data.models.* // Import all your models (ThreatAlert, ActivityItem, WifiNetwork, SystemStats)
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ============================================
// EXPORT MANAGER
// ============================================

/**
 * ExportManager - Handles CSV/JSON export
 *
 * Implements NFDLC Presentation Phase
 */
class ExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Export all forensic data to CSV
     * Returns the File path
     */
    suspend fun exportToCSV(
        threats: List<ThreatAlert>,
        events: List<ActivityItem>, // Updated from ActivityEvent
        networks: List<WifiNetwork> // Updated from NetworkData
    ): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "forensic_report_$timestamp.csv"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            // CSV Header
            writer.append("Forensic Report - WiFi Evil Twin Detector\n")
            writer.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.append("\n")

            // Threat Alerts Section
            writer.append("=== THREAT ALERTS ===\n")
            writer.append("ID,Title,Description,Severity,Network,Timestamp,Resolved,Action\n")
            threats.forEach { threat ->
                writer.append("${escapeCsv(threat.id)},")
                writer.append("${escapeCsv(threat.title)},")
                writer.append("${escapeCsv(threat.description)},")
                writer.append("${threat.severity.name},")
                writer.append("${escapeCsv(threat.networkName)},")
                writer.append("${formatTimestamp(threat.timestamp)},")
                writer.append("${threat.isResolved},")
                writer.append("${escapeCsv(threat.recommendedAction)}\n")
            }
            writer.append("\n")

            // Activity Timeline Section
            writer.append("=== ACTIVITY TIMELINE ===\n")
            writer.append("ID,Title,Description,Timestamp,Type\n")
            events.forEach { event ->
                writer.append("${escapeCsv(event.id)},")
                writer.append("${escapeCsv(event.title)},")
                writer.append("${escapeCsv(event.description)},")
                writer.append("${formatTimestamp(event.timestamp)},")
                writer.append("${event.type.name}\n")
            }
            writer.append("\n")

            // Network Scan History Section
            writer.append("=== NETWORK SCAN HISTORY ===\n")
            writer.append("SSID,BSSID,Signal,Frequency,Security,Timestamp,Connected,IsEvilTwin\n")
            networks.forEach { network ->
                writer.append("${escapeCsv(network.ssid)},")
                writer.append("${network.macAddress},") // Updated: bssid -> macAddress
                writer.append("${network.signalStrength},")
                writer.append("${network.frequency},")
                writer.append("${network.encryption},") // Updated: securityType -> encryption
                // Timestamp is missing from WifiNetwork model, using current time for CSV row or 0
                // Ideally, add 'timestamp' to WifiNetwork model. For now, we will assume 0 or current.
                // But looking at your WifiNetwork model, it doesn't have a timestamp field.
                // We will skip timestamp or use a placeholder.
                writer.append("${formatTimestamp(System.currentTimeMillis())},")
                writer.append("${network.isConnected},")
                // Check classification instead of isDuplicate
                val isEvilTwin = network.classification == NetworkClassification.EVIL_TWIN
                writer.append("$isEvilTwin\n")
            }
        }

        return file
    }

    /**
     * Export all forensic data to JSON
     * Returns the File path
     */
    suspend fun exportToJSON(
        threats: List<ThreatAlert>,
        events: List<ActivityItem>, // Updated
        networks: List<WifiNetwork>, // Updated
        stats: SystemStats
    ): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "forensic_report_$timestamp.json"
        val file = File(context.getExternalFilesDir(null), fileName)

        val rootObject = JSONObject().apply {
            put("report_type", "WiFi Evil Twin Forensic Report")
            put("generated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
            put("app_version", "1.0.0")

            // System Statistics
            put("system_stats", JSONObject().apply {
                put("total_networks_scanned", stats.networksScanned)
                put("total_threats_detected", stats.threatsDetected)
                put("total_scans", stats.totalScans)
                put("last_scan_time", formatTimestamp(stats.lastScanTime))
            })

            // Threat Alerts
            put("threat_alerts", JSONArray().apply {
                threats.forEach { threat ->
                    put(JSONObject().apply {
                        put("id", threat.id)
                        put("title", threat.title)
                        put("description", threat.description)
                        put("severity", threat.severity.name)
                        put("network_name", threat.networkName)
                        put("timestamp", formatTimestamp(threat.timestamp))
                        put("timestamp_unix", threat.timestamp)
                        put("is_resolved", threat.isResolved)
                        put("recommended_action", threat.recommendedAction)
                    })
                }
            })

            // Activity Timeline
            put("activity_timeline", JSONArray().apply {
                events.forEach { event ->
                    put(JSONObject().apply {
                        put("id", event.id)
                        put("title", event.title)
                        put("description", event.description)
                        put("timestamp", formatTimestamp(event.timestamp))
                        put("timestamp_unix", event.timestamp)
                        put("type", event.type.name)
                    })
                }
            })

            // Network Scan History
            put("network_scan_history", JSONArray().apply {
                networks.forEach { network ->
                    put(JSONObject().apply {
                        put("ssid", network.ssid)
                        put("bssid", network.macAddress) // Updated
                        put("signal_strength_dbm", network.signalStrength)
                        put("frequency", network.frequency)
                        put("security_type", network.encryption) // Updated
                        put("is_connected", network.isConnected)
                        put("classification", network.classification.name) // Updated
                    })
                }
            })
        }

        // Write to file with pretty printing
        FileWriter(file).use { writer ->
            writer.write(rootObject.toString(2)) // Indent with 2 spaces
        }

        return file
    }

    /**
     * Generate human-readable forensic report (TXT)
     */
    suspend fun generateForensicReport(
        threats: List<ThreatAlert>,
        events: List<ActivityItem>, // Updated
        stats: SystemStats
    ): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "forensic_report_$timestamp.txt"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            writer.append("═══════════════════════════════════════════════════════\n")
            writer.append("     FORENSIC REPORT: EVIL TWIN ATTACK DETECTION\n")
            writer.append("═══════════════════════════════════════════════════════\n\n")

            writer.append("Generated: ${SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.append("Report ID: ${UUID.randomUUID()}\n")
            writer.append("Device: Android ${android.os.Build.VERSION.RELEASE}\n\n")

            // Executive Summary
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("EXECUTIVE SUMMARY\n")
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("Total Networks Scanned: ${stats.networksScanned}\n")
            writer.append("Total Threats Detected: ${stats.threatsDetected}\n")
            writer.append("Total Scans Performed: ${stats.totalScans}\n")
            writer.append("Last Scan: ${formatTimestamp(stats.lastScanTime)}\n\n")

            // Threat Assessment
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("THREAT ASSESSMENT\n")
            writer.append("─────────────────────────────────────────────────────────\n")

            if (threats.isEmpty()) {
                writer.append("✓ No threats detected. All networks appear legitimate.\n\n")
            } else {
                val critical = threats.count { it.severity == ThreatSeverity.CRITICAL }
                val high = threats.count { it.severity == ThreatSeverity.HIGH }
                val medium = threats.count { it.severity == ThreatSeverity.MEDIUM }
                val low = threats.count { it.severity == ThreatSeverity.LOW }

                writer.append("⚠ THREATS IDENTIFIED:\n")
                if (critical > 0) writer.append("  • CRITICAL: $critical\n")
                if (high > 0) writer.append("  • HIGH: $high\n")
                if (medium > 0) writer.append("  • MEDIUM: $medium\n")
                if (low > 0) writer.append("  • LOW: $low\n\n")

                // Detailed Threat List
                writer.append("DETAILED THREAT ANALYSIS:\n\n")
                threats.forEachIndexed { index, threat ->
                    writer.append("${index + 1}. ${threat.title}\n")
                    writer.append("   Severity: ${threat.severity.name}\n")
                    writer.append("   Network: ${threat.networkName}\n")
                    writer.append("   Detected: ${formatTimestamp(threat.timestamp)}\n")
                    writer.append("   Status: ${if (threat.isResolved) "RESOLVED" else "ACTIVE"}\n")
                    writer.append("   Description: ${threat.description}\n")
                    writer.append("   Recommended Action: ${threat.recommendedAction}\n\n")
                }
            }

            // Timeline Reconstruction
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("TIMELINE RECONSTRUCTION\n")
            writer.append("─────────────────────────────────────────────────────────\n")

            events.forEach { event ->
                writer.append("[${formatTimestamp(event.timestamp)}] ")
                writer.append("${event.type.name}: ${event.title}\n")
                if (event.description.isNotEmpty()) {
                    writer.append("   ${event.description}\n")
                }
                writer.append("\n")
            }

            // Legal Notice
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("LEGAL NOTICE\n")
            writer.append("─────────────────────────────────────────────────────────\n")
            writer.append("This report contains forensic evidence collected in\n")
            writer.append("accordance with the Network Forensic Development Life\n")
            writer.append("Cycle (NFDLC) methodology.\n\n")
            writer.append("Data Privacy: This report contains only network metadata.\n")
            writer.append("No personal communications or packet contents were captured.\n")
            writer.append("Compliant with R.A. 10173 (Data Privacy Act of 2012).\n\n")

            writer.append("═══════════════════════════════════════════════════════\n")
            writer.append("                    END OF REPORT\n")
            writer.append("═══════════════════════════════════════════════════════\n")
        }

        return file
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    private fun escapeCsv(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}