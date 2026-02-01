package com.example.forenscan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Table for Network Snapshots
@Entity(tableName = "network_data")
data class NetworkDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val frequency: Int,      // Stored as Int (e.g., 2412)
    val channel: Int,        // Calculated Channel
    val securityType: String,
    val timestamp: Long,
    val isConnected: Boolean,
    val classification: String // UPDATED: Was Boolean, now String to store "SUSPICIOUS"
)

// 2. Table for Threat Alerts
@Entity(tableName = "threat_alerts")
data class ThreatAlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val severity: String,    // Stored as String (Enum converted)
    val networkName: String,
    val macAddress: String,
    val timestamp: Long,
    val isResolved: Boolean,
    val recommendedAction: String
)

// 3. Table for Activity Timeline
@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String         // Stored as String
)

// 4. Table for System Stats (Counters)
@Entity(tableName = "system_stats")
data class SystemStatsEntity(
    @PrimaryKey val id: Int = 1, // Always 1 (Single row)
    val networksScanned: Int = 0,
    val threatsDetected: Int = 0,
    val lastScanTime: Long = 0,
    val totalScans: Int = 0
)