package com.example.taxisign.pasajero

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R

class HistorialViaje : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_viajes)

        val btnBack = findViewById<ImageButton>(R.id.btnBackHistorial)

        // Al presionar atrás aquí, regresas a la pantalla de Ganancias
        btnBack.setOnClickListener {
            finish()
        }
    }
}