package com.example.forenscan.data.models

data class ThreatAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: ThreatSeverity,
    val networkName: String,
    val macAddress: String,
    val timestamp: Long,
    val isResolved: Boolean = false,
    val recommendedAction: String
)