package com.taxisign.pro.conductor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.taxisign.pro.ChatConductorActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.taxisign.pro.R
import com.taxisign.pro.Seguridad.LocationForegroundService
import com.taxisign.pro.Seguridad.PermisoUbicacionFondoManager


class DuranteViajeConductor : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var vibrator: Vibrator
    private lateinit var mMap: GoogleMap

    private var viajeId = ""
    private var pasajeroId = ""
    private var nombrePasajero = "Pasajero"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var latDestino = 0.0
    private var lngDestino = 0.0
    private var direccionDestino = "Destino"
    private var primeraUbicacionDetectada = false

    private val mDatabase = FirebaseDatabase.getInstance().reference
    private var viajeListener: ValueEventListener? = null

    private var tvInstruccionNavegacion: TextView? = null
    private var tvDistanciaRestante: TextView? = null
    private var tvTiempoEstimado: TextView? = null

    private var tvNombrePasajeroView: TextView? = null
    private var tvNombreConductorView: TextView? = null
    private var nombreConductor = "Conductor"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2002
    }

    /**
     * Aviso destacado + consentimiento + permiso de ubicacion en segundo plano.
     * Mantiene tu posicion visible para el pasajero durante todo el viaje.
     */
    private val permisoUbicacionFondo = PermisoUbicacionFondoManager(this) { concedido ->
        if (concedido) {
            iniciarServicioUbicacionFondo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.duranteviaje_conductor)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        pasajeroId = intent.getStringExtra("PASAJERO_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        tvInstruccionNavegacion = findViewById(
            resources.getIdentifier("tvInstruccionNavegacion", "id", packageName)
                .takeIf { it != 0 } ?: -1
        )

        tvDistanciaRestante = findViewById(
            resources.getIdentifier("tvDistanciaRestante", "id", packageName)
                .takeIf { it != 0 } ?: -1
        )

        tvTiempoEstimado = findViewById(
            resources.getIdentifier("tvTiempoEstimado", "id", packageName)
                .takeIf { it != 0 } ?: -1
        )

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarViaje)

        findViewById<Button>(R.id.btnNavegarDestino).setOnClickListener {
            abrirNavegacionGoogleMaps()
        }
        val btnSos = findViewById<CardView>(R.id.btnSos)
        val btnChat = findViewById<ImageButton>(R.id.btnMensajePasajero)

        findViewById<ImageButton>(R.id.btnLlamarPasajero).setOnClickListener {
            llamarPasajero()
        }

        tvNombrePasajeroView = findViewById(R.id.tvNombrePasajero)
        tvNombreConductorView = findViewById(R.id.tvNombreConductorViaje)
        nombreConductor = com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Conductor" }
        actualizarNombres()

        btnSos.setOnClickListener {
            vibrarLargo()
            Toast.makeText(this, "¡ALERTA SOS ACTIVADA!", Toast.LENGTH_LONG).show()
        }

        btnChat.setOnClickListener {
            vibrarBreve()

            if (viajeId.isEmpty()) {
                Toast.makeText(this, "Error: viaje no iniciado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ChatConductorActivity::class.java).apply {
                putExtra("CHAT_ID", viajeId)
                putExtra("NOMBRE_PASAJERO", nombrePasajero)
            }

            startActivity(intent)
        }

        btnFinalizar.setOnClickListener {
            vibrarBreve()
            mostrarDialogoFinalizar()
        }

        configurarRequestGps()
        conectarConViajeFirebase()

        // El pasajero te sigue en el mapa durante todo el viaje.
        permisoUbicacionFondo.iniciar()

        // Mensaje amigable de inicio de viaje para el pasajero
        if (viajeId.isNotEmpty()) {
            com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                viajeId = viajeId,
                aPasajero = true,
                tipo = "VIAJE",
                titulo = "En camino a tu destino 🛣️",
                mensaje = "Relájate y disfruta el viaje. Cualquier cosa, usa el chat 💬"
            )
        }
    }

    /** Inicia el servicio en primer plano que comparte la ubicacion del viaje. */
    private fun iniciarServicioUbicacionFondo() {
        if (viajeId.isEmpty()) return

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        LocationForegroundService.iniciar(
            this,
            "viajes/$viajeId/ubicacionConductor",
            uid,
            LocationForegroundService.MODO_VIAJE
        )
    }

    private fun registrarGananciaYNotificar() {
        if (viajeId.isEmpty()) return
        mDatabase.child("viajes").child(viajeId).get().addOnSuccessListener { s ->
            val conductorId = s.child("conductorId").value?.toString()
                ?: s.child("idConductor").value?.toString() ?: ""
            val monto = s.child("tarifa").getValue(Double::class.java)
                ?: s.child("precio").getValue(Double::class.java) ?: 0.0
            val destino = s.child("destino").value?.toString()
                ?: s.child("direccionDestino").value?.toString() ?: "Destino"
            val pasajero = s.child("nombrePasajero").value?.toString() ?: nombrePasajero

            // 1) Guardar la ganancia del conductor
            com.taxisign.pro.notificaciones.NotificacionHelper.registrarGanancia(
                conductorId = conductorId,
                viajeId = viajeId,
                monto = monto,
                pasajero = pasajero,
                destino = destino
            )

            // 2) Aviso al PASAJERO
            com.taxisign.pro.notificaciones.NotificacionHelper.enviarAViaje(
                viajeId = viajeId,
                aPasajero = true,
                tipo = "VIAJE",
                titulo = "¡Llegaste a tu destino! ✅",
                mensaje = "Gracias por viajar con TaxiSign. ¡Cuídate! 🙌"
            )

            // 3) Aviso al CONDUCTOR con su ganancia
            com.taxisign.pro.notificaciones.NotificacionHelper.enviar(
                uidDestino = conductorId,
                rol = "CONDUCTOR",
                tipo = "GANANCIA",
                titulo = "Viaje completado ✅",
                mensaje = "Ganaste S/ %.2f en este viaje. ¡Buen trabajo!".format(monto)
            )
        }
    }

    /**
     * El pasajero canceló estando ya en pleno viaje (recogido o en ruta).
     * Le preguntamos al conductor si alcanzó a cobrar el viaje, ya que en
     * este punto es muy probable que el servicio ya se haya realizado.
     * La respuesta queda registrada para soporte/auditoría y, si confirma
     * el pago, se registra la ganancia igual que en un viaje finalizado.
     */
    private fun mostrarDialogoConfirmarPagoCancelacion() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("El pasajero canceló el viaje")
            .setMessage("$nombrePasajero canceló el servicio mientras estaba en curso.\n\n¿Ya te había pagado el viaje?")
            .setPositiveButton("SÍ, YA ME PAGÓ") { _, _ ->
                registrarReporteCancelacion(pasajeroPago = true)
            }
            .setNegativeButton("NO, NO ME PAGÓ") { _, _ ->
                registrarReporteCancelacion(pasajeroPago = false)
            }
            .show()
    }

    private fun registrarReporteCancelacion(pasajeroPago: Boolean) {
        val reporte = hashMapOf<String, Any>(
            "viajeId" to viajeId,
            "conductorNombre" to nombreConductor,
            "pasajeroNombre" to nombrePasajero,
            "pasajeroConfirmaPago" to pasajeroPago,
            "estadoViajeAlCancelar" to "cancelado_pasajero_en_viaje",
            "timestamp" to System.currentTimeMillis()
        )

        if (viajeId.isNotEmpty()) {
            mDatabase.child("viajes").child(viajeId)
                .child("reporteCancelacion").setValue(reporte)
        }

        if (pasajeroPago) {
            // Se considera el viaje como cobrado: registramos la ganancia
            // igual que si hubiera finalizado normalmente.
            registrarGananciaYNotificar()
            Toast.makeText(
                this,
                "Gracias por confirmar. El cobro quedó registrado.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            crearIncidenciaSoporte()
            Toast.makeText(
                this,
                "Reportado a soporte. Revisaremos el caso del pasajero.",
                Toast.LENGTH_LONG
            ).show()
        }

        regresarAlPanel()
    }

    private fun crearIncidenciaSoporte() {
        val conductorUid = com.taxisign.pro.SessionManager.idUsuario.toString()

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

    private fun llamarPasajero() {
        if (pasajeroId.isEmpty()) {
            Toast.makeText(this, "No tengo los datos del pasajero", Toast.LENGTH_SHORT).show()
            return
        }
        mDatabase.child("usuarios").child("pasajeros").child(pasajeroId)
            .child("telefono").get()
            .addOnSuccessListener { snap ->
                val tel = snap.value?.toString() ?: ""
                if (tel.isEmpty()) {
                    Toast.makeText(this, "El pasajero no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:$tel")
                    })
                }
            }
    }

    private fun abrirNavegacionGoogleMaps() {
        if (latDestino == 0.0 && lngDestino == 0.0) {
            Toast.makeText(this, "Aún no tengo las coordenadas del destino", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = android.net.Uri.parse("google.navigation:q=$latDestino,$lngDestino&mode=d")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latDestino,$lngDestino&travelmode=driving")
                )
            )
        }
    }

    private fun actualizarNombres() {
        tvNombrePasajeroView?.text = nombrePasajero
        tvNombreConductorView?.text = "Conductor: $nombreConductor"
    }

    private fun configurarRequestGps() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        ).setMinUpdateIntervalMillis(1500).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    actualizarPosicionConductor(location)
                }
            }
        }
    }

    private fun conectarConViajeFirebase() {
        if (viajeId.isEmpty()) return

        viajeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val estado = snapshot.child("estado").value?.toString() ?: ""

                if (
                    estado == "cancelado" ||
                    estado == "cancelado_pasajero" ||
                    estado == "cancelada"
                ) {
                    vibrarLargo()
                    mostrarDialogoConfirmarPagoCancelacion()
                    return
                }

                if (pasajeroId.isEmpty()) {
                    pasajeroId =
                        snapshot.child("pasajeroId").value?.toString()
                            ?: snapshot.child("idPasajero").value?.toString()
                                    ?: ""
                }

                if (nombrePasajero == "Pasajero") {
                    nombrePasajero =
                        snapshot.child("nombrePasajero").value?.toString()
                            ?: "Pasajero"
                }

                snapshot.child("nombreConductor").value?.toString()?.let {
                    if (it.isNotEmpty()) nombreConductor = it
                }

                actualizarNombres()

                latDestino =
                    snapshot.child("destinoLat").getValue(Double::class.java)
                        ?: snapshot.child("latDestino").getValue(Double::class.java)
                                ?: 0.0

                lngDestino =
                    snapshot.child("destinoLng").getValue(Double::class.java)
                        ?: snapshot.child("lngDestino").getValue(Double::class.java)
                                ?: 0.0

                direccionDestino =
                    snapshot.child("direccionDestino").value?.toString()
                        ?: snapshot.child("destino").value?.toString()
                                ?: "Destino"

                if (::mMap.isInitialized && latDestino != 0.0 && lngDestino != 0.0) {
                    mMap.clear()

                    mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(latDestino, lngDestino))
                            .title("Destino de viaje")
                            .snippet(direccionDestino)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        mDatabase.child("viajes")
            .child(viajeId)
            .addValueEventListener(viajeListener!!)
    }

    private fun actualizarPosicionConductor(location: Location) {
        val coordenadasActuales = LatLng(location.latitude, location.longitude)

        val posicionNavegacion = CameraPosition.Builder()
            .target(coordenadasActuales)
            .zoom(18.5f)
            .tilt(45f)
            .bearing(location.bearing)
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(posicionNavegacion))
        primeraUbicacionDetectada = true

        if (latDestino != 0.0 && lngDestino != 0.0) {
            val resultados = FloatArray(1)

            Location.distanceBetween(
                location.latitude,
                location.longitude,
                latDestino,
                lngDestino,
                resultados
            )

            actualizarNavegacionReal(coordenadasActuales, resultados[0])
        }

        if (viajeId.isNotEmpty()) {
            val mapaCoordenadas = mapOf(
                "latitud" to location.latitude,
                "longitud" to location.longitude,
                "bearing" to location.bearing,
                "timestamp" to System.currentTimeMillis()
            )

            mDatabase.child("viajes")
                .child(viajeId)
                .child("ubicacionConductor")
                .setValue(mapaCoordenadas)
        }
    }

    private var pasosRuta: List<com.taxisign.pro.NavegacionHelper.PasoRuta> = emptyList()
    private var pasoActualIndex = 0
    private var rutaSolicitada = false
    private var rutaPolyline: com.google.android.gms.maps.model.Polyline? = null

    private fun actualizarNavegacionReal(posicion: LatLng, distanciaDestinoMetros: Float) {
        // Cargar la ruta real una sola vez (cuando ya hay posición y destino)
        if (!rutaSolicitada && latDestino != 0.0 && lngDestino != 0.0) {
            rutaSolicitada = true
            com.taxisign.pro.NavegacionHelper.obtenerRuta(
                this, posicion, LatLng(latDestino, lngDestino)
            ) { pasos, puntos ->
                pasosRuta = pasos
                pasoActualIndex = 0
                if (puntos.isNotEmpty() && ::mMap.isInitialized) {
                    rutaPolyline?.remove()
                    rutaPolyline = mMap.addPolyline(
                        com.google.android.gms.maps.model.PolylineOptions()
                            .addAll(puntos)
                            .width(14f)
                            .color(android.graphics.Color.parseColor("#FFD600"))
                    )
                }
            }
        }

        if (pasosRuta.isEmpty()) {
            // Respaldo: instrucciones genéricas si la Directions API no respondió
            actualizarInstruccionesGps(distanciaDestinoMetros)
            return
        }

        // Avanzar de paso cuando el conductor llega al punto de maniobra (~30 m)
        while (pasoActualIndex < pasosRuta.size - 1) {
            val maniobra = pasosRuta[pasoActualIndex].maniobra
            val d = FloatArray(1)
            Location.distanceBetween(posicion.latitude, posicion.longitude, maniobra.latitude, maniobra.longitude, d)
            if (d[0] < 30f) pasoActualIndex++ else break
        }

        val paso = pasosRuta[pasoActualIndex]
        val d = FloatArray(1)
        Location.distanceBetween(posicion.latitude, posicion.longitude, paso.maniobra.latitude, paso.maniobra.longitude, d)
        val metrosAlGiro = d[0].toInt()

        tvInstruccionNavegacion?.text = paso.instruccion
        tvDistanciaRestante?.text = if (distanciaDestinoMetros < 1000)
            "${distanciaDestinoMetros.toInt()} m al destino"
        else String.format("%.1f km al destino", distanciaDestinoMetros / 1000f)
        val tiempo = ((distanciaDestinoMetros / 1000f / 25f) * 60f).toInt().coerceAtLeast(1)
        tvTiempoEstimado?.text = "$tiempo min · giro en ${metrosAlGiro} m"
    }

    private fun actualizarInstruccionesGps(distanciaMetros: Float) {
        val distanciaKm = distanciaMetros / 1000f
        val tiempoMinutos = ((distanciaKm / 25f) * 60f).toInt().coerceAtLeast(1)

        val textoDistancia = if (distanciaMetros < 1000) {
            "${distanciaMetros.toInt()} m"
        } else {
            String.format("%.1f km", distanciaKm)
        }

        tvDistanciaRestante?.text = textoDistancia
        tvTiempoEstimado?.text = "$tiempoMinutos min"

        val instruccionGps = when {
            distanciaMetros > 3000 -> "Continúe recto por la vía principal hacia $direccionDestino"
            distanciaMetros in 1500f..3000f -> "Siga derecho, manténgase en el carril central con dirección a $direccionDestino"
            distanciaMetros in 800f..1500f -> "En 500 metros, prepárese para girar a la derecha"
            distanciaMetros in 400f..800f -> "En 300 metros, gire a la izquierda en la siguiente intersección"
            distanciaMetros in 150f..400f -> "Gire a la derecha y continúe recto hacia su destino"
            distanciaMetros in 50f..150f -> "Prepárese para detenerse, su destino se encuentra a la derecha"
            else -> "¡Ha llegado a su destino! Confirme la llegada con el pasajero."
        }

        tvInstruccionNavegacion?.text = instruccionGps
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
            )
        } catch (_: Exception) {}

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        verificarYActivarGps()
    }

    private fun verificarYActivarGps() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (_: SecurityException) {}

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null && !primeraUbicacionDetectada) {
                    val coordenadas = LatLng(location.latitude, location.longitude)

                    val posicionInicial = CameraPosition.Builder()
                        .target(coordenadas)
                        .zoom(18.5f)
                        .tilt(45f)
                        .bearing(location.bearing)
                        .build()

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(posicionInicial))
                }
            }

            iniciarActualizacionesUbicacion()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun iniciarActualizacionesUbicacion() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun detenerActualizacionesUbicacion() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun mostrarDialogoFinalizar() {
        AlertDialog.Builder(this)
            .setTitle("¿Llegaste al destino?")
            .setMessage("Confirma que el viaje ha terminado para procesar el pago.")
            .setPositiveButton("SÍ, FINALIZAR") { _, _ ->

                if (viajeId.isEmpty()) {
                    Toast.makeText(this, "Error: viaje no encontrado", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                mDatabase.child("viajes")
                    .child(viajeId)
                    .child("estado")
                    .setValue("finalizado")
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Viaje completado con éxito",
                            Toast.LENGTH_SHORT
                        ).show()

                        registrarGananciaYNotificar()

                        // El viaje termino: deja de compartirse la ubicacion.
                        LocationForegroundService.detener(this)

                        abrirViajeFinalizadoConductor()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            "Error al finalizar: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun abrirViajeFinalizadoConductor() {
        val intent = Intent(
            this,
            ViajeFinalizadoConductor::class.java
        ).apply {
            putExtra("VIAJE_ID", viajeId)
            putExtra("PASAJERO_ID", pasajeroId)
            putExtra("NOMBRE_PASAJERO", nombrePasajero)
            putExtra("DISTANCIA", tvDistanciaRestante?.text?.toString() ?: "-- km")
            putExtra("TIEMPO", tvTiempoEstimado?.text?.toString() ?: "-- min")
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun regresarAlPanel() {
        val intent = Intent(this, PanelConductorActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun vibrarBreve() {
        if (!::vibrator.isInitialized || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    50,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun vibrarLargo() {
        if (!::vibrator.isInitialized || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    500,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::locationRequest.isInitialized && ::locationCallback.isInitialized) {
            iniciarActualizacionesUbicacion()
        }
    }

    override fun onPause() {
        super.onPause()
        detenerActualizacionesUbicacion()
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerActualizacionesUbicacion()

        viajeListener?.let {
            if (viajeId.isNotEmpty()) {
                mDatabase.child("viajes")
                    .child(viajeId)
                    .removeEventListener(it)
            }
        }
    }
}