package com.taxisign.pro.conductor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.taxisign.pro.ChatConductorActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.taxisign.pro.R
import com.taxisign.pro.Seguridad.LocationForegroundService
import com.taxisign.pro.Seguridad.PermisoUbicacionFondoManager

class IrHaciaPasajero : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var vibrator: Vibrator

    private var primeraCarga = true

    private var viajeId: String = ""

    private var etaNotificada = false
    private var casiLlegoNotificado = false
    private var nombrePasajero: String = ""
    private var telefonoPasajero: String = ""
    private var esSordoMudo: Boolean = false
    private var latRecojo: Double = 0.0
    private var lngRecojo: Double = 0.0

    private val mDatabase = FirebaseDatabase.getInstance().reference

    private var viajeEstadoListener: ValueEventListener? = null
    private var ubicacionPasajeroListener: ValueEventListener? = null
    private var marcadorPasajero: Marker? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
    }

    /**
     * Aviso destacado + consentimiento + permiso de ubicacion en segundo plano.
     * El pasajero necesita ver tu posicion aunque bloquees la pantalla.
     */
    private val permisoUbicacionFondo = PermisoUbicacionFondoManager(this) { concedido ->
        if (concedido) {
            iniciarServicioUbicacionFondo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ir_traspasajero)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"
        telefonoPasajero = intent.getStringExtra("TELEFONO_PASAJERO") ?: ""
        esSordoMudo = intent.getBooleanExtra("ES_SORDO_MUDO", false)
        latRecojo = intent.getDoubleExtra("LAT_RECOJO", 0.0)
        lngRecojo = intent.getDoubleExtra("LNG_RECOJO", 0.0)

        val direccionRecojo = intent.getStringExtra("DIRECCION_RECOJO") ?: "Punto de recojo"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_ir_pasajero) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<TextView>(R.id.tv_nombre_pasajero_ir).text = nombrePasajero
        findViewById<TextView>(R.id.tv_avatar_pasajero_ir).text =
            nombrePasajero.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        findViewById<TextView>(R.id.tv_direccion_recojo).text = direccionRecojo

        if (esSordoMudo) {
            findViewById<TextView>(R.id.badge_sordo_ir).visibility =
                android.view.View.VISIBLE
        }

        findViewById<FloatingActionButton>(R.id.btn_centrar_mapa_ir).setOnClickListener {
            vibrarBreve(30)
            centrarEnMiUbicacion()
        }

        findViewById<LinearLayout>(R.id.btn_chat_ir_pasajero).setOnClickListener {
            vibrarBreve(40)

            if (viajeId.isEmpty()) {
                Toast.makeText(this, "Error: chat no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ChatConductorActivity::class.java).apply {
                putExtra("CHAT_ID", viajeId)
                putExtra("NOMBRE_PASAJERO", nombrePasajero)
            }
            startActivity(intent)


        }

        findViewById<LinearLayout>(R.id.btn_llamar_pasajero).setOnClickListener {
            vibrarBreve(40)
            llamarPasajero()
        }

        findViewById<Button>(R.id.btn_llegue_recojo).setOnClickListener {
            vibrarBreve(60)

            if (viajeId.isNotEmpty()) {
                mDatabase.child("viajes")
                    .child(viajeId)
                    .child("estado")
                    .setValue("conductor_llego")

                // Aviso al PASAJERO: llegó + ponte el cinturón
                com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                    viajeId = viajeId,
                    aPasajero = true,
                    tipo = "LLEGADA",
                    titulo = "Tu conductor llegó 🚕",
                    mensaje = "Sube con calma y ponte el cinturón 🔒. ¡Buen viaje!"
                )

                // Aviso amigable al CONDUCTOR
                com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                    viajeId = viajeId,
                    aPasajero = false,
                    tipo = "VIAJE",
                    titulo = "Recoge a $nombrePasajero 🙂",
                    mensaje = "Recuerda saludar y confirmar el destino con calma."
                )
            }

            val intent = Intent(this, QrSeguridadConductorActivity::class.java).apply {
                putExtra("VIAJE_ID", viajeId)
                putExtra("NOMBRE_PASAJERO", nombrePasajero)
            }

            startActivity(intent)
            finish()
        }

        escucharCancelacionPasajero()

        // El pasajero te sigue en el mapa: pedimos el permiso en segundo plano
        // (con aviso destacado previo) para que no se corte el rastreo.
        permisoUbicacionFondo.iniciar()
    }

    /** Inicia el servicio en primer plano que comparte la ubicacion del viaje. */
    private fun iniciarServicioUbicacionFondo() {
        if (viajeId.isEmpty()) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Deja de aparecer como "disponible" mientras atiende este viaje.
        if (uid.isNotEmpty()) {
            mDatabase.child("conductores_disponibles").child(uid).removeValue()
        }

        LocationForegroundService.iniciar(
            this,
            "viajes/$viajeId/ubicacionConductor",
            uid,
            LocationForegroundService.MODO_VIAJE
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
            )
        } catch (_: Exception) {}

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false

        if (latRecojo != 0.0 && lngRecojo != 0.0) {
            val puntoRecojo = LatLng(latRecojo, lngRecojo)

            marcadorPasajero = mMap.addMarker(
                MarkerOptions()
                    .position(puntoRecojo)
                    .title(nombrePasajero)
                    .snippet("Punto de recojo")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
        }

        escucharUbicacionPasajero()
        verificarPermisosYIniciar()
    }

    private fun escucharUbicacionPasajero() {
        if (viajeId.isEmpty()) return

        ubicacionPasajeroListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (lat == null || lng == null) return

                val posicionPasajero = LatLng(lat, lng)

                if (marcadorPasajero == null) {
                    marcadorPasajero = mMap.addMarker(
                        MarkerOptions()
                            .position(posicionPasajero)
                            .title(nombrePasajero)
                            .snippet("Pasajero en tiempo real")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )
                } else {
                    marcadorPasajero?.position = posicionPasajero
                }

                latRecojo = lat
                lngRecojo = lng
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        mDatabase.child("viajes")
            .child(viajeId)
            .child("ubicacionPasajero")
            .addValueEventListener(ubicacionPasajeroListener!!)
    }

    private fun verificarPermisosYIniciar() {
        if (tienePermiso()) {
            activarCapaUbicacion()
            iniciarActualizaciones()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun tienePermiso(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun activarCapaUbicacion() {
        if (!tienePermiso()) return

        try {
            mMap.isMyLocationEnabled = true
        } catch (_: SecurityException) {}
    }

    private fun iniciarActualizaciones() {
        if (!tienePermiso()) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(1500L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val posicion = LatLng(location.latitude, location.longitude)

                if (primeraCarga) {
                    if (latRecojo != 0.0 && lngRecojo != 0.0) {
                        val bounds = LatLngBounds.builder()
                            .include(posicion)
                            .include(LatLng(latRecojo, lngRecojo))
                            .build()

                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, 180)
                        )
                    } else {
                        val posInicial = CameraPosition.Builder()
                            .target(posicion)
                            .zoom(18.5f)
                            .tilt(45f)
                            .bearing(location.bearing)
                            .build()

                        mMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(posInicial)
                        )
                    }

                    primeraCarga = false
                } else {
                    val posicionNavegacion3D = CameraPosition.Builder()
                        .target(posicion)
                        .zoom(18.5f)
                        .tilt(45f)
                        .bearing(location.bearing)
                        .build()

                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(posicionNavegacion3D)
                    )
                }

                subirUbicacionConductor(location)
                actualizarEtaYDistancia(posicion)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}
    }

    private fun subirUbicacionConductor(location: android.location.Location) {
        if (viajeId.isEmpty()) return

        val ubicacion = mapOf(
            "latitud" to location.latitude,
            "longitud" to location.longitude,
            "bearing" to location.bearing,
            "timestamp" to System.currentTimeMillis()
        )

        mDatabase.child("viajes")
            .child(viajeId)
            .child("ubicacionConductor")
            .setValue(ubicacion)
    }

    private fun escucharCancelacionPasajero() {
        if (viajeId.isEmpty()) return

        viajeEstadoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.value?.toString() ?: ""

                if (
                    estado == "cancelado_pasajero" ||
                    estado == "cancelada"
                ) {
                    vibrarBreve(500)
                    mostrarDialogoConfirmarPagoCancelacion()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        mDatabase.child("viajes")
            .child(viajeId)
            .child("estado")
            .addValueEventListener(viajeEstadoListener!!)
    }

    /**
     * Cuando el pasajero cancela el viaje, le preguntamos al conductor si
     * en realidad llegó a recogerlo / cobrar el viaje antes de la
     * cancelación. Esto evita que un pasajero se suba, pague en efectivo
     * y luego cancele la app para "borrar" el viaje sin dejar registro,
     * o que cancele sin haber pagado y el conductor se quede sin saber
     * qué hacer. La respuesta queda guardada para soporte/auditoría.
     */
    private fun mostrarDialogoConfirmarPagoCancelacion() {
        if (isFinishing || isDestroyed) return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("El pasajero canceló el viaje")
            .setMessage("$nombrePasajero canceló este servicio.\n\n¿Llegaste a recogerlo y/o ya te pagó el viaje?")
            .setPositiveButton("SÍ, YA ME PAGÓ") { _, _ ->
                registrarReporteCancelacion(pasajeroPago = true)
            }
            .setNegativeButton("NO, NO ME PAGÓ") { _, _ ->
                registrarReporteCancelacion(pasajeroPago = false)
            }
            .show()
    }

    private fun registrarReporteCancelacion(pasajeroPago: Boolean) {
        val nombreConductor = com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Conductor" }

        val reporte = hashMapOf<String, Any>(
            "viajeId" to viajeId,
            "conductorNombre" to nombreConductor,
            "pasajeroId" to (telefonoPasajero.takeIf { it.isNotEmpty() } ?: ""),
            "pasajeroNombre" to nombrePasajero,
            "pasajeroConfirmaPago" to pasajeroPago,
            "estadoViajeAlCancelar" to "cancelado_pasajero",
            "timestamp" to System.currentTimeMillis()
        )

        if (viajeId.isNotEmpty()) {
            mDatabase.child("viajes").child(viajeId)
                .child("reporteCancelacion").setValue(reporte)
        }

        if (pasajeroPago) {
            Toast.makeText(
                this,
                "Gracias por confirmar. El cobro quedó registrado.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Si el pasajero NO pagó, se levanta una incidencia para que
            // soporte pueda revisar el caso (posible viaje sin cobrar).
            crearIncidenciaSoporte()
            Toast.makeText(
                this,
                "Reportado a soporte. Revisaremos el caso del pasajero.",
                Toast.LENGTH_LONG
            ).show()
        }

        irAlPanelTrasCancelacion()
    }

    private fun crearIncidenciaSoporte() {
        val conductorUid = com.taxisign.pro.SessionManager.idUsuario.toString()
        val nombreConductor = com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Conductor" }

        val incidencia = hashMapOf<String, Any>(
            "tipo" to "CANCELACION_SIN_PAGO",
            "viajeId" to viajeId,
            "conductorUid" to conductorUid,
            "conductorNombre" to nombreConductor,
            "pasajeroNombre" to nombrePasajero,
            "estado" to "pendiente_revision",
            "timestamp" to System.currentTimeMillis()
        )

        mDatabase.child("incidencias").push().setValue(incidencia)
    }

    private fun irAlPanelTrasCancelacion() {
        val intent = Intent(
            this@IrHaciaPasajero,
            PanelConductorActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancelar viaje")
            .setMessage("¿Seguro que quieres cancelar este viaje? Se le avisará al pasajero.")
            .setPositiveButton("Sí, cancelar") { _, _ -> cancelarViajeConductor() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelarViajeConductor() {
        if (viajeId.isNotEmpty()) {
            mDatabase.child("viajes").child(viajeId).child("estado")
                .setValue("cancelado_conductor")
            // Notificación al pasajero
            com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                viajeId = viajeId,
                aPasajero = true,
                tipo = "VIAJE",
                titulo = "Viaje cancelado",
                mensaje = "El conductor canceló el viaje. Puedes solicitar otro taxi."
            )
        }
        Toast.makeText(this, "Viaje cancelado", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, PanelConductorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun actualizarEtaYDistancia(posicionActual: LatLng) {
        if (latRecojo == 0.0 && lngRecojo == 0.0) return

        val resultados = FloatArray(1)

        android.location.Location.distanceBetween(
            posicionActual.latitude,
            posicionActual.longitude,
            latRecojo,
            lngRecojo,
            resultados
        )

        val distanciaMetros = resultados[0]
        val distanciaKm = distanciaMetros / 1000f
        val tiempoMinutos = ((distanciaKm / 22f) * 60f).toInt().coerceAtLeast(1)

        findViewById<TextView>(R.id.tv_distancia_recojo).text =
            String.format("%.1f km", distanciaKm)

        findViewById<TextView>(R.id.tv_eta_minutos).text =
            "$tiempoMinutos min"

        // Guardar ETA en tiempo real (se ve en otras pantallas)
        if (viajeId.isNotEmpty()) {
            val updates = hashMapOf<String, Any>(
                "viajes/$viajeId/etaMin" to tiempoMinutos,
                "viajes/$viajeId/etaDistanciaKm" to String.format("%.1f", distanciaKm)
            )
            mDatabase.updateChildren(updates)
        }

        // Notificación al pasajero con el tiempo de llegada
        if (!etaNotificada) {
            etaNotificada = true
            com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                viajeId = viajeId,
                aPasajero = true,
                tipo = "LLEGADA",
                titulo = "Tu conductor está en camino 🚕",
                mensaje = "Llegará en aproximadamente $tiempoMinutos min."
            )
        } else if (tiempoMinutos <= 2 && !casiLlegoNotificado) {
            casiLlegoNotificado = true
            com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                viajeId = viajeId,
                aPasajero = true,
                tipo = "LLEGADA",
                titulo = "¡Tu conductor está por llegar! 🚕",
                mensaje = "Está a menos de 2 minutos. Prepárate 😊"
            )
        }
    }

    private fun centrarEnMiUbicacion() {
        if (!tienePermiso()) {
            verificarPermisosYIniciar()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val posicionCentrada = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(18.5f)
                        .tilt(45f)
                        .bearing(location.bearing)
                        .build()

                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(posicionCentrada)
                    )
                } else {
                    Toast.makeText(
                        this,
                        "GPS buscando señal satelital...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (_: SecurityException) {}
    }

    private fun llamarPasajero() {
        if (telefonoPasajero.isNotEmpty()) {
            marcar(telefonoPasajero)
            return
        }
        // Si no llegó el teléfono por extra, leerlo de Firebase
        if (viajeId.isNotEmpty()) {
            mDatabase.child("viajes").child(viajeId).child("pasajeroId").get()
                .addOnSuccessListener { ps ->
                    val pid = ps.value?.toString() ?: ""
                    if (pid.isEmpty()) { avisoSinTelefono(); return@addOnSuccessListener }
                    mDatabase.child("usuarios").child("pasajeros").child(pid).child("telefono").get()
                        .addOnSuccessListener { t ->
                            val tel = t.value?.toString() ?: ""
                            if (tel.isEmpty()) avisoSinTelefono()
                            else { telefonoPasajero = tel; marcar(tel) }
                        }
                }
        } else avisoSinTelefono()
    }

    private fun avisoSinTelefono() {
        Toast.makeText(this, "Número de teléfono no disponible", Toast.LENGTH_SHORT).show()
    }

    private fun marcar(tel: String) {
        startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$tel") })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            activarCapaUbicacion()
            iniciarActualizaciones()
        } else {
            Toast.makeText(
                this,
                "Permiso de ubicación denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun vibrarBreve(milisegundos: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    milisegundos,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milisegundos)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::mMap.isInitialized && tienePermiso()) {
            iniciarActualizaciones()
        }
    }

    override fun onPause() {
        super.onPause()

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        viajeEstadoListener?.let {
            if (viajeId.isNotEmpty()) {
                mDatabase.child("viajes")
                    .child(viajeId)
                    .child("estado")
                    .removeEventListener(it)
            }
        }

        ubicacionPasajeroListener?.let {
            if (viajeId.isNotEmpty()) {
                mDatabase.child("viajes")
                    .child(viajeId)
                    .child("ubicacionPasajero")
                    .removeEventListener(it)
            }
        }
    }
}