package com.taxisign.pro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.taxisign.pro.Login.LoginActivity

class MainActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        try {

            setContentView(R.layout.activity_main)

            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

            val cardPasajero =
                findViewById<CardView>(R.id.cardPasajero)

            val cardConductor =
                findViewById<CardView>(R.id.cardConductor)

            val animEntrada =
                AnimationUtils.loadAnimation(
                    this,
                    android.R.anim.fade_in
                )

            cardPasajero.startAnimation(animEntrada)
            cardConductor.startAnimation(animEntrada)

            cardPasajero.setOnClickListener {

                abrirLogin("PASAJERO")
            }

            cardConductor.setOnClickListener {

                abrirLogin("CONDUCTOR")
            }

            // Si ya hay sesión activa, redirige al panel correspondiente
            verificarSesionActiva()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al iniciar pantalla principal",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun verificarSesionActiva() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference

        db.child("usuarios").child("pasajeros").child(uid).get()
            .addOnSuccessListener { sp ->
                if (sp.exists()) {
                    SessionManager.idUsuario = uid.hashCode()
                    SessionManager.nombreUsuario = sp.child("nombre").value?.toString() ?: "Pasajero"
                    SessionManager.correoUsuario = sp.child("email").value?.toString() ?: ""
                    SessionManager.rolUsuario = "PASAJERO"
                    irAPanel(com.taxisign.pro.pasajero.PanelPasajeroActivity::class.java)
                } else {
                    db.child("usuarios").child("conductores").child(uid).get()
                        .addOnSuccessListener { sc ->
                            if (sc.exists()) {
                                SessionManager.idUsuario = uid.hashCode()
                                SessionManager.nombreUsuario = sc.child("nombre").value?.toString() ?: "Conductor"
                                SessionManager.correoUsuario = sc.child("email").value?.toString() ?: ""
                                SessionManager.rolUsuario = "CONDUCTOR"
                                irAPanel(com.taxisign.pro.conductor.PanelConductorActivity::class.java)
                            }
                        }
                }
            }
    }

    private fun irAPanel(clase: Class<*>) {
        val intent = Intent(this, clase).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun abrirLogin(rol: String) {

        try {

            vibrarConfirmacion()

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            intent.putExtra(
                "ROL_SELECCIONADO",
                rol
            )

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al abrir Login",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun vibrarConfirmacion() {

        vibrator?.let {

            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                it.vibrate(
                    VibrationEffect.createOneShot(
                        80,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")

                it.vibrate(80)
            }
        }
    }
}