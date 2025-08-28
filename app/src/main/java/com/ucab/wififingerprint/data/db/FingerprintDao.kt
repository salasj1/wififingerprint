package com.ucab.wififingerprint.data.db

import androidx.room.*
import com.ucab.wififingerprint.data.model.LocationWithReadings
import com.ucab.wififingerprint.data.model.WifiFingerprint
import com.ucab.wififingerprint.data.model.WifiReading
import kotlinx.coroutines.flow.Flow

@Dao
interface FingerprintDao {
    @Transaction
    @Query("SELECT * FROM wifi_fingerprints")
    fun getAllLocationsWithReadings(): Flow<List<LocationWithReadings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFingerprint(fingerprint: WifiFingerprint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<WifiReading>)

    @Transaction
    suspend fun saveNewLocation(locationName: String, readings: List<Map<String, Int>>) {
        insertFingerprint(WifiFingerprint(locationName))
        val readingEntities = readings.flatMap { map: Map<String, Int> ->
            // Tipo explícito para 'map'
            map.map { (bssid: String, rssi: Int) ->
                // Tipos explícitos para bssid y rssi
                WifiReading(fingerprintOwnerName = locationName, bssid = bssid, rssi = rssi)
            }
        }
        insertReadings(readingEntities)
    }
}
