package com.ucab.wififingerprint.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "wifi_readings")
data class WifiReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: Long, // Foreign key to LocationEntity
    val bssid: String,
    val rssi: Int
)
