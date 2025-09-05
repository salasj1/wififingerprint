package com.ucab.wififingerprint

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ucab.wififingerprint.data.FingerprintRepository
import com.ucab.wififingerprint.data.db.AppDatabase
import kotlinx.coroutines.launch

open class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val repository: FingerprintRepository
    // Para mostrar todas las huellas guardadas (si lo necesitas en la UI)
    val fingerprints: LiveData<Map<String, Map<String, Int>>>

    // Estado para el escaneo actual, observado por la UI de Compose
    private val _currentScan = mutableStateOf<Map<String, Int>>(emptyMap())
    val currentScan: State<Map<String, Int>> = _currentScan

    // Para las ubicaciones guardadas que la UI utiliza para la predicción y habilitar botones.
    // Se carga al iniciar el ViewModel y se actualiza al guardar una nueva ubicación.
    val savedLocations = mutableMapOf<String, Map<String, Int>>()

    init {
        Log.d(TAG, "MainViewModel initializing...")
        try {
            val fingerprintDao = AppDatabase.getInstance(application).fingerprintDao()
            repository = FingerprintRepository(fingerprintDao)
            fingerprints = repository.allFingerprints // Para observación continua si es necesario en otra parte

            // Cargar las ubicaciones guardadas al iniciar
            loadSavedFingerprints()

            Log.d(TAG, "MainViewModel initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MainViewModel. THIS IS LIKELY THE CAUSE OF THE CRASH.", e)
            // Considera si quieres que la app crashee o manejarlo de otra forma.
            // Por ahora, se relanza para que el error sea visible.
            throw RuntimeException("Failed to initialize MainViewModel due to: ${e.message}", e)
        }
    }

    private fun loadSavedFingerprints() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading saved fingerprints from repository...")
                val loadedFingerprints = repository.getAllFingerprintsMap()
                if (loadedFingerprints.isNotEmpty()) {
                    savedLocations.putAll(loadedFingerprints)
                    Log.d(TAG, "Loaded ${loadedFingerprints.size} saved locations into savedLocations map.")
                } else {
                    Log.d(TAG, "No saved locations found in the database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved fingerprints: ", e)
                // Decide cómo manejar este error. ¿Mostrar un mensaje al usuario? ¿Intentar de nuevo?
            }
        }
    }

    /**
     * Actualiza el estado de [_currentScan] con los resultados de un nuevo escaneo WiFi.
     * La UI observará los cambios en [currentScan].
     */
    fun updateCurrentScan(newScan: Map<String, Int>) {
        _currentScan.value = newScan
        Log.d(TAG, "ViewModel _currentScan.value actualizada con ${newScan.size} redes. _currentScan.value ahora tiene: ${_currentScan.value.size} elementos.")
    }

    /**
     * Guarda la huella digital (scan actual) para un nombre de ubicación dado en la BD
     * y actualiza el mapa local `savedLocations`.
     */
    fun saveFingerprintAndLocationUi(locationName: String, scanToSave: Map<String, Int>): Boolean {
        if (locationName.isBlank() || scanToSave.isEmpty()) {
            Log.w(TAG, "Attempted to save empty location name or scan data for '$locationName'.")
            return false
        }
        viewModelScope.launch {
            Log.d(TAG, "Saving fingerprint for location: $locationName with ${scanToSave.size} APs via Repository")
            try {
                repository.saveFingerprint(locationName, scanToSave)
                // Después de guardar en la BD, actualiza el mapa local `savedLocations`
                savedLocations[locationName] = HashMap(scanToSave)
                Log.d(TAG, "Save operation for $locationName completed. savedLocations map updated.")
                // Opcional: Limpiar el _currentScan después de guardar si es deseado
                // _currentScan.value = emptyMap()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving fingerprint for $locationName via Repository", e)
                // Considera cómo informar este error a la UI si es necesario
            }
        }
        return true // Se asume éxito si la corutina se lanza; considera un callback para errores
    }

    // Esta función ahora llamará a la nueva función unificada.
    fun saveLocationUiHelper(name: String, scanData: Map<String, Int>): Boolean {
        Log.d(TAG, "saveLocationUiHelper called for '$name'. Delegating to saveFingerprintAndLocationUi.")
        return saveFingerprintAndLocationUi(name, scanData)
    }


    fun clearAllFingerprints() {
        viewModelScope.launch {
            Log.d(TAG, "Clearing all fingerprints via Repository...")
            try {
                repository.clearAllData()
                _currentScan.value = emptyMap() // Limpiar también el escaneo actual
                savedLocations.clear() // Limpiar el mapa local
                Log.d(TAG, "Clear all data operation completed.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing fingerprints via Repository", e)
            }
        }
    }
}
