package com.taxisign.pro.conductor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.taxisign.pro.Login.LoginActivity
import com.taxisign.pro.R
import com.taxisign.pro.Seguridad.SeguridadContraseña
import com.taxisign.pro.SessionManager

class AjustesConductor : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes_conductor)

        val switchAuto = findViewById<SwitchCompat>(R.id.switchAutoAccept)
        val btnBack = findViewById<ImageButton>(R.id.btnBackAjustes)
        val btnCambiarPass = findViewById<Button>(R.id.btnCambiarPass)
        val btnSoporte = findViewById<Button>(R.id.btnSoporteConductor)
        val btnEliminarCuenta = findViewById<Button>(R.id.btnEliminarCuentaConductor)

        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                if (isChecked) "Modo manos libres activado" else "Aceptación manual activada",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnCambiarPass.setOnClickListener {
            startActivity(Intent(this, SeguridadContraseña::class.java))
        }

        btnSoporte.setOnClickListener {
            abrirSoporte()
        }

        btnEliminarCuenta.setOnClickListener {
            confirmarEliminarCuenta()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun abrirSoporte() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:taxisign@hotmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Soporte TaxiSign - Conductor")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hola equipo TaxiSign, necesito ayuda con mi cuenta de conductor."
            )
        }

        try {
            startActivity(Intent.createChooser(intent, "Contactar soporte"))
        } catch (_: Exception) {
            Toast.makeText(this, "No hay una aplicación de correo instalada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage(
                "Esta acción eliminará tu perfil de conductor y cerrará tu sesión. " +
                    "¿Deseas continuar?"
            )
            .setPositiveButton("Sí, eliminar") { _, _ ->
                eliminarCuenta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuenta() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            irLogin()
            return
        }

        val uid = user.uid
        dbRoot.child("usuarios")
            .child("conductores")
            .child(uid)
            .removeValue()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        limpiarSesion()
                        Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()
                        irLogin()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            "Por seguridad, vuelve a iniciar sesión y reintenta eliminar tu cuenta: ${error.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "No se pudo eliminar el perfil: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun limpiarSesion() {
        auth.signOut()
        SessionManager.idUsuario = 0
        SessionManager.nombreUsuario = ""
        SessionManager.correoUsuario = ""
        SessionManager.telefonoUsuario = ""
        SessionManager.dniUsuario = ""
        SessionManager.tipoDiscapacidadUsuario = ""
        SessionManager.rolUsuario = ""
    }

    private fun irLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
