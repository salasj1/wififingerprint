package com.ucab.wififingerprint.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_readings",
    foreignKeys = [ForeignKey(entity = WifiFingerprint::class,
        parentColumns = ["locationName"],
        childColumns = ["fingerprintOwnerName"],
        onDelete = ForeignKey.CASCADE)])
data class WifiReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fingerprintOwnerName: String, // Clave foránea
    val bssid: String,
    val rssi: Int
)