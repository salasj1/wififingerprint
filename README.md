# WiFi Fingerprint - Un Prototipo en Android para Localización en Interiores
Este repositorio contiene el código fuente para el proyecto de localización en interiores mediante WiFi Fingerprinting, desarrollado en Android Studio.

---
## 1. Sincronización y Construcción del Proyecto

Para configurar el entorno de desarrollo y construir el proyecto, sigue estos pasos con precisión.

### Prerrequisitos

-   [Android Studio](https://developer.android.com/studio) (versión Android Studio Narwhal Feature Drop | 2025.1.2 o superior).
-   Git instalado y configurado en tu sistema.

### Pasos de Configuración

1.  **Clonar el Repositorio:**
    Abre una terminal o Git Bash y ejecuta el siguiente comando para clonar el proyecto en tu máquina local.

    ```bash
    git clone https://github.com/salasj1/wififingerprint.git
    ```

2.  **Abrir en Android Studio:**
    -   Inicia Android Studio.
    -   Selecciona **"Open"** (o "File" > "Open").
    -   Navega hasta el directorio donde clonaste el repositorio (`wififingerprint`) y selecciónelo.
    -   Android Studio detectará automáticamente la estructura del proyecto Gradle.

3.  **Sincronización de Gradle:**
    -   Espera a que Android Studio indexe los archivos y sincronice el proyecto. Este proceso descargará todas las dependencias necesarias.
    -   Puedes monitorear el progreso en la barra de estado inferior. Si es necesario, puedes forzar una sincronización manual desde "File" > "Sync Project with Gradle Files".

4.  **Construir y Ejecutar:**
    -   Una vez que la sincronización se complete sin errores, puedes construir el proyecto desde "Build" > "Make Project".
    -   Para ejecutar la aplicación, selecciona un emulador disponible o conecta un dispositivo Android físico y presiona el botón **"Run 'app'"** (el icono de play verde).

### Solución de Problemas de Construcción

Si encuentras errores inesperados durante la compilación, limpiar el proyecto puede resolver el problema. Esto elimina los archivos de compilación anteriores (`build`) y fuerza a Android Studio a reconstruir todo desde cero.

**Opción 1: Usando la Terminal**

1.  Abre la terminal integrada en Android Studio ("View" > "Tool Windows" > "Terminal").
2.  Ejecuta el siguiente comando:
    ```bash
    gradlew clean
    ```

**Opción 2: Usando la Interfaz de Gradle**

1.  A la derecha lateral de la ventana de Android Studio, busca y abre la pestaña **"Gradle"** que es un ícono de un elefante.Aqui acontinuación:


<p align="center">
  <img width="46" height="855" alt="image" src="https://github.com/user-attachments/assets/48b00886-cba0-4c80-9006-d6b5a9cbe81c" />
</p>

3.  Expande el nombre de tu proyecto (`wififingerprint`) > **"Tasks"** > **"build"**.
4.  Haz doble clic en la tarea **`clean`**. Verás el progreso en la consola "Build".

Una vez que el proceso de limpieza termine, intenta construir y ejecutar el proyecto de nuevo.

---
## 2. Ejecutar en un Dispositivo Físico (Depuración por USB)

Para probar la aplicación directamente en tu dispositivo Android, necesitas activar la "Depuración por USB". Es un proceso que se hace una sola vez por dispositivo.

### Paso 1: Habilitar las Opciones de Desarrollador

1.  En tu teléfono, ve a **Ajustes** > **Acerca del teléfono**.
2.  Busca la opción **"Número de compilación"**.
3.  **Toca 7 veces seguidas** sobre "Número de compilación". Verás un mensaje que dice "¡Ahora eres un desarrollador!". Es posible que te pida tu PIN o patrón de desbloqueo.

### Paso 2: Activar la Depuración por USB

1.  Vuelve al menú principal de **Ajustes**.
2.  Busca un nuevo menú llamado **"Opciones de desarrollador"** (a menudo se encuentra dentro de "Sistema" o "Ajustes adicionales").
3.  Entra en "Opciones de desarrollador" y busca el interruptor para **"Depuración por USB"**. Actívalo.

### Paso 3: Conectar y Ejecutar

1.  **Conecta tu teléfono** a la computadora con un cable USB.
2.  En la pantalla de tu teléfono, aparecerá una ventana emergente pidiendo **"Permitir depuración por USB"**. Marca la casilla "Permitir siempre desde esta computadora" y presiona **"Permitir"**.
3.  En Android Studio, verás que el nombre de tu dispositivo aparece en la barra de herramientas superior, junto al botón de "Run".
4.  ¡Listo! Simplemente presiona el **botón de "Run 'app'"** (el icono de play verde) y la aplicación se instalará y se ejecutará directamente en tu teléfono.
