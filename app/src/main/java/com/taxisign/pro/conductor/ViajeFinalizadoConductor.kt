package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.android.material.button.MaterialButton

class ViajeFinalizadoConductor : AppCompatActivity() {

    @Suppress("DEPRECATION")
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }

    private var viajeId = ""
    private var pasajeroId = ""
    private var nombrePasajero = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viaje_finalizado_conductor)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        pasajeroId = intent.getStringExtra("PASAJERO_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        val monto = intent.getStringExtra("PRECIO") ?: "S/ 0.00"
        val distancia = intent.getStringExtra("DISTANCIA") ?: "-- km"
        val tiempo = intent.getStringExtra("TIEMPO") ?: "-- min"

        findViewById<TextView>(R.id.tv_monto).text = monto
        findViewById<TextView>(R.id.tv_distancia).text = distancia
        findViewById<TextView>(R.id.tv_tiempo).text = tiempo
        findViewById<TextView>(R.id.tv_nombre_pasajero).text = nombrePasajero
        findViewById<TextView>(R.id.tv_avatar_pasajero).text =
            nombrePasajero.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        findViewById<TextView>(R.id.tv_nombre_conductor).text =
            "Conductor: " + com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Conductor" }

        if (viajeId.isNotEmpty()) cargarDatosRealesPasajero()

        vibrarCelebracion()

        findViewById<MaterialButton>(R.id.btn_calificar).setOnClickListener {
            vibrar()

            val intent = Intent(this, CalificarPasajero::class.java).apply {
                putExtra("VIAJE_ID", viajeId)
                putExtra("PASAJERO_ID", pasajeroId)
                putExtra("NOMBRE_PASAJERO", nombrePasajero)
            }

            startActivity(intent)
        }

        findViewById<android.widget.Button>(R.id.btn_volver_buscar).setOnClickListener {
            vibrar()

            Toast.makeText(this, "Buscando nuevo viaje...", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, PanelConductorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            startActivity(intent)
            finish()
        }
    }

    private fun vibrar() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    60,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }

    private fun cargarDatosRealesPasajero() {
        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("viajes").child(viajeId).get()
            .addOnSuccessListener { v ->
                if (!v.exists()) return@addOnSuccessListener
                val nom = v.child("nombrePasajero").value?.toString()
                if (!nom.isNullOrEmpty()) {
                    nombrePasajero = nom
                    findViewById<TextView>(R.id.tv_nombre_pasajero).text = nom
                    findViewById<TextView>(R.id.tv_avatar_pasajero).text =
                        nom.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                }
                val tarifaFb = v.child("tarifa").getValue(Double::class.java)
                    ?: v.child("precio").getValue(Double::class.java)
                if (tarifaFb != null && tarifaFb > 0)
                    findViewById<TextView>(R.id.tv_monto).text = "S/ %.2f".format(tarifaFb)

                v.child("distancia").value?.toString()?.let {
                    if (it.isNotEmpty()) findViewById<TextView>(R.id.tv_distancia).text = it
                }
                v.child("tiempo").value?.toString()?.let {
                    if (it.isNotEmpty()) findViewById<TextView>(R.id.tv_tiempo).text = it
                }
                if (pasajeroId.isEmpty())
                    pasajeroId = v.child("pasajeroId").value?.toString() ?: ""
            }
    }

    private fun vibrarCelebracion() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 60, 80, 60, 200),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 80, 60, 80, 60, 200), -1)
        }
    }
}