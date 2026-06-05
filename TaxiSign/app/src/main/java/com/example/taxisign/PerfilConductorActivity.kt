package com.example.taxisign

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PerfilConductorActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_conductor)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val btnBack = findViewById<ImageButton>(R.id.btn_back_driver)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_driver)
        val etPlaca = findViewById<EditText>(R.id.et_placa)

        // Botón volver
        btnBack.setOnClickListener {
            vibrarBreve()
            finish()
        }

        // Lógica de actualización
        btnGuardar.setOnClickListener {
            val placa = etPlaca.text.toString().trim()

            if (placa.length >= 6) {
                vibrarBreve()
                Toast.makeText(this, "Datos del vehículo [$placa] actualizados", Toast.LENGTH_SHORT).show()
            } else {
                vibrarBreve()
                etPlaca.error = "Ingresa una placa válida"
            }
        }
    }

    private fun vibrarBreve() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}