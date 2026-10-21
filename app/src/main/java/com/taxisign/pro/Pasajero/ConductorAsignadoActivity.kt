package com.taxisign.pro.pasajero

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

class ConductorAsignadoActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    private var idSolicitud = ""
    private var idConductor = ""
    private var viajeId = ""
    private var chatId = ""

    private var nombreConductor = ""
    private var datosVehiculo = ""
    private var direccionDestino = ""
    private var precio = ""
    private var distancia = ""
    private var telefonoConductor = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conductor_asignado)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        idSolicitud = intent.getStringExtra("id_solicitud") ?: ""
        idConductor = intent.getStringExtra("id_conductor") ?: ""
        viajeId = intent.getStringExtra("viaje_id") ?: ""
        chatId = intent.getStringExtra("CHAT_ID") ?: viajeId

        nombreConductor = intent.getStringExtra("nombre_conductor")
            ?: "Conductor asignado"

        datosVehiculo = intent.getStringExtra("datos_vehiculo")
            ?: "Vehículo no registrado"

        direccionDestino = intent.getStringExtra("direccion_destino")
            ?: "Destino no especificado"

        precio = intent.getStringExtra("PRECIO") ?: "Por calcular"
        distancia = intent.getStringExtra("DISTANCIA") ?: "Por calcular"
        telefonoConductor = intent.getStringExtra("TELEFONO_CONDUCTOR") ?: ""

        val imagenConductor = findViewById<ImageView>(R.id.img_conductor)
        val btnSubirTaxi = findViewById<LinearLayout>(R.id.btn_subir_taxi)

        val tvNombre = findViewById<TextView>(R.id.tv_nombre_conductor)
        val tvVehiculo = findViewById<TextView>(R.id.tv_datos_vehiculo)
        val tvDestino = findViewById<TextView>(R.id.tv_direccion_destino)
        val tvPrecioDistancia = findViewById<TextView>(R.id.tv_precio_distancia)

        tvNombre.text = nombreConductor
        tvVehiculo.text = datosVehiculo
        tvDestino.text = direccionDestino
        tvPrecioDistancia.text = "$precio • $distancia"

        imagenConductor.setOnClickListener {
            vibrar()

            Toast.makeText(
                this,
                nombreConductor,
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSubirTaxi.setOnClickListener {
            vibrar()

            val intent = Intent(this, EsperaConductorActivity::class.java).apply {
                putExtra("VIAJE_ID", viajeId)
                putExtra("CHAT_ID", chatId)
                putExtra("ID_SOLICITUD", idSolicitud)
                putExtra("ID_CONDUCTOR", idConductor)
                putExtra("NOMBRE_CONDUCTOR", nombreConductor)
                putExtra("DATOS_VEHICULO", datosVehiculo)
                putExtra("DIRECCION_DESTINO", direccionDestino)
                putExtra("PRECIO", precio)
                putExtra("DISTANCIA", distancia)
                putExtra("TELEFONO_CONDUCTOR", telefonoConductor)
            }

            startActivity(intent)
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