package com.example.forenscan.service
import com.example.forenscan.utils.MLPrediction
import com.example.forenscan.utils.MLModelHelper

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

    // Add model as a property
    private var mlModel: MLModelHelper? = null

    // Signal History for Anomaly Detection
    private val signalHistory = mutableMapOf<String, MutableList<Int>>()

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Managers
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        repository = ForensicRepository(applicationContext)
        threatDetector = ThreatDetector(applicationContext) // Initialize the Hybrid Brain

        // Initialize the ML ModelHelper
        mlModel = try {
            MLModelHelper(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FSWifiScanner", "LiteRT initialization failed: ${e.message}")
            null // Fallback to rule-based detection if model initialization fails
        }

//        // Register Receiver for Scan Results
//        val intentFilter = IntentFilter()
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//        registerReceiver(scanReceiver, intentFilter)

        // Register Receiver for Scan Results
        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
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
                                // Prepare data for ML [cite: 176, 177]
                                val features = extractFeaturesForML(scanResult)

                                // Run LiteRT prediction (Calls CompiledModel.run internally)
                                val probability = mlModel?.predict(features) ?: -1f
                                val prediction = if (probability >= 0) MLPrediction.from(probability) else null

                                // Determine classification: ML primary, fallback to your spike logic [cite: 167, 170]
                                val isEvil = prediction?.isEvilTwin ?: checkSignalAnomaly(scanResult.BSSID, scanResult.level)

                                val network = WifiNetwork(
                                    ssid = scanResult.SSID ?: "Hidden",
                                    macAddress = scanResult.BSSID,
                                    encryption = scanResult.capabilities,
                                    frequency = "${scanResult.frequency}MHz",
                                    signalStrength = scanResult.level,
                                    classification = if (isEvil) NetworkClassification.SUSPICIOUS else NetworkClassification.SAFE,
                                    timestamp = System.currentTimeMillis().toString()
                                )

                                // Save real network data to DB
                                repository.saveNetworkSnapshot(network)

                                // Trigger specific ML Alert if detected [cite: 185, 186]
                                if (prediction?.isEvilTwin == true) {
                                    triggerMLThreatAlert(network, prediction)
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

    private suspend fun triggerMLThreatAlert(network: WifiNetwork, prediction: MLPrediction) {
        val alert = ThreatAlert(
            id = UUID.randomUUID().toString(),
            title = "ML-Detected Evil Twin",
        description = "Machine learning model detected suspicious patterns in '${network.ssid}' with ${(prediction.probability * 100).toInt()}% confidence.",
        severity = if (prediction.probability >= 0.9f) ThreatSeverity.CRITICAL else ThreatSeverity.HIGH,
        networkName = network.ssid,
            macAddress = network.macAddress,
        timestamp = System.currentTimeMillis(),
        recommendedAction = "Avoid connecting to this network. ML confidence: ${prediction.confidence}"
        )
        repository.insertThreat(alert)
    }

    override fun onDestroy() {
        // Critical Change: Properly close the LiteRT resources
        // This ensures the CompiledModel and associated buffers are released
        try {
            mlModel?.close()
            mlModel = null
            unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
            Log.e("FSWifiScanner", "Error during cleanup: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Feature Extraction Logic
    private fun extractFeaturesForML(scanResult: android.net.wifi.ScanResult): FloatArray {
        // 1. Frame protection (Encrypted = 1.0, Open = 0.0) [cite: 218, 222]
        val protection = if (scanResult.capabilities.contains("Open", ignoreCase = true)) 0.0f else 1.0f

        // 2. Signal strength (Normalize -100 to 0 dBm -> 0.0 to 1.0) [cite: 219, 227]
        val signal = (scanResult.level + 100f) / 100f

        // 3. Estimated Data Rate (Proxy based on frequency) [cite: 220, 230]
        val dataRate = if (scanResult.frequency >= 5000) 0.8f else 0.5f

        return floatArrayOf(protection, signal, dataRate)
    }

    /**
     * DETECT ANOMALY:
     * Checks if the signal strength (dBm) suddenly spikes, which often indicates
     * an attacker (Evil Twin) has just activated a stronger transmitter nearby.
     */
    private fun checkSignalAnomaly(bssid: String, currentLevel: Int): Boolean {
        // 1. Get or create the history list for this specific BSSID
        val history = signalHistory.getOrPut(bssid) { mutableListOf() }

        // 2. Add the new reading
        history.add(currentLevel)

        // 3. Maintain a small window (e.g., last 5 scans) to save memory
        if (history.size > 5) {
            history.removeAt(0)
        }

        // 4. We need at least 3 data points to establish a "baseline" average
        if (history.size < 3) return false

        // 5. Calculate Average of previous readings (excluding the current one)
        val previousReadings = history.take(history.size - 1)
        val averageSignal = previousReadings.average()

        // 6. DETECT SPIKE:
        // If current signal is significantly stronger (> 10-15 dBm difference) than the average,
        // it is suspicious. (Note: dBm is negative, so closer to 0 is stronger)
        // Example: Average was -80, Current is -50. Diff is 30.
        val spikeThreshold = 15 // dBm
        val isSpike = (currentLevel - averageSignal) > spikeThreshold

        if (isSpike) {
            Log.w("FSWifiScanner", "ANOMALY: Signal spike detected for $bssid. Avg: $averageSignal, Current: $currentLevel")
        }

        return isSpike
    }

}