package com.taxisign.pro.Seguridad

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager

/**
 * LocationForegroundService
 *
 * Servicio en primer plano con tipo "location" (foregroundServiceType).
 *
 * Por que existe: en Android 11+ (API 30) el permiso ACCESS_BACKGROUND_LOCATION
 * solo tiene sentido si la app sigue leyendo la ubicacion cuando NO esta
 * visible. Un servicio en primer plano es la via soportada para hacerlo, y ademas
 * garantiza que el sistema no lo mate por falta de memoria.
 *
 * Requisitos de Play Console que cubre:
 *  - Aviso destacado + consentimiento explico antes de pedir el permiso
 *    (ver PermisoUbicacionHelper / PermisoUbicacionFondoManager).
 *  - Aviso visible al usuario mientras se recopila la ubicacion
 *    (notificacion permanente obligatoria del servicio en primer plano).
 *
 * El servicio escribe las coordenadas en el nodo de Firebase que se le indique
 * mediante los extras del Intent.
 */
class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var pathFirebase: String = ""
    private var uidConductor: String = ""
    private var modo: String = MODO_DISPONIBLE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACCION_DETENER) {
            detener()
            return START_NOT_STICKY
        }

        pathFirebase = intent.getStringExtra(EXTRA_PATH_FIREBASE).orEmpty()
        uidConductor = intent.getStringExtra(EXTRA_UID).orEmpty()
        modo = intent.getStringExtra(EXTRA_MODO) ?: MODO_DISPONIBLE

        if (pathFirebase.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Sin los permisos no se puede promover el servicio a primer plano con
        // tipo "location": se cierra de inmediato (no se llega al timeout de
        // startForegroundService).
        if (!tienePermisoUbicacion()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Debe llamarse dentro de los primeros segundos tras startForegroundService.
        if (!iniciarEnPrimerPlano()) {
            stopSelf()
            return START_NOT_STICKY
        }

        iniciarActualizaciones()

        return START_NOT_STICKY
    }

    private fun iniciarEnPrimerPlano(): Boolean {
        val notificacion = construirNotificacion()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICACION_ID,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICACION_ID, notificacion)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun construirNotificacion(): Notification {
        val intentAbrir = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intentAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.ic_stat_ubicacion)
            .setContentTitle(getString(R.string.fgs_ubicacion_titulo))
            .setContentText(getString(R.string.fgs_ubicacion_mensaje))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.fgs_ubicacion_mensaje))
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_ID,
            getString(R.string.fgs_ubicacion_canal),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.fgs_ubicacion_mensaje)
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(canal)
    }

    private fun iniciarActualizaciones() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            INTERVALO_ACTUALIZACION_MS
        ).setMinUpdateIntervalMillis(INTERVALO_MINIMO_MS).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                subirUbicacion(location)
            }
        }

        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun subirUbicacion(location: Location) {
        val database = FirebaseDatabase.getInstance().reference

        val datos = if (modo == MODO_VIAJE) {
            mapOf(
                "latitud" to location.latitude,
                "longitud" to location.longitude,
                "bearing" to location.bearing,
                "timestamp" to System.currentTimeMillis()
            )
        } else {
            mapOf(
                "uid" to uidConductor,
                "nombre" to (SessionManager.nombreUsuario ?: "Conductor"),
                "latitud" to location.latitude,
                "longitud" to location.longitude,
                "estado" to "disponible",
                "timestamp" to System.currentTimeMillis()
            )
        }

        database.child(pathFirebase).setValue(datos)
    }

    private fun tienePermisoUbicacion(): Boolean {
        val permisoFino = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val permisoFondo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return permisoFino && permisoFondo
    }

    private fun detener() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: SecurityException) {
            }
        }
        locationCallback = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: SecurityException) {
            }
        }
        locationCallback = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {

        const val ACCION_INICIAR = "com.taxisign.pro.ACCION_UBICACION_FONDO_INICIAR"
        const val ACCION_DETENER = "com.taxisign.pro.ACCION_UBICACION_FONDO_DETENER"

        const val EXTRA_PATH_FIREBASE = "extra_path_firebase"
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_MODO = "extra_modo"

        const val MODO_DISPONIBLE = "disponible"
        const val MODO_VIAJE = "viaje"

        private const val CANAL_ID = "taxisign_ubicacion_fondo"
        private const val NOTIFICACION_ID = 4201
        private const val INTERVALO_ACTUALIZACION_MS = 5000L
        private const val INTERVALO_MINIMO_MS = 3000L

        /**
         * Inicia el servicio en primer plano.
         * Debe llamarse desde una Activity visible (restriccion de Android 12+).
         */
        fun iniciar(
            context: Context,
            pathFirebase: String,
            uid: String,
            modo: String = MODO_DISPONIBLE
        ) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACCION_INICIAR
                putExtra(EXTRA_PATH_FIREBASE, pathFirebase)
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_MODO, modo)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Si el sistema no permite iniciar el servicio, el app sigue
                // funcionando con el rastreo en primer plano de la Activity.
            }
        }

        /** Detiene el servicio en primer plano. */
        fun detener(context: Context) {
            try {
                context.stopService(Intent(context, LocationForegroundService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
