package com.taxisign.pro.pasajero

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.taxisign.pro.R

class BuscarConductorActivity : AppCompatActivity() {

    private lateinit var txtDestinoSeleccionado: TextView
    private var vibrator: Vibrator? = null
    private var idSolicitud = ""
    private var destinoSeleccionado = ""

    private val mDatabase by lazy { FirebaseDatabase.getInstance().reference }
    private var listenerSolicitud: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_conductor)

        idSolicitud = intent.getStringExtra("id_solicitud") ?: ""
        destinoSeleccionado = intent.getStringExtra("destino") ?: "Destino no definido"

        txtDestinoSeleccionado = findViewById(R.id.txtDestinoSeleccionado)
        txtDestinoSeleccionado.text = destinoSeleccionado

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        configurarAnimacionRadar()

        findViewById<ImageButton>(R.id.btn_back_busqueda).setOnClickListener {
            vibrar()
            cancelarSolicitud()
        }

        findViewById<LinearLayout>(R.id.btn_cancelar_busqueda).setOnClickListener {
            vibrar()
            cancelarSolicitud()
        }

        iniciarMonitoreoFirebase()
        escucharOfertas()
    }

    private val ofertas = mutableListOf<OfertaConductor>()
    private var ofertasListener: ValueEventListener? = null

    private fun escucharOfertas() {
        if (idSolicitud.isEmpty()) return
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_ofertas)
        val titulo = findViewById<TextView>(R.id.tv_titulo_ofertas)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = OfertasAdapter(ofertas) { elegirConductor(it) }
        recycler.adapter = adapter

        ofertasListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ofertas.clear()
                for (c in snapshot.children) {
                    val o = c.getValue(OfertaConductor::class.java) ?: continue
                    ofertas.add(o)
                }
                ofertas.sortByDescending { it.rating }
                adapter.notifyDataSetChanged()
                titulo.text = "Conductores disponibles (${ofertas.size})"
                if (ofertas.isNotEmpty()) vibrar()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        mDatabase.child("solicitudes").child(idSolicitud).child("ofertas")
            .addValueEventListener(ofertasListener!!)
    }

    private fun elegirConductor(oferta: OfertaConductor) {
        if (idSolicitud.isEmpty()) return
        val refOferta = mDatabase.child("solicitudes").child(idSolicitud)
            .child("ofertas").child(oferta.conductorId)
        val precio = oferta.precioEnMesa()

        if (oferta.turno != "pasajero") {
            Toast.makeText(this, "Esperando la respuesta del conductor…", Toast.LENGTH_SHORT).show()
            return
        }

        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Tu contraoferta (S/)"
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("${oferta.nombreConductor} ofrece S/ %.2f".format(precio))
            .setMessage("Calificación: ${if (oferta.rating > 0) "%.1f".format(oferta.rating) else "Nuevo"}\nPlaca: ${oferta.placa}\n\n¿Aceptas ese precio o haces una contraoferta?")
            .setView(input)
            .setPositiveButton("Aceptar S/ %.2f".format(precio)) { _, _ ->
                // Acuerdo: el conductor crea el viaje
                val updates = mapOf<String, Any>(
                    "estado" to "acuerdo",
                    "precioActual" to precio
                )
                refOferta.updateChildren(updates)
                mDatabase.child("solicitudes").child(idSolicitud)
                    .child("conductorElegido").setValue(oferta.conductorId)
                Toast.makeText(this, "¡Trato hecho con ${oferta.nombreConductor}! Conectando…", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Contraofertar") { _, _ ->
                val nuevo = input.text.toString().trim().toDoubleOrNull()
                if (nuevo != null && nuevo > 0) {
                    val updates = mapOf<String, Any>(
                        "precioActual" to nuevo,
                        "tarifa" to nuevo,
                        "turno" to "conductor",
                        "estado" to "negociando"
                    )
                    refOferta.updateChildren(updates)
                    Toast.makeText(this, "Contraoferta enviada: S/ %.2f".format(nuevo), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Escribe un precio válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarAnimacionRadar() {
        val radar = findViewById<ImageView>(R.id.iv_radar)
        val animacionRadar = AnimationUtils.loadAnimation(this, R.anim.rotate_radar)
        radar.startAnimation(animacionRadar)
    }

    private fun cancelarSolicitud() {
        if (idSolicitud.isNotEmpty()) {
            mDatabase.child("solicitudes")
                .child(idSolicitud)
                .child("estado")
                .setValue("cancelada")
                .addOnSuccessListener {
                    Toast.makeText(this, "Búsqueda cancelada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    finish()
                }
        } else {
            finish()
        }
    }

    private fun iniciarMonitoreoFirebase() {
        if (idSolicitud.isEmpty()) {
            Toast.makeText(this, "Error: solicitud no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        listenerSolicitud = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val estado = snapshot.child("estado").value?.toString() ?: "pendiente"

                if (estado == "aceptada") {
                    val idConductor = snapshot.child("conductorId").value?.toString()
                        ?: snapshot.child("idConductor").value?.toString()
                        ?: ""

                    val nombreConductor = snapshot.child("nombreConductor").value?.toString()
                        ?: "Conductor"

                    val viajeId = snapshot.child("viajeId").value?.toString() ?: ""

                    val direccionDestino = snapshot.child("direccionDestino").value?.toString()
                        ?: snapshot.child("destino").value?.toString()
                        ?: destinoSeleccionado

                    val precioFinal = snapshot.child("precio").value?.toString()
                        ?: "Por calcular"

                    val distanciaFinal = snapshot.child("distancia").value?.toString()
                        ?: "Por calcular"

                    obtenerDatosConductorYContinuar(
                        idConductor = idConductor,
                        nombreConductor = nombreConductor,
                        viajeId = viajeId,
                        direccionDestino = direccionDestino,
                        precioFinal = precioFinal,
                        distanciaFinal = distanciaFinal
                    )
                }

                if (estado == "cancelada") {
                    detenerMonitoreo()
                    Toast.makeText(
                        this@BuscarConductorActivity,
                        "Solicitud cancelada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BuscarConductorActivity,
                    "Error Firebase: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mDatabase.child("solicitudes")
            .child(idSolicitud)
            .addValueEventListener(listenerSolicitud!!)
    }

    private fun obtenerDatosConductorYContinuar(
        idConductor: String,
        nombreConductor: String,
        viajeId: String,
        direccionDestino: String,
        precioFinal: String,
        distanciaFinal: String
    ) {
        if (idConductor.isEmpty()) {
            abrirConductorAsignado(
                idConductor = "",
                nombreConductor = nombreConductor,
                viajeId = viajeId,
                datosVehiculo = "Vehículo no registrado",
                direccionDestino = direccionDestino,
                precioFinal = precioFinal,
                distanciaFinal = distanciaFinal,
                telefonoConductor = ""
            )
            return
        }

        mDatabase.child("usuarios")
            .child("conductores")
            .child(idConductor)
            .get()
            .addOnSuccessListener { usuarioSnapshot ->

                val nombreReal = usuarioSnapshot.child("nombre").value?.toString()
                    ?: nombreConductor

                val placa = usuarioSnapshot.child("placa").value?.toString()
                    ?: "Sin placa"

                val datosVehiculo = "Placa: $placa"
                val telefonoConductor = usuarioSnapshot.child("telefono").value?.toString()
                    ?: usuarioSnapshot.child("celular").value?.toString()
                    ?: ""

                abrirConductorAsignado(
                    idConductor = idConductor,
                    nombreConductor = nombreReal,
                    viajeId = viajeId,
                    datosVehiculo = datosVehiculo,
                    direccionDestino = direccionDestino,
                    precioFinal = precioFinal,
                    distanciaFinal = distanciaFinal,
                    telefonoConductor = telefonoConductor
                )
            }
            .addOnFailureListener {
                abrirConductorAsignado(
                    idConductor = idConductor,
                    nombreConductor = nombreConductor,
                    viajeId = viajeId,
                    datosVehiculo = "Vehículo no registrado",
                    direccionDestino = direccionDestino,
                    precioFinal = precioFinal,
                    distanciaFinal = distanciaFinal,
                    telefonoConductor = ""
                )
            }
    }

    private fun abrirConductorAsignado(
        idConductor: String,
        nombreConductor: String,
        viajeId: String,
        datosVehiculo: String,
        direccionDestino: String,
        precioFinal: String,
        distanciaFinal: String,
        telefonoConductor: String
    ) {
        detenerMonitoreo()

        val intent = Intent(this, ConductorAsignadoActivity::class.java).apply {
            putExtra("id_solicitud", idSolicitud)
            putExtra("id_conductor", idConductor)
            putExtra("viaje_id", viajeId)
            putExtra("CHAT_ID", viajeId)
            putExtra("nombre_conductor", nombreConductor)
            putExtra("datos_vehiculo", datosVehiculo)
            putExtra("direccion_destino", direccionDestino)
            putExtra("PRECIO", precioFinal)
            putExtra("DISTANCIA", distanciaFinal)
            putExtra("TELEFONO_CONDUCTOR", telefonoConductor)
        }

        startActivity(intent)
        finish()
    }

    private fun detenerMonitoreo() {
        listenerSolicitud?.let {
            mDatabase.child("solicitudes")
                .child(idSolicitud)
                .removeEventListener(it)
            listenerSolicitud = null
        }
        ofertasListener?.let {
            mDatabase.child("solicitudes").child(idSolicitud).child("ofertas")
                .removeEventListener(it)
            ofertasListener = null
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

    override fun onDestroy() {
        detenerMonitoreo()
        super.onDestroy()
    }
}