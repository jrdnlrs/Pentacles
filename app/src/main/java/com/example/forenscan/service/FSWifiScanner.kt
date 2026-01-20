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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.forenscan.R
import com.example.forenscan.data.models.*
import com.example.forenscan.data.repository.ForensicRepository
import com.example.forenscan.ml.ThreatDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class FSWifiScanner : Service() {

    // --- Core System Managers ---
    private lateinit var wifiManager: WifiManager
    private lateinit var repository: ForensicRepository
    private lateinit var threatDetector: ThreatDetector

    // --- Threading & Timing ---
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_INTERVAL = 30000L // 30 Seconds (Android restriction)
    private var isScanning = false

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Managers
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        repository = ForensicRepository(applicationContext)
        threatDetector = ThreatDetector(applicationContext) // Initialize the Hybrid Brain

        // 2. Register Receiver to listen for scan results
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(scanReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startScanningLoop()
        return START_STICKY
    }

    // --- FOREGROUND NOTIFICATION (Required for Background Scans) ---
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

    // --- THE SCAN LOOP ---
    private fun startScanningLoop() {
        if (isScanning) return
        isScanning = true

        val scanRunnable = object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                // 1. Safety Check: Do we have permission?
                if (hasLocationPermission()) {
                    try {
                        // 2. Trigger the scan
                        val success = wifiManager.startScan()
                        if (!success) {
                            Log.w("FSWifiScanner", "Scan trigger throttled by Android system.")
                        }
                    } catch (e: SecurityException) {
                        Log.e("FSWifiScanner", "Permission rejected during scan start: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("FSWifiScanner", "General scan error: ${e.message}")
                    }
                } else {
                    Log.e("FSWifiScanner", "Missing Location Permission - Cannot Scan")
                }

                // 3. Repeat
                handler.postDelayed(this, SCAN_INTERVAL)
            }
        }
        handler.post(scanRunnable)
    }

    // --- RECEIVER (Where the Data Arrives) ---
    private val scanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

                if (hasLocationPermission()) {
                    try {
                        // A. Get Raw Scan Results
                        val results = wifiManager.scanResults

                        // B. Get Current Connection Info (To identify "Active" network)
                        val connectionInfo = wifiManager.connectionInfo
                        val currentBSSID = connectionInfo.bssid // MAC of the router we are currently using

                        serviceScope.launch {
                            results.forEach { scanResult ->

                                // 1. Check Connection Status
                                // If the MAC matches our current router, mark it as connected
                                val isConnected = (scanResult.BSSID == currentBSSID)

                                // 2. Hybrid Detection (ML + Rules)
                                val isEvilTwin = threatDetector.analyzeNetwork(
                                    ssid = scanResult.SSID,
                                    bssid = scanResult.BSSID,
                                    signalStrength = scanResult.level,
                                    encryption = scanResult.capabilities
                                )

                                // 3. Classify
                                val classification = if (isEvilTwin) NetworkClassification.EVIL_TWIN else NetworkClassification.SAFE

                                // 4. Create the Data Model
                                val network = WifiNetwork(
                                    ssid = scanResult.SSID,
                                    macAddress = scanResult.BSSID,
                                    encryption = scanResult.capabilities,
                                    frequency = "${scanResult.frequency}MHz",
                                    signalStrength = scanResult.level,
                                    classification = classification,
                                    timestamp = System.currentTimeMillis().toString(),
                                    isConnected = isConnected // Pass status to UI
                                )

                                // 5. Save to Database
                                repository.saveNetworkSnapshot(network)

                                // 6. Alert if Threat Found
                                if (isEvilTwin) {
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

    // --- HELPERS ---

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun triggerThreatAlert(network: WifiNetwork) {
        val alert = ThreatAlert(
            id = UUID.randomUUID().toString(),
            title = "Potential Evil Twin Detected",
            description = "Suspicious signal patterns detected for '${network.ssid}'. Possible cloning attempt.",
            severity = ThreatSeverity.HIGH,
            networkName = network.ssid,
            macAddress = network.macAddress,
            timestamp = System.currentTimeMillis(),
            recommendedAction = "Disconnect immediately and verify the Access Point physically."
        )
        repository.insertThreat(alert)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered if setup failed
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}