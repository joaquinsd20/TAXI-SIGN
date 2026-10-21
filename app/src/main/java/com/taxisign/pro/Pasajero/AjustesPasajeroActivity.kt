package com.taxisign.pro.pasajero

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.taxisign.pro.Login.LoginActivity
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.Seguridad.SeguridadContraseña

class AjustesPasajeroActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes_pasajero)

        findViewById<ImageButton>(R.id.btnBackAjustesPasajero).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnSeguridadPasajero).setOnClickListener {
            startActivity(Intent(this, SeguridadContraseña::class.java))
        }

        findViewById<Button>(R.id.btnSoportePasajero).setOnClickListener {
            abrirSoporte()
        }

        findViewById<Button>(R.id.btnEliminarCuentaPasajero).setOnClickListener {
            confirmarEliminarCuenta()
        }
    }

    private fun abrirSoporte() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:taxisign@hotmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Soporte TaxiSign - Pasajero")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hola equipo TaxiSign, necesito ayuda con mi cuenta de pasajero."
            )
        }

        try {
            startActivity(Intent.createChooser(intent, "Contactar soporte"))
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No hay una aplicación de correo instalada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmarEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage(
                "Esta acción eliminará tu perfil de pasajero y cerrará tu sesión. " +
                    "Tus datos se eliminarán de TaxiSign. ¿Deseas continuar?"
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
            .child("pasajeros")
            .child(uid)
            .removeValue()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        limpiarSesion()
                        Toast.makeText(
                            this,
                            "Cuenta eliminada correctamente",
                            Toast.LENGTH_LONG
                        ).show()
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
