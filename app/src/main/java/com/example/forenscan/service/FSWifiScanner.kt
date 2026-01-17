package com.example.forenscan.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.forenscan.R
import com.example.forenscan.data.models.*
import com.example.forenscan.data.repository.ForensicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class FSWifiScanner : Service() {

    private lateinit var wifiManager: WifiManager
    private lateinit var repository: ForensicRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    // Scan every 30 seconds (Android Limitation)
    private val SCAN_INTERVAL = 30000L
    private var isScanning = false

    // Signal History for Anomaly Detection
    private val signalHistory = mutableMapOf<String, MutableList<Int>>()

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        repository = ForensicRepository(applicationContext)

        // Register Receiver for Scan Results
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(scanReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startScanningLoop()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "ForenScanChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Forensic Scanning", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ForenScan Active")
            .setContentText("Monitoring Wi-Fi environment...")
            .setSmallIcon(R.drawable.ic_shield)
            .build()
        startForeground(1, notification)
    }

    private fun startScanningLoop() {
        if (isScanning) return
        isScanning = true

        val scanRunnable = object : Runnable {
            @SuppressLint("MissingPermission") // We check explicitly below
            override fun run() {
                // 1. Explicit Check for Compiler/Runtime Safety
                if (hasLocationPermission()) {
                    try {
                        // 2. Try-Catch for SecurityException
                        val success = wifiManager.startScan()
                        if (!success) {
                            Log.w("FSWifiScanner", "Scan trigger failed (Throttle or hardware issue)")
                        }
                    } catch (e: SecurityException) {
                        Log.e("FSWifiScanner", "Permission rejected during scan: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("FSWifiScanner", "General scan error: ${e.message}")
                    }
                } else {
                    Log.e("FSWifiScanner", "Missing Location Permission - Cannot Scan")
                }

                // Schedule next scan
                handler.postDelayed(this, SCAN_INTERVAL)
            }
        }
        handler.post(scanRunnable)
    }

    private val scanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission") // We check explicitly below
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

                // 1. Explicit Check before accessing data
                if (hasLocationPermission()) {
                    try {
                        // 2. Accessing scanResults can throw SecurityException
                        val results = wifiManager.scanResults

                        serviceScope.launch {
                            results.forEach { scanResult ->

                                // Logic: Check for Signal Spikes
                                val isSpike = checkSignalAnomaly(scanResult.BSSID, scanResult.level)
                                val classification = if (isSpike) NetworkClassification.SUSPICIOUS else NetworkClassification.SAFE

                                // Create Model
                                val network = WifiNetwork(
                                    ssid = scanResult.SSID,
                                    macAddress = scanResult.BSSID,
                                    encryption = scanResult.capabilities,
                                    frequency = "${scanResult.frequency}MHz",
                                    signalStrength = scanResult.level,
                                    classification = classification,
                                    timestamp = System.currentTimeMillis().toString()
                                )

                                // Save to DB
                                repository.saveNetworkSnapshot(network)

                                // Alert if Suspicious
                                if (isSpike) {
                                    triggerThreatAlert(network)
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("FSWifiScanner", "Permission rejected during result fetch: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("FSWifiScanner", "Error processing scan results: ${e.message}")
                    }
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSignalAnomaly(bssid: String, currentLevel: Int): Boolean {
        val history = signalHistory.getOrPut(bssid) { mutableListOf() }
        history.add(currentLevel)
        if (history.size > 10) history.removeAt(0)

        if (history.size < 3) return false
        val avg = history.average()
        return currentLevel > (avg + 15)
    }

    private suspend fun triggerThreatAlert(network: WifiNetwork) {
        val alert = ThreatAlert(
            id = UUID.randomUUID().toString(),
            title = "Signal Anomaly Detected",
            description = "Sudden signal spike detected for ${network.ssid}.",
            severity = ThreatSeverity.HIGH,
            networkName = network.ssid,
            macAddress = network.macAddress,
            timestamp = System.currentTimeMillis(),
            recommendedAction = "Verify physical location of AP."
        )
        repository.insertThreat(alert)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}