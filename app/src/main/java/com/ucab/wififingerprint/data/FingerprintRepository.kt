package com.ucab.wififingerprint.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.ucab.wififingerprint.data.db.FingerprintDao
import com.ucab.wififingerprint.data.db.LocationEntity
import com.ucab.wififingerprint.data.db.WifiReadingEntity
import com.ucab.wififingerprint.data.model.WifiFingerprint // Importación añadida
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FingerprintRepository(private val fingerprintDao: FingerprintDao) {

    // LiveData para observar todos los fingerprints (nombre de ubicación a mapa de BSSID-RSSI)
    val allFingerprints: LiveData<Map<String, Map<String, Int>>> = liveData(Dispatchers.IO) {
        val data = fingerprintDao.getAllFingerprints().associate { wifiFingerprint ->
            // wifiFingerprint es de tipo com.ucab.wififingerprint.data.model.WifiFingerprint
            // wifiFingerprint.location es LocationEntity
            // wifiFingerprint.readings es List<WifiReadingEntity>
            wifiFingerprint.location.name to wifiFingerprint.readings.associate { reading -> reading.bssid to reading.rssi }
        }
        emit(data)
    }

    suspend fun getAllFingerprintsMap(): Map<String, Map<String, Int>> {
        return withContext(Dispatchers.IO) {
            fingerprintDao.getAllFingerprints().associate { wifiFingerprint ->
                wifiFingerprint.location.name to wifiFingerprint.readings.associate { reading -> reading.bssid to reading.rssi }
            }
        }
    }

    suspend fun saveFingerprint(locationName: String, scanData: Map<String, Int>) {
        withContext(Dispatchers.IO) {
            // NOTA: La guía que proporcionaste sugiere que LocationEntity podría tener un campo 'minigameScene'.
            // La definición actual de LocationEntity (en context[8]) solo tiene 'id' y 'name'.
            // Si actualizas LocationEntity para incluir 'minigameScene', deberás actualizar la siguiente línea:
            // val locationEntity = LocationEntity(name = locationName, minigameScene = "DefaultScene") // o similar
            val locationEntity = LocationEntity(name = locationName)
            val locationId = fingerprintDao.insertLocation(locationEntity) // Asume que insertLocation devuelve el ID

            val readingEntities = scanData.map { (bssid, rssi) ->
                WifiReadingEntity(locationId = locationId, bssid = bssid, rssi = rssi)
            }
            fingerprintDao.insertWifiReadings(readingEntities)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            fingerprintDao.deleteAllDataInTransaction() // Ahora esta línea es funcional
        }
    }

}
