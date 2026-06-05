package com.example.taxisign.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taxisign.ChatConductorActivity
import com.example.taxisign.R

class DuranteViajeConductor : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    // El viajeId se usa como chatId — mismo ID para ambos
    private lateinit var viajeId: String
    private lateinit var nombrePasajero: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.duranteviaje_conductor)

        // Recibir datos del viaje
        viajeId        = intent.getStringExtra("VIAJE_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarViaje)
        val btnSos       = findViewById<CardView>(R.id.btnSos)
        val btnChat      = findViewById<ImageButton>(R.id.btnMensajePasajero)

        // 1. SOS
        btnSos.setOnClickListener {
            vibrarLargo()
            Toast.makeText(this, "¡ALERTA SOS ACTIVADA!", Toast.LENGTH_LONG).show()
        }

        // 2. Chat — abre ChatConductorActivity con el viajeId como chatId
        btnChat.setOnClickListener {
            vibrarBreve()

            if (viajeId.isEmpty()) {
                Toast.makeText(this, "Error: viaje no iniciado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ChatConductorActivity::class.java)
            intent.putExtra("CHAT_ID", viajeId)          // viajeId == chatId
            intent.putExtra("NOMBRE_PASAJERO", nombrePasajero)
            startActivity(intent)
        }

        // 3. Finalizar viaje
        btnFinalizar.setOnClickListener {
            vibrarBreve()
            mostrarDialogoFinalizar()
        }
    }

    private fun mostrarDialogoFinalizar() {
        AlertDialog.Builder(this)
            .setTitle("¿Llegaste al destino?")
            .setMessage("Confirma que el viaje ha terminado para procesar el pago.")
            .setPositiveButton("SÍ, FINALIZAR") { _, _ ->
                Toast.makeText(this, "Viaje completado con éxito", Toast.LENGTH_SHORT).show()

                // 1. Crear el Intent para ir a ViajeConductor
                val intent = Intent(this, ViajeFinalizadoConductor::class.java)

                // 2. Opcional: Limpiar el stack de actividades para que el usuario no pueda volver atrás
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // 3. Iniciar la actividad
                startActivity(intent)

                // 4. Finalizar la actividad actual
                finish()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }
    private fun vibrarBreve() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(50)
    }

    private fun vibrarLargo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(500)
    }
}