package com.taxisign.pro.Seguridad

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.taxisign.pro.R

/**
 * SeguridadContraseña
 *
 * Cambia la contraseña REAL del usuario en Firebase Authentication
 * (sirve igual para pasajero y conductor, según quién esté logueado).
 * La nueva contraseña queda válida de inmediato para iniciar sesión.
 *
 * Nota de seguridad: la contraseña NO se guarda en texto plano en la base de
 * datos (eso sería inseguro). Firebase Auth la guarda cifrada; en la base solo
 * se deja una marca de cuándo se cambió (passwordUpdatedAt).
 */
class SeguridadContraseña : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguridad_contrasena)

        val btnBack = findViewById<ImageButton>(R.id.btnBackSeguridad)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarClave)

        val etActual = findViewById<EditText>(R.id.etPassActualSegura)
        val etNueva = findViewById<EditText>(R.id.etPassNuevaSegura)
        val etConfirmar = findViewById<EditText>(R.id.etPassConfirmarSegura)

        btnBack.setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            val txtActual = etActual.text.toString().trim()
            val txtNueva = etNueva.text.toString().trim()
            val txtConfirmar = etConfirmar.text.toString().trim()

            if (txtActual.isEmpty() || txtNueva.isEmpty() || txtConfirmar.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (txtNueva.length < 6) {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (txtNueva != txtConfirmar) {
                Toast.makeText(this, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cambiarContrasena(txtActual, txtNueva, btnGuardar)
        }
    }

    private fun cambiarContrasena(actual: String, nueva: String, btn: Button) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(this, "No se pudo identificar tu cuenta", Toast.LENGTH_SHORT).show()
            return
        }

        btn.isEnabled = false

        // 1) Re-autenticar con la contraseña actual (seguridad de Firebase)
        val credencial = EmailAuthProvider.getCredential(email, actual)
        user.reauthenticate(credencial)
            .addOnSuccessListener {
                // 2) Actualizar a la nueva contraseña
                user.updatePassword(nueva)
                    .addOnSuccessListener {
                        marcarEnBaseDeDatos(user.uid)
                        Toast.makeText(this, "Contraseña actualizada con éxito", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btn.isEnabled = true
                        Toast.makeText(this, "No se pudo actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                btn.isEnabled = true
                Toast.makeText(this, "La contraseña actual es incorrecta", Toast.LENGTH_LONG).show()
            }
    }

    /** Deja una marca (no la contraseña) en el nodo del usuario que exista. */
    private fun marcarEnBaseDeDatos(uid: String) {
        val marca = System.currentTimeMillis()
        listOf("pasajeros", "conductores").forEach { rol ->
            dbRoot.child("usuarios").child(rol).child(uid).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists()) {
                        snap.ref.child("passwordUpdatedAt").setValue(marca)
                    }
                }
        }
    }
}