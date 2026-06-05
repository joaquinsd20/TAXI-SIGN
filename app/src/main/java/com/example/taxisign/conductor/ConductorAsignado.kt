package com.example.taxisign.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.MensajesActivity
import com.example.taxisign.R

class ConductorAsignado : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.conductor_asignado)

        vibrator =
            getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val imagenConductor =
            findViewById<ImageView>(
                R.id.img_conductor
            )

        val btnSubirTaxi =
            findViewById<LinearLayout>(
                R.id.btn_subir_taxi
            )

        imagenConductor.setOnClickListener {

            vibrar()

            Toast.makeText(
                this,
                "Conductor asignado",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSubirTaxi.setOnClickListener {

            vibrar()

            Toast.makeText(
                this,
                "Viaje iniciado",
                Toast.LENGTH_SHORT
            ).show()

            val intent =
                Intent(
                    this,
                    MensajesActivity::class.java
                )

            startActivity(intent)
        }
    }

    private fun vibrar() {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        80,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")

                it.vibrate(80)
            }
        }
    }
}