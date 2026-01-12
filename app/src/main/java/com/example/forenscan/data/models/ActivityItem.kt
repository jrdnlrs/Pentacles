package com.example.forenscan.data.models

enum class ActivityType {
    SCAN_COMPLETED,
    THREAT_DETECTED,
    WIFI_CONNECTED,
    PROTECTION_ENABLED,
    SYSTEM_STATUS_CHANGE
}

data class ActivityItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: ActivityType
)