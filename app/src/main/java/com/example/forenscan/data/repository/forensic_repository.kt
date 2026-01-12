package com.example.forenscan.data.repository

import android.content.Context
import com.example.forenscan.data.database.*
import com.example.forenscan.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ForensicRepository - Central data management
 *
 * Handles all database operations and data export
 * Implements NFDLC Preservation and Presentation phases
 */
class ForensicRepository(context: Context) {

    private val database = ForensicDatabase.getDatabase(context)
    private val threatAlertDao = database.threatAlertDao()
    private val activityEventDao = database.activityEventDao()
    private val networkDataDao = database.networkDataDao()
    private val systemStatsDao = database.systemStatsDao()

    // ============================================
    // THREAT ALERTS
    // ============================================

    /**
     * Get all threats as Flow (automatically updates UI)
     */
    fun getAllThreats(): Flow<List<ThreatAlert>> {
        return threatAlertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get active (unresolved) threats
     */
    fun getActiveThreats(): Flow<List<ThreatAlert>> {
        return threatAlertDao.getActiveAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get resolved threats
     */
    fun getResolvedThreats(): Flow<List<ThreatAlert>> {
        return threatAlertDao.getResolvedAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Save a threat alert
     */
    suspend fun saveThreatAlert(alert: ThreatAlert) {
        threatAlertDao.insertAlert(alert.toEntity())
    }

    /**
     * Mark threat as resolved
     */
    suspend fun resolveAlert(alertId: String) {
        threatAlertDao.markAsResolved(alertId)
    }

    /**
     * Delete all alerts
     */
    suspend fun deleteAllAlerts() {
        threatAlertDao.deleteAllAlerts()
    }

    // ============================================
    // ACTIVITY EVENTS (Timeline)
    // ============================================

    /**
     * Get recent activity events
     * NOTE: Uses ActivityItem to match your Adapter
     */
    fun getRecentActivity(limit: Int = 20): Flow<List<ActivityItem>> {
        return activityEventDao.getRecentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get all activity events
     */
    fun getAllActivity(): Flow<List<ActivityItem>> {
        return activityEventDao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Save activity event
     */
    suspend fun saveActivityEvent(event: ActivityItem) {
        activityEventDao.insertEvent(event.toEntity())
    }

    /**
     * Delete all activity
     */
    suspend fun deleteAllActivity() {
        activityEventDao.deleteAllEvents()
    }

    /**
     * Delete old events (older than 30 days)
     */
    suspend fun cleanOldActivity() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        activityEventDao.deleteOldEvents(thirtyDaysAgo)
    }

    // ============================================
    // NETWORK DATA (Scan History)
    // ============================================

    /**
     * Get recent network scans
     * NOTE: Uses WifiNetwork to match your Adapter
     */
    fun getRecentNetworks(limit: Int = 100): Flow<List<WifiNetwork>> {
        return networkDataDao.getRecentNetworks(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Save network scan results
     */
    suspend fun saveNetworkScan(networks: List<WifiNetwork>) {
        val entities = networks.map { it.toEntity() }
        networkDataDao.insertNetworks(entities)
    }

    /**
     * Delete all network history
     */
    suspend fun deleteAllNetworks() {
        networkDataDao.deleteAllNetworks()
    }

    // ============================================
    // SYSTEM STATS
    // ============================================

    /**
     * Get system statistics
     */
    fun getSystemStats(): Flow<SystemStats> {
        return systemStatsDao.getStats().map { entity ->
            entity?.toDomain() ?: SystemStats()
        }
    }

    /**
     * Update system statistics
     */
    suspend fun updateStats(stats: SystemStats) {
        systemStatsDao.insertStats(stats.toEntity())
    }

    /**
     * Increment total scans counter
     */
    suspend fun incrementScanCount() {
        systemStatsDao.incrementTotalScans()
    }

    // ============================================
    // BULK OPERATIONS
    // ============================================

    /**
     * Clear all forensic data
     */
    suspend fun clearAllData() {
        threatAlertDao.deleteAllAlerts()
        activityEventDao.deleteAllEvents()
        networkDataDao.deleteAllNetworks()
        // Don't delete stats, just reset them
        systemStatsDao.insertStats(SystemStats().toEntity())
    }
}

// ============================================
// MAPPER EXTENSIONS (COPY THESE LINES!)
// ============================================

// 1. ThreatAlert Mappings
fun ThreatAlert.toEntity() = ThreatAlertEntity(
    id = id,
    title = title,
    description = description,
    severity = severity.name,
    networkName = networkName,
    timestamp = timestamp,
    isResolved = isResolved,
    recommendedAction = recommendedAction
)

fun ThreatAlertEntity.toDomain() = ThreatAlert(
    id = id,
    title = title,
    description = description,
    severity = ThreatSeverity.valueOf(severity),
    networkName = networkName,
    timestamp = timestamp,
    isResolved = isResolved,
    recommendedAction = recommendedAction
)

// 2. ActivityItem Mappings
fun ActivityItem.toEntity() = ActivityEventEntity(
    id = id,
    title = title,
    description = description,
    timestamp = timestamp,
    type = type.name
)

fun ActivityEventEntity.toDomain() = ActivityItem(
    id = id,
    title = title,
    description = description,
    timestamp = timestamp,
    type = ActivityType.valueOf(type)
)

// 3. WifiNetwork Mappings
fun WifiNetwork.toEntity() = NetworkDataEntity(
    ssid = ssid,
    bssid = macAddress,
    signalStrength = signalStrength,
    channel = 0,
    frequency = if (frequency.contains("5")) 5000 else 2400,
    securityType = encryption,
    timestamp = System.currentTimeMillis(),
    isConnected = isConnected,
    isDuplicate = classification == NetworkClassification.EVIL_TWIN
)

fun NetworkDataEntity.toDomain() = WifiNetwork(
    ssid = ssid,
    macAddress = bssid,
    encryption = securityType,
    frequency = "${frequency}MHz",
    signalStrength = signalStrength,
    classification = when {
        isDuplicate -> NetworkClassification.EVIL_TWIN
        else -> NetworkClassification.SAFE
    },
    isConnected = isConnected
)

// 4. SystemStats Mappings
fun SystemStats.toEntity() = SystemStatsEntity(
    id = 1,
    networksScanned = networksScanned,
    threatsDetected = threatsDetected,
    lastScanTime = lastScanTime,
    totalScans = totalScans
)

fun SystemStatsEntity.toDomain() = SystemStats(
    networksScanned = networksScanned,
    threatsDetected = threatsDetected,
    lastScanTime = lastScanTime,
    totalScans = totalScans
)