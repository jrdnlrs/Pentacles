package com.example.forenscan.data.database

import android.content.Context
import androidx.room.*
import com.example.forenscan.data.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Room Database for Forensic Data Storage
 * * This database stores all forensic evidence:
 * - Threat alerts detected
 * - Activity timeline events
 * - Network scan history
 * - System statistics
 * * Follows NFDLC Preservation Phase
 */
@Database(
    entities = [
        ThreatAlertEntity::class,
        ActivityEventEntity::class,
        NetworkDataEntity::class,
        SystemStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ForensicDatabase : RoomDatabase() {

    abstract fun threatAlertDao(): ThreatAlertDao
    abstract fun activityEventDao(): ActivityEventDao
    abstract fun networkDataDao(): NetworkDataDao
    abstract fun systemStatsDao(): SystemStatsDao

    companion object {
        @Volatile
        private var INSTANCE: ForensicDatabase? = null

        fun getDatabase(context: Context): ForensicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ForensicDatabase::class.java,
                    "forensic_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================================
// DATABASE ENTITIES (Table Definitions)
// ============================================

/**
 * ThreatAlert table - stores detected threats
 */
@Entity(tableName = "threat_alerts")
data class ThreatAlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val severity: String,              // Store as String: "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val networkName: String,
    val timestamp: Long,
    val isResolved: Boolean,
    val recommendedAction: String
)

/**
 * ActivityEvent table - stores timeline events
 */
@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String                   // Store as String: "SCAN_COMPLETED", etc.
)

/**
 * NetworkData table - stores scan history
 */
@Entity(tableName = "network_data")
data class NetworkDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val channel: Int,
    val frequency: Int,
    val securityType: String,
    val timestamp: Long,
    val isConnected: Boolean,
    val isDuplicate: Boolean
)

/**
 * SystemStats table - stores app statistics
 */
@Entity(tableName = "system_stats")
data class SystemStatsEntity(
    @PrimaryKey val id: Int = 1,       // Always use ID = 1 (single row)
    val networksScanned: Int,
    val threatsDetected: Int,
    val lastScanTime: Long,
    val totalScans: Int
)

// ============================================
// DATA ACCESS OBJECTS (DAOs)
// ============================================

/**
 * DAO for ThreatAlerts
 */
@Dao
interface ThreatAlertDao {

    @Query("SELECT * FROM threat_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<ThreatAlertEntity>>

    @Query("SELECT * FROM threat_alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<ThreatAlertEntity>>

    @Query("SELECT * FROM threat_alerts WHERE isResolved = 1 ORDER BY timestamp DESC")
    fun getResolvedAlerts(): Flow<List<ThreatAlertEntity>>

    @Query("SELECT * FROM threat_alerts WHERE id = :alertId")
    suspend fun getAlertById(alertId: String): ThreatAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: ThreatAlertEntity)

    @Update
    suspend fun updateAlert(alert: ThreatAlertEntity)

    @Query("UPDATE threat_alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun markAsResolved(alertId: String)

    @Query("DELETE FROM threat_alerts")
    suspend fun deleteAllAlerts()

    @Query("SELECT COUNT(*) FROM threat_alerts WHERE isResolved = 0")
    suspend fun getActiveAlertCount(): Int
}

/**
 * DAO for ActivityEvents
 */
@Dao
interface ActivityEventDao {

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 20): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ActivityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity)

    @Query("DELETE FROM activity_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM activity_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)
}

/**
 * DAO for NetworkData
 */
@Dao
interface NetworkDataDao {

    @Query("SELECT * FROM network_data ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNetworks(limit: Int = 100): Flow<List<NetworkDataEntity>>

    @Query("SELECT * FROM network_data WHERE ssid = :ssid ORDER BY timestamp DESC")
    fun getNetworksBySSID(ssid: String): Flow<List<NetworkDataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: NetworkDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworks(networks: List<NetworkDataEntity>)

    @Query("DELETE FROM network_data")
    suspend fun deleteAllNetworks()

    @Query("DELETE FROM network_data WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNetworks(cutoffTime: Long)
}

/**
 * DAO for SystemStats
 */
@Dao
interface SystemStatsDao {

    @Query("SELECT * FROM system_stats WHERE id = 1")
    fun getStats(): Flow<SystemStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: SystemStatsEntity)

    @Query("UPDATE system_stats SET networksScanned = :count WHERE id = 1")
    suspend fun updateNetworksScanned(count: Int)

    @Query("UPDATE system_stats SET threatsDetected = :count WHERE id = 1")
    suspend fun updateThreatsDetected(count: Int)

    @Query("UPDATE system_stats SET lastScanTime = :time WHERE id = 1")
    suspend fun updateLastScanTime(time: Long)

    @Query("UPDATE system_stats SET totalScans = totalScans + 1 WHERE id = 1")
    suspend fun incrementTotalScans()
}

// ============================================
// TYPE CONVERTERS
// ============================================

/**
 * Converters for complex data types
 */
class Converters {

    @TypeConverter
    fun fromThreatSeverity(severity: ThreatSeverity): String {
        return severity.name
    }

    @TypeConverter
    fun toThreatSeverity(severity: String): ThreatSeverity {
        return ThreatSeverity.valueOf(severity)
    }

    @TypeConverter
    fun fromActivityType(type: ActivityType): String {
        return type.name
    }

    @TypeConverter
    fun toActivityType(type: String): ActivityType {
        return ActivityType.valueOf(type)
    }
}