package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.Inicio
import com.example.taxisign.R

class ConfirmacionViajeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_viaje)

        findViewById<Button>(R.id.btn_buscar_conductor).setOnClickListener {
            val intent = Intent(this, Inicio::class.java) // O a ConductorAsignado
            startActivity(intent)
        }
    }
}