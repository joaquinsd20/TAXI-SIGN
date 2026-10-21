package com.taxisign.pro.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.taxisign.pro.R
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

/**
 * NotificacionHelper
 *
 * Centro de notificaciones de TaxiSign. Hace 3 cosas:
 *  1. ENVIAR un aviso → lo guarda en Firebase: notificaciones/{uidDestino}
 *  2. ESCUCHAR mi propio nodo → cuando llega un aviso nuevo muestra una
 *     notificación del sistema + vibración + FLASH de cámara (para sordos).
 *  3. Crear el canal de notificaciones (Android 8+).
 *
 * No usa FCM ni servidor: cada dispositivo escucha su propio nodo en Firebase.
 */
object NotificacionHelper {

    const val CHANNEL_ID = "taxisign_canal"
    private const val CHANNEL_NOMBRE = "TaxiSign"

    private val db get() = FirebaseDatabase.getInstance().reference

    // ───────────────────────── ENVIAR ─────────────────────────
    fun enviar(
        uidDestino: String,
        rol: String,
        tipo: String,
        titulo: String,
        mensaje: String
    ) {
        if (uidDestino.isEmpty()) return
        val ref = db.child("notificaciones").child(uidDestino).push()
        val noti = Notificacion(
            id = ref.key ?: "",
            tipo = tipo,
            titulo = titulo,
            mensaje = mensaje,
            timestamp = System.currentTimeMillis(),
            leida = false,
            paraUid = uidDestino,
            rol = rol
        )
        ref.setValue(noti)
    }

    // ───────────────────── ENVIAR SEGÚN VIAJE ─────────────────────
    /**
     * Envía un aviso al pasajero o al conductor de un viaje, leyendo su uid
     * directamente del nodo viajes/{viajeId}. Útil cuando el emisor no tiene
     * a la mano el uid del destinatario.
     */
    fun enviarAViaje(
        viajeId: String,
        aPasajero: Boolean,
        tipo: String,
        titulo: String,
        mensaje: String
    ) {
        if (viajeId.isEmpty()) return
        db.child("viajes").child(viajeId).get().addOnSuccessListener { s ->
            val uid = if (aPasajero)
                s.child("pasajeroId").value?.toString()
                    ?: s.child("idPasajero").value?.toString()
            else
                s.child("conductorId").value?.toString()
                    ?: s.child("idConductor").value?.toString()
            val rol = if (aPasajero) "PASAJERO" else "CONDUCTOR"
            if (!uid.isNullOrEmpty()) enviar(uid, rol, tipo, titulo, mensaje)
        }
    }

    // ───────────────────── GANANCIAS ─────────────────────
    /**
     * Registra la ganancia de un viaje finalizado en:
     *   usuarios/conductores/{conductorId}/ganancias/{viajeId}
     * Así el conductor puede ver el detalle de todo lo que ganó.
     */
    fun registrarGanancia(
        conductorId: String,
        viajeId: String,
        monto: Double,
        pasajero: String,
        destino: String
    ) {
        if (conductorId.isEmpty() || viajeId.isEmpty()) return
        val ganancia = hashMapOf<String, Any>(
            "viajeId" to viajeId,
            "monto" to monto,
            "pasajero" to pasajero,
            "destino" to destino,
            "timestamp" to System.currentTimeMillis()
        )
        db.child("usuarios").child("conductores").child(conductorId)
            .child("ganancias").child(viajeId)
            .setValue(ganancia)
    }

    // ───────────────────── CANAL ─────────────────────────
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, CHANNEL_NOMBRE, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de viajes y seguridad"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(canal)
        }
    }

    // ───────────────────── ESCUCHAR MI NODO ─────────────────────
    /**
     * Escucha notificaciones nuevas dirigidas a [miUid]. Solo muestra avisos
     * del sistema para los que llegan DESPUÉS de empezar a escuchar
     * (evita repetir el historial al abrir la app).
     *
     * @param flash si true, parpadea la linterna además de vibrar.
     * @return el listener para poder quitarlo en onDestroy().
     */
    fun escuchar(
        context: Context,
        miUid: String,
        flash: Boolean = true,
        onNueva: ((Notificacion) -> Unit)? = null
    ): ChildEventListener? {
        if (miUid.isEmpty()) return null
        crearCanal(context)
        val desde = System.currentTimeMillis()

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                val noti = snapshot.getValue(Notificacion::class.java) ?: return
                onNueva?.invoke(noti)
                if (noti.timestamp >= desde && !noti.leida) {
                    mostrarSistema(context, noti)
                    vibrar(context)
                    if (flash) flashCamara(context)
                }
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("notificaciones").child(miUid).addChildEventListener(listener)
        return listener
    }

    fun dejarDeEscuchar(miUid: String, listener: ChildEventListener?) {
        if (miUid.isEmpty() || listener == null) return
        db.child("notificaciones").child(miUid).removeEventListener(listener)
    }

    // ───────────────────── NOTIFICACIÓN SISTEMA ─────────────────────
    private fun mostrarSistema(context: Context, noti: Notificacion) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(noti.titulo)
            .setContentText(noti.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(noti.mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))

        val nm = context.getSystemService(NotificationManager::class.java)
        // En Android 13+ se necesita permiso POST_NOTIFICATIONS (lo pide el Panel).
        try {
            nm.notify(noti.timestamp.toInt(), builder.build())
        } catch (_: SecurityException) { }
    }

    // ───────────────────── FLASH DE CÁMARA ─────────────────────
    /** Parpadea la linterna 3 veces (ayuda visual para personas sordas). */
    fun flashCamara(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val idCam = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return

            val handler = Handler(Looper.getMainLooper())
            var encendido = false
            val veces = 6 // 3 destellos (on/off)
            for (i in 0 until veces) {
                handler.postDelayed({
                    encendido = !encendido
                    try { cm.setTorchMode(idCam, encendido) } catch (_: Exception) {}
                }, i * 250L)
            }
            // Asegurar apagado final
            handler.postDelayed({
                try { cm.setTorchMode(idCam, false) } catch (_: Exception) {}
            }, veces * 250L + 100)
        } catch (_: Exception) { }
    }

    // ───────────────────── VIBRACIÓN ─────────────────────
    @Suppress("DEPRECATION")
    private fun vibrar(context: Context) {
        val v = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        } else {
            v.vibrate(longArrayOf(0, 200, 100, 200), -1)
        }
    }

    // ───────────────────── MARCAR LEÍDAS ─────────────────────
    fun marcarTodasLeidas(miUid: String) {
        if (miUid.isEmpty()) return
        db.child("notificaciones").child(miUid).get().addOnSuccessListener { snap ->
            snap.children.forEach { it.ref.child("leida").setValue(true) }
        }
    }
}
