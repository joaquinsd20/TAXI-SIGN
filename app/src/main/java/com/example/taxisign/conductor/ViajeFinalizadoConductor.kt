package com.example.taxisign.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R
import com.google.android.material.button.MaterialButton

class ViajeFinalizadoConductor : AppCompatActivity() {

    @Suppress("DEPRECATION")
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viaje_finalizado_conductor)

        // Recibir datos del viaje
        val nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"
        val monto          = intent.getStringExtra("PRECIO")          ?: "S/ 0.00"
        val distancia      = intent.getStringExtra("DISTANCIA")       ?: "-- km"
        val tiempo         = intent.getStringExtra("TIEMPO")          ?: "-- min"

        // Mostrar datos
        findViewById<TextView>(R.id.tv_monto).text           = monto
        findViewById<TextView>(R.id.tv_distancia).text       = distancia
        findViewById<TextView>(R.id.tv_tiempo).text          = tiempo
        findViewById<TextView>(R.id.tv_nombre_pasajero).text = nombrePasajero
        findViewById<TextView>(R.id.tv_avatar_pasajero).text =
            nombrePasajero.firstOrNull()?.uppercaseChar()?.toString() ?: "P"

        // Vibración de celebración al llegar
        vibrarCelebracion()

        // Calificar pasajero
        findViewById<MaterialButton>(R.id.btn_calificar).setOnClickListener {
            vibrar()
            val intent = Intent(this, CalificarPasajero::class.java)
            intent.putExtra("NOMBRE_PASAJERO", nombrePasajero)
            startActivity(intent)
        }

        // Volver a buscar viajes
        findViewById<android.widget.Button>(R.id.btn_volver_buscar).setOnClickListener {
            vibrar()
            Toast.makeText(this, "Buscando nuevo viaje...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PanelConductorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun vibrar() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(60)
    }

    private fun vibrarCelebracion() {
        if (!vibrator.hasVibrator()) return
        // Patrón: corto-corto-largo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 80, 60, 80, 60, 200), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 80, 60, 80, 60, 200), -1)
    }
}