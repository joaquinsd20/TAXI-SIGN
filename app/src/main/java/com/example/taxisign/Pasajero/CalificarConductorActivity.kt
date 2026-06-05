package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taxisign.pasajero.PanelPasajeroActivity
import com.example.taxisign.R

class CalificarConductorActivity : AppCompatActivity() {

    private var calificacion = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_calificar_conductor
        )

        val btnBack =
            findViewById<ImageButton>(
                R.id.btn_back
            )

        val btnEnviar =
            findViewById<LinearLayout>(
                R.id.btn_enviar
            )

        val star1 =
            findViewById<ImageView>(
                R.id.star1
            )

        val star2 =
            findViewById<ImageView>(
                R.id.star2
            )

        val star3 =
            findViewById<ImageView>(
                R.id.star3
            )

        val star4 =
            findViewById<ImageView>(
                R.id.star4
            )

        val star5 =
            findViewById<ImageView>(
                R.id.star5
            )

        val estrellas = listOf(
            star1,
            star2,
            star3,
            star4,
            star5
        )

        btnBack.setOnClickListener {

            finish()
        }

        // SELECCIONAR ESTRELLAS

        for (i in estrellas.indices) {

            estrellas[i]
                .setOnClickListener {

                    calificacion =
                        i + 1

                    for (
                    j in estrellas.indices
                    ) {

                        if (j <= i) {

                            estrellas[j]
                                .setColorFilter(

                                    ContextCompat
                                        .getColor(
                                            this,
                                            R.color.yellow_primary
                                        )
                                )

                        } else {

                            estrellas[j]
                                .setColorFilter(

                                    ContextCompat
                                        .getColor(
                                            this,
                                            android.R.color.darker_gray
                                        )
                                )
                        }
                    }
                }
        }

        // ENVIAR CALIFICACIÓN

        btnEnviar.setOnClickListener {

            if (
                calificacion == 0
            ) {

                Toast.makeText(
                    this,
                    "Selecciona una calificación",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Gracias por tu calificación",
                    Toast.LENGTH_SHORT
                ).show()

                val intent =
                    Intent(
                        this,
                        PanelPasajeroActivity::class.java
                    )

                startActivity(
                    intent
                )

                finish()
            }
        }
    }
}