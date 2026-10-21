package com.taxisign.pro.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.ChatPasajeroActivity
import com.taxisign.pro.R

class EstadoViajeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_viaje)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnMensaje = findViewById<LinearLayout>(R.id.btn_mensaje)
        val btnEmergencia = findViewById<LinearLayout>(R.id.btn_emergencia)

        btnBack.setOnClickListener {
            finish()
        }

        btnMensaje.setOnClickListener {

            val intent = Intent(this, ChatPasajeroActivity::class.java)
            startActivity(intent)

        }

        btnEmergencia.setOnClickListener {

            // Aquí irá EmergenciaActivity

        }
    }
}