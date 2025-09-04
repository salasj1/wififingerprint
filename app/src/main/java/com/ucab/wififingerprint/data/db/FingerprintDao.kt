package com.ucab.wififingerprint.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ucab.wififingerprint.data.model.WifiFingerprint

@Dao
interface FingerprintDao {

    @Insert
    suspend fun insertLocation(location: LocationEntity): Long

    @Insert
    suspend fun insertWifiReadings(readings: List<WifiReadingEntity>)

    @Transaction
    @Query("SELECT * FROM locations")
    suspend fun getAllFingerprints(): List<WifiFingerprint> // Un objeto que agrupa Location y sus Readings

    @Query("DELETE FROM wifi_readings")
    suspend fun clearAllWifiReadings()

    @Query("DELETE FROM locations")
    suspend fun clearAllLocations()

    @Transaction
    suspend fun deleteAllDataInTransaction() {
        clearAllWifiReadings()
        clearAllLocations()
    }
}
