package com.example.forenscan.ml

import android.content.Context
import com.example.forenscan.data.models.NetworkClassification

/**
 * ThreatDetector
 * * The "Brain" of the operation.
 * - COMBINES: Rule-Based Logic (Fast/Immediate) + Machine Learning (Smart/Future).
 * - Currently uses heuristic checks for Evil Twins and signal spikes.
 * - Ready for TensorFlow Lite integration.
 */
class ThreatDetector(context: Context) {

    // ==========================================
    // 1. MEMORY (For Rule-Based Logic)
    // ==========================================
    // History of Signal Levels (for Spike Detection)
    private val signalHistory = mutableMapOf<String, MutableList<Int>>()

    // History of Known Networks (for Evil Twin Detection)
    // Map of SSID -> BSSID (MAC Address)
    private val knownNetworks = mutableMapOf<String, String>()

    // History of Security Protocols (for Downgrade Attacks)
    // Map of SSID -> Capabilities (e.g., "[WPA2-PSK...]")
    private val knownSecurity = mutableMapOf<String, String>()

    // ==========================================
    // 2. SETUP (For Machine Learning Team)
    // ==========================================
    // private var tflite: Interpreter? = null

    init {
        // TODO: [ML TEAM] Load your TFLite model here
        // try {
        //     val modelFile = FileUtil.loadMappedFile(context, "threat_model.tflite")
        //     tflite = Interpreter(modelFile)
        // } catch (e: IOException) {
        //     e.printStackTrace()
        // }
    }

    /**
     * MAIN ANALYSIS FUNCTION
     * Called by the Service for every single network found.
     * Returns a Classification: SAFE, SUSPICIOUS, or EVIL_TWIN
     */
    fun analyzeNetwork(ssid: String, bssid: String, signalStrength: Int, capabilities: String): NetworkClassification {

        // PHASE A: Rule-Based Checks (The Reflex)
        // Checks for known patterns: Duplicate MACs, Signal Spikes, Protocol Downgrades
        val ruleResult = checkRuleBasedAnomaly(ssid, bssid, signalStrength, capabilities)

        // PHASE B: Machine Learning Checks (The Brain)
        // TODO: [ML TEAM] implementation needed in checkMLPrediction()
        val isMLThreat = checkMLPrediction(ssid, signalStrength, capabilities)

        // DECISION LOGIC:
        // 1. If Rules identify an EVIL TWIN, that is the highest priority.
        if (ruleResult == NetworkClassification.EVIL_TWIN) {
            return NetworkClassification.EVIL_TWIN
        }

        // 2. If EITHER the Rules OR the ML Model flags it as suspicious, mark it.
        if (ruleResult == NetworkClassification.SUSPICIOUS || isMLThreat) {
            return NetworkClassification.SUSPICIOUS
        }

        // 3. Otherwise, it's safe.
        return NetworkClassification.SAFE
    }

    // ==========================================
    // LOGIC 1: RULE-BASED DETECTION
    // ==========================================
    private fun checkRuleBasedAnomaly(ssid: String, bssid: String, signalStrength: Int, capabilities: String): NetworkClassification {

        // RULE 1: EVIL TWIN / DOPPELGÄNGER CHECK
        if (knownNetworks.containsKey(ssid)) {
            val knownBssid = knownNetworks[ssid]

            // If SSID matches but MAC (BSSID) is different...
            if (knownBssid != bssid) {
                // ...AND the security is different (e.g. Real is WPA2, this one is Open)
                val knownCaps = knownSecurity[ssid] ?: ""
                if (knownCaps != capabilities) {
                    return NetworkClassification.EVIL_TWIN
                }
                // If security matches, might be Mesh, but flag as Suspicious to be safe
                return NetworkClassification.SUSPICIOUS
            }
        } else {
            // Memorize new network
            knownNetworks[ssid] = bssid
            knownSecurity[ssid] = capabilities
        }

        // RULE 2: SIGNAL ANOMALY (The "Impossible" Spike)
        if (checkSignalSpike(bssid, signalStrength)) {
            return NetworkClassification.SUSPICIOUS
        }

        // RULE 3: OPEN NETWORK HONEYPOTS
        // If network is Open (No Password) and not a known public hotspot
        val lowerCap = capabilities.lowercase()
        if (!lowerCap.contains("wpa") && !lowerCap.contains("wep") && !lowerCap.contains("sae")) {
            return NetworkClassification.SUSPICIOUS
        }

        return NetworkClassification.SAFE
    }

    private fun checkSignalSpike(bssid: String, currentLevel: Int): Boolean {
        val history = signalHistory.getOrPut(bssid) { mutableListOf() }
        history.add(currentLevel)

        // Keep history short (last 10 scans)
        if (history.size > 10) history.removeAt(0)

        // Need baseline
        if (history.size < 3) return false

        val avg = history.average()
        // If current signal is > 15dB stronger than average, flag it.
        return (currentLevel > avg + 15)
    }

    // ==========================================
    // LOGIC 2: MACHINE LEARNING (Placeholder)
    // ==========================================
    private fun checkMLPrediction(ssid: String, signal: Int, caps: String): Boolean {
        // TODO: [ML TEAM] Implement inference logic here

        // 1. Data Preprocessing (Normalize inputs)
        // val normSignal = (signal + 100) / 100.0f
        // val isEncrypted = if (caps.contains("WPA")) 1.0f else 0.0f

        // 2. Inference (Run TFLite model)
        // val inputs = floatArrayOf(normSignal, isEncrypted)
        // val outputs = FloatArray(1)
        // tflite?.run(inputs, outputs)

        // 3. Return Result (Threshold check)
        // return outputs[0] > 0.85f

        // Default to FALSE (Safe) until model is implemented
        return false
    }
}