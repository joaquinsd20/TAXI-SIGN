package com.taxisign.pro.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R

class ViajeFinalizadoActivity : AppCompatActivity() {

    private lateinit var txtConductor: TextView
    private lateinit var txtDestino: TextView
    private lateinit var txtTiempo: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtTarifa: TextView
    private lateinit var txtMetodoPago: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viaje_finalizado)

        txtConductor = findViewById(R.id.txt_conductor)
        txtDestino = findViewById(R.id.txt_destino)
        txtTiempo = findViewById(R.id.txt_tiempo)
        txtDistancia = findViewById(R.id.txt_distancia)
        txtTarifa = findViewById(R.id.txt_tarifa)
        txtMetodoPago = findViewById(R.id.txt_metodo_pago)

        findViewById<TextView>(R.id.txt_pasajero).text =
            com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Pasajero" }

        val btnCalificar = findViewById<LinearLayout>(R.id.btn_calificar)
        val btnInicio = findViewById<LinearLayout>(R.id.btn_inicio)

        // Datos recibidos del viaje

        val conductor =
            intent.getStringExtra("NOMBRE_CONDUCTOR")
                ?: "Conductor"

        val destino =
            intent.getStringExtra("DESTINO")
                ?: "Destino no disponible"

        val tiempo =
            intent.getStringExtra("TIEMPO")
                ?: "0 min"

        val distancia =
            intent.getStringExtra("DISTANCIA")
                ?: "0 km"

        val tarifa =
            intent.getDoubleExtra("TARIFA", 0.0)

        val metodoPago =
            intent.getStringExtra("METODO_PAGO")
                ?: "Efectivo"

        // Mostrar información (inicial desde extras)
        txtConductor.text = conductor
        txtDestino.text = destino
        txtTiempo.text = "⏱ Tiempo total: $tiempo"
        txtDistancia.text = "📍 Distancia recorrida: $distancia"
        txtTarifa.text = "💵 S/ %.2f".format(tarifa)
        txtMetodoPago.text = "Pago: $metodoPago"

        // Cargar datos confiables desde Firebase (viaje + solicitud)
        val viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        if (viajeId.isNotEmpty()) cargarDesdeFirebase(viajeId)

        btnCalificar.setOnClickListener {

            val intentCalificar = Intent(
                this,
                CalificarConductorActivity::class.java
            ).apply {
                putExtra("VIAJE_ID", intent.getStringExtra("VIAJE_ID") ?: "")
                putExtra("ID_CONDUCTOR", intent.getStringExtra("ID_CONDUCTOR") ?: "")
                putExtra("NOMBRE_CONDUCTOR", conductor)
                putExtra("DESTINO", destino)
                putExtra("TARIFA", tarifa)
            }

            startActivity(intentCalificar)
        }

        btnInicio.setOnClickListener {

            val intent = Intent(
                this,
                PanelPasajeroActivity::class.java
            )

            startActivity(intent)

            finishAffinity()
        }
    }

    private fun cargarDesdeFirebase(viajeId: String) {
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        db.child("viajes").child(viajeId).get().addOnSuccessListener { v ->
            if (!v.exists()) return@addOnSuccessListener

            v.child("nombreConductor").value?.toString()?.let { if (it.isNotEmpty()) txtConductor.text = it }
            val dest = v.child("destino").value?.toString()
                ?: v.child("direccionDestino").value?.toString()
            if (!dest.isNullOrEmpty()) txtDestino.text = dest

            val tarifaFb = v.child("tarifa").getValue(Double::class.java)
                ?: v.child("precio").getValue(Double::class.java)
            if (tarifaFb != null && tarifaFb > 0) txtTarifa.text = "💵 S/ %.2f".format(tarifaFb)

            var dist = v.child("distancia").value?.toString()
            var tiem = v.child("tiempo").value?.toString()
            var pago = v.child("metodoPago").value?.toString()

            fun pintar() {
                if (!dist.isNullOrEmpty()) txtDistancia.text = "📍 Distancia recorrida: $dist"
                if (!tiem.isNullOrEmpty()) txtTiempo.text = "⏱ Tiempo total: $tiem"
                if (!pago.isNullOrEmpty()) txtMetodoPago.text = "Pago: $pago"
            }
            pintar()

            if (dist.isNullOrEmpty() || tiem.isNullOrEmpty() || pago.isNullOrEmpty()) {
                val idSolicitud = v.child("idSolicitud").value?.toString() ?: ""
                if (idSolicitud.isNotEmpty()) {
                    db.child("solicitudes").child(idSolicitud).get().addOnSuccessListener { s ->
                        if (dist.isNullOrEmpty()) dist = s.child("distancia").value?.toString()
                        if (tiem.isNullOrEmpty()) tiem = s.child("tiempo").value?.toString()
                        if (pago.isNullOrEmpty()) pago = s.child("metodoPago").value?.toString()
                        pintar()
                    }
                }
            }
        }
    }
}