package com.ucab.wififingerprint


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult // No se usa directamente aquí, pero es bueno tenerlo si se expande
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
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // Necesario para delegados de State
import androidx.compose.runtime.setValue // Necesario para delegados de State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        var scanResultsText by remember { mutableStateOf("Presiona 'Escanear WiFi' para comenzar") }
        var locationName by remember { mutableStateOf("") }
        var isScanningState by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
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
                                scanResultsText = detailedScanResults ?: "Error durante el escaneo o no se encontraron redes."
                                isScanningState = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanningState
                    ) {
                        val isCurrentlyScanningForThisButton = isScanningState
                        if (isCurrentlyScanningForThisButton) {
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

                    val currentScanValue = vm.currentScan.value
                    val currentScanNotEmpty = currentScanValue.isNotEmpty()
                    val notScanning = !isScanningState
                    val isSaveButtonEnabled = locationName.isNotBlank() && currentScanNotEmpty && notScanning

                    Log.d(TAG, "UI Check for SAVE BUTTON: locationName='${locationName}', isNotBlank=${locationName.isNotBlank()}, currentScanNotEmpty=${currentScanNotEmpty} (size=${currentScanValue.size}), notScanning=${notScanning}, 최종 ENABLED=${isSaveButtonEnabled}")

                    Button(
                        onClick = {
                            if (!currentScanNotEmpty) {
                                Toast.makeText(context, "Realiza un escaneo primero", Toast.LENGTH_SHORT).show()
                            } else if (locationName.isNotBlank()) {
                                if (vm.saveLocationUiHelper(locationName, currentScanValue)) {
                                    Toast.makeText(context, "Ubicación '$locationName' guardada", Toast.LENGTH_SHORT).show()
                                    scanResultsText = formatSavedLocationsForDisplay(vm)
                                    locationName = ""
                                } else {
                                    Toast.makeText(context, "Error al guardar ubicación. Nombre vacío o sin datos de escaneo.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Ingresa un nombre para la ubicación", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSaveButtonEnabled
                    ) {
                        Text("💾 Guardar Ubicación Actual")
                    }
                }
            }

            // Card 3: Predecir Ubicación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Predecir Ubicación", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    val predictButtonEnabled by remember(vm.savedLocations.size, isScanningState) {
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
                            startWiFiScan { _ ->
                                if (vm.currentScan.value.isNotEmpty()) {
                                    val predictionResult = predictLocation(vm)
                                    scanResultsText = predictionResult
                                } else {
                                    scanResultsText = "No se pudo obtener el escaneo actual para predecir."
                                    Toast.makeText(context, "Escaneo para predicción falló o no devolvió redes.", Toast.LENGTH_LONG).show()
                                }
                                isScanningState = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictButtonEnabled
                    ) {
                        val isCurrentlyPredicting = isScanningState && predictButtonEnabled
                        if (isCurrentlyPredicting) {
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
            Card(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }
    }

    /*@OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun WiFiScannerApp(vm: MainViewModel) {
        var scanResultsText by remember { mutableStateOf("Presiona 'Escanear WiFi' o '¿Dónde Estoy?'") }
        var locationName by remember { mutableStateOf("") }
        var isScanningState by remember { mutableStateOf(false) }
        val context = LocalContext.current

        // Observa currentScan del ViewModel.
        // No necesitamos `by vm.currentScan` aquí porque lo accederemos como `vm.currentScan.value`
        // cuando sea necesario para las condiciones y para pasar los datos.

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
                            startWiFiScan { detailedScanResults -> // detailedScanResults es el String formateado
                                scanResultsText = detailedScanResults ?: "Error durante el escaneo o no se encontraron redes."
                                isScanningState = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanningState
                    ) {
                        // El estado del botón Escanear se basa solo en isScanningState
                        val isCurrentlyScanningForThisButton = isScanningState // Podríamos refinar esto si "Predecir" también usa isScanningState
                        if (isCurrentlyScanningForThisButton) {
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

                    // Lógica para habilitar el botón de guardar
                    val currentScanValue = vm.currentScan.value // Acceder al valor del State
                    val currentScanNotEmpty = currentScanValue.isNotEmpty()
                    val notScanning = !isScanningState
                    val isSaveButtonEnabled = locationName.isNotBlank() && currentScanNotEmpty && notScanning

                    Log.d(TAG, "UI Check for SAVE BUTTON: locationName='${locationName}', isNotBlank=${locationName.isNotBlank()}, currentScanNotEmpty=${currentScanNotEmpty} (size=${currentScanValue.size}), notScanning=${notScanning}, 최종 ENABLED=${isSaveButtonEnabled}")

                    Button(
                        onClick = {
                            if (!currentScanNotEmpty) { // Usar la variable ya calculada
                                Toast.makeText(context, "Realiza un escaneo primero", Toast.LENGTH_SHORT).show()
                            } else if (locationName.isNotBlank()) {
                                // Llamamos a saveLocationUiHelper que ahora también llama a saveFingerprint internamente
                                if (vm.saveLocationUiHelper(locationName, currentScanValue)) {
                                    Toast.makeText(context, "Ubicación '$locationName' guardada", Toast.LENGTH_SHORT).show()
                                    scanResultsText = formatSavedLocationsForDisplay(vm) // Actualizar UI con ubicaciones guardadas
                                    locationName = "" // Limpiar el campo de texto
                                    // Opcional: Limpiar el escaneo actual en el ViewModel si es deseado
                                    // vm.updateCurrentScan(emptyMap())
                                } else {
                                    Toast.makeText(context, "Error al guardar ubicación. Nombre vacío o sin datos de escaneo.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Ingresa un nombre para la ubicación", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSaveButtonEnabled
                    ) {
                        Text("💾 Guardar Ubicación Actual")
                    }
                }
            }

            // Card 3: Predecir Ubicación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Predecir Ubicación", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // El botón de predecir depende de si hay ubicaciones guardadas y si no se está escaneando.
                    // vm.savedLocations es un MutableMap normal, su tamaño se puede verificar directamente.
                    val predictButtonEnabled by remember(vm.savedLocations.size, isScanningState) {
                        derivedStateOf {
                            val slNotEmpty = vm.savedLocations.isNotEmpty()
                            Log.d(TAG, "derivedStateOf for predictButton: slNotEmpty=$slNotEmpty (size ${vm.savedLocations.size}), isScanningState=$isScanningState, enabled=${slNotEmpty && !isScanningState}")
                            slNotEmpty && !isScanningState
                        }
                    }
                    Log.d(TAG, "Predict Button final enabled state to be used: $predictButtonEnabled")

                    Button(
                        onClick = {
                            isScanningState = true // Indicar que estamos iniciando una operación de escaneo
                            Toast.makeText(context, "Escaneando para predecir...", Toast.LENGTH_SHORT).show()
                            startWiFiScan { _ -> // El resultado String no se usa aquí, vm.currentScan se actualiza internamente
                                // vm.currentScan.value ya está actualizado por startWiFiScan
                                if (vm.currentScan.value.isNotEmpty()) {
                                    val predictionResult = predictLocation(vm) // predictLocation usará vm.currentScan.value
                                    scanResultsText = predictionResult
                                } else {
                                    scanResultsText = "No se pudo obtener el escaneo actual para predecir."
                                    Toast.makeText(context, "Escaneo para predicción falló o no devolvió redes.", Toast.LENGTH_LONG).show()
                                }
                                isScanningState = false // Finalizar el estado de escaneo
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictButtonEnabled // Habilitado si hay ubicaciones guardadas y no se está escaneando.
                    ) {
                        // El estado de "Prediciendo..." es un poco más complejo si ambos botones usan `isScanningState`.
                        // Este es un intento de hacerlo más específico.
                        // Si el botón de predecir está habilitado Y isScanningState es true, entonces es probable que "Predecir" haya iniciado el escaneo.
                        val isCurrentlyPredicting = isScanningState && predictButtonEnabled
                        if (isCurrentlyPredicting) {
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        // .height(2000.dp) // Quitamos la altura fija temporal
                ) {
                    Text(
                        text = "Resultados:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = scanResultsText, // Asegúrate de que scanResultsText tenga suficiente contenido
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }*/

    @Suppress("DEPRECATION") // Para WifiManager.startScan()
    private fun startWiFiScan(onComplete: (detailedScanResults: String?) -> Unit) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi está deshabilitado. Por favor, habilítalo.", Toast.LENGTH_LONG).show()
            onComplete("WiFi deshabilitado.")
            viewModel.updateCurrentScan(emptyMap()) // Asegurarse de que el ViewModel refleje que no hay escaneo
            return
        }

        val wifiScanReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(context: Context?, intent: Intent?) {
                unregisterReceiver(this) // Importante: desregistrar el receiver aquí, al inicio

                // Primero, verifica el permiso necesario para acceder a scanResults
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, // Usa el contexto de MainActivity
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "onReceive: No se tiene el permiso ACCESS_FINE_LOCATION para obtener los resultados del escaneo.")
                    viewModel.updateCurrentScan(emptyMap()) // Actualizar ViewModel para reflejar el fallo/falta de permiso
                    onComplete("Permiso de ubicación necesario no concedido para obtener resultados del escaneo.")
                    return // Salir si no hay permiso
                }

                // Si llegamos aquí, tenemos el permiso ACCESS_FINE_LOCATION
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                } else {
                    true
                }
                Log.d(TAG, "onReceive: Escaneo WiFi completado. Success: $success. Permiso verificado.")

                // Ahora es más seguro acceder a wifiManager.scanResults
                // La condición original (success || !results.isNullOrEmpty()) se puede simplificar si siempre obtenemos los resultados
                val results = wifiManager.scanResults // Acceso a los resultados del escaneo

                if (success || !results.isNullOrEmpty()) { // Puedes mantener 'success' si es relevante para tu lógica más allá de solo tener resultados
                    val tempScan = mutableMapOf<String, Int>()
                    val detailedResultsBuilder = StringBuilder()
                    detailedResultsBuilder.append("Redes Encontradas (${results.size}):\n")

                    if (results.isEmpty()) {
                        detailedResultsBuilder.append("Ninguna red WiFi encontrada en este escaneo.\n")
                    } else {
                        results.forEach { result ->
                            val bssid = result.BSSID ?: "N/A"
                            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                result.wifiSsid?.toString() ?: "<unknown_ssid>"
                            } else {
                                @Suppress("DEPRECATION")
                                result.SSID ?: "<unknown_ssid>"
                            }
                            val rssi = result.level
                            tempScan[bssid] = rssi
                            detailedResultsBuilder.append("  SSID: $ssid, BSSID: $bssid, RSSI: $rssi dBm\n")
                        }
                    }
                    viewModel.updateCurrentScan(tempScan)
                    Log.d(TAG, "Escaneo completado. ViewModel currentScan actualizado con ${tempScan.size} redes.")
                    onComplete(detailedResultsBuilder.toString())
                } else {
                    Log.w(TAG, "Escaneo WiFi falló o no devolvió resultados actualizados (incluso después de verificar el permiso).")
                    viewModel.updateCurrentScan(emptyMap())
                    onComplete("Escaneo WiFi falló o no se encontraron redes (post-verificación de permiso).")
                }
            }
        }


        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        Log.d(TAG, "Iniciando escaneo WiFi desde startWiFiScan()...")
        val scanStarted = wifiManager.startScan()
        if (!scanStarted) {
            Log.e(TAG, "wifiManager.startScan() devolvió false. El escaneo no pudo iniciarse.")
            unregisterReceiver(wifiScanReceiver) // Desregistrar si el inicio falla inmediatamente
            viewModel.updateCurrentScan(emptyMap())
            onComplete("No se pudo iniciar el escaneo WiFi.")
        } else {
            Log.d(TAG, "Escaneo WiFi iniciado...")
        }
    }

    // --- Funciones de Lógica de Predicción y Formato ---
    // (Estas funciones deben tomar `vm.currentScan.value` si necesitan el escaneo actual)

    private fun formatScanResultsForDisplay(scanData: Map<String, Int>): String {
        if (scanData.isEmpty()) return "No hay datos de escaneo para mostrar."
        val builder = StringBuilder("Escaneo Actual (${scanData.size} APs):\n")
        scanData.forEach { (bssid, rssi) ->
            builder.append("  BSSID: $bssid, RSSI: $rssi dBm\n")
        }
        return builder.toString()
    }

    private fun formatSavedLocationsForDisplay(vm: MainViewModel): String {
        if (vm.savedLocations.isEmpty()) return "No hay ubicaciones guardadas."
        val builder = StringBuilder("Ubicaciones Guardadas (${vm.savedLocations.size}):\n")
        vm.savedLocations.forEach { (locationName, fingerprint) ->
            builder.append("\nUbicación: $locationName\n")
            fingerprint.forEach { (bssid, rssi) ->
                builder.append("  BSSID: $bssid, RSSI: $rssi dBm\n")
            }
        }
        return builder.toString()
    }

    private fun euclideanDistance(fp1: Map<String, Int>, fp2: Map<String, Int>, defaultValue: Int = -100): Double {
        val allBssids = (fp1.keys + fp2.keys).distinct()
        var sumOfSquares = 0.0
        for (bssid in allBssids) {
            val rssi1 = fp1.getOrDefault(bssid, defaultValue)
            val rssi2 = fp2.getOrDefault(bssid, defaultValue)
            sumOfSquares += (rssi1 - rssi2).toDouble().pow(2)
        }
        return sqrt(sumOfSquares)
    }

    private fun predictLocation(vm: MainViewModel): String {
        val currentFingerprint = vm.currentScan.value // Usa vm.currentScan.value
        if (currentFingerprint.isEmpty()) {
            return "No se pudo obtener el escaneo actual para la predicción."
        }
        if (vm.savedLocations.isEmpty()) {
            return "No hay ubicaciones guardadas para comparar."
        }

        var bestMatchLocation: String? = null
        var minDistance = Double.MAX_VALUE

        vm.savedLocations.forEach { (locationName, savedFingerprint) ->
            val distance = euclideanDistance(currentFingerprint, savedFingerprint)
            Log.d(TAG, "Distancia a '$locationName': $distance")
            if (distance < minDistance) {
                minDistance = distance
                bestMatchLocation = locationName
            }
        }

        return if (bestMatchLocation != null) {
            "Predicción: Estás en '$bestMatchLocation' (Distancia: ${"%.2f".format(minDistance)})"
        } else {
            "No se pudo predecir la ubicación."
        }
    }
}
