package com.example.taxisign.Login

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.taxisign.ApiConfig
import com.example.taxisign.R
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class CrearCuenta : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.crear_cuenta)

        vibrator =
            getSystemService(
                VIBRATOR_SERVICE
            ) as? Vibrator

        val tvTitulo =
            findViewById<TextView>(
                R.id.tvTitulo
            )

        val texto =
            SpannableString(
                "Crear Cuenta"
            )

        texto.setSpan(
            ForegroundColorSpan(
                Color.parseColor(
                    "#FFD600"
                )
            ),
            6,
            texto.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvTitulo.text = texto

        val rol =
            intent.getStringExtra(
                "ROL_SELECCIONADO"
            ) ?: "PASAJERO"

        val btnVolver =
            findViewById<ImageButton>(
                R.id.btnVolverRegistro
            )

        val btnRegistrar =
            findViewById<Button>(
                R.id.btnRegistrarAhora
            )

        val tvYaTengoCuenta =
            findViewById<TextView>(
                R.id.tvYaTengoCuenta
            )

        val etNombre =
            findViewById<TextInputEditText>(
                R.id.etNombreCompleto
            )

        val etEmail =
            findViewById<TextInputEditText>(
                R.id.etEmailRegistro
            )

        val etPass =
            findViewById<TextInputEditText>(
                R.id.etPassRegistro
            )

        btnVolver.setOnClickListener {

            finish()
        }

        tvYaTengoCuenta.setOnClickListener {

            finish()
        }

        btnRegistrar.setOnClickListener {

            val nombre =
                etNombre.text
                    .toString()
                    .trim()

            val email =
                etEmail.text
                    .toString()
                    .trim()

            val pass =
                etPass.text
                    .toString()
                    .trim()

            // VALIDAR CAMPOS VACÍOS

            if (
                nombre.isEmpty() ||
                email.isEmpty() ||
                pass.isEmpty()
            ) {

                mostrarToast(
                    "Completa todos los campos"
                )

                vibrar(60)

                return@setOnClickListener
            }

            // VALIDAR NOMBRE

            if (
                nombre.length < 3
            ) {

                mostrarToast(
                    "El nombre es demasiado corto"
                )

                vibrar(60)

                return@setOnClickListener
            }

            if (
                !nombre.matches(
                    Regex(
                        "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$"
                    )
                )
            ) {

                mostrarToast(
                    "El nombre solo debe contener letras"
                )

                vibrar(60)

                return@setOnClickListener
            }

            // VALIDAR CORREO

            if (
                !Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                mostrarToast(
                    "Correo electrónico inválido"
                )

                vibrar(60)

                return@setOnClickListener
            }

            // VALIDAR CONTRASEÑA

            val passwordRegex =
                Regex(
                    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
                )

            if (
                !pass.matches(passwordRegex)
            ) {

                mostrarToast(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número"
                )

                vibrar(60)

                return@setOnClickListener
            }

            btnRegistrar.isEnabled =
                false

            val requestQueue =
                Volley.newRequestQueue(
                    this
                )

            val jsonBody =
                JSONObject()

            jsonBody.put(
                "nombre",
                nombre
            )

            jsonBody.put(
                "correo",
                email
            )

            jsonBody.put(
                "password",
                pass
            )

            jsonBody.put(
                "rol",
                rol
            )

            val request =
                JsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.REGISTER,
                    jsonBody,

                    { response ->

                        val success =
                            response.getBoolean(
                                "success"
                            )

                        val message =
                            response.getString(
                                "message"
                            )

                        mostrarToast(
                            message
                        )

                        if (success) {

                            vibrar(120)

                            val intentLogin =
                                Intent(
                                    this,
                                    LoginActivity::class.java
                                )

                            intentLogin.putExtra(
                                "ROL_SELECCIONADO",
                                rol
                            )

                            startActivity(
                                intentLogin
                            )

                            finish()
                        }

                        btnRegistrar.isEnabled =
                            true
                    },

                    { error ->

                        error.printStackTrace()

                        mostrarToast(
                            "Error de conexión"
                        )

                        btnRegistrar.isEnabled =
                            true
                    }
                )

            requestQueue.add(
                request
            )
        }
    }

    private fun mostrarToast(
        mensaje: String
    ) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun vibrar(
        duracion: Long
    ) {

        vibrator?.let {

            if (
                !it.hasVibrator()
            ) return

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        duracion,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress(
                    "DEPRECATION"
                )

                it.vibrate(
                    duracion
                )
            }
        }
    }
}