package com.example.taxisign.conductor

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
import com.example.taxisign.R

class PasajeroAsignado : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pasajero_asignado)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val imgConductor      = findViewById<ImageView>(R.id.img_conductor)
        val tvNombreConductor = findViewById<TextView>(R.id.tv_nombre_conductor)
        val btnSubirTaxi      = findViewById<LinearLayout>(R.id.btn_subir_taxi)

        // Recibir datos del viaje asignado
        val viajeId        = intent.getStringExtra("VIAJE_ID") ?: ""
        val nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        tvNombreConductor.text = nombrePasajero

        btnSubirTaxi.setOnClickListener {
            vibrarConfirmacion(80)
            Toast.makeText(this, "Iniciando ruta hacia el pasajero.", Toast.LENGTH_SHORT).show()

            // Conductor va a la pantalla de durante el viaje
            val intent = Intent(this, DuranteViajeConductor::class.java)
            intent.putExtra("VIAJE_ID", viajeId)
            intent.putExtra("NOMBRE_PASAJERO", nombrePasajero)
            startActivity(intent)
            finish()
        }
    }

    private fun vibrarConfirmacion(milisegundos: Long) {
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") it.vibrate(milisegundos)
            }
        }
    }
}