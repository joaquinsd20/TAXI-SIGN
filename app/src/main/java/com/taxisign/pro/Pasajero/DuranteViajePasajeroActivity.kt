package com.taxisign.pro.pasajero

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.ChatPasajeroActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.taxisign.pro.R

class DuranteViajePasajeroActivity : AppCompatActivity(), OnMapReadyCallback {

    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var mMap: GoogleMap

    private var conductorMarker: Marker? = null
    private var destinoMarker: Marker? = null

    private var ubicacionListener: ValueEventListener? = null
    private var estadoListener: ValueEventListener? = null

    private var viajeId = ""
    private var chatId = ""
    private var idSolicitud = ""
    private var idConductor = ""
    private var nombreConductor = ""
    private var direccionDestino = ""
    private var metodoPago = "Efectivo"
    private var tarifa = 0.0

    private var destinoLat = 0.0
    private var destinoLng = 0.0

    private lateinit var txtDestino: TextView
    private lateinit var txtTiempo: TextView
    private lateinit var txtDistancia: TextView
    private var txtNombreConductor: TextView? = null
    private var txtNombrePasajero: TextView? = null
    private var nombrePasajero = ""

    private var yaPasoAFinalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_durante_viaje_pasajero)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        chatId = intent.getStringExtra("CHAT_ID") ?: viajeId
        idSolicitud = intent.getStringExtra("ID_SOLICITUD") ?: ""
        idConductor = intent.getStringExtra("ID_CONDUCTOR") ?: ""
        nombreConductor = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"
        direccionDestino = intent.getStringExtra("DIRECCION_DESTINO") ?: "Destino"
        metodoPago = intent.getStringExtra("METODO_PAGO") ?: "Efectivo"
        tarifa = intent.getDoubleExtra("TARIFA", 0.0)

        destinoLat = intent.getDoubleExtra("DESTINO_LAT", 0.0)
        destinoLng = intent.getDoubleExtra("DESTINO_LNG", 0.0)

        txtDestino = findViewById(R.id.txt_destino)
        txtTiempo = findViewById(R.id.txt_tiempo)
        txtDistancia = findViewById(R.id.txt_distancia)
        txtNombreConductor = findViewById(R.id.txt_nombre_conductor)
        txtNombrePasajero = findViewById(R.id.txt_nombre_pasajero)

        nombrePasajero = com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Tú" }
        actualizarNombres()

        txtDestino.text = direccionDestino
        txtTiempo.text = "Calculando..."
        txtDistancia.text = "Calculando..."

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.btn_mensaje).setOnClickListener {
            abrirChat()
        }

        findViewById<LinearLayout>(R.id.btn_compartir).setOnClickListener {
            compartirViaje()
        }

        findViewById<LinearLayout>(R.id.btn_emergencia).setOnClickListener {
            mostrarEmergencia()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_durante_viaje_pasajero) as SupportMapFragment

        mapFragment.getMapAsync(this)

        cargarDatosViajeDesdeFirebase()
        escucharEstadoViaje()
        configurarBotonLlamar()
    }

    private fun configurarBotonLlamar() {
        val btnLlamar = findViewById<LinearLayout>(R.id.btn_llamar)

        // Solo los pasajeros OYENTES pueden llamar; sordos/mudos usan el chat
        val tipo = com.taxisign.pro.SessionManager.tipoDiscapacidadUsuario.lowercase()
        val esSordoMudo = tipo.contains("sordo") || tipo.contains("mudo")

        if (esSordoMudo) {
            btnLlamar.visibility = android.view.View.GONE
            return
        }

        btnLlamar.visibility = android.view.View.VISIBLE
        btnLlamar.setOnClickListener {
            if (idConductor.isEmpty()) {
                Toast.makeText(this, "No hay número del conductor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("usuarios").child("conductores").child(idConductor)
                .child("telefono").get()
                .addOnSuccessListener { snap ->
                    val tel = snap.value?.toString() ?: ""
                    if (tel.isEmpty()) {
                        Toast.makeText(this, "El conductor no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel:$tel")
                        startActivity(intent)
                    }
                }
        }
    }

    private fun actualizarNombres() {
        txtNombreConductor?.text = "🚖 $nombreConductor"
        txtNombrePasajero?.text = nombrePasajero
    }

    private fun cargarDatosViajeDesdeFirebase() {
        if (viajeId.isEmpty()) return

        dbRoot.child("viajes")
            .child(viajeId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener

                if (direccionDestino == "Destino") {
                    direccionDestino =
                        snapshot.child("direccionDestino").value?.toString()
                            ?: snapshot.child("destino").value?.toString()
                                    ?: direccionDestino
                    txtDestino.text = direccionDestino
                }

                if (nombreConductor == "Conductor") {
                    nombreConductor =
                        snapshot.child("nombreConductor").value?.toString()
                            ?: nombreConductor
                }

                snapshot.child("nombrePasajero").value?.toString()?.let {
                    if (it.isNotEmpty()) nombrePasajero = it
                }
                actualizarNombres()

                if (tarifa == 0.0) {
                    tarifa =
                        snapshot.child("tarifa").getValue(Double::class.java)
                            ?: 0.0
                }

                metodoPago =
                    snapshot.child("metodoPago").value?.toString()
                        ?: metodoPago

                if (destinoLat == 0.0) {
                    destinoLat =
                        snapshot.child("destinoLat").getValue(Double::class.java)
                            ?: 0.0
                }

                if (destinoLng == 0.0) {
                    destinoLng =
                        snapshot.child("destinoLng").getValue(Double::class.java)
                            ?: 0.0
                }
            }
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

        agregarMarcadorDestino()
        escucharUbicacionConductor()
    }

    private fun agregarMarcadorDestino() {
        if (destinoLat == 0.0 || destinoLng == 0.0) return

        val destino = LatLng(destinoLat, destinoLng)

        destinoMarker = mMap.addMarker(
            MarkerOptions()
                .position(destino)
                .title("Destino")
                .snippet(direccionDestino)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destino, 15f))
    }

    private fun escucharUbicacionConductor() {
        if (viajeId.isEmpty()) return

        ubicacionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitud").getValue(Double::class.java)
                val lng = snapshot.child("longitud").getValue(Double::class.java)

                if (lat == null || lng == null) return

                val posicionConductor = LatLng(lat, lng)

                if (conductorMarker == null) {
                    conductorMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(posicionConductor)
                            .title(nombreConductor)
                            .snippet("Conductor en ruta")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )
                } else {
                    conductorMarker?.position = posicionConductor
                }

                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(posicionConductor, 16f)
                )

                actualizarDistanciaYTiempo(posicionConductor)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DuranteViajePasajeroActivity,
                    "Error leyendo ubicación del conductor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dbRoot.child("viajes")
            .child(viajeId)
            .child("ubicacionConductor")
            .addValueEventListener(ubicacionListener!!)
    }

    private fun actualizarDistanciaYTiempo(posicionConductor: LatLng) {
        if (destinoLat == 0.0 || destinoLng == 0.0) {
            txtDistancia.text = "Sin destino"
            txtTiempo.text = "--"
            return
        }

        val resultados = FloatArray(1)

        android.location.Location.distanceBetween(
            posicionConductor.latitude,
            posicionConductor.longitude,
            destinoLat,
            destinoLng,
            resultados
        )

        val distanciaKm = resultados[0] / 1000f
        val tiempoMin = ((distanciaKm / 25f) * 60f).toInt().coerceAtLeast(1)

        txtDistancia.text = String.format("%.1f km", distanciaKm)
        txtTiempo.text = "$tiempoMin min"
    }

    private fun escucharEstadoViaje() {
        if (viajeId.isEmpty()) return

        estadoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.value?.toString() ?: ""

                when (estado) {
                    "finalizado" -> {
                        abrirViajeFinalizado()
                    }

                    "cancelado_conductor" -> {
                        Toast.makeText(
                            this@DuranteViajePasajeroActivity,
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

    private fun abrirViajeFinalizado() {
        if (yaPasoAFinalizado) return
        yaPasoAFinalizado = true

        Toast.makeText(
            this,
            "Viaje finalizado",
            Toast.LENGTH_SHORT
        ).show()

        val intentFinal = Intent(
            this,
            ViajeFinalizadoActivity::class.java
        ).apply {
            putExtra("VIAJE_ID", viajeId)
            putExtra("ID_CONDUCTOR", idConductor)
            putExtra("NOMBRE_CONDUCTOR", nombreConductor)
            putExtra("DESTINO", direccionDestino)
            putExtra("DISTANCIA", txtDistancia.text.toString())
            putExtra("TIEMPO", txtTiempo.text.toString())
            putExtra("TARIFA", tarifa)
            putExtra("METODO_PAGO", metodoPago)
        }

        startActivity(intentFinal)
        finish()
    }

    private fun abrirChat() {
        if (chatId.isEmpty()) {
            Toast.makeText(this, "Error: chat no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ChatPasajeroActivity::class.java).apply {
            putExtra("VIAJE_ID", viajeId)
            putExtra("CHAT_ID", chatId)
            putExtra("ID_SOLICITUD", idSolicitud)
            putExtra("ID_CONDUCTOR", idConductor)
            putExtra("NOMBRE_CONDUCTOR", nombreConductor)
            putExtra("DIRECCION_DESTINO", direccionDestino)
        }

        startActivity(intent)
    }

    private fun compartirViaje() {
        val mensaje = """
            Estoy en un viaje de TaxiSign.
            Conductor: $nombreConductor
            Destino: $direccionDestino
            Código de viaje: $viajeId
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }

        startActivity(Intent.createChooser(intent, "Compartir viaje"))
    }

    private fun mostrarEmergencia() {
        AlertDialog.Builder(this)
            .setTitle("Emergencia")
            .setMessage("¿Deseas llamar a emergencias?")
            .setPositiveButton("Llamar") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:105")
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        ubicacionListener?.let {
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
    }
}