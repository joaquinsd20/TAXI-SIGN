package com.example.taxisign

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MensajesActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mensajes)

        vibrator =
            getSystemService(VIBRATOR_SERVICE) as? Vibrator

        findViewById<ImageButton>(
            R.id.btn_back_chat
        ).setOnClickListener {

            finish()
        }

        findViewById<LinearLayout>(
            R.id.msg_llegando
        ).setOnClickListener {

            enviarMensaje(
                "Estoy llegando"
            )
        }

        findViewById<LinearLayout>(
            R.id.msg_esperame
        ).setOnClickListener {

            enviarMensaje(
                "Espéreme por favor"
            )
        }

        findViewById<LinearLayout>(
            R.id.msg_nopuedo
        ).setOnClickListener {

            enviarMensaje(
                "No puedo hablar"
            )
        }

        findViewById<LinearLayout>(
            R.id.msg_afuera
        ).setOnClickListener {

            enviarMensaje(
                "Estoy afuera"
            )
        }

        findViewById<LinearLayout>(
            R.id.msg_gracias
        ).setOnClickListener {

            enviarMensaje(
                "Gracias"
            )
        }
    }

    private fun enviarMensaje(
        mensaje: String
    ) {

        vibrar()

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun vibrar() {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        60,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")

                it.vibrate(60)
            }
        }
    }
}