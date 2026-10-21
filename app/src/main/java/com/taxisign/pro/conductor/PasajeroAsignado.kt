package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R

class PasajeroAsignado : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    private var viajeId = ""
    private var nombrePasajero = ""
    private var telefonoPasajero = ""
    private var direccionRecojo = ""
    private var direccionDestino = ""
    private var distancia = ""
    private var tarifa = 0.0
    private var latRecojo = 0.0
    private var lngRecojo = 0.0
    private var destinoLat = 0.0
    private var destinoLng = 0.0
    private var esSordoMudo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pasajero_asignado)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"
        telefonoPasajero = intent.getStringExtra("TELEFONO_PASAJERO") ?: ""
        direccionRecojo = intent.getStringExtra("DIRECCION_RECOJO") ?: "Origen no especificado"
        direccionDestino = intent.getStringExtra("DIRECCION_DESTINO") ?: "Destino no especificado"
        distancia = intent.getStringExtra("DISTANCIA") ?: "Por calcular"
        tarifa = intent.getDoubleExtra("TARIFA", 0.0)
        latRecojo = intent.getDoubleExtra("LAT_RECOJO", 0.0)
        lngRecojo = intent.getDoubleExtra("LNG_RECOJO", 0.0)
        destinoLat = intent.getDoubleExtra("DESTINO_LAT", 0.0)
        destinoLng = intent.getDoubleExtra("DESTINO_LNG", 0.0)
        esSordoMudo = intent.getBooleanExtra("ES_SORDO_MUDO", false)

        val tvNombre = findViewById<TextView>(R.id.tv_nombre_pasajero)
        val tvTipo = findViewById<TextView>(R.id.tv_tipo_pasajero)
        val tvRecojo = findViewById<TextView>(R.id.tv_direccion_recojo)
        val tvPrecio = findViewById<TextView>(R.id.tv_precio_viaje)
        val tvDistancia = findViewById<TextView>(R.id.tv_distancia_viaje)
        val btnIrARecojo = findViewById<LinearLayout>(R.id.btn_subir_taxi)
        val imgPasajero = findViewById<ImageView>(R.id.img_conductor)

        tvNombre.text = nombrePasajero
        tvTipo.text = if (esSordoMudo) "Usuario SordoMudo" else "Usuario"
        tvRecojo.text = direccionRecojo
        tvPrecio.text = "Tarifa: S/ %.2f".format(tarifa)
        tvDistancia.text = "Distancia: $distancia"
        imgPasajero.setImageResource(R.drawable.ic_person)

        btnIrARecojo.setOnClickListener {
            vibrarConfirmacion(80)
            Toast.makeText(
                this,
                "Ruta de recojo iniciada hacia el pasajero.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, IrHaciaPasajero::class.java).apply {
                putExtra("VIAJE_ID", viajeId)
                putExtra("NOMBRE_PASAJERO", nombrePasajero)
                putExtra("TELEFONO_PASAJERO", telefonoPasajero)
                putExtra("DIRECCION_RECOJO", direccionRecojo)
                putExtra("DIRECCION_DESTINO", direccionDestino)
                putExtra("DISTANCIA", distancia)
                putExtra("TARIFA", tarifa)
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

    private fun vibrarConfirmacion(milisegundos: Long) {
        vibrator?.let {
            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        milisegundos,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(milisegundos)
            }
        }
    }
}