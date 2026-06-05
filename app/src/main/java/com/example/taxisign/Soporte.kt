package com.example.taxisign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class Soporte : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soporte)

        findViewById<CardView>(R.id.btnSosEmergencia).setOnClickListener {
            // Llamar al número de emergencia
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:911")
            startActivity(intent)
        }

        // Botón regresar
        findViewById<ImageButton>(R.id.btnBackSoporte).setOnClickListener {
            finish()
        }
    }
}