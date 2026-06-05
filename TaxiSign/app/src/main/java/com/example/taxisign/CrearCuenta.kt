package com.example.taxisign

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
import com.google.android.material.textfield.TextInputEditText

class CrearCuenta : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_cuenta)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val tvTitulo = findViewById<TextView>(R.id.tvTitulo)

        val texto = SpannableString("Crear Cuenta")

        texto.setSpan(
            ForegroundColorSpan(Color.parseColor("#FFD600")),
            6,
            texto.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvTitulo.text = texto

        val rol = intent.getStringExtra("ROL_SELECCIONADO") ?: "PASAJERO"

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarAhora)
        val tvYaTengoCuenta = findViewById<TextView>(R.id.tvYaTengoCuenta)

        val etNombre = findViewById<TextInputEditText>(R.id.etNombreCompleto)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmailRegistro)
        val etPass = findViewById<TextInputEditText>(R.id.etPassRegistro)

        btnVolver.setOnClickListener {
            finish()
        }

        tvYaTengoCuenta.setOnClickListener {
            finish()
        }

        btnRegistrar.setOnClickListener {

            try {

                val nombre = etNombre.text?.toString()?.trim() ?: ""
                val email = etEmail.text?.toString()?.trim() ?: ""
                val pass = etPass.text?.toString()?.trim() ?: ""

                if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {

                    mostrarToast("Completa todos los campos")
                    vibrar(60)

                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                    mostrarToast("Correo electrónico inválido")
                    vibrar(60)

                    return@setOnClickListener
                }

                if (pass.length < 6) {

                    mostrarToast("La contraseña debe tener mínimo 6 caracteres")
                    vibrar(60)

                    return@setOnClickListener
                }

                btnRegistrar.isEnabled = false
                SessionManager.nombreUsuario = nombre
                SessionManager.correoUsuario = email
                SessionManager.passwordUsuario = pass
                SessionManager.rolUsuario = rol

                vibrar(120)

                mostrarToast("Cuenta creada con éxito")

                val intentLogin =
                    Intent(this, LoginActivity::class.java)

                intentLogin.putExtra(
                    "ROL_SELECCIONADO",
                    rol
                )

                startActivity(intentLogin)

                finish()

            } catch (e: Exception) {

                e.printStackTrace()

                mostrarToast("Ocurrió un error inesperado")
            }
        }
    }

    private fun mostrarToast(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun vibrar(duracion: Long) {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        duracion,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")
                it.vibrate(duracion)
            }
        }
    }
}