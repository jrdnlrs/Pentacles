package com.example.forenscan.data.database

import android.content.Context
import androidx.room.*
import com.example.forenscan.data.models.ThreatSeverity
import kotlinx.coroutines.flow.Flow

// --- TYPE CONVERTERS ---
class Converters {
    @TypeConverter
    fun fromSeverity(value: ThreatSeverity) = value.name
    @TypeConverter
    fun toSeverity(value: String) = ThreatSeverity.valueOf(value)
}

// --- DATABASE DEFINITION ---
@Database(
    entities = [
        NetworkDataEntity::class,
        ThreatAlertEntity::class,
        ActivityEventEntity::class,
        SystemStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ForensicDatabase : RoomDatabase() {

    abstract fun networkDataDao(): NetworkDataDao
    abstract fun threatAlertDao(): ThreatAlertDao
    abstract fun activityEventDao(): ActivityEventDao
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- DAOs (DATA ACCESS OBJECTS) ---

@Dao
interface NetworkDataDao {
    @Query("SELECT * FROM network_data ORDER BY timestamp DESC")
    fun getAllNetworks(): Flow<List<NetworkDataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: NetworkDataEntity)

    // Used to clean up old data but keep evidence
    @Query("DELETE FROM network_data WHERE isDuplicate = 0 AND timestamp < :cutoffTime")
    suspend fun deleteOldSafeNetworks(cutoffTime: Long)
}

@Dao
interface ThreatAlertDao {
    @Query("SELECT * FROM threat_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<ThreatAlertEntity>>

    @Query("SELECT * FROM threat_alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<ThreatAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: ThreatAlertEntity)

    @Query("UPDATE threat_alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun markAsResolved(alertId: String)
}

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT 50")
    fun getAllEvents(): Flow<List<ActivityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity)
}

@Dao
interface SystemStatsDao {
    @Query("SELECT * FROM system_stats WHERE id = 1")
    fun getStats(): Flow<SystemStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: SystemStatsEntity)

    @Query("UPDATE system_stats SET totalScans = totalScans + 1, lastScanTime = :time WHERE id = 1")
    suspend fun updateScanStats(time: Long)

    @Query("UPDATE system_stats SET threatsDetected = :count WHERE id = 1")
    suspend fun updateThreatCount(count: Int)

    // Simple helper to increment total scans
    @Query("UPDATE system_stats SET totalScans = totalScans + 1")
    suspend fun incrementTotalScans()

    @Query("UPDATE system_stats SET lastScanTime = :time WHERE id = 1")
    suspend fun updateLastScanTime(time: Long)

    @Query("UPDATE system_stats SET threatsDetected = threatsDetected + 1 WHERE id = 1")
    suspend fun incrementThreatsDetected()
}