package com.ucab.wififingerprint

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    // Stores saved fingerprints: Location Name -> (BSSID -> RSSI)
    val savedLocations = mutableStateMapOf<String, Map<String, Int>>()

    // Stores the current WiFi scan results: BSSID -> RSSI
    var currentScan = mutableMapOf<String, Int>()
        private set // Restrict external modification of the map instance

    fun updateCurrentScan(newScan: Map<String, Int>) {
        currentScan.clear()
        currentScan.putAll(newScan)
        Log.d(TAG, "ViewModel currentScan updated with ${newScan.size} networks.")
    }

    fun saveLocation(locationName: String, scanData: Map<String, Int>): Boolean {
        if (locationName.isBlank() || scanData.isEmpty()) {
            Log.w(TAG, "Attempted to save empty location name or scan data.")
            return false
        }
        savedLocations[locationName] = HashMap(scanData) // Save a copy
        Log.d(TAG, "ViewModel saved location '$locationName' with ${scanData.size} networks. Total saved: ${savedLocations.size}")
        return true
    }

    fun clearAllLocations() {
        savedLocations.clear()
        Log.d(TAG, "ViewModel all locations cleared.")
    }

    // Puedes añadir más lógica aquí si es necesario, como cargar/guardar desde persistencia.
    init {
        Log.d(TAG, "MainViewModel initialized")
    }
}