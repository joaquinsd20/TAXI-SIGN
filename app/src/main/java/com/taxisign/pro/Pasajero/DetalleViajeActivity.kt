package com.taxisign.pro.pasajero

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.database.FirebaseDatabase

/**
 * DetalleViajeActivity
 *
 * Muestra el detalle de un viaje del historial. Sirve para AMBOS roles
 * (pasajero y conductor) porque siempre muestra los dos nombres.
 *
 * Recibe por Intent los datos básicos (para pintar de inmediato) y, si llega
 * un VIAJE_ID, refresca con la información completa desde Firebase: viajes/{id}
 */
class DetalleViajeActivity : AppCompatActivity() {

    private lateinit var txtConductor: TextView
    private lateinit var txtPasajero: TextView
    private lateinit var txtOrigen: TextView
    private lateinit var txtDestino: TextView
    private lateinit var txtTiempo: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtFecha: TextView
    private lateinit var txtTarifa: TextView
    private lateinit var txtEstado: TextView

    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_viaje)

        txtConductor = findViewById(R.id.txt_conductor)
        txtPasajero = findViewById(R.id.txt_pasajero)
        txtOrigen = findViewById(R.id.txt_origen)
        txtDestino = findViewById(R.id.txt_destino)
        txtTiempo = findViewById(R.id.txt_tiempo)
        txtDistancia = findViewById(R.id.txt_distancia)
        txtFecha = findViewById(R.id.txt_fecha)
        txtTarifa = findViewById(R.id.txt_tarifa)
        txtEstado = findViewById(R.id.txt_estado)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // 1) Pintado inmediato con lo que vino del historial
        val viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        txtConductor.text = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"
        txtPasajero.text = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"
        txtOrigen.text = intent.getStringExtra("ORIGEN")?.ifEmpty { "—" } ?: "—"
        txtDestino.text = intent.getStringExtra("DESTINO") ?: "—"
        txtFecha.text = intent.getStringExtra("FECHA") ?: "—"
        txtEstado.text = formatearEstado(intent.getStringExtra("ESTADO") ?: "")
        txtTarifa.text = "S/ %.2f".format(intent.getDoubleExtra("TARIFA", 0.0))

        // 2) Refresco completo desde Firebase
        if (viajeId.isNotEmpty()) cargarDesdeFirebase(viajeId)
    }

    private fun cargarDesdeFirebase(viajeId: String) {
        dbRoot.child("viajes").child(viajeId).get()
            .addOnSuccessListener { s ->
                if (!s.exists()) return@addOnSuccessListener

                txtConductor.text = s.child("nombreConductor").value?.toString()
                    ?: txtConductor.text
                txtPasajero.text = s.child("nombrePasajero").value?.toString()
                    ?: txtPasajero.text

                val origen = s.child("origen").value?.toString()
                    ?: s.child("direccionRecojo").value?.toString()
                if (!origen.isNullOrEmpty()) txtOrigen.text = origen

                val destino = s.child("destino").value?.toString()
                    ?: s.child("direccionDestino").value?.toString()
                if (!destino.isNullOrEmpty()) txtDestino.text = destino

                val tarifa = s.child("tarifa").getValue(Double::class.java)
                if (tarifa != null) txtTarifa.text = "S/ %.2f".format(tarifa)

                val estado = s.child("estado").value?.toString() ?: ""
                if (estado.isNotEmpty()) txtEstado.text = formatearEstado(estado)

                val ts = s.child("timestamp").getValue(Long::class.java) ?: 0L
                if (ts > 0) {
                    val sdf = java.text.SimpleDateFormat(
                        "dd/MM/yyyy - hh:mm a", java.util.Locale.getDefault()
                    )
                    txtFecha.text = sdf.format(java.util.Date(ts))
                }

                // tiempo/distancia: el viaje no siempre los guarda; usamos lo disponible
                val tiempo = s.child("tiempo").value?.toString()
                    ?: s.child("tiempoMin").value?.toString()
                txtTiempo.text = tiempo?.let { if (it.contains("min")) it else "$it min" } ?: "—"

                val distancia = s.child("distancia").value?.toString()
                    ?: s.child("distanciaKm").value?.toString()
                txtDistancia.text = distancia?.let { if (it.contains("km")) it else "$it km" } ?: "—"
            }
    }

    private fun formatearEstado(estado: String): String = when (estado.lowercase()) {
        "finalizado" -> "Viaje completado"
        "cancelado", "cancelada", "cancelado_pasajero", "cancelado_conductor" -> "Viaje cancelado"
        "conductor_asignado" -> "Conductor asignado"
        "pendiente" -> "Pendiente"
        "" -> "—"
        else -> estado.replaceFirstChar { it.uppercase() }
    }
}
