package com.example.taxisign

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taxisign.Login.LoginActivity

class MainActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        try {

            setContentView(R.layout.activity_main)

            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

            val cardPasajero =
                findViewById<CardView>(R.id.cardPasajero)

            val cardConductor =
                findViewById<CardView>(R.id.cardConductor)

            val animEntrada =
                AnimationUtils.loadAnimation(
                    this,
                    android.R.anim.fade_in
                )

            cardPasajero.startAnimation(animEntrada)
            cardConductor.startAnimation(animEntrada)

            cardPasajero.setOnClickListener {

                abrirLogin("PASAJERO")
            }

            cardConductor.setOnClickListener {

                abrirLogin("CONDUCTOR")
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al iniciar pantalla principal",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirLogin(rol: String) {

        try {

            vibrarConfirmacion()

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            intent.putExtra(
                "ROL_SELECCIONADO",
                rol
            )

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al abrir Login",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun vibrarConfirmacion() {

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