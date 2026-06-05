package com.example.taxisign.conductor

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R

class CalificarPasajero : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificar_pasajero)

        val ratingBar = findViewById<RatingBar>(R.id.ratingPasajero)
        val btnEnviar = findViewById<Button>(R.id.btnEnviarCalificacion)

        btnEnviar.setOnClickListener {
            val pts = ratingBar.rating
            Toast.makeText(this, "Calificaste con $pts estrellas", Toast.LENGTH_SHORT).show()
            finish() // Regresa al panel principal
        }
    }
}