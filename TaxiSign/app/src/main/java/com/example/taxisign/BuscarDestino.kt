package com.example.taxisign

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class BuscarDestino : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_destino)

        findViewById<LinearLayout>(R.id.item_destino_ejemplo).setOnClickListener {
            // Al seleccionar el destino, vamos a confirmar el precio y ruta
            val intent = Intent(this, ConfirmarDestino::class.java)
            startActivity(intent)
        }
    }
}