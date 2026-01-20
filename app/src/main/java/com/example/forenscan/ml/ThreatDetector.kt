package com.example.forenscan.ml

import android.content.Context
import kotlin.math.abs

/**
 * ThreatDetector
 * * The "Brain" of the operation.
 * - COMBINES: Rule-Based Logic (Fast) + Machine Learning (Smart).
 * - Currently uses a heuristic check for signal spikes.
 * - Ready for TensorFlow Lite integration.
 */
class ThreatDetector(context: Context) {

    // --- 1. MEMORY (For Rule-Based Logic) ---
    // We need to remember previous signal levels to detect "spikes"
    private val signalHistory = mutableMapOf<String, MutableList<Int>>()

    // --- 2. SETUP (For Machine Learning) ---
    // private var tflite: Interpreter? = null
    init {
        // Future: Load your TFLite model here
        // loadModel(context)
    }

    /**
     * MAIN ANALYSIS FUNCTION
     * Called by the Service for every single network found.
     * Returns TRUE if the network is suspicious.
     */
    fun analyzeNetwork(ssid: String, bssid: String, signalStrength: Int, encryption: String): Boolean {

        // CHECK A: Rule-Based (The Reflex)
        // Did the signal jump up suddenly? (Indicative of an Evil Twin turning on nearby)
        val isRuleThreat = checkRuleBasedAnomaly(bssid, signalStrength)

        // CHECK B: Machine Learning (The Brain)
        // Does the data match known attack patterns?
        val isMLThreat = checkMLPrediction(ssid, signalStrength, encryption)

        // DECISION:
        // If EITHER method flags it, we treat it as a threat.
        return isRuleThreat || isMLThreat
    }

    // ==========================================
    // LOGIC 1: RULE-BASED DETECTION
    // ==========================================
    private fun checkRuleBasedAnomaly(bssid: String, currentLevel: Int): Boolean {
        val history = signalHistory.getOrPut(bssid) { mutableListOf() }

        // Add current signal to history
        history.add(currentLevel)

        // Maintain a small window (last 10 scans) to save memory
        if (history.size > 10) history.removeAt(0)

        // We need at least 3 data points to establish a "baseline" average
        if (history.size < 3) return false

        val avg = history.average()

        // THE RULE:
        // If the current signal is 15dBm stronger than the average,
        // it means a transmitter suddenly appeared much closer to you.
        return (currentLevel > avg + 15)
    }

    // ==========================================
    // LOGIC 2: MACHINE LEARNING (Placeholder)
    // ==========================================
    private fun checkMLPrediction(ssid: String, signal: Int, caps: String): Boolean {
        // 1. Data Preprocessing (Normalize inputs)
        val normSignal = (signal + 100) / 100.0f

        // 2. Inference (This is where your .tflite model will run)
        // val inputs = floatArrayOf(normSignal)
        // val outputs = FloatArray(1)
        // tflite?.run(inputs, outputs)

        // 3. Return Result
        // return outputs[0] > 0.85f

        // For now, return false so we rely on the Rule-Based check
        return false
    }
}