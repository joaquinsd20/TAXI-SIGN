package com.taxisign.pro.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.taxisign.pro.R
import com.taxisign.pro.pasajero.SeleccionarCiudadActivity

/**
 * CompletarPerfilPasajeroGoogle
 *
 * Pantalla intermedia que aparece cuando un PASAJERO NUEVO inicia sesión
 * con Google por primera vez. Firebase Auth ya creó la cuenta con nombre
 * y correo, pero aún faltan:
 *   - Número de teléfono celular
 *   - Tipo de discapacidad auditiva/del habla (sordo, mudo, sordomudo, o normal)
 *
 * Al completar estos datos, se guarda el perfil completo en Firebase y
 * se redirige a SeleccionarCiudadActivity antes de ir al panel principal.
 */
class CompletarPerfilPasajeroGoogle : AppCompatActivity() {

    private val mAuth by lazy { FirebaseAuth.getInstance() }
    private val mDatabase by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var tvSaludo: TextView
    private lateinit var etTelefono: TextInputEditText
    private lateinit var rgAccesibilidad: RadioGroup
    private lateinit var btnContinuar: Button

    private var uidGoogle: String = ""
    private var nombreGoogle: String = ""
    private var emailGoogle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.completar_perfil_pasajero_google)

        uidGoogle = intent.getStringExtra("EXTRA_UID_GOOGLE")
            ?: mAuth.currentUser?.uid ?: ""
        nombreGoogle = intent.getStringExtra("EXTRA_NOMBRE")
            ?: mAuth.currentUser?.displayName ?: "Pasajero"
        emailGoogle = intent.getStringExtra("EXTRA_EMAIL")
            ?: mAuth.currentUser?.email ?: ""

        tvSaludo = findViewById(R.id.tvSaludoGooglePasajero)
        etTelefono = findViewById(R.id.etTelefonoGooglePasajero)
        rgAccesibilidad = findViewById(R.id.rgAccesibilidadGooglePasajero)
        btnContinuar = findViewById(R.id.btnContinuarGooglePasajero)

        tvSaludo.text = "¡Hola, ${nombreGoogle.substringBefore(" ")}!"

        // Bloquear retroceso físico para no dejar el registro a medias
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { confirmarSalida() }
        })

        btnContinuar.setOnClickListener { validarYGuardar() }
    }

    private fun validarYGuardar() {
        val telefono = etTelefono.text.toString().trim()

        if (telefono.isEmpty()) {
            etTelefono.error = "Ingresa tu número de celular"
            etTelefono.requestFocus()
            return
        }

        if (telefono.length != 9) {
            etTelefono.error = "El celular debe tener 9 dígitos"
            etTelefono.requestFocus()
            return
        }

        if (uidGoogle.isEmpty()) {
            Toast.makeText(this,
                "No se pudo identificar tu cuenta de Google. Vuelve a iniciar sesión.",
                Toast.LENGTH_LONG).show()
            return
        }

        val tipoDiscapacidad = when (rgAccesibilidad.checkedRadioButtonId) {
            R.id.rbSordoGoogle    -> "Sordo"
            R.id.rbMudoGoogle     -> "Mudo"
            R.id.rbSordomudoGoogle -> "Sordomudo"
            else                   -> "Normal"
        }
        val esSordoMudo = tipoDiscapacidad != "Normal"

        btnContinuar.isEnabled = false
        btnContinuar.text = "GUARDANDO..."

        val datos = mapOf(
            "telefono" to "+51 $telefono",
            "tipoDiscapacidad" to tipoDiscapacidad,
            "esSordoMudo" to esSordoMudo,
            "ciudadConfigurada" to false
        )

        mDatabase.child("usuarios").child("pasajeros").child(uidGoogle)
            .updateChildren(datos)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Perfil completado!", Toast.LENGTH_SHORT).show()
                // Redirigir a selección de ciudad (como en el registro normal)
                val intent = Intent(this, SeleccionarCiudadActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnContinuar.isEnabled = true
                btnContinuar.text = "CONTINUAR"
                Toast.makeText(this, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmarSalida() {
        AlertDialog.Builder(this)
            .setTitle("¿Salir del registro?")
            .setMessage("Ya iniciaste sesión con Google. Si sales ahora, tu perfil quedará incompleto y deberás completarlo la próxima vez que ingreses.")
            .setPositiveButton("Cerrar sesión y salir") { _, _ ->
                mAuth.signOut()
                finish()
            }
            .setNegativeButton("Continuar", null)
            .show()
    }
}
