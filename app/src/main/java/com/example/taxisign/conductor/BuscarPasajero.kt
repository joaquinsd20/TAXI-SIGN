package com.example.taxisign.conductor

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R
import com.example.taxisign.conductor.SolicitudDetalleConductor
import kotlin.random.Random

class BuscarPasajero : AppCompatActivity() {

    private val handlerRadar = Handler(Looper.getMainLooper())
    private lateinit var runnableRadar: Runnable

    private val INTERVALO_CHEQUEO = 4000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_pasajeros)

        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_busqueda)
        btnCancelar.setOnClickListener {
            detenerRadar()
            Toast.makeText(this, "Búsqueda pausada", Toast.LENGTH_SHORT).show()
            finish()
        }

        iniciarRadarInifinito()
    }

    private fun iniciarRadarInifinito() {
        runnableRadar = object : Runnable {
            override fun run() {
                if (isFinishing) return

                val seEncontroPasajero = Random.nextInt(100) < 40

                if (seEncontroPasajero) {
                    vibrateNotification()
                    Toast.makeText(this@BuscarPasajero, "¡Nueva solicitud entrante!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@BuscarPasajero, SolicitudDetalleConductor::class.java)
                    startActivity(intent)

                    finish()
                } else {
                    handlerRadar.postDelayed(this, INTERVALO_CHEQUEO)
                }
            }
        }

        handlerRadar.postDelayed(runnableRadar, INTERVALO_CHEQUEO)
    }

    private fun detenerRadar() {
        if (::runnableRadar.isInitialized) {
            handlerRadar.removeCallbacks(runnableRadar)
        }
    }


    private fun vibrateNotification() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator
        vibrator?.let {
            if (it.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(150)
                }
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        detenerRadar()
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerRadar()
    }
}