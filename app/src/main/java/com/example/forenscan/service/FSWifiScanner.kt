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

    // Add model as a property

    private var mlModel: MLModelHelper? = null

    // Signal History for Anomaly Detection
    private val signalHistory = mutableMapOf<String, MutableList<Int>>()

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        repository = ForensicRepository(applicationContext)

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
                                // Prepare data for ML [cite: 176, 177]
                                val features = extractFeaturesForML(scanResult)

                                // Run LiteRT prediction (Calls CompiledModel.run internally)
                                val probability = mlModel?.predict(features) ?: -1f
                                val prediction = if (probability >= 0) MLPrediction.from(probability) else null

                                // Determine classification: ML primary, fallback to your spike logic [cite: 167, 170]
                                val isEvil = prediction?.isEvilTwin ?: checkSignalAnomaly(scanResult.BSSID, scanResult.level)

                                val network = WifiNetwork(
                                    ssid = scanResult.SSID,
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

}