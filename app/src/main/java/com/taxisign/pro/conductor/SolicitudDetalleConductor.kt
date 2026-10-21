package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SolicitudDetalleConductor : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private var idSolicitud = ""
    private var pasajeroId = ""
    private var nombrePasajero = ""
    private var telefonoPasajero = ""
    private var esSordoMudo = false
    private var direccionRecojo = ""
    private var direccionDestino = ""
    private var precio = ""
    private var distancia = ""
    private var tarifa = 0.0
    private var tarifaOfertada = 0.0
    private var latRecojo = 0.0
    private var lngRecojo = 0.0
    private var destinoLat = 0.0
    private var destinoLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitud_detalle_conductor)

        idSolicitud = intent.getStringExtra("ID_SOLICITUD") ?: ""
        pasajeroId = intent.getStringExtra("PASAJERO_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"
        telefonoPasajero = intent.getStringExtra("TELEFONO_PASAJERO") ?: ""
        esSordoMudo = intent.getBooleanExtra("ES_SORDO_MUDO", false)

        direccionRecojo = intent.getStringExtra("DIRECCION_RECOJO")
            ?: intent.getStringExtra("ORIGEN")
                    ?: "Ubicación actual del pasajero"

        direccionDestino = intent.getStringExtra("DIRECCION_DESTINO")
            ?: intent.getStringExtra("DESTINO")
                    ?: "Destino no especificado"

        precio = intent.getStringExtra("PRECIO") ?: "Por calcular"
        distancia = intent.getStringExtra("DISTANCIA") ?: "Por calcular"
        tarifa = intent.getDoubleExtra("TARIFA", 0.0)

        latRecojo = intent.getDoubleExtra("LAT_RECOJO", 0.0)
        lngRecojo = intent.getDoubleExtra("LNG_RECOJO", 0.0)
        destinoLat = intent.getDoubleExtra("DESTINO_LAT", 0.0)
        destinoLng = intent.getDoubleExtra("DESTINO_LNG", 0.0)

        findViewById<TextView>(R.id.tv_nombre_pasajero).text = nombrePasajero
        findViewById<TextView>(R.id.tv_origen).text = direccionRecojo
        findViewById<TextView>(R.id.tv_destino).text = direccionDestino
        findViewById<TextView>(R.id.tv_distancia).text = distancia
        findViewById<TextView>(R.id.tv_precio).text = precio
        tarifaOfertada = if (tarifa > 0) tarifa else 0.0

        // El conductor puede proponer su propio precio (negociación)
        findViewById<TextView>(R.id.tv_precio).setOnClickListener {
            val input = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(String.format(java.util.Locale.US, "%.2f", if (tarifaOfertada > 0) tarifaOfertada else tarifa))
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tu precio para este viaje (S/)")
                .setMessage("El pasajero propuso S/ ${String.format(java.util.Locale.US, "%.2f", tarifa)}. Puedes ofrecer el tuyo.")
                .setView(input)
                .setPositiveButton("Aplicar") { _, _ ->
                    val v = input.text.toString().trim().toDoubleOrNull()
                    if (v != null && v > 0) {
                        tarifaOfertada = v
                        precio = "S/ " + String.format(java.util.Locale.US, "%.2f", v)
                        findViewById<TextView>(R.id.tv_precio).text = precio
                        Toast.makeText(this, "Tu oferta: $precio", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<Button>(R.id.btn_aceptar).apply {
            text = "ENVIAR OFERTA"
            setOnClickListener { enviarOferta() }
        }

        findViewById<Button>(R.id.btn_rechazar).setOnClickListener {
            rechazarSolicitud()
        }
    }

    private var elegidoListener: com.google.firebase.database.ValueEventListener? = null

    /** El conductor ofrece; el pasajero elegirá entre varias ofertas. */
    private fun enviarOferta() {
        if (idSolicitud.isEmpty()) {
            Toast.makeText(this, "Error: solicitud no encontrada", Toast.LENGTH_SHORT).show()
            return
        }
        val conductorId = auth.currentUser?.uid
        if (conductorId.isNullOrEmpty()) {
            Toast.makeText(this, "Debe iniciar sesión como conductor", Toast.LENGTH_LONG).show()
            return
        }

        dbRoot.child("usuarios").child("conductores").child(conductorId).get()
            .addOnSuccessListener { snap ->
                val nombreConductor = snap.child("nombre").value?.toString()
                    ?: auth.currentUser?.displayName ?: "Conductor"
                val placa = snap.child("placa").value?.toString() ?: "Sin placa"
                telefonoConductorActual = snap.child("telefono").value?.toString()
                    ?: snap.child("celular").value?.toString()
                    ?: auth.currentUser?.phoneNumber
                    ?: ""

                var suma = 0; var n = 0
                for (r in snap.child("resenas").children) {
                    val c = r.child("calificacion").getValue(Int::class.java) ?: continue
                    suma += c; n++
                }
                val rating = if (n > 0) suma.toDouble() / n else 0.0

                val precio = if (tarifaOfertada > 0) tarifaOfertada else tarifa

                val oferta = mapOf(
                    "conductorId" to conductorId,
                    "nombreConductor" to nombreConductor,
                    "placa" to placa,
                    "rating" to rating,
                    "tarifa" to precio,          // precio actual sobre la mesa
                    "precioActual" to precio,
                    "turno" to "pasajero",       // le toca responder al pasajero
                    "estado" to "negociando",
                    "timestamp" to System.currentTimeMillis()
                )
                dbRoot.child("solicitudes").child(idSolicitud)
                    .child("ofertas").child(conductorId).setValue(oferta)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Oferta enviada por S/ %.2f. Esperando al pasajero…".format(precio), Toast.LENGTH_LONG).show()
                        findViewById<Button>(R.id.btn_aceptar).apply { text = "NEGOCIANDO…"; isEnabled = false }
                        escucharNegociacion(conductorId, nombreConductor)
                    }
            }
    }

    /** Escucha la negociación de ESTA oferta (regateo de varias rondas). */
    private fun escucharNegociacion(conductorId: String, nombreConductor: String) {
        quitarElegidoListener()
        val ref = dbRoot.child("solicitudes").child(idSolicitud).child("ofertas").child(conductorId)
        elegidoListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!snapshot.exists()) return
                val estado = snapshot.child("estado").value?.toString() ?: "negociando"
                val turno = snapshot.child("turno").value?.toString() ?: ""
                val precioActual = snapshot.child("precioActual").getValue(Double::class.java)
                    ?: snapshot.child("tarifa").getValue(Double::class.java) ?: tarifa

                when (estado) {
                    "acuerdo" -> {
                        // El pasajero aceptó tu precio → crear el viaje
                        quitarElegidoListener()
                        tarifaOfertada = precioActual
                        crearViajeFirebase(conductorId, nombreConductor)
                    }
                    "rechazado" -> {
                        quitarElegidoListener()
                        Toast.makeText(this@SolicitudDetalleConductor,
                            "El pasajero canceló la negociación", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    else -> {
                        // negociando: si es tu turno, el pasajero hizo una contraoferta
                        if (turno == "conductor") {
                            mostrarContraofertaPasajero(conductorId, precioActual)
                        }
                    }
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        ref.addValueEventListener(elegidoListener!!)
    }

    private var dialogoNegociacionAbierto = false
    private var telefonoConductorActual = ""

    private fun mostrarContraofertaPasajero(conductorId: String, precioPasajero: Double) {
        if (dialogoNegociacionAbierto) return
        dialogoNegociacionAbierto = true

        val ref = dbRoot.child("solicitudes").child(idSolicitud).child("ofertas").child(conductorId)
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Tu nuevo precio"
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("El pasajero ofrece S/ %.2f".format(precioPasajero))
            .setMessage("¿Aceptas ese precio o haces una contraoferta?")
            .setView(input)
            .setPositiveButton("Aceptar S/ %.2f".format(precioPasajero)) { _, _ ->
                dialogoNegociacionAbierto = false
                ref.child("estado").setValue("acuerdo")
                ref.child("precioActual").setValue(precioPasajero)
                // El listener detectará "acuerdo" y creará el viaje
            }
            .setNeutralButton("Contraofertar") { _, _ ->
                dialogoNegociacionAbierto = false
                val nuevo = input.text.toString().trim().toDoubleOrNull()
                if (nuevo != null && nuevo > 0) {
                    val updates = mapOf<String, Any>(
                        "precioActual" to nuevo,
                        "tarifa" to nuevo,
                        "turno" to "pasajero",
                        "estado" to "negociando"
                    )
                    ref.updateChildren(updates)
                    Toast.makeText(this, "Contraoferta enviada: S/ %.2f".format(nuevo), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Escribe un precio válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setOnDismissListener { dialogoNegociacionAbierto = false }
            .show()
    }

    private fun quitarElegidoListener() {
        elegidoListener?.let {
            val conductorId = auth.currentUser?.uid ?: return@let
            dbRoot.child("solicitudes").child(idSolicitud).child("ofertas").child(conductorId)
                .removeEventListener(it)
            elegidoListener = null
        }
    }

    private fun aceptarViaje() {
        if (idSolicitud.isEmpty()) {
            Toast.makeText(this, "Error: solicitud no encontrada", Toast.LENGTH_SHORT).show()
            return
        }

        val conductorId = auth.currentUser?.uid

        if (conductorId.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Debe iniciar sesión como conductor",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        dbRoot.child("usuarios")
            .child("conductores")
            .child(conductorId)
            .get()
            .addOnSuccessListener { snapshot ->
                val nombreConductor =
                    snapshot.child("nombre").value?.toString()
                        ?: auth.currentUser?.displayName
                        ?: "Conductor Asignado"

                telefonoConductorActual = snapshot.child("telefono").value?.toString()
                    ?: snapshot.child("celular").value?.toString()
                    ?: auth.currentUser?.phoneNumber
                    ?: ""

                crearViajeFirebase(conductorId, nombreConductor)
            }
            .addOnFailureListener {
                val nombreConductor =
                    auth.currentUser?.displayName ?: "Conductor Asignado"

                crearViajeFirebase(conductorId, nombreConductor)
            }
    }

    private fun crearViajeFirebase(
        conductorId: String,
        nombreConductor: String
    ) {
        val viajeRef = dbRoot.child("viajes").push()
        val viajeId = viajeRef.key ?: "viaje_${System.currentTimeMillis()}"

        val datosViaje = mapOf(
            "idSolicitud" to idSolicitud,
            "viajeId" to viajeId,

            "conductorId" to conductorId,
            "idConductor" to conductorId,
            "nombreConductor" to nombreConductor,
            "telefonoConductor" to telefonoConductorActual,

            "pasajeroId" to pasajeroId,
            "idPasajero" to pasajeroId,
            "nombrePasajero" to nombrePasajero,
            "telefonoPasajero" to telefonoPasajero,
            "esSordoMudo" to esSordoMudo,

            "origen" to direccionRecojo,
            "direccionRecojo" to direccionRecojo,
            "destino" to direccionDestino,
            "direccionDestino" to direccionDestino,

            "precio" to (if (tarifaOfertada > 0) "S/ %.2f".format(tarifaOfertada) else precio),
            "tarifa" to (if (tarifaOfertada > 0) tarifaOfertada else tarifa),
            "distancia" to distancia,

            "latRecojo" to latRecojo,
            "lngRecojo" to lngRecojo,
            "destinoLat" to destinoLat,
            "destinoLng" to destinoLng,

            "estado" to "conductor_asignado",
            "timestamp" to System.currentTimeMillis()
        )

        val updates = hashMapOf<String, Any>(
            "solicitudes/$idSolicitud/estado" to "aceptada",
            "solicitudes/$idSolicitud/conductorId" to conductorId,
            "solicitudes/$idSolicitud/idConductor" to conductorId,
            "solicitudes/$idSolicitud/nombreConductor" to nombreConductor,
            "solicitudes/$idSolicitud/viajeId" to viajeId,

            "viajes/$viajeId" to datosViaje,

            "chats/$viajeId/activo" to true,
            "chats/$viajeId/idSolicitud" to idSolicitud,
            "chats/$viajeId/pasajeroId" to pasajeroId,
            "chats/$viajeId/conductorId" to conductorId
        )

        dbRoot.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud aceptada", Toast.LENGTH_SHORT).show()

                // Aviso amigable al pasajero
                com.taxisign.pro.notificaciones.NotificacionHelper.enviar(
                    uidDestino = pasajeroId,
                    rol = "PASAJERO",
                    tipo = "VIAJE",
                    titulo = "¡Conductor asignado! 🚕",
                    mensaje = "$nombreConductor va en camino a recogerte. Prepárate 😊"
                )

                irAPasajeroAsignado(viajeId)
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error Firebase: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun rechazarSolicitud() {
        Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        quitarElegidoListener()
    }

    private fun irAPasajeroAsignado(viajeId: String) {
        val tarifaFinal = if (tarifaOfertada > 0) tarifaOfertada else tarifa
        val precioFinal = if (tarifaFinal > 0) "S/ %.2f".format(tarifaFinal) else precio

        val intent = Intent(this, PasajeroAsignado::class.java).apply {
            putExtra("VIAJE_ID", viajeId)
            putExtra("NOMBRE_PASAJERO", nombrePasajero)
            putExtra("TELEFONO_PASAJERO", telefonoPasajero)
            putExtra("DIRECCION_RECOJO", direccionRecojo)
            putExtra("DIRECCION_DESTINO", direccionDestino)
            putExtra("PRECIO", precioFinal)
            putExtra("DISTANCIA", distancia)
            putExtra("TARIFA", tarifaFinal)
            putExtra("LAT_RECOJO", latRecojo)
            putExtra("LNG_RECOJO", lngRecojo)
            putExtra("DESTINO_LAT", destinoLat)
            putExtra("DESTINO_LNG", destinoLng)
            putExtra("ES_SORDO_MUDO", esSordoMudo)
        }

        startActivity(intent)
        finish()
    }
}