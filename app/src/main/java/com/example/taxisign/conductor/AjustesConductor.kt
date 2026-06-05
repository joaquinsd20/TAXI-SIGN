package com.example.taxisign.conductor

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.taxisign.R

class AjustesConductor : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes_conductor)

        // Referencia al switch con el ID que acabamos de crear en el XML
        val switchAuto = findViewById<SwitchCompat>(R.id.switchAutoAccept)
        val btnBack = findViewById<ImageButton>(R.id.btnBackAjustes)

        // Lógica del Switch
        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Modo manos libres activado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Aceptación manual activada", Toast.LENGTH_SHORT).show()
            }
        }

        // Volver al Panel
        btnBack.setOnClickListener {
            finish()
        }
    }
}