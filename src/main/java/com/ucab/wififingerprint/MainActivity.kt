package com.ucab.wififingerprint

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ucab.wififingerprint.ui.theme.WifiFingerprintTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WiFiFingerprint"
    }

    private lateinit var wifiManager: WifiManager
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Se necesitan todos los permisos para funcionar", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        requestAppPermissions()

        setContent {
            WifiFingerprintTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WiFiScannerApp(viewModel)
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val notGrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${notGrantedPermissions.joinToString()}")
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All permissions already granted.")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun WiFiScannerApp(vm: MainViewModel) {
        var scanResultsText by remember { mutableStateOf("Presiona 'Escanear WiFi' o '¿Dónde Estoy?'") }
        var locationName by remember { mutableStateOf("") }
        var isScanningState by remember { mutableStateOf(false) } // Used for UI feedback (e.g., progress indicators)
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "WiFi Fingerprinting PoC",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            // Card 1: Escanear Redes WiFi
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Escanear Redes WiFi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            isScanningState = true
                            Toast.makeText(context, "Iniciando escaneo WiFi...", Toast.LENGTH_SHORT).show()
                            startWiFiScan { detailedScanResults ->
                                scanResultsText = detailedScanResults ?: "Error durante el escaneo."
                                isScanningState = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanningState
                    ) {
                        if (isScanningState) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escaneando...")
                        } else {
                            Text("🔍 Escanear WiFi")
                        }
                    }
                }
            }

            // Card 2: Guardar Ubicación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Guardar Ubicación", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Nombre de la ubicación") },
                        placeholder = { Text("ej: Laboratorio A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (vm.currentScan.isEmpty()) {
                                Toast.makeText(context, "Realiza un escaneo primero", Toast.LENGTH_SHORT).show()
                            } else if (locationName.isNotBlank()) {
                                if (vm.saveLocation(locationName, vm.currentScan)) {
                                    Toast.makeText(context, "Ubicación '$locationName' guardada", Toast.LENGTH_SHORT).show()
                                    scanResultsText = formatSavedLocationsForDisplay(vm)
                                    locationName = ""
                                } else {
                                    Toast.makeText(context, "Error al guardar ubicación", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Ingresa un nombre para la ubicación", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = locationName.isNotBlank() && vm.currentScan.isNotEmpty() && !isScanningState
                    ) {
                        Text("💾 Guardar Ubicación Actual")
                    }
                }
            }

            // Card 3: Predecir Ubicación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Predecir Ubicación", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    val predictButtonEnabled by remember(vm.savedLocations.size, isScanningState) { // Depends on saved locations and not currently scanning
                        derivedStateOf {
                            val slNotEmpty = vm.savedLocations.isNotEmpty()
                            Log.d(TAG, "derivedStateOf for predictButton: slNotEmpty=$slNotEmpty (size ${vm.savedLocations.size}), isScanningState=$isScanningState, enabled=${slNotEmpty && !isScanningState}")
                            slNotEmpty && !isScanningState
                        }
                    }
                    Log.d(TAG, "Predict Button final enabled state to be used: $predictButtonEnabled")

                    Button(
                        onClick = {
                            isScanningState = true
                            Toast.makeText(context, "Escaneando para predecir...", Toast.LENGTH_SHORT).show()
                            startWiFiScan { _ -> // vm.currentScan is updated by startWiFiScan's onReceive
                                val predictionResult = predictLocation(vm)
                                scanResultsText = predictionResult
                                isScanningState = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictButtonEnabled
                    ) {
                        // Check isScanningState specifically for this button's action
                        val isPredicting = isScanningState && predictButtonEnabled // True if this button initiated the scan
                        if (isPredicting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prediciendo...")
                        } else {
                            Text("🎯 ¿Dónde Estoy?")
                        }
                    }
                }
            }

            // Card 4: Resultados
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resultados:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = scanResultsText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startWiFiScan(onComplete: (detailedScanResults: String?) -> Unit) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi está deshabilitado. Por favor, habilítalo.", Toast.LENGTH_LONG).show()
            onComplete("WiFi deshabilitado.")
            return
        }

        val wifiScanReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(context: Context?, intent: Intent?) {
                val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                var detailedResultsOutput: String?

                if (success || intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    try {
                        val scanResultsList: List<ScanResult> = wifiManager.scanResults
                        val tempScan = mutableMapOf<String, Int>()
                        val resultBuilder = StringBuilder()
                        resultBuilder.append("=== ÚLTIMO ESCANEO ===\n")
                        resultBuilder.append("Redes detectadas: ${scanResultsList.size}\n\n")

                        for (result in scanResultsList) {
                            val bssid = result.BSSID
                            val ssid = if (result.SSID.isNullOrEmpty()) "<SSID Oculto>" else result.SSID
                            val rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                result.level
                            } else {
                                @Suppress("DEPRECATION")
                                result.level
                            }
                            tempScan[bssid] = rssi
                            resultBuilder.append("SSID: $ssid\n")
                            resultBuilder.append("BSSID: $bssid\n")
                            resultBuilder.append("RSSI: $rssi dBm\n--------------------\n")
                        }
                        viewModel.updateCurrentScan(tempScan)
                        detailedResultsOutput = resultBuilder.toString()
                        Log.d(TAG, "Escaneo completado. ViewModel currentScan actualizado con ${tempScan.size} redes.")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException al obtener scan results: ${e.message}")
                        Toast.makeText(this@MainActivity, "Error de permisos al escanear.", Toast.LENGTH_SHORT).show()
                        detailedResultsOutput = "Error de permisos al escanear."
                        viewModel.updateCurrentScan(emptyMap()) // Clear scan on error
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception al obtener scan results: ${e.message}")
                        Toast.makeText(this@MainActivity, "Error inesperado al escanear.", Toast.LENGTH_SHORT).show()
                        detailedResultsOutput = "Error inesperado al escanear."
                        viewModel.updateCurrentScan(emptyMap()) // Clear scan on error
                    }
                } else {
                    Log.d(TAG, "Escaneo fallido o sin resultados actualizados.")
                    Toast.makeText(this@MainActivity, "Escaneo fallido.", Toast.LENGTH_SHORT).show()
                    detailedResultsOutput = "Escaneo fallido."
                    viewModel.updateCurrentScan(emptyMap()) // Clear scan on error
                }
                try {
                    unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Receiver no registrado: ${e.message}")
                }
                onComplete(detailedResultsOutput)
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, intentFilter)
        }

        val scanStarted = wifiManager.startScan()
        if (!scanStarted) {
            Log.e(TAG, "startScan() devolvió false.")
            Toast.makeText(this, "No se pudo iniciar el escaneo WiFi.", Toast.LENGTH_SHORT).show()
            try {
                unregisterReceiver(wifiScanReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver no registrado (al fallar startScan): ${e.message}")
            }
            viewModel.updateCurrentScan(emptyMap()) // Ensure currentScan is cleared if scan fails to start
            onComplete("No se pudo iniciar el escaneo.")
        } else {
            Log.d(TAG, "Escaneo WiFi iniciado...")
        }
    }

    private fun formatSavedLocationsForDisplay(vm: MainViewModel): String {
        if (vm.savedLocations.isEmpty()) {
            return "No hay ubicaciones guardadas aún."
        }
        val builder = StringBuilder("=== UBICACIONES GUARDADAS ===\n")
        vm.savedLocations.forEach { (name, scanData) ->
            builder.append("📍 $name (${scanData.size} redes)\n")
        }
        return builder.toString()
    }

    private fun predictLocation(vm: MainViewModel): String {
        if (vm.savedLocations.isEmpty()) {
            return "No hay ubicaciones guardadas para predecir.\nAsegúrate de guardar algunas ubicaciones primero."
        }
        if (vm.currentScan.isEmpty()) {
            return "El escaneo actual está vacío. Esto puede ocurrir si el último escaneo falló.\nIntenta escanear de nuevo antes de predecir."
        }

        val similarities = mutableMapOf<String, Double>()
        vm.savedLocations.forEach { (locationName, savedScan) ->
            similarities[locationName] = calculateSimilarity(vm.currentScan, savedScan)
        }

        // Sort similarities in descending order
        val sortedSimilarities = similarities.toList().sortedByDescending { (_, value) -> value }

        val resultBuilder = StringBuilder()
        resultBuilder.append("=== PREDICCIÓN DE UBICACIÓN ===\n")

        if (sortedSimilarities.isNotEmpty()) {
            val bestMatch = sortedSimilarities.first()
            // Consider a threshold for a "good enough" match, e.g., > 0.1 or some other value based on your algorithm's output range
            val predictionThreshold = 0.01 // Example: if max similarity is very low, treat as unknown

            if (bestMatch.second > predictionThreshold) {
                resultBuilder.append("Ubicación más probable: ${bestMatch.first}\n")
                resultBuilder.append("Cercanía (Similitud): ${"%.2f".format(bestMatch.second)}\n\n")
                resultBuilder.append("--- Otras ubicaciones (por cercanía) ---\n")
                sortedSimilarities.forEach { (name, sim) ->
                    resultBuilder.append("$name: ${"%.2f".format(sim)}\n")
                }
            } else {
                resultBuilder.append("No se pudo identificar una ubicación conocida con suficiente certeza.\n")
                resultBuilder.append("--- Similitudes calculadas (todas bajas) ---\n")
                sortedSimilarities.forEach { (name, sim) ->
                    resultBuilder.append("$name: ${"%.2f".format(sim)}\n")
                }
            }
        } else {
            // This case should ideally not be reached if vm.savedLocations is not empty.
            resultBuilder.append("No se calcularon similitudes. ¿Hay ubicaciones guardadas?\n")
        }
        return resultBuilder.toString()
    }

    private fun calculateSimilarity(currentScan: Map<String, Int>, savedScan: Map<String, Int>): Double {
        if (currentScan.isEmpty() || savedScan.isEmpty()) return 0.0

        val commonBSSIDs = currentScan.keys.intersect(savedScan.keys)
        if (commonBSSIDs.isEmpty()) return 0.0

        var sumOfSquaredDifferences = 0.0
        for (bssid in commonBSSIDs) {
            val rssi1 = currentScan[bssid] ?: 0
            val rssi2 = savedScan[bssid] ?: 0
            sumOfSquaredDifferences += (rssi1 - rssi2).toDouble().pow(2)
        }

        val euclideanDistance = sqrt(sumOfSquaredDifferences / commonBSSIDs.size) // RMSE

        // Similarity: Higher for lower distance. Max similarity of 1 for perfect match (distance 0).
        // Normalize based on a max possible distance or an expected range.
        // For RSSI, differences can range, let's say a 30dBm difference is very dissimilar.
        val maxReasonableRssiDifference = 30.0
        val similarityScore = 1.0 - (euclideanDistance / maxReasonableRssiDifference)
        val normalizedSimilarity = similarityScore.coerceIn(0.0, 1.0)


        // Consider the proportion of common networks (Jaccard-like factor)
        // A high overlap of networks is important.
        val totalUniqueNetworks = currentScan.keys.union(savedScan.keys).size
        val overlapFactor = if (totalUniqueNetworks > 0) commonBSSIDs.size.toDouble() / totalUniqueNetworks else 0.0

        // Combine: give more weight to RSSI similarity if overlap is good
        val finalSimilarity = normalizedSimilarity * overlapFactor

        Log.d(TAG, "calculateSimilarity for BSSIDs: ${commonBSSIDs.joinToString()}")
        Log.d(TAG, "calculateSimilarity: common=${commonBSSIDs.size}, currentTotal=${currentScan.size}, savedTotal=${savedScan.size}")
        Log.d(TAG, "calculateSimilarity: RMSE=$euclideanDistance, normalizedSimilarity=$normalizedSimilarity, overlapFactor=$overlapFactor, finalSimilarity=$finalSimilarity")

        return finalSimilarity
    }
}
