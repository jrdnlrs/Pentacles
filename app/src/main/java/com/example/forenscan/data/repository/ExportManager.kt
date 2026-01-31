package com.example.forenscan.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.ThreatAlert
import com.example.forenscan.data.models.WifiNetwork
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context) {

    // ==========================================
    // 1. GENERAL FORENSIC REPORT (Scan Data)
    // ==========================================
    fun generateReport(networks: List<WifiNetwork>, threats: List<ThreatAlert>): Uri? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ForenScan_Report_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            // Header
            writer.append("Type,Timestamp,SSID,BSSID/MAC,Signal,Encryption,Classification,Details\n")

            // Write Threats First (High Priority)
            threats.forEach { threat ->
                val date = formatTime(threat.timestamp)
                writer.append("THREAT,$date,${threat.networkName},${threat.macAddress},N/A,N/A,${threat.severity},${cleanText(threat.description)}\n")
            }

            // Write Networks
            networks.forEach { net ->
                val date = formatTime(net.timestamp.toLongOrNull() ?: System.currentTimeMillis())
                writer.append("NETWORK,$date,${net.ssid},${net.macAddress},${net.signalStrength},${net.encryption},${net.classification},Connected: ${net.isConnected}\n")
            }

            writer.flush()
            writer.close()
            return getUriForFile(file)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // ==========================================
    // 2. TIMELINE EXPORT (CSV)
    // ==========================================
    fun generateTimelineCsv(history: List<ActivityItem>): Uri? {
        val fileName = "ForenScan_Timeline_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            val writer = FileWriter(file)
            writer.append("Event ID,Timestamp,Type,Title,Description\n")

            history.forEach { item ->
                val date = formatTime(item.timestamp)
                // We wrap description in quotes or clean commas to prevent breaking the CSV format
                val cleanDesc = cleanText(item.description)
                writer.append("${item.id},$date,${item.type},${item.title},$cleanDesc\n")
            }
            writer.flush()
            writer.close()
            return getUriForFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // ==========================================
    // 3. TIMELINE EXPORT (JSON)
    // ==========================================
    fun generateTimelineJson(history: List<ActivityItem>): Uri? {
        val fileName = "ForenScan_Timeline_${System.currentTimeMillis()}.json"
        val file = File(context.cacheDir, fileName)

        try {
            val writer = FileWriter(file)
            val jsonBuilder = StringBuilder()

            // Manually build JSON string to avoid adding external dependencies (Gson/Moshi) for now
            jsonBuilder.append("[\n") // Start Array

            history.forEachIndexed { index, item ->
                jsonBuilder.append("  {\n")
                jsonBuilder.append("    \"id\": \"${item.id}\",\n")
                jsonBuilder.append("    \"timestamp\": \"${formatTime(item.timestamp)}\",\n")
                jsonBuilder.append("    \"type\": \"${item.type}\",\n")
                jsonBuilder.append("    \"title\": \"${item.title}\",\n")
                jsonBuilder.append("    \"description\": \"${cleanText(item.description)}\"\n")
                jsonBuilder.append("  }")

                if (index < history.size - 1) {
                    jsonBuilder.append(",")
                }
                jsonBuilder.append("\n")
            }

            jsonBuilder.append("]") // End Array

            writer.write(jsonBuilder.toString())
            writer.flush()
            writer.close()
            return getUriForFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match AndroidManifest provider authority
            file
        )
    }

    private fun formatTime(ts: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
    }

    private fun cleanText(input: String): String {
        // Removes commas or newlines that might break CSV formatting
        return input.replace(",", ";").replace("\n", " ").trim()
    }
}