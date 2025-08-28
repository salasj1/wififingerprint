package com.ucab.wififingerprint.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "wifi_fingerprints")
data class WifiFingerprint(
    @PrimaryKey val locationName: String
)

// Clase para agrupar una ubicación con sus lecturas
data class LocationWithReadings(
    @Embedded val fingerprint: WifiFingerprint,
    @Relation(
        parentColumn = "locationName",
        entityColumn = "fingerprintOwnerName"
    )
    val readings: List<WifiReading>
)
