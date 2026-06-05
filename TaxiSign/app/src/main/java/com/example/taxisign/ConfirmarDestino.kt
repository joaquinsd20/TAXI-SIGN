package com.example.taxisign

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ConfirmarDestino : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirmar_destino)

        findViewById<Button>(R.id.btn_confirmar_viaje).setOnClickListener {
            val intent = Intent(this, Inicio::class.java) // O a ConductorAsignado
            startActivity(intent)
        }
    }
}