package com.ucab.wififingerprint.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ucab.wififingerprint.data.db.LocationEntity
import com.ucab.wififingerprint.data.db.WifiReadingEntity

data class WifiFingerprint(
    @Embedded val location: LocationEntity,
    @Relation(
        parentColumn = "id", // Primary key of LocationEntity
        entityColumn = "locationId" // Foreign key in WifiReadingEntity
    )
    val readings: List<WifiReadingEntity>
)
