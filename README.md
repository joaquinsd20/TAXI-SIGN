# 🚖 TaxiSign (Taxi-Sign)

Aplicación Android nativa (Kotlin) para solicitar taxis, **diseñada con accesibilidad para personas sordas/mudas**.

TaxiSign conecta pasajeros y conductores en tiempo real usando Firebase y Google Maps, e incorpora un **traductor de señas (dactilología)** que convierte los mensajes de texto del conductor en imágenes de señas, además de alertas por vibración y destello de cámara.

---

## 📱 Funcionalidades

### Para el pasajero
| Módulo | Descripción |
|---|---|
| **Selección de rol** | Pantalla inicial para elegir rol (Pasajero / Conductor) con validación de sesión activa. |
| **Registro / Login** | Correo y contraseña (Firebase Auth) o **Google Sign-In** con completado de perfil. |
| **Búsqueda de destino** | Autocomplete de Google Places, destinos frecuentes, lugares recientes y selección de ciudad. |
| **Solicitud de viaje** | Creación de solicitud, recepción de **ofertas de conductores** y selección de la mejor oferta. |
| **Mapa en tiempo real** | Google Maps con posición del conductor, trazado de ruta y tiempo/distancia estimados (Directions API). |
| **Chat con señas** | Mensajería en vivo con **traductor de señas**: texto del conductor → secuencia de imágenes de dactilología. |
| **Código QR de seguridad** | QR del viaje que el conductor escanea para dar inicio al trayecto (estilo PIN de Uber). |
| **Historial y reseñas** | Historial de viajes, detalle de viaje y calificación mutua. |
| **Notificaciones** | Avisos en vivo con notificación del sistema, vibración y destello de cámara (accesibilidad). |
| **Seguridad** | Contactos de confianza, botón SOS, cambio de contraseña y soporte. |

### Para el conductor
| Módulo | Descripción |
|---|---|
| **Panel del conductor** | Solicitudes entrantes; aceptar o rechazar viajes en tiempo real. |
| **Navegación** | Ruta hacia el pasajero, mapa GPS y seguimiento del viaje en curso. |
| **Ofertas** | Envío de oferta (tarifa y tiempo estimado) al pasajero. |
| **Validación de documentos** | Carga y verificación de documentos + estado de aprobación antes de operar. |
| **Ganancias** | Historial de ingresos por viaje y totales. |
| **Perfil y ajustes** | Datos del vehículo, foto, reseñas recibidas y configuración de cuenta. |

### Accesibilidad (feature principal)
- 👋 **Traductor de señas** (`TraductorSenas.kt`): reconoce palabras con seña propia (`sena_palabra_*`) y, si no existen, aplica **dactilología** letra por letra (`sena_a` … `sena_z`, `sena_nn` para la ñ).
- 🔔 **Vibración + flash de cámara** en cada notificación importante.
- 🔤 Tipografía **Nunito** y `contentDescription` en todos los controles relevantes.

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin (Android SDK 36, `minSdk 24`) |
| Build | Gradle 9.4.1 + Kotlin DSL + Version Catalog (`libs.versions.toml`) |
| Plugin | Android Gradle Plugin 9.2.1 |
| UI | View Binding, AppCompat, Material Components, ConstraintLayout, CircleImageView |
| Mapas / Ubicación | Google Maps SDK, Places API, Location API, Directions API |
| Backend | Firebase (Auth, Realtime Database, Storage, Firestore) vía Firebase BoM 33.1.0 |
| RED | Volley 1.2.1 (endpoints REST legacy en PHP) |
| QR | ZXing (`core` + `zxing-android-embedded`) |
| Identidad | Google Sign-In (Play Services Auth) |

---

## 📂 Estructura del proyecto

```
TaxiSign/
├── app/
│   ├── build.gradle.kts          # Configuración del módulo app
│   ├── google-services.json      # Configuración de Firebase (reemplazar por la propia)
│   └── src/main/
│       ├── AndroidManifest.xml    # activities, permisos y API key de Maps
│       ├── java/com/taxisign/pro/
│       │   ├── MainActivity.kt            # Selección de rol
│       │   ├── SessionManager.kt          # Sesión en memoria
│       │   ├── RutaHelper.kt              # Distancia/tiempo por carretera
│       │   ├── NavegacionHelper.kt        # Navegación entre pantallas
│       │   ├── MapaBaseFragment.kt        # Base de mapas reutilizable
│       │   ├── Viaje.kt                   # Modelo de viaje
│       │   ├── ApiConfig/                 # Endpoints REST legacy (PHP)
│       │   ├── Chat/                      # Chat + traductor de señas
│       │   ├── Login/                     # Auth, Google Sign-In, documentos
│       │   ├── Notificaciones/            # Notificaciones + vibracion/flash
│       │   ├── Pasajero/                  # Flujo completo del pasajero
│       │   ├── conductor/                 # Flujo completo del conductor
│       │   └── Seguridad/                 # Contraseña y soporte
│       └── res/
│           ├── layout/           # Layouts XML
│           ├── drawable/         # Recursos gráficos + imágenes de señas
│           ├── font/             # Nunito
│           └── values/           # strings, colors, themes
├── gradle/libs.versions.toml     # Catálogo de dependencias
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## ✅ Requisitos previos

- **Android Studio** (Ladybug o superior) con **JDK 17**.
- **Android SDK 36** instalado (compileSdk / targetSdk).
- Un proyecto de **Firebase** propio (ver abajo).
- Una **API key de Google Maps** propia.

---

## 🚀 Configuración e instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/joaquinsd20/TAXI-SIGN.git
cd TAXI-SIGN
```

### 2. Configurar Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com).
2. En **Authentication → Sign-in method**, habilita **Email/Password** y **Google**.
3. En **Realtime Database**, crea la base de datos y define las reglas (ver abajo).
4. En **Storage**, habilita el bucket.
5. Descarga `google-services.json` y colócalo en `app/google-services.json`
   (reemplazando el de ejemplo que viene en el repositorio).

**Reglas sugeridas para Realtime Database:**

```json
{
  "rules": {
    "usuarios": {
      "$uid": { ".read": "$uid === auth.uid", ".write": "$uid === auth.uid" }
    },
    "viajes": {
      "$viajeId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "notificaciones": {
      "$uid": { ".read": "$uid === auth.uid", ".write": "auth != null" }
    }
  }
}
```

> ⚠️ Ajusta estas reglas al modelo de datos real de tu proyecto antes de publicar en producción.

### 3. Configurar Google Maps

Habilita en Google Cloud Console las APIs **Maps SDK for Android**, **Places API** y **Directions API**, y restringe la key por `package name` + `SHA-1`.

Luego actualiza la clave en los dos lugares del proyecto:

- `app/src/main/AndroidManifest.xml` → `<meta-data android:name="com.google.android.geo.API_KEY" .../>`
- `app/src/main/res/values/strings.xml` → `<string name="google_maps_key">TU_CLAVE</string>`

### 4. (Opcional) Backend PHP

`app/src/main/java/com/taxisign/pro/ApiConfig/ApiConfig.kt` contiene los endpoints REST legacy hacia un backend PHP:

```kotlin
private const val BASE_URL = "http://192.168.100.12/taxisign_db/"
```

Apunta `BASE_URL` a tu servidor si vas a usar ese flujo. (El flujo actual de viajes trabaja directamente sobre Firebase Realtime Database.)

### 5. Compilar y ejecutar

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/`.

---

## 🔐 Compilar en modo release

El keystore **no** se incluye en el repositorio (está en `.gitignore`). Crea el tuyo y ajusta `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("release/taxisign-key.jks")   // tu keystore local
        storePassword = "TU_PASSWORD"
        keyAlias = "TU_ALIAS"
        keyPassword = "TU_PASSWORD"
    }
}
```

```bash
./gradlew bundleRelease   # genera app/build/outputs/bundle/release/app-release.aab
```

---

## 🗄️ Modelo de datos en Firebase (resumen)

| Nodo | Contenido |
|---|---|
| `usuarios/pasajeros/{uid}` | Perfil del pasajero (nombre, teléfono, foto, tipo de usuario). |
| `usuarios/conductores/{uid}` | Perfil del conductor (nombre, vehículo, documentos, estado de aprobación). |
| `solicitudes/{id}` | Solicitudes de viaje pendientes y ofertas recibidas. |
| `viajes/{id}` | Viaje en curso: conductor, pasajero, ruta, `codigoSeguridad`, `viajeIniciado`, estado. |
| `notificaciones/{uid}` | Avisos dirigidos a un usuario concreto. |
| `ofertas/{idViaje}/{uidConductor}` | Ofertas de tarifa de los conductores. |

---

## 🛡️ Seguridad

- Las **claves de firma** (`*.jks`), los **AAB/APK** compilados y `local.properties` están excluidos por `.gitignore`.
- `app/google-services.json` y la API key de Maps incluidos son **de ejemplo**: reemplázalos por los tuyos y restringe la key por aplicación y SHA-1.
- Nunca subas credenciales al repositorio.

---

## 🧪 Pruebas

```bash
./gradlew test              # pruebas unitarias (JUnit)
./gradlew connectedAndroidTest   # pruebas instrumentadas (Espresso)
```

---

## 📄 Licencia

MIT — ver el archivo [LICENSE](LICENSE).

---

## 👥 Autores

Desarrollado por el equipo de **TaxiSign** — [joaquinsd20](https://github.com/joaquinsd20).

> *“Porque todos merecen poder pedir un taxi.”*
