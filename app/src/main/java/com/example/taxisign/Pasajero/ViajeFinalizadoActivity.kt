package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R

class ViajeFinalizadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viaje_finalizado)

        val btnCalificar = findViewById<LinearLayout>(R.id.btn_calificar)
        val btnInicio = findViewById<LinearLayout>(R.id.btn_inicio)

        btnCalificar.setOnClickListener {

            val intent = Intent(this, CalificarConductorActivity::class.java)
            startActivity(intent)

        }

        btnInicio.setOnClickListener {

            val intent = Intent(this, PanelPasajeroActivity::class.java)
            startActivity(intent)

            finish()

        }

    }
}