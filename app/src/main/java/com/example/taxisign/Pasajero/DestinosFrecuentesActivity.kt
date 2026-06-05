package com.example.taxisign.pasajero

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taxisign.R

class DestinosFrecuentesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_destinos_frecuentes)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val recycler = findViewById<RecyclerView>(R.id.recycler_destinos)

        btnBack.setOnClickListener {
            finish()
        }

        val listaDestinos = listOf(

            DestinoFrecuente(
                "Casa",
                "Av. América Oeste"
            ),

            DestinoFrecuente(
                "Universidad",
                "Universidad Nacional de Trujillo"
            ),

            DestinoFrecuente(
                "Trabajo",
                "Centro Histórico de Trujillo"
            ),

            DestinoFrecuente(
                "Mall Plaza",
                "Mall Plaza Trujillo"
            )

        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = DestinosFrecuentesAdapter(listaDestinos)

    }
}