package com.example.forenscan.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.forenscan.data.models.ActivityItem
import com.example.forenscan.data.models.SystemStats
import com.example.forenscan.data.models.ThreatAlert
import com.example.forenscan.data.models.WifiNetwork
import com.example.forenscan.data.repository.ExportManager
import com.example.forenscan.data.repository.ForensicRepository
import com.example.forenscan.service.FSWifiScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FSViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForensicRepository(application)
    private val exportManager = ExportManager(application)

    // --- LIVE DATA (Database -> UI) ---
    val networks: LiveData<List<WifiNetwork>> = repository.getAllNetworks().asLiveData()
    val threats: LiveData<List<ThreatAlert>> = repository.getAllThreats().asLiveData()
    val stats: LiveData<SystemStats?> = repository.getSystemStats().asLiveData()
    val history: LiveData<List<ActivityItem>> = repository.getRecentActivity().asLiveData()

    // --- EXPORT STATUS (UI listens to these) ---
    val exportStatus = MutableLiveData<Uri?>()          // For General Report
    val timelineExportStatus = MutableLiveData<Uri?>()  // For Timeline Report (JSON/CSV)

    // --- SERVICE CONTROLS ---
    fun startScanningService() {
        val intent = Intent(getApplication(), FSWifiScanner::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopScanningService() {
        val intent = Intent(getApplication(), FSWifiScanner::class.java)
        getApplication<Application>().stopService(intent)
    }

    fun isMLModelAvailable(): Boolean {
        return try {
            val helper = com.example.forenscan.utils.MLModelHelper(getApplication())
            val available = helper != null
            helper.close()
            available
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkStatus(): Map<String, String> {
        val wifiManager = getApplication<Application>().getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val info = wifiManager.connectionInfo
        val dhcp = wifiManager.dhcpInfo

        // 1. IP Address
        val ipString = if (info.ipAddress != 0) {
            android.text.format.Formatter.formatIpAddress(info.ipAddress)
        } else {
            "Disconnected"
        }

        // 2. Band / Frequency
        val frequency = info.frequency
        val band = when {
            frequency > 5000 -> "5GHz"
            frequency > 2400 -> "2.4GHz"
            else -> "Unknown"
        }

        // 3. Gateway (Router IP)
        val gateway = android.text.format.Formatter.formatIpAddress(dhcp.gateway)

        return mapOf(
            "IP Address" to ipString,
            "Gateway" to gateway,
            "Band" to band,
            "Speed" to "${info.linkSpeed} Mbps"
        )
    }

    // --- EXPORT LOGIC ---
    fun exportLogs() {
        viewModelScope.launch {
            val currentNetworks = repository.getAllNetworks().first()
            val currentThreats = repository.getAllThreats().first()
            val fileUri = exportManager.generateReport(currentNetworks, currentThreats)
            exportStatus.postValue(fileUri)
        }
    }

    // --- UPDATED FUNCTION: Export Timeline ---
    // Now accepts an optional 'filteredList' so you can export exactly what is on screen
    fun exportTimeline(format: String, filteredList: List<ActivityItem>? = null) {
        viewModelScope.launch {
            // 1. If the UI sent a list, use it. Otherwise, fetch all from DB.
            val dataToExport = filteredList ?: repository.getRecentActivity().first()

            // 2. Pass that data to your ExportManager
            val uri = if (format == "JSON") {
                exportManager.generateTimelineJson(dataToExport)
            } else {
                exportManager.generateTimelineCsv(dataToExport)
            }

            // 3. Notify UI
            timelineExportStatus.postValue(uri)
        }
    }

    fun resolveThreat(threatId: String) {
        viewModelScope.launch {
            val currentList = threats.value ?: return@launch
            val threatToUpdate = currentList.find { it.id == threatId }

            if (threatToUpdate != null) {
                val updatedThreat = threatToUpdate.copy(isResolved = true)
                repository.insertThreat(updatedThreat)
            }
        }
    }
}