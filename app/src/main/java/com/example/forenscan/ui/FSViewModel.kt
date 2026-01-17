package com.example.forenscan.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.forenscan.data.models.ThreatAlert
import com.example.forenscan.data.models.WifiNetwork
import com.example.forenscan.data.repository.ForensicRepository
import com.example.forenscan.service.FSWifiScanner
import kotlinx.coroutines.launch

class FSViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForensicRepository(application)

    // --- LIVE DATA ---
    // These automatically update the UI whenever the Database changes
    val networks: LiveData<List<WifiNetwork>> = repository.getAllNetworks().asLiveData()
    val threats: LiveData<List<ThreatAlert>> = repository.getAllThreats().asLiveData()

    // --- SERVICE CONTROLS ---
    fun startScanningService() {
        val intent = Intent(getApplication(), FSWifiScanner::class.java)

        // Android O+ requires startForegroundService for background tasks
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

    // --- EXPORT LOGIC ---
    fun exportLogs() {
        viewModelScope.launch {
            // Placeholder for ExportManager connection
        }
    }
}