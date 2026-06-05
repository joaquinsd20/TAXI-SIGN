package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.android.volley.Request.Method
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

import com.example.taxisign.ApiConfig
import com.example.taxisign.R

import org.json.JSONObject

class BuscarConductorActivity : AppCompatActivity() {

    private lateinit var txtDestinoSeleccionado: TextView

    private var vibrator: Vibrator? = null

    private var idSolicitud = 0

    private val handler =
        Handler(Looper.getMainLooper())

    private val verificarRunnable =
        object : Runnable {

            override fun run() {

                verificarEstado()

                handler.postDelayed(
                    this,
                    3000
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.buscar_conductor)

        txtDestinoSeleccionado =
            findViewById(
                R.id.txtDestinoSeleccionado
            )

        val destino =
            intent.getStringExtra(
                "destino"
            ) ?: "Destino no definido"

        txtDestinoSeleccionado.text =
            destino

        idSolicitud =
            intent.getIntExtra(
                "id_solicitud",
                0
            )

        vibrator =
            getSystemService(
                VIBRATOR_SERVICE
            ) as? Vibrator

        val radar =
            findViewById<ImageView>(
                R.id.iv_radar
            )

        val animacionRadar =
            AnimationUtils.loadAnimation(
                this,
                R.anim.rotate_radar
            )

        radar.startAnimation(
            animacionRadar
        )

        findViewById<ImageButton>(
            R.id.btn_back_busqueda
        ).setOnClickListener {

            vibrar()

            finish()
        }

        findViewById<LinearLayout>(
            R.id.btn_cancelar_busqueda
        ).setOnClickListener {

            vibrar()

            cancelarSolicitud()
        }

        handler.post(
            verificarRunnable
        )
    }
    private fun cancelarSolicitud() {

        val request = object : StringRequest(
            Method.POST,
            ApiConfig.CANCELAR_SOLICITUD,

            { response ->

                try {

                    val json =
                        JSONObject(response)

                    if (json.getBoolean("success")) {

                        Toast.makeText(
                            this,
                            "Solicitud cancelada",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } else {

                        Toast.makeText(
                            this,
                            "No se pudo cancelar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            },

            {

                Toast.makeText(
                    this,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            }

        ) {

            override fun getParams(): MutableMap<String, String> {

                return hashMapOf(

                    "id_solicitud" to
                            idSolicitud.toString()
                )
            }
        }

        Volley.newRequestQueue(this)
            .add(request)
    }
    private fun verificarEstado() {

        val request =
            object : StringRequest(
                Method.POST,
                ApiConfig.VERIFICAR_ESTADO,

                { response ->

                    try {

                        val json =
                            JSONObject(response)

                        if (
                            json.getBoolean(
                                "success"
                            )
                        ) {

                            val estado =
                                json.getString(
                                    "estado"
                                )

                            if (
                                estado == "aceptada"
                            ) {

                                handler.removeCallbacks(
                                    verificarRunnable
                                )

                                startActivity(
                                    Intent(
                                        this,
                                        ConductorAsignadoActivity::class.java
                                    )
                                )

                                finish()
                            }
                        }

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }
                },

                { error ->

                    error.printStackTrace()
                }

            ) {

                override fun getParams():
                        MutableMap<String, String> {

                    val params =
                        HashMap<String, String>()

                    params["id_solicitud"] =
                        idSolicitud.toString()

                    return params
                }
            }

        Volley
            .newRequestQueue(this)
            .add(request)
    }

    private fun vibrar() {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

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

    override fun onDestroy() {

        handler.removeCallbacks(
            verificarRunnable
        )

        super.onDestroy()
    }
}