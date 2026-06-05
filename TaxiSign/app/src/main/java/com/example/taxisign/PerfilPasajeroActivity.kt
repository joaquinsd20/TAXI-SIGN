package com.example.taxisign

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class PerfilPasajeroActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_pasajero)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar)
        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etCorreo = findViewById<EditText>(R.id.et_correo)

        // Botón para regresar a la pantalla anterior
        btnBack.setOnClickListener {
            vibrarBreve()
            finish()
        }

        // Lógica para guardar (Simulada)
        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                vibrarBreve()
                Toast.makeText(this, "Perfil de $nombre actualizado con éxito", Toast.LENGTH_SHORT).show()
                // Aquí conectarías con Firebase o SharedPreferences más adelante
            } else {
                Toast.makeText(this, "Por favor, ingresa un nombre", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrarBreve() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}