package com.example.taxisign.conductor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.pasajero.HistorialViaje
import com.example.taxisign.R

class ActivityGanancias : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganancias)

        val btnBack = findViewById<ImageButton>(R.id.btnBackGanancias)
        val btnHistorial = findViewById<Button>(R.id.btnVerHistorial)
        val btnRetirar = findViewById<Button>(R.id.btnRetirar)

        // Volver al Mapa
        btnBack.setOnClickListener { finish() }

        // IR AL HISTORIAL (Aquí está la conexión que pediste)
        btnHistorial.setOnClickListener {
            val intent = Intent(this, HistorialViaje::class.java)
            startActivity(intent)
        }

        btnRetirar.setOnClickListener {
            Toast.makeText(this, "Enviando dinero a tu cuenta...", Toast.LENGTH_SHORT).show()
        }
    }
}