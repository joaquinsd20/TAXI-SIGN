package com.taxisign.pro.pasajero

import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Locale
import kotlin.math.ceil

class ConfirmacionViajeActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private var vibrator: Vibrator? = null

    private var origen = ""
    private var destino = ""
    private var distancia = ""
    private var tiempo = ""
    private var precio = ""
    private var metodoPago = "Efectivo"

    private var origenLat = 0.0
    private var origenLng = 0.0
    private var destinoLat = 0.0
    private var destinoLng = 0.0
    private var tarifa = 0.0
    private var tarifaEditadaManual = false
    private var distanciaKm = 0.0
    private var tiempoMin = 0

    private lateinit var txtOrigen: TextView
    private lateinit var txtDestino: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtTiempo: TextView
    private lateinit var txtPrecio: TextView
    private lateinit var txtMetodoPago: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_viaje)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        origen = intent.getStringExtra("origen") ?: "Origen seleccionado"
        destino = intent.getStringExtra("destino") ?: "Destino seleccionado"

        origenLat = intent.getDoubleExtra("origenLat", 0.0)
        origenLng = intent.getDoubleExtra("origenLng", 0.0)
        destinoLat = intent.getDoubleExtra("destinoLat", 0.0)
        destinoLng = intent.getDoubleExtra("destinoLng", 0.0)

        metodoPago = intent.getStringExtra("metodo_pago") ?: "Efectivo"

        txtOrigen = findViewById(R.id.txt_origen)
        txtDestino = findViewById(R.id.txt_destino)
        txtDistancia = findViewById(R.id.txt_distancia)
        txtTiempo = findViewById(R.id.txt_tiempo)
        txtPrecio = findViewById(R.id.txt_precio)
        txtMetodoPago = findViewById(R.id.txt_metodo_pago)

        txtOrigen.text = origen
        txtDestino.text = destino
        txtMetodoPago.text = metodoPago

        calcularDatosViaje()

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            vibrar()
            finish()
        }

        findViewById<LinearLayout>(R.id.layout_metodo_pago).setOnClickListener {
            vibrar()
            mostrarSelectorPago()
        }

        // El pasajero puede proponer / editar la tarifa
        txtPrecio.setOnClickListener {
            vibrar()
            editarTarifa()
        }

        findViewById<LinearLayout>(R.id.btn_buscar_conductor).setOnClickListener {
            vibrar()
            crearSolicitudFirebase()
        }
    }

    private fun editarTarifa() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", tarifa))
            hint = "Ej: 12.50"
        }
        AlertDialog.Builder(this)
            .setTitle("Ofrece tu tarifa (S/)")
            .setMessage("Sugerida: S/ ${String.format(Locale.US, "%.2f", tarifa)}")
            .setView(input)
            .setPositiveButton("Aplicar") { _, _ ->
                val nueva = input.text.toString().trim().toDoubleOrNull()
                if (nueva == null || nueva <= 0.0) {
                    Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                } else {
                    tarifa = nueva
                    tarifaEditadaManual = true
                    precio = "S/ " + String.format(Locale.US, "%.2f", tarifa)
                    txtPrecio.text = precio
                    Toast.makeText(this, "Tarifa propuesta: $precio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun resolverCoordenadasFaltantes() {
        // Origen faltante: usar última ubicación conocida del dispositivo
        if (origenLat == 0.0 || origenLng == 0.0) {
            try {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                    val prov = listOf(
                        android.location.LocationManager.GPS_PROVIDER,
                        android.location.LocationManager.NETWORK_PROVIDER
                    )
                    for (p in prov) {
                        val loc = lm.getLastKnownLocation(p)
                        if (loc != null) { origenLat = loc.latitude; origenLng = loc.longitude; break }
                    }
                }
            } catch (_: Exception) {}
        }
        // Destino faltante pero con texto: geocodificar
        if ((destinoLat == 0.0 || destinoLng == 0.0) && destino.isNotBlank() &&
            destino != "Destino seleccionado") {
            try {
                val geo = android.location.Geocoder(this, Locale.getDefault())
                val r = geo.getFromLocationName(destino, 1)
                r?.firstOrNull()?.let { destinoLat = it.latitude; destinoLng = it.longitude }
            } catch (_: Exception) {}
        }
    }

    private fun calcularDatosViaje() {
        resolverCoordenadasFaltantes()
        if (origenLat == 0.0 || origenLng == 0.0 || destinoLat == 0.0 || destinoLng == 0.0) {
            distanciaKm = 0.0
            tiempoMin = 0
            tarifa = 0.0
        } else {
            val resultados = FloatArray(1)

            Location.distanceBetween(
                origenLat,
                origenLng,
                destinoLat,
                destinoLng,
                resultados
            )

            distanciaKm = resultados[0] / 1000.0
            tiempoMin = ceil((distanciaKm / 25.0) * 60.0).toInt()

            val tarifaBase = 5.0
            val costoPorKm = 1.8
            val costoPorMin = 0.25

            tarifa = tarifaBase + (distanciaKm * costoPorKm) + (tiempoMin * costoPorMin)

            if (tarifa < 7.0) {
                tarifa = 7.0
            }
        }

        distancia = String.format(Locale.US, "%.1f km", distanciaKm)
        tiempo = "$tiempoMin min"
        precio = "S/ " + String.format(Locale.US, "%.2f", tarifa)

        txtDistancia.text = distancia
        txtTiempo.text = tiempo
        txtPrecio.text = precio

        // Refinar con la RUTA REAL por carretera (cae a línea recta si falla)
        if (origenLat != 0.0 && destinoLat != 0.0) {
            com.taxisign.pro.RutaHelper.calcular(
                this,
                com.google.android.gms.maps.model.LatLng(origenLat, origenLng),
                com.google.android.gms.maps.model.LatLng(destinoLat, destinoLng)
            ) { km, min, esReal ->
                if (esReal) {
                    distanciaKm = km
                    tiempoMin = min
                    distancia = String.format(Locale.US, "%.1f km", km)
                    tiempo = "$min min"
                    txtDistancia.text = distancia
                    txtTiempo.text = tiempo
                    // No pisar la tarifa si el pasajero ya la editó a mano
                    if (!tarifaEditadaManual) {
                        val tarifaBase = 5.0
                        val costoPorKm = 1.8
                        val costoPorMin = 0.25
                        tarifa = (tarifaBase + (km * costoPorKm) + (min * costoPorMin)).coerceAtLeast(7.0)
                        precio = "S/ " + String.format(Locale.US, "%.2f", tarifa)
                        txtPrecio.text = precio
                    }
                }
            }
        }
    }

    private fun mostrarSelectorPago() {
        val opciones = arrayOf("Efectivo", "Yape", "Plin")

        AlertDialog.Builder(this)
            .setTitle("Selecciona método de pago")
            .setItems(opciones) { dialog, which ->
                metodoPago = opciones[which]
                txtMetodoPago.text = metodoPago
                dialog.dismiss()
            }
            .show()
    }

    private fun crearSolicitudFirebase() {
        val usuario = auth.currentUser

        if (usuario == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val pasajeroId = usuario.uid

        dbRoot.child("usuarios")
            .child("pasajeros")
            .child(pasajeroId)
            .get()
            .addOnSuccessListener { snapshot ->

                val nombrePasajero =
                    snapshot.child("nombre").value?.toString()
                        ?: usuario.displayName
                        ?: "Pasajero"

                val telefonoPasajero =
                    snapshot.child("telefono").value?.toString()
                        ?: usuario.phoneNumber
                        ?: ""

                val tipoDiscapacidad =
                    snapshot.child("tipoDiscapacidad").value?.toString()
                        ?: ""

                val esSordoMudo =
                    snapshot.child("esSordoMudo").getValue(Boolean::class.java)
                        ?: (
                                tipoDiscapacidad.contains("sordo", ignoreCase = true) ||
                                        tipoDiscapacidad.contains("mudo", ignoreCase = true)
                                )

                val fotoPerfil =
                    snapshot.child("fotoPerfil").value?.toString()
                        ?: ""

                crearSolicitudConDatosPasajero(
                    pasajeroId = pasajeroId,
                    nombrePasajero = nombrePasajero,
                    telefonoPasajero = telefonoPasajero,
                    tipoDiscapacidad = tipoDiscapacidad,
                    esSordoMudo = esSordoMudo,
                    fotoPerfil = fotoPerfil
                )
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "No se pudieron cargar los datos del pasajero: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun crearSolicitudConDatosPasajero(
        pasajeroId: String,
        nombrePasajero: String,
        telefonoPasajero: String,
        tipoDiscapacidad: String,
        esSordoMudo: Boolean,
        fotoPerfil: String
    ) {
        val solicitudRef = dbRoot.child("solicitudes").push()
        val idSolicitud = solicitudRef.key ?: ""

        if (idSolicitud.isEmpty()) {
            Toast.makeText(this, "Error al crear solicitud", Toast.LENGTH_SHORT).show()
            return
        }

        val solicitud = hashMapOf<String, Any>(
            "idSolicitud" to idSolicitud,

            "idPasajero" to pasajeroId,
            "pasajeroId" to pasajeroId,
            "nombrePasajero" to nombrePasajero,
            "telefonoPasajero" to telefonoPasajero,
            "tipoDiscapacidad" to tipoDiscapacidad,
            "esSordoMudo" to esSordoMudo,
            "fotoPerfilPasajero" to fotoPerfil,

            "origen" to origen,
            "direccionRecojo" to origen,
            "destino" to destino,
            "direccionDestino" to destino,

            "distancia" to distancia,
            "distanciaKm" to distanciaKm,
            "tiempo" to tiempo,
            "tiempoMin" to tiempoMin,
            "precio" to precio,
            "tarifa" to tarifa,
            "metodoPago" to metodoPago,

            "origenLat" to origenLat,
            "origenLng" to origenLng,
            "destinoLat" to destinoLat,
            "destinoLng" to destinoLng,

            "estado" to "pendiente",
            "timestamp" to ServerValue.TIMESTAMP
        )

        solicitudRef.setValue(solicitud)
            .addOnSuccessListener {
                val intent = Intent(this, BuscarConductorActivity::class.java).apply {
                    putExtra("id_solicitud", idSolicitud)
                    putExtra("destino", destino)
                }

                startActivity(intent)
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Error Firebase: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun vibrar() {
        vibrator?.let {
            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        80,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(80)
            }
        }
    }
}