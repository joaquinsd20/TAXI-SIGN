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
| Ubicación en segundo plano | Foreground Service (`foregroundServiceType="location"`) + `ACCESS_BACKGROUND_LOCATION` |

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
│       │   └── Seguridad/                 # Contraseña, soporte y cumplimiento de permisos
│       │       ├── PermisoUbicacionHelper.kt        # Aviso destacado + consentimiento
│       │       ├── PermisoUbicacionFondoManager.kt  # Solicitud escalonada de permisos
│       │       └── LocationForegroundService.kt     # Servicio en primer plano (ubicación)
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

## 📍 Cumplimiento de Google Play — Aviso destacado y consentimiento

La app solicita `ACCESS_BACKGROUND_LOCATION` para que el pasajero pueda ver la posición del conductor
en tiempo real aunque la app esté en segundo plano o la pantalla bloqueada. Play Console exige
**un aviso destacado (prominent disclosure) + consentimiento explícito** antes de pedir ese permiso.

### Cómo está implementado

| Pieza | Archivo | Función |
|---|---|---|
| Aviso destacado (UI) | `res/layout/dialog_aviso_ubicacion_fondo.xml` | Pantalla completa, no cancelable, con *qué* se recopila, *por qué*, *cómo se usa* y *tus controles*. |
| Consentimiento | `Seguridad/PermisoUbicacionHelper.kt` | Persiste la decisión en `SharedPreferences` y permite revocarla. |
| Solicitud escalonada | `Seguridad/PermisoUbicacionFondoManager.kt` | Orden exigido por Android 11+: `ACCESS_FINE_LOCATION` → `ACCESS_BACKGROUND_LOCATION` en una segunda solicitud. El permiso de segundo plano **solo** se pide si el usuario aceptó el aviso. |
| Servicio en primer plano | `Seguridad/LocationForegroundService.kt` | `foregroundServiceType="location"` con notificación permanente obligatoria mientras se comparte la ubicación. |

**Puntos clave para pasar la revisión:**

1. El aviso destacado aparece **antes** de cualquier diálogo del sistema, y no se puede cerrar con
   «Atrás» ni tocando fuera.
2. Si el usuario lo rechaza, la app **sigue funcionando con normalidad** (solo se desactiva el
   rastreo en segundo plano). Esto es un requisito explícito de Play.
3. El usuario puede **revocar** el consentimiento en *Configuración → Ubicación en segundo plano*
   (interruptor) y revocar el permiso del sistema en los ajustes de Android.
4. Mientras la ubicación se recopila, hay una **notificación persistente** del servicio en primer plano.

### Dónde se activa

El flujo se pide al conductor en los momentos en que su posición debe seguir siendo visible:

| Pantalla | Cuándo |
|---|---|
| `PanelConductorActivity` | Al conectar el turno (*INICIAR TURNO*). |
| `IrHaciaPasajero` | Al ir a recoger al pasajero. |
| `DuranteViajeConductor` | Durante el viaje. |

El pasajero **no** necesita ubicación en segundo plano: solo la usa mientras la app está visible.

### Formulario de Datos de Usuario (Data Safety)

Respuestas a declarar en Play Console:

- **¿Recopila datos de ubicación?** → **Sí**.
- **Tipo**: Ubicación precisa (`ACCESS_FINE_LOCATION`) → **Sí**; Ubicación aproximada → **Sí**.
- **¿Se comparte con terceros?** → **Sí** → se comparte con **el pasajero que tiene asignado el viaje**
  (compartido en tiempo real dentro de la app). No se vende ni se usa con fines publicitarios.
- **Es opcional**: **Sí** → el usuario puede rechazarlo sin perder funcionalidad.
- **No se recoge historial de ubicación** fuera de los turnos/viajes activos.

---

## 🛡️ Seguridad

- Las **claves de firma** (`*.jks`), los **AAB/APK** compilados y `local.properties` están excluidos por `.gitignore`.
- `app/google-services.json` y la API key de Maps incluidos son **de ejemplo**: reemplázalos por los tuyos y restringe la key por aplicación y SHA-1.
- La contraseña del keystore está escrita en `app/build.gradle.kts`: muévela a `local.properties`
  o a variables de entorno antes de publicar el repositorio.
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
