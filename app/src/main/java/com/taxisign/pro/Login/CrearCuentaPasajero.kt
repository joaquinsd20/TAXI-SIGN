package com.taxisign.pro.Login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CrearCuentaPasajero : AppCompatActivity() {

    private lateinit var btnVolverPasajero: ImageButton
    private lateinit var etNombrePasajero: TextInputEditText
    private lateinit var etTelefonoPasajero: TextInputEditText
    private lateinit var etEdadPasajero: TextInputEditText
    private lateinit var etEmailPasajero: TextInputEditText
    private lateinit var etPassPasajero: TextInputEditText
    private lateinit var rgAccesibilidad: RadioGroup
    private lateinit var btnRegistrarPasajero: Button
    private lateinit var tvYaTengoCuentaPasajero: TextView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_cuenta_pasajero)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference

        // Vincular vistas con el XML usando sus IDs exactos
        initViews()

        // Configurar los listeners de clics
        setupClickListeners()
    }

    private fun initViews() {
        btnVolverPasajero = findViewById(R.id.btnVolverPasajero)
        etNombrePasajero = findViewById(R.id.etNombrePasajero)
        etTelefonoPasajero = findViewById(R.id.etTelefonoPasajero)
        etEdadPasajero = findViewById(R.id.etEdadPasajero)
        etEmailPasajero = findViewById(R.id.etEmailPasajero)
        etPassPasajero = findViewById(R.id.etPassPasajero)
        rgAccesibilidad = findViewById(R.id.rgAccesibilidad)
        btnRegistrarPasajero = findViewById(R.id.btnRegistrarPasajero)
        tvYaTengoCuentaPasajero = findViewById(R.id.tvYaTengoCuentaPasajero)
    }

    private fun setupClickListeners() {
        btnVolverPasajero.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvYaTengoCuentaPasajero.setOnClickListener {
            irAlLogin()
        }

        btnRegistrarPasajero.setOnClickListener {
            ejecutarRegistroPasajero()
        }
    }

    private fun ejecutarRegistroPasajero() {
        val nombre = etNombrePasajero.text.toString().trim()
        val telefono = etTelefonoPasajero.text.toString().trim()
        val edadString = etEdadPasajero.text.toString().trim()
        val email = etEmailPasajero.text.toString().trim()
        val password = etPassPasajero.text.toString().trim()

        // Validar campos vacíos
        if (nombre.isEmpty() || telefono.isEmpty() || edadString.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar celular peruano (9 dígitos)
        if (telefono.length != 9) {
            etTelefonoPasajero.error = "El celular debe tener exactamente 9 dígitos"
            etTelefonoPasajero.requestFocus()
            return
        }

        // Validar edad
        val edad = edadString.toIntOrNull()
        if (edad == null || edad < 18) {
            etEdadPasajero.error = "Debes ser mayor de 18 años para viajar solo"
            etEdadPasajero.requestFocus()
            return
        }

        // Validar formato de correo
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailPasajero.error = "Ingresa un correo electrónico válido"
            etEmailPasajero.requestFocus()
            return
        }

        // Validar longitud de la contraseña
        if (password.length < 6) {
            etPassPasajero.error = "La contraseña debe tener al menos 6 caracteres"
            etPassPasajero.requestFocus()
            return
        }

        // Obtener la condición auditiva/del habla seleccionada
        val tipoDiscapacidad = when (rgAccesibilidad.checkedRadioButtonId) {
            R.id.rbSordo -> "Sordo"
            R.id.rbMudo -> "Mudo"
            R.id.rbSordomudo -> "Sordomudo"
            else -> "Normal"
        }
        val esSordoMudo = tipoDiscapacidad != "Normal"

        btnRegistrarPasajero.isEnabled = false
        btnRegistrarPasajero.text = "REGISTRANDO..."

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid ?: ""

                    // Crear el mapa con toda la información consolidada del pasajero
                    val datosPasajero = hashMapOf(
                        "uid" to uid,
                        "nombre" to nombre,
                        "telefono" to "+51 $telefono",
                        "edad" to edad,
                        "email" to email,
                        "tipoDiscapacidad" to tipoDiscapacidad,
                        "esSordoMudo" to esSordoMudo,
                        "tipoUsuario" to "PASAJERO",
                        "ciudadConfigurada" to false
                    )

                    mDatabase.child("usuarios").child("pasajeros").child(uid).setValue(datosPasajero)
                        .addOnSuccessListener {
                            Toast.makeText(this, "¡Registro completado con éxito!", Toast.LENGTH_LONG).show()
                            irASeleccionarCiudad()
                        }
                        .addOnFailureListener { e ->
                            restaurarBotonRegistro("Error al guardar datos: ${e.localizedMessage}")
                        }
                } else {
                    val mensaje = if (task.exception is FirebaseAuthUserCollisionException) {
                        "Esta cuenta ya existe. Inicia sesión o usa otro correo."
                    } else {
                        task.exception?.localizedMessage ?: "Error al registrar credenciales"
                    }
                    restaurarBotonRegistro(mensaje)
                }
            }
    }

    private fun restaurarBotonRegistro(mensajeError: String) {
        btnRegistrarPasajero.isEnabled = true
        btnRegistrarPasajero.text = "REGISTRARME"
        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
    }

    private fun irAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irASeleccionarCiudad() {
        val intent = Intent(this, com.taxisign.pro.pasajero.SeleccionarCiudadActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}