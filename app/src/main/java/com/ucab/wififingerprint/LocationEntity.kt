package com.ucab.wififingerprint


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val name: String,
    // Puedes añadir más metadatos, como una descripción
    // o el nombre de la escena del minijuego
    val minigameScene: String,
    val description: String = ""
)

@Entity(tableName = "fingerprints")
data class FingerprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationName: String, // Foreign key to LocationEntity
    val bssid: String,
    val rssi: Int
)