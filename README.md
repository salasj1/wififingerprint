# wififingerprint – WiFi Fingerprint (PoC)

Una app prototipo de Android (Jetpack Compose + Room) que:
- Escanea redes WiFi cercanas y muestra SSID, BSSID y nivel de señal (RSSI), además del número total de redes detectadas.
- Permite “guardar” huellas (fingerprints) de señal asociadas a un nombre de ubicación.
- Predice la ubicación actual comparando la huella actual con las guardadas mediante distancia euclidiana.


## ¿Qué hace este repositorio?

Este repositorio contiene una aplicación Android desarrollada principalmente en Kotlin (Jetpack Compose) y con persistencia local usando Room. Sirve como prueba de concepto para localización en interiores a partir de “huellas” de redes WiFi:
- Escanea las redes WiFi circundantes (BSSID → RSSI).
- Guarda esas lecturas como fingerprint bajo un nombre de ubicación.
- Calcula la ubicación más probable comparando la huella actual vs. huellas guardadas usando distancia euclidiana.

## Características clave

- Escaneo de redes con WifiManager y BroadcastReceiver.
- UI moderna con Jetpack Compose y Material 3.
- Persistencia local con Room (RoomDatabase, Entities y Repository).
- Lógica de predicción simple basada en distancia euclidiana de RSSI por BSSID.

## Requisitos

- Android Studio Narwhal Feature Drop | 2025.1.2 o superior (recomendado).
- Gradle con soporte para Compose (proyecto ya configurado).
- Un dispositivo Android con WiFi habilitado (recomendado físico; los emuladores suelen no exponer bien el hardware WiFi).
- Permisos de ubicación y WiFi concedidos en tiempo de ejecución.

Notas de plataforma:
- Android 10+ (API 29+): Para escanear redes, Android requiere permisos de ubicación y, en muchos dispositivos, que la ubicación del sistema esté activada.
- Android 13+ (API 33): Existe el permiso NEARBY_WIFI_DEVICES. Si apuntas/compilas con SDK 33+, considera declararlo en el Manifest y gestionarlo en tiempo de ejecución. Actualmente la app solicita ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION.

## ¿Cómo ejecutar la aplicación?

1) Clonar e importar
- Clona el repositorio:
  ```bash
  git clone https://github.com/salasj1/wififingerprint.git
  ```
- Abre el proyecto en Android Studio (Open an existing project).

2) Sincronizar y compilar
- Espera a que Gradle sincronice dependencias (Sync Now).
- Construir con Gradle (opcional, desde la terminal del proyecto):
  ```bash
  ./gradlew build
  ```
- Selecciona un dispositivo de destino (preferiblemente físico) con WiFi encendido.

3) Permisos
- En el primer inicio, la app te pedirá permisos:
  - ACCESS_FINE_LOCATION
  - ACCESS_COARSE_LOCATION
  - ACCESS_WIFI_STATE
  - CHANGE_WIFI_STATE
- Acepta todos para que el escaneo funcione correctamente.

4) Ejecutar
- Pulsa Run (▶️) en Android Studio.
- La app se instalará y abrirá en el dispositivo.

## ¿Cómo usar la app?

1. Escanear redes WiFi:
   - Pulsa “Escanear WiFi”.
   - Verás la lista de redes detectadas con SSID, BSSID y RSSI en dBm, y el total de redes.

2. Guardar ubicación:
   - Introduce un “Nombre de la ubicación” (ej.: “Laboratorio A”).
   - Realiza un escaneo (la app usa los resultados más recientes).
   - Pulsa “Guardar Ubicación Actual”.
   - Se almacenará el fingerprint (mapa BSSID → RSSI) asociado a esa ubicación.

3. ¿Dónde estoy? (Predicción):
   - Pulsa “¿Dónde Estoy?” para que la app haga un nuevo escaneo y compare contra las ubicaciones guardadas.
   - La app muestra la ubicación cuya huella es más cercana (menor distancia euclidiana).

4. Resultados:
   - La sección “Resultados” muestra el último escaneo, las huellas guardadas o la predicción.

## Estructura de archivos principales en app/src/main

Estructura típica de un módulo Android:

- AndroidManifest.xml  
  Define permisos, actividades y componentes.

- java/ o kotlin/  
  Código fuente. En este proyecto el paquete base es `com.ucab.wififingerprint`.

- res/  
  Recursos:
  - values/: strings, colores, temas.
  - mipmap/: íconos.
  - drawable/: imágenes, vectores.

- assets/ (opcional)  
  Archivos estáticos adicionales si fueran necesarios.

- schemas/  
  Esquemas de Room (cuando exportSchema=true), útiles para migraciones y verificación.

## Archivos clave explicados (app/src/main/java)

- com/ucab/wififingerprint/MainActivity.kt
  - Pantalla principal y UI con Jetpack Compose (Material 3).
  - Gestiona permisos en runtime (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE).
  - Escaneo:
    - Registra un BroadcastReceiver con `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION`.
    - Verifica permisos antes de leer `wifiManager.scanResults`.
    - Construye el fingerprint (BSSID → RSSI), actualiza el ViewModel y genera texto detallado de resultados.
  - Acciones de UI:
    - “Escanear WiFi”: inicia el escaneo y muestra resultados.
    - “Guardar Ubicación”: guarda la huella actual con el nombre ingresado (vía ViewModel).
    - “¿Dónde Estoy?”: re-escanea y llama a `predictLocation` para mostrar la ubicación más probable.
  - Utilidades:
    - `formatSavedLocationsForDisplay`, `formatScanResultsForDisplay`.
    - `predictLocation`: compara la huella actual con las guardadas usando distancia euclidiana (`euclideanDistance`), aplicando un valor por defecto (-100 dBm) cuando falta un BSSID en alguna huella.

- com/ucab/wififingerprint/MainViewModel.kt
  - AndroidViewModel que expone estado observable para Compose.
  - `currentScan: State<Map<String, Int>>` con el escaneo actual (clave: BSSID, valor: RSSI).
  - `savedLocations: MutableMap<String, Map<String, Int>>` en memoria para predicción y habilitar botones.
  - Integración con persistencia:
    - Obtiene `FingerprintDao` desde `AppDatabase.getInstance(...)`.
    - Usa `FingerprintRepository` para leer/guardar datos.
  - Funciones:
    - `loadSavedFingerprints()`: carga huellas desde la base de datos a `savedLocations`.
    - `updateCurrentScan(...)`: actualiza el estado de escaneo actual.
    - `saveFingerprintAndLocationUi(...)`: guarda fingerprint en BD y actualiza `savedLocations`.
    - `clearAllFingerprints()`: borra todo en BD y limpia estados.

- com/ucab/wififingerprint/LocationEntity.kt
  - @Entity(tableName = "locations") data class `LocationEntity`:
    - `name` (PrimaryKey): nombre de la ubicación.
    - `minigameScene`, `description`: metadatos opcionales.
  - @Entity(tableName = "fingerprints") data class `FingerprintEntity`:
    - `id` (autoGenerate), `locationName`, `bssid`, `rssi`.
    - Representa lecturas RSSI por BSSID asociadas a una ubicación.

- com/ucab/wififingerprint/data/db/AppDatabase.kt
  - `AppDatabase` (RoomDatabase singleton).
  - Expone `fingerprintDao()`.
  - Nota técnica: El decorador actual indica `@Database(entities = [LocationEntity::class, WifiReadingEntity::class], ...)`, pero en el código disponible la entity se llama `FingerprintEntity`. Asegúrate de alinear las entidades en `@Database` con las definiciones reales (probablemente `[LocationEntity::class, FingerprintEntity::class]`) y de definir/usar el `FingerprintDao` correspondiente.

- com/ucab/wififingerprint/ui/theme/Color.kt, Type.kt, Theme.kt
  - Tema Material 3 (paleta, tipografía y configuración claro/oscuro con dynamic color en Android 12+).

- tests:  
  - app/src/test/java/com/ucab/wififingerprint/ExampleUnitTest.kt: prueba básica (JUnit).

## Flujo interno (resumen)

1) UI (MainActivity + Compose)  
- Botones para escanear, guardar y predecir.  
- Texto con resultados formateados (monospace).

2) Escaneo de WiFi  
- `WifiManager.startScan()` + BroadcastReceiver.  
- Requiere WiFi habilitado y permisos concedidos.

3) Persistencia (Room)  
- `AppDatabase` → `FingerprintDao` → `FingerprintRepository`.  
- Guarda/lee fingerprints y los expone al ViewModel/Compose.

4) Predicción  
- Distancia euclidiana entre fingerprint actual y cada una de las guardadas:  
  - `allBssids` = unión de BSSID de ambas huellas.  
  - Si falta un BSSID, se usa un valor por defecto (-100 dBm).  
  - La menor distancia determina la ubicación candidata.

## Permisos (AndroidManifest.xml)

Incluye (ajusta según tu targetSdk y caso de uso):
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Android 13+ (opcional, si apuntas/compilas con SDK 33+) -->
<!-- <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" /> -->
```

Recuerda solicitar permisos en tiempo de ejecución como hace `MainActivity.kt`.

## Consejos y consideraciones

- Políticas de escaneo: Android limita la frecuencia de escaneo y requiere permisos de ubicación. Si `startScan()` devuelve false o no aparecen resultados:
  - Verifica WiFi encendido y Ubicación del sistema activa.
  - Asegura permisos concedidos en runtime.
  - Ten en cuenta restricciones del fabricante (escaneos en background).

- Consistencia Room:
  - Alinea Entities declaradas en `@Database` con las clases reales (`FingerprintEntity` vs `WifiReadingEntity`).
  - Define y utiliza `FingerprintDao` y `FingerprintRepository` de forma coherente con las tablas.

- Calidad de predicción:
  - La distancia euclidiana con RSSI es simple y sensible al ruido. Puedes mejorar:
    - Promediando múltiples escaneos por ubicación.
    - Normalizando RSSI.
    - Filtrando BSSID muy débiles o inestables.
    - Implementando KNN o ponderaciones por estabilidad.

## Scripts/Comandos útiles

- Limpiar y compilar:
  ```bash
  ./gradlew clean assembleDebug
  ```

- Ejecutar pruebas:
  ```bash
  ./gradlew test
  ```

## Licencia

Proyecto prototipo con fines educativos/demostrativos. 
