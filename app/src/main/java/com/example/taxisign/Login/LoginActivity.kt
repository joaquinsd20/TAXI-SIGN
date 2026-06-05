package com.example.taxisign.Login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.taxisign.ApiConfig
// ARREGLADO: Cambiamos el import para que apunte a la carpeta real del conductor
import com.example.taxisign.conductor.PanelConductorActivity
import com.example.taxisign.SessionManager
import com.example.taxisign.pasajero.PanelPasajeroActivity
import com.example.taxisign.R
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val rol = intent.getStringExtra("ROL_SELECCIONADO") ?: "PASAJERO"

        val btnBack = findViewById<ImageButton>(R.id.btnBackLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<CardView>(R.id.btnGoogleLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPass)
        val tvIrARegistro = findViewById<TextView>(R.id.tvIrARegistro)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPassword)

        btnBack.setOnClickListener {
            finish()
        }

        tvForgot.setOnClickListener {
            vibrar(60)
            mostrarToast("Función disponible próximamente")
        }

        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, CrearCuenta::class.java)
            intent.putExtra("ROL_SELECCIONADO", rol)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            try {
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString().trim()

                if (email.isEmpty() || pass.isEmpty()) {
                    mostrarToast("Completa todos los campos")
                    vibrar(60)
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    mostrarToast("Correo inválido")
                    vibrar(60)
                    return@setOnClickListener
                }

                btnLogin.isEnabled = false

                val requestQueue = Volley.newRequestQueue(this)
                val jsonBody = JSONObject()
                jsonBody.put("correo", email)
                jsonBody.put("password", pass)
                jsonBody.put("rol", rol)

                val request = JsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.LOGIN,
                    jsonBody,
                    { response ->
                        val success = response.getBoolean("success")

                        if (success) {
                            SessionManager.idUsuario = response.getInt("id_usuario")
                            SessionManager.nombreUsuario = response.getString("nombre")
                            SessionManager.correoUsuario = response.getString("correo")
                            SessionManager.rolUsuario = response.getString("rol")

                            vibrar(120)
                            mostrarToast("Bienvenido ${SessionManager.nombreUsuario}")

                            val destinoActivity = if (SessionManager.rolUsuario == "CONDUCTOR") {
                                PanelConductorActivity::class.java
                            } else {
                                PanelPasajeroActivity::class.java
                            }

                            val intentPanel = Intent(this, destinoActivity)
                            intentPanel.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intentPanel)
                            finish()
                        } else {
                            mostrarToast(response.getString("message"))
                            vibrar(80)
                        }
                        btnLogin.isEnabled = true
                    },
                    { error ->
                        error.printStackTrace()
                        mostrarToast("Error de conexión")
                        btnLogin.isEnabled = true
                    }
                )
                requestQueue.add(request)

            } catch (e: Exception) {
                e.printStackTrace()
                mostrarToast("Ocurrió un error inesperado")
            }
        }

        btnGoogle.setOnClickListener {
            vibrar(80)
            mostrarToast("Google Login próximamente")
        }
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun vibrar(duracion: Long) {
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duracion, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duracion)
            }
        }
    }
}