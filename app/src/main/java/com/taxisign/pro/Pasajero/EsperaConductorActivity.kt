package com.taxisign.pro.pasajero

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.taxisign.pro.ChatPasajeroActivity
import com.taxisign.pro.SessionManager
import com.taxisign.pro.notificaciones.NotificacionHelper
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.taxisign.pro.R

class EsperaConductorActivity : AppCompatActivity(), OnMapReadyCallback {

    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var marcadorConductor: Marker? = null
    private var ubicacionConductorListener: ValueEventListener? = null
    private var estadoListener: ValueEventListener? = null

    private var viajeId = ""
    private var chatId = ""
    private var idSolicitud = ""
    private var idConductor = ""
    private var nombreConductor = ""
    private var datosVehiculo = ""
    private var direccionDestino = ""
    private var telefonoConductor = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espera_conductor)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        chatId = intent.getStringExtra("CHAT_ID") ?: viajeId
        idSolicitud = intent.getStringExtra("ID_SOLICITUD") ?: ""
        idConductor = intent.getStringExtra("ID_CONDUCTOR") ?: ""
        nombreConductor = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"
        datosVehiculo = intent.getStringExtra("DATOS_VEHICULO") ?: "Vehículo no registrado"
        direccionDestino = intent.getStringExtra("DIRECCION_DESTINO") ?: "Destino no especificado"
        telefonoConductor = intent.getStringExtra("TELEFONO_CONDUCTOR") ?: ""

        findViewById<TextView>(R.id.txt_nombre).text = nombreConductor
        findViewById<TextView>(R.id.txt_auto).text = datosVehiculo
        findViewById<TextView>(R.id.txt_destino).text = direccionDestino

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.btn_mensaje).setOnClickListener {
            abrirChat()
        }

        findViewById<LinearLayout>(R.id.btn_llamar).setOnClickListener {
            llamarConductor()
        }

        cargarTelefonoConductor()

        findViewById<LinearLayout>(R.id.btn_cancelar).setOnClickListener {
            confirmarCancelacion()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_espera_conductor) as SupportMapFragment
        mapFragment.getMapAsync(this)

        escucharEstadoViaje()
    }


    private fun cargarTelefonoConductor() {
        if (telefonoConductor.isNotBlank()) return

        if (viajeId.isNotBlank()) {
            dbRoot.child("viajes").child(viajeId).get()
                .addOnSuccessListener { snap ->
                    telefonoConductor = snap.child("telefonoConductor").value?.toString()
                        ?: snap.child("telefono").value?.toString()
                                ?: telefonoConductor
                }
        }

        if (idConductor.isNotBlank()) {
            dbRoot.child("usuarios").child("conductores").child(idConductor).get()
                .addOnSuccessListener { snap ->
                    val telefono = snap.child("telefono").value?.toString()
                        ?: snap.child("celular").value?.toString()
                        ?: ""
                    if (telefono.isNotBlank()) telefonoConductor = telefono
                }
        }
    }

    private fun llamarConductor() {
        val telefono = telefonoConductor.trim()

        if (telefono.isBlank()) {
            Toast.makeText(
                this,
                "El conductor no tiene un teléfono registrado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$telefono")
        }
        startActivity(intent)
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

        verificarPermisosYSubirUbicacionPasajero()
        escucharUbicacionConductor()
        escucharEta()
    }

    private var etaListener: com.google.firebase.database.ValueEventListener? = null

    private fun escucharEta() {
        if (viajeId.isEmpty()) return
        etaListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val eta = snapshot.child("etaMin").getValue(Int::class.java)
                val km = snapshot.child("etaDistanciaKm").value?.toString()
                if (eta != null) {
                    val txtKm = if (!km.isNullOrEmpty()) " • $km km" else ""
                    findViewById<TextView>(R.id.txt_tiempo).text =
                        "Conductor a $eta min$txtKm"
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        dbRoot.child("viajes").child(viajeId).addValueEventListener(etaListener!!)
    }

    private fun verificarPermisosYSubirUbicacionPasajero() {
        if (tienePermiso()) {
            iniciarActualizacionUbicacionPasajero()
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

    private fun iniciarActualizacionUbicacionPasajero() {
        if (!tienePermiso() || viajeId.isEmpty()) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(1500L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                subirUbicacionPasajero(location)
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

    private fun subirUbicacionPasajero(location: android.location.Location) {
        if (viajeId.isEmpty()) return

        val ubicacion = mapOf(
            "latitud" to location.latitude,
            "longitud" to location.longitude,
            "bearing" to location.bearing,
            "timestamp" to System.currentTimeMillis()
        )

        dbRoot.child("viajes")
            .child(viajeId)
            .child("ubicacionPasajero")
            .setValue(ubicacion)
    }

    private fun escucharUbicacionConductor() {
        if (viajeId.isEmpty()) return

        ubicacionConductorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (lat == null || lng == null) return

                val posicionConductor = LatLng(lat, lng)

                if (marcadorConductor == null) {
                    marcadorConductor = mMap.addMarker(
                        MarkerOptions()
                            .position(posicionConductor)
                            .title(nombreConductor)
                            .snippet(datosVehiculo)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(posicionConductor, 17f)
                    )
                } else {
                    marcadorConductor?.position = posicionConductor
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(posicionConductor, 17f)
                    )
                }

                findViewById<TextView>(R.id.txt_tiempo).text =
                    "Ubicación del conductor actualizada"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@EsperaConductorActivity,
                    "Error leyendo ubicación del conductor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dbRoot.child("viajes")
            .child(viajeId)
            .child("ubicacionConductor")
            .addValueEventListener(ubicacionConductorListener!!)
    }

    private fun escucharEstadoViaje() {
        if (viajeId.isEmpty()) return

        estadoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.value?.toString() ?: ""

                when (estado) {
                    "conductor_llego" -> {
                        // ── Alerta accesible para pasajeros sordos ──────────────
                        // Combinamos flash de cámara + vibración intensa + diálogo
                        // visual prominente, así el pasajero se entera aunque no
                        // escuche el sonido del sistema.
                        alertarConductorLlego()
                        // ───────────────────────────────────────────────────────

                        val intent = Intent(
                            this@EsperaConductorActivity,
                            QrSeguridadPasajeroActivity::class.java
                        ).apply {
                            putExtra("VIAJE_ID", viajeId)
                            putExtra("CHAT_ID", chatId)
                            putExtra("ID_SOLICITUD", idSolicitud)
                            putExtra("ID_CONDUCTOR", idConductor)
                            putExtra("NOMBRE_CONDUCTOR", nombreConductor)
                            putExtra("DATOS_VEHICULO", datosVehiculo)
                            putExtra("DIRECCION_DESTINO", direccionDestino)
                        }

                        startActivity(intent)
                        finish()
                    }

                    "cancelado_conductor" -> {
                        Toast.makeText(
                            this@EsperaConductorActivity,
                            "El conductor canceló el viaje",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    "cancelado_pasajero" -> {
                        finish()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        dbRoot.child("viajes")
            .child(viajeId)
            .child("estado")
            .addValueEventListener(estadoListener!!)
    }

    /**
     * Alerta multimodal para cuando el conductor llega al punto de recojo.
     * Combina flash de cámara + vibración larga + diálogo visual prominente
     * para que el pasajero sordo/sordomudo se entere aunque no escuche el sonido.
     *
     * Solo activa flash si el pasajero tiene discapacidad auditiva; la vibración
     * siempre se activa pues es útil universalmente.
     */
    private fun alertarConductorLlego() {
        val tipoDiscapacidad = SessionManager.tipoDiscapacidadUsuario

        val esSordo = tipoDiscapacidad.contains("sordo", ignoreCase = true) ||
                tipoDiscapacidad.contains("mudo", ignoreCase = true) ||
                SessionManager.esSordoMudo

        // Banner visual prominente (útil para todos; esencial para sordos)
        try {
            val banner = findViewById<LinearLayout>(R.id.bannerConductorLlego)
            banner?.visibility = android.view.View.VISIBLE
        } catch (_: Exception) {}

        // Vibración larga e intensa (patrón: 3 pulsos largos)
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 500, 200, 500, 200, 500), -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                }
            }
        } catch (_: Exception) {}

        // Flash de cámara: solo si el pasajero es sordo/mudo
        if (esSordo) {
            NotificacionHelper.flashCamara(this)
        }
    }

    private fun abrirChat() {
        val intent = Intent(this, ChatPasajeroActivity::class.java).apply {
            putExtra("VIAJE_ID", viajeId)
            putExtra("CHAT_ID", chatId)
            putExtra("ID_SOLICITUD", idSolicitud)
            putExtra("ID_CONDUCTOR", idConductor)
            putExtra("NOMBRE_CONDUCTOR", nombreConductor)
            putExtra("DATOS_VEHICULO", datosVehiculo)
            putExtra("DIRECCION_DESTINO", direccionDestino)
        }

        startActivity(intent)
    }

    private fun confirmarCancelacion() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar viaje")
            .setMessage("¿Seguro que deseas cancelar este viaje?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                cancelarViaje()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelarViaje() {
        if (idSolicitud.isEmpty() || viajeId.isEmpty()) {
            finish()
            return
        }

        val updates = hashMapOf<String, Any>(
            "solicitudes/$idSolicitud/estado" to "cancelada",
            "viajes/$viajeId/estado" to "cancelado_pasajero",
            "chats/$chatId/activo" to false
        )

        dbRoot.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Viaje cancelado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cancelar", Toast.LENGTH_SHORT).show()
            }
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
            iniciarActualizacionUbicacionPasajero()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        ubicacionConductorListener?.let {
            if (viajeId.isNotEmpty()) {
                dbRoot.child("viajes")
                    .child(viajeId)
                    .child("ubicacionConductor")
                    .removeEventListener(it)
            }
        }

        estadoListener?.let {
            if (viajeId.isNotEmpty()) {
                dbRoot.child("viajes")
                    .child(viajeId)
                    .child("estado")
                    .removeEventListener(it)
            }
        }

        etaListener?.let {
            if (viajeId.isNotEmpty()) {
                dbRoot.child("viajes").child(viajeId).removeEventListener(it)
            }
        }
    }
}