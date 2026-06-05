package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R
import com.example.taxisign.SessionManager

class PerfilPasajeroActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.perfil_pasajero)

        vibrator =
            getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val btnBack =
            findViewById<ImageButton>(R.id.btn_back)

        val btnGuardar =
            findViewById<LinearLayout>(R.id.btn_guardar)

        val etNombre =
            findViewById<EditText>(R.id.et_nombre)

        val etCorreo =
            findViewById<EditText>(R.id.et_correo)

        val etTelefono =
            findViewById<EditText>(R.id.et_telefono)

        val etDni =
            findViewById<EditText>(R.id.et_dni)

        val etCiudad =
            findViewById<EditText>(R.id.et_ciudad)

        // Cargar datos guardados
        etNombre.setText(SessionManager.nombreUsuario)

        etCorreo.setText(SessionManager.correoUsuario)

        etTelefono.setText(SessionManager.telefonoUsuario)

        etDni.setText(SessionManager.dniUsuario)

        etCiudad.setText(SessionManager.ciudadUsuario)

        btnBack.setOnClickListener {

            vibrarBreve()

            finish()
        }

        btnGuardar.setOnClickListener {

            val nombre =
                etNombre.text.toString().trim()

            val correo =
                etCorreo.text.toString().trim()

            val telefono =
                etTelefono.text.toString().trim()

            val dni =
                etDni.text.toString().trim()

            val ciudad =
                etCiudad.text.toString().trim()

            // Validar campos vacíos
            if (
                nombre.isEmpty() ||
                correo.isEmpty() ||
                telefono.isEmpty() ||
                dni.isEmpty() ||
                ciudad.isEmpty()
            ) {

                vibrarBreve()

                Toast.makeText(
                    this,
                    "Completa toda la información",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Validar correo
            if (
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(correo)
                    .matches()
            ) {

                vibrarBreve()

                Toast.makeText(
                    this,
                    "Correo inválido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Validar teléfono
            if (telefono.length < 9) {

                vibrarBreve()

                Toast.makeText(
                    this,
                    "Número de teléfono inválido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Validar DNI
            if (dni.length != 8) {

                vibrarBreve()

                Toast.makeText(
                    this,
                    "DNI inválido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Guardar datos
            SessionManager.nombreUsuario =
                nombre

            SessionManager.correoUsuario =
                correo

            SessionManager.telefonoUsuario =
                telefono

            SessionManager.dniUsuario =
                dni

            SessionManager.ciudadUsuario =
                ciudad

            vibrarBreve()

            Toast.makeText(
                this,
                "Perfil actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            val intent =
                Intent(
                    this,
                    PanelPasajeroActivity::class.java
                )

            startActivity(intent)

            finish()
        }
    }

    private fun vibrarBreve() {

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