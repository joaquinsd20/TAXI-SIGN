package com.example.taxisign

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PanelPasajeroActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    private lateinit var progressUbicacion: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.panel_pasajero)

        vibrator =
            getSystemService(VIBRATOR_SERVICE) as? Vibrator

        progressUbicacion =
            findViewById(R.id.progress_ubicacion)

        progressUbicacion.visibility = View.GONE

        findViewById<ImageButton>(
            R.id.btn_menu
        ).setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Menú de usuario",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<LinearLayout>(
            R.id.layout_buscar
        ).setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Buscar destino",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<LinearLayout>(
            R.id.chip_casa
        ).setOnClickListener {

            vibrarConfirmacion()

            actualizarDestinoVisual("Casa")
        }

        findViewById<LinearLayout>(
            R.id.chip_trabajo
        ).setOnClickListener {

            vibrarConfirmacion()

            actualizarDestinoVisual("Trabajo")
        }

        findViewById<Button>(
            R.id.btn_pedir_taxi
        ).setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Conectando con un conductor...",
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<LinearLayout>(
            R.id.tab_mensajes
        ).setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Mensajes",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun actualizarDestinoVisual(
        destino: String
    ) {

        Toast.makeText(
            this,
            "Destino seleccionado: $destino",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun vibrarConfirmacion() {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        100,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")

                it.vibrate(100)
            }
        }
    }
}