package com.example.taxisign

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class Inicio : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_pasajero) // Aquí usamos el diseño de Home que te gustó

        // Botón principal de tu imagen: PEDIR TAXI
        findViewById<Button>(R.id.btn_pedir_taxi).setOnClickListener {
            val intent = Intent(this, BuscarDestino::class.java)
            startActivity(intent)
        }

        // Acceso al perfil
        findViewById<LinearLayout>(R.id.tab_perfil).setOnClickListener {
            val intent = Intent(this, PerfilPasajeroActivity::class.java)
            startActivity(intent)
        }
    }
}