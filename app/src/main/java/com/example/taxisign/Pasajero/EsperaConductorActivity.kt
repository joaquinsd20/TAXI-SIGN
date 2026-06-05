package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.MensajesActivity
import com.example.taxisign.R

class EsperaConductorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espera_conductor)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnMensaje = findViewById<LinearLayout>(R.id.btn_mensaje)
        val btnCancelar = findViewById<LinearLayout>(R.id.btn_cancelar)

        btnBack.setOnClickListener {
            finish()
        }

        btnMensaje.setOnClickListener {

            val intent = Intent(this, MensajesActivity::class.java)
            startActivity(intent)

        }

        btnCancelar.setOnClickListener {

            finish()

        }

    }
}