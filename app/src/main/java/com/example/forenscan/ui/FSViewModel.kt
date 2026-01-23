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
    private val exportManager = ExportManager(application) // Connects to the ExportManager

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

    // --- FUNCTION 1: Export General Report (Settings Tab) ---
    fun exportLogs() {
        viewModelScope.launch {
            val currentNetworks = repository.getAllNetworks().first()
            val currentThreats = repository.getAllThreats().first()
            val fileUri = exportManager.generateReport(currentNetworks, currentThreats)
            exportStatus.postValue(fileUri)
        }
    }

    // --- FUNCTION 2: Export Timeline (Timeline Tab) ---
    // This is the missing function causing your error!
    fun exportTimeline(format: String) {
        viewModelScope.launch {
            val currentHistory = repository.getRecentActivity().first()

            val uri = if (format == "JSON") {
                exportManager.generateTimelineJson(currentHistory)
            } else {
                exportManager.generateTimelineCsv(currentHistory)
            }

            timelineExportStatus.postValue(uri)
        }
    }
    fun resolveThreat(threatId: String) {
        viewModelScope.launch {
            // Find the threat and mark it as resolved
            val currentList = threats.value ?: return@launch
            val threatToUpdate = currentList.find { it.id == threatId }

            if (threatToUpdate != null) {
                // Create a copy with isResolved = true
                val updatedThreat = threatToUpdate.copy(isResolved = true)

                // Update in Database (Insert with same ID = Overwrite)
                repository.insertThreat(updatedThreat)
            }
        }
    }
}