package com.taxisign.pro.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth

class CrearCuentaConductor : AppCompatActivity() {

    private val mAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var btnVolverConductor: ImageButton
    private lateinit var etNombreConductor: TextInputEditText
    private lateinit var etDniConductor: TextInputEditText
    private lateinit var etPlacaConductor: TextInputEditText
    private lateinit var etModeloVehiculo: TextInputEditText
    private lateinit var etColorVehiculo: TextInputEditText
    private lateinit var etEmailConductor: TextInputEditText
    private lateinit var etPassConductor: TextInputEditText
    private lateinit var btnSiguienteDocumentos: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_cuenta_conductor)

        initViews()

        btnVolverConductor.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSiguienteDocumentos.setOnClickListener {
            procesarRegistroConductor()
        }
    }

    private fun initViews() {
        btnVolverConductor = findViewById(R.id.btnVolverConductor)
        etNombreConductor = findViewById(R.id.etNombreConductor)
        etDniConductor = findViewById(R.id.etDniConductor)
        etPlacaConductor = findViewById(R.id.etPlacaConductor)
        etModeloVehiculo = findViewById(R.id.etModeloVehiculo)
        etColorVehiculo = findViewById(R.id.etColorVehiculo)
        etEmailConductor = findViewById(R.id.etEmailConductor)
        etPassConductor = findViewById(R.id.etPassConductor)
        btnSiguienteDocumentos = findViewById(R.id.btnSiguienteDocumentos)
    }

    private fun procesarRegistroConductor() {
        val nombre = etNombreConductor.text.toString().trim()
        val dni = etDniConductor.text.toString().trim()
        val placa = etPlacaConductor.text.toString().trim().uppercase()
        val modeloVehiculo = etModeloVehiculo.text.toString().trim()
        val colorVehiculo = etColorVehiculo.text.toString().trim()
        val email = etEmailConductor.text.toString().trim()
        val password = etPassConductor.text.toString().trim()

        if (
            nombre.isEmpty() ||
            dni.isEmpty() ||
            placa.isEmpty() ||
            modeloVehiculo.isEmpty() ||
            colorVehiculo.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Por favor, complete todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (dni.length != 8) {
            etDniConductor.error = "El DNI debe tener exactamente 8 dígitos"
            etDniConductor.requestFocus()
            return
        }

        if (placa.length < 6 || placa.length > 7) {
            etPlacaConductor.error = "La placa debe tener entre 6 y 7 caracteres"
            etPlacaConductor.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailConductor.error = "Ingresa un correo electrónico válido"
            etEmailConductor.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassConductor.error = "La contraseña debe tener al menos 6 caracteres"
            etPassConductor.requestFocus()
            return
        }

        btnSiguienteDocumentos.isEnabled = false
        btnSiguienteDocumentos.text = "VERIFICANDO..."

        mAuth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                val existe = !result.signInMethods.isNullOrEmpty()
                if (existe) {
                    btnSiguienteDocumentos.isEnabled = true
                    btnSiguienteDocumentos.text = "SIGUIENTE"
                    Toast.makeText(
                        this,
                        "Esta cuenta ya existe. Inicia sesión o usa otro correo.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    abrirValidacionDocumentos(
                        nombre,
                        dni,
                        placa,
                        modeloVehiculo,
                        colorVehiculo,
                        email,
                        password
                    )
                }
            }
            .addOnFailureListener {
                btnSiguienteDocumentos.isEnabled = true
                btnSiguienteDocumentos.text = "SIGUIENTE"
                Toast.makeText(
                    this,
                    "No se pudo verificar el correo. Intenta nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirValidacionDocumentos(
        nombre: String,
        dni: String,
        placa: String,
        modeloVehiculo: String,
        colorVehiculo: String,
        email: String,
        password: String
    ) {
        val intent = Intent(this, ValidarDocumentosConductor::class.java).apply {
            putExtra("EXTRA_NOMBRE", nombre)
            putExtra("EXTRA_DNI", dni)
            putExtra("EXTRA_PLACA", placa)
            putExtra("EXTRA_MODELO_VEHICULO", modeloVehiculo)
            putExtra("EXTRA_COLOR_VEHICULO", colorVehiculo)
            putExtra("EXTRA_EMAIL", email)
            putExtra("EXTRA_PASSWORD", password)
        }

        startActivity(intent)

        Toast.makeText(
            this,
            "Datos validados. Cargando sección de documentos...",
            Toast.LENGTH_SHORT
        ).show()
    }
}