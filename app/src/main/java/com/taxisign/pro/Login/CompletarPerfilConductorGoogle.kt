package com.taxisign.pro.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth

/**
 * CompletarPerfilConductorGoogle
 *
 * Cuando un conductor NUEVO inicia sesión con Google, Firebase Auth ya
 * crea la cuenta automáticamente (con su nombre y correo), pero todavía
 * no tenemos sus datos de conductor: DNI, teléfono, placa, modelo y
 * color del vehículo.
 *
 * Esta pantalla intermedia completa esos datos y luego continúa hacia
 * ValidarDocumentosConductor para subir brevete, SOAT y antecedentes,
 * tal como en el registro tradicional con correo y contraseña.
 *
 * Como el usuario YA está autenticado en Firebase Auth (vía Google), NO
 * se le pide contraseña aquí ni se vuelve a crear una cuenta: solo se
 * reutiliza el UID existente.
 */
class CompletarPerfilConductorGoogle : AppCompatActivity() {

    private val mAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var btnVolver: ImageButton
    private lateinit var tvSaludo: TextView
    private lateinit var etDni: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etPlaca: TextInputEditText
    private lateinit var etModelo: TextInputEditText
    private lateinit var etColor: TextInputEditText
    private lateinit var btnSiguiente: Button

    private var uidGoogle: String = ""
    private var nombreGoogle: String = ""
    private var emailGoogle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.completar_perfil_conductor_google)

        uidGoogle = intent.getStringExtra("EXTRA_UID_GOOGLE")
            ?: mAuth.currentUser?.uid ?: ""
        nombreGoogle = intent.getStringExtra("EXTRA_NOMBRE")
            ?: mAuth.currentUser?.displayName ?: "Conductor"
        emailGoogle = intent.getStringExtra("EXTRA_EMAIL")
            ?: mAuth.currentUser?.email ?: ""

        initViews()

        tvSaludo.text = "¡Hola, ${nombreGoogle.substringBefore(" ")}!"

        btnVolver.setOnClickListener { confirmarSalida() }

        // Bloqueamos el botón físico de retroceso para que no deje el
        // registro a medias sin avisar (ya inició sesión con Google).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmarSalida()
            }
        })

        btnSiguiente.setOnClickListener {
            validarYContinuar()
        }
    }

    private fun initViews() {
        btnVolver = findViewById(R.id.btnVolverCompletarPerfil)
        tvSaludo = findViewById(R.id.tvSaludoGoogle)
        etDni = findViewById(R.id.etDniGoogleConductor)
        etTelefono = findViewById(R.id.etTelefonoGoogleConductor)
        etPlaca = findViewById(R.id.etPlacaGoogleConductor)
        etModelo = findViewById(R.id.etModeloGoogleConductor)
        etColor = findViewById(R.id.etColorGoogleConductor)
        btnSiguiente = findViewById(R.id.btnSiguienteCompletarPerfil)
    }

    private fun validarYContinuar() {
        val dni = etDni.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val placa = etPlaca.text.toString().trim().uppercase()
        val modelo = etModelo.text.toString().trim()
        val color = etColor.text.toString().trim()

        if (dni.isEmpty() || telefono.isEmpty() || placa.isEmpty() || modelo.isEmpty() || color.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (dni.length != 8) {
            etDni.error = "El DNI debe tener exactamente 8 dígitos"
            etDni.requestFocus()
            return
        }

        if (telefono.length < 9) {
            etTelefono.error = "Ingresa un número de celular válido (9 dígitos)"
            etTelefono.requestFocus()
            return
        }

        if (placa.length < 6 || placa.length > 7) {
            etPlaca.error = "La placa debe tener entre 6 y 7 caracteres"
            etPlaca.requestFocus()
            return
        }

        if (uidGoogle.isEmpty()) {
            Toast.makeText(
                this,
                "No se pudo identificar tu cuenta de Google. Vuelve a intentar el inicio de sesión.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        abrirValidacionDocumentos(dni, telefono, placa, modelo, color)
    }

    private fun abrirValidacionDocumentos(
        dni: String,
        telefono: String,
        placa: String,
        modelo: String,
        color: String
    ) {
        val intent = Intent(this, ValidarDocumentosConductor::class.java).apply {
            putExtra("LOGIN_GOOGLE", true)
            putExtra("EXTRA_UID_GOOGLE", uidGoogle)
            putExtra("EXTRA_NOMBRE", nombreGoogle)
            putExtra("EXTRA_EMAIL", emailGoogle)
            putExtra("EXTRA_DNI", dni)
            putExtra("EXTRA_TELEFONO", telefono)
            putExtra("EXTRA_PLACA", placa)
            putExtra("EXTRA_MODELO_VEHICULO", modelo)
            putExtra("EXTRA_COLOR_VEHICULO", color)
        }
        startActivity(intent)

        Toast.makeText(
            this,
            "Datos guardados. Ahora sube tus documentos para validación.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmarSalida() {
        AlertDialog.Builder(this)
            .setTitle("¿Salir del registro?")
            .setMessage("Ya iniciaste sesión con Google. Si sales ahora, tu cuenta quedará sin completar y deberás continuar la próxima vez que ingreses.")
            .setPositiveButton("Cerrar sesión y salir") { _, _ ->
                mAuth.signOut()
                finish()
            }
            .setNegativeButton("Continuar registro", null)
            .show()
    }
}
