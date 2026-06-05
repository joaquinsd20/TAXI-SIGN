package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.MensajesActivity
import com.example.taxisign.R

class DuranteViajePasajeroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_durante_viaje_pasajero)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnMensaje = findViewById<LinearLayout>(R.id.btn_mensaje)
        val btnCompartir = findViewById<LinearLayout>(R.id.btn_compartir)
        val btnEmergencia = findViewById<LinearLayout>(R.id.btn_emergencia)

        btnBack.setOnClickListener {
            finish()
        }

        btnMensaje.setOnClickListener {

            val intent = Intent(this, MensajesActivity::class.java)
            startActivity(intent)

        }

        btnCompartir.setOnClickListener {

            // Compartir viaje

        }

        btnEmergencia.setOnClickListener {

            // Emergencia

        }

    }
}