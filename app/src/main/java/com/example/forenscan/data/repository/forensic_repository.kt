package com.example.forenscan.data.repository

import android.content.Context
import com.example.forenscan.data.database.ForensicDatabase
import com.example.forenscan.data.database.NetworkDataEntity
import com.example.forenscan.data.database.ThreatAlertEntity
import com.example.forenscan.data.database.ActivityEventEntity
import com.example.forenscan.data.database.SystemStatsEntity
import com.example.forenscan.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * ForensicRepository
 * * The Single Source of Truth for the application.
 */
class ForensicRepository(context: Context) {

    // --- Database Initialization ---
    private val database = ForensicDatabase.getDatabase(context)
    private val networkDataDao = database.networkDataDao()
    private val threatAlertDao = database.threatAlertDao()
    private val activityEventDao = database.activityEventDao()
    private val systemStatsDao = database.systemStatsDao()

    // =================================================================
    // 1. GET DATA (Used by ViewModel to update UI)
    // =================================================================

    fun getAllNetworks(): Flow<List<WifiNetwork>> {
        return networkDataDao.getAllNetworks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllThreats(): Flow<List<ThreatAlert>> {
        return threatAlertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveThreats(): Flow<List<ThreatAlert>> {
        return threatAlertDao.getActiveAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getSystemStats(): Flow<SystemStats?> {
        return systemStatsDao.getStats().map { it?.toDomain() }
    }

    fun getRecentActivity(): Flow<List<ActivityItem>> {
        return activityEventDao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // =================================================================
    // 2. SAVE DATA (Used by FSWifiScanner Service)
    // =================================================================

    suspend fun saveNetworkSnapshot(network: WifiNetwork) {
        // 1. Save the Network
        val entity = network.toEntity()
        networkDataDao.insertNetwork(entity)

        // 2. Ensure Stats Row Exists
        val stats = systemStatsDao.getStats().firstOrNull()
        if (stats == null) {
            systemStatsDao.insertStats(SystemStatsEntity())
        }

        // 3. Update Counters
        systemStatsDao.incrementTotalScans()
        systemStatsDao.updateLastScanTime(System.currentTimeMillis())

        // 4. If it's an Evil Twin, log it to the Timeline automatically
        if (network.classification == NetworkClassification.EVIL_TWIN) {
            val event = ActivityItem(
                id = UUID.randomUUID().toString(),
                title = "Evil Twin Identified",
                description = "SSID: ${network.ssid} | Signal: ${network.signalStrength}dBm",
                timestamp = System.currentTimeMillis(),
                type = ActivityType.THREAT_DETECTED
            )
            activityEventDao.insertEvent(event.toEntity())
        }
    }

    suspend fun insertThreat(alert: ThreatAlert) {
        threatAlertDao.insertAlert(alert.toEntity())
        systemStatsDao.incrementThreatsDetected()

        val event = ActivityItem(
            id = UUID.randomUUID().toString(),
            title = "Threat Detected",
            description = "${alert.severity} Alert: ${alert.title}",
            timestamp = System.currentTimeMillis(),
            type = ActivityType.THREAT_DETECTED
        )
        activityEventDao.insertEvent(event.toEntity())
    }

    suspend fun markThreatResolved(alertId: String) {
        threatAlertDao.markAsResolved(alertId)
    }

    // =================================================================
    // 3. MAPPERS (Convert Database Objects <-> App Objects)
    // =================================================================

    // --- WifiNetwork Mappings ---
    private fun WifiNetwork.toEntity() = NetworkDataEntity(
        ssid = ssid,
        bssid = macAddress,
        signalStrength = signalStrength,
        frequency = parseFrequency(frequency),
        channel = frequencyToChannel(parseFrequency(frequency)),
        securityType = encryption,
        timestamp = System.currentTimeMillis(),
        isConnected = isConnected,
        classification = classification.name // UPDATED: Saves "SAFE", "SUSPICIOUS", etc.
    )

    private fun NetworkDataEntity.toDomain() = WifiNetwork(
        ssid = ssid,
        macAddress = bssid,
        encryption = securityType,
        frequency = "${frequency}MHz",
        signalStrength = signalStrength,
        classification = try {
            NetworkClassification.valueOf(classification) // UPDATED: Reads back Enum
        } catch (e: Exception) {
            NetworkClassification.SAFE
        },
        isConnected = isConnected,
        isDuplicate = classification == "EVIL_TWIN",
        timestamp = timestamp.toString()
    )

    // --- ThreatAlert Mappings ---
    private fun ThreatAlert.toEntity() = ThreatAlertEntity(
        id = id,
        title = title,
        description = description,
        severity = severity.name,
        networkName = networkName,
        macAddress = macAddress,
        timestamp = timestamp,
        isResolved = isResolved,
        recommendedAction = recommendedAction
    )

    private fun ThreatAlertEntity.toDomain() = ThreatAlert(
        id = id,
        title = title,
        description = description,
        severity = ThreatSeverity.valueOf(severity),
        networkName = networkName,
        macAddress = macAddress,
        timestamp = timestamp,
        isResolved = isResolved,
        recommendedAction = recommendedAction
    )

    // --- ActivityItem Mappings ---
    private fun ActivityItem.toEntity() = ActivityEventEntity(
        id = id,
        title = title,
        description = description,
        timestamp = timestamp,
        type = type.name
    )

    private fun ActivityEventEntity.toDomain() = ActivityItem(
        id = id,
        title = title,
        description = description,
        timestamp = timestamp,
        type = ActivityType.valueOf(type)
    )

    // --- SystemStats Mappings ---
    private fun SystemStatsEntity.toDomain() = SystemStats(
        networksScanned = networksScanned,
        threatsDetected = threatsDetected,
        lastScanTime = lastScanTime,
        totalScans = totalScans
    )

    // =================================================================
    // 4. UTILITIES (Helpers)
    // =================================================================

    private fun parseFrequency(freqString: String): Int {
        return freqString.replace("MHz", "").trim().toIntOrNull() ?: 2400
    }

    private fun frequencyToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq < 2484 -> (freq - 2407) / 5
            freq in 4910..4980 -> (freq - 4000) / 5
            freq < 5935 -> (freq - 5000) / 5
            else -> 0
        }
    }
}