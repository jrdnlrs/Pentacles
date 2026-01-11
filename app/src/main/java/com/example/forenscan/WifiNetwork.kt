package com.example.forenscan

enum class NetworkClassification {
    SAFE,
    SUSPICIOUS,
    EVIL_TWIN
}

data class WifiNetwork(
    val ssid: String,
    val macAddress: String,
    val encryption: String,
    val frequency: String,
    val signalStrength: Int,
    val classification: NetworkClassification,
    val isConnected: Boolean = false
)