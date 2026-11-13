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

        // Sesion abierta: entra directo a su panel, sin mostrar la pantalla de inicio
        val usuario = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val rolGuardado = SessionManager.rolGuardado(this)
        if (usuario != null && rolGuardado.isNotEmpty()) {
            SessionManager.restaurar(this, usuario.uid)
            irAPanel(
                if (rolGuardado == "CONDUCTOR") com.taxisign.pro.conductor.PanelConductorActivity::class.java
                else com.taxisign.pro.pasajero.PanelPasajeroActivity::class.java
            )
            return
        }

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

            findViewById<android.widget.TextView>(R.id.tvPoliticaPrivacidadMain)
                .setOnClickListener {
                    com.taxisign.pro.Seguridad.PoliticaPrivacidad.abrir(this)
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

    /**
     * Si hay una sesion activa abre directamente el panel de su rol.
     * Mientras se comprueba, los botones de rol se ocultan para que el usuario
     * no abra el login de un rol y luego la app lo mande al panel del otro.
     */
    private fun verificarSesionActiva() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        val cardPasajero = findViewById<CardView>(R.id.cardPasajero)
        val cardConductor = findViewById<CardView>(R.id.cardConductor)

        fun mostrarOpciones() {
            cardPasajero.visibility = android.view.View.VISIBLE
            cardConductor.visibility = android.view.View.VISIBLE
        }
        cardPasajero.visibility = android.view.View.INVISIBLE
        cardConductor.visibility = android.view.View.INVISIBLE

        fun entrar(snap: com.google.firebase.database.DataSnapshot, rol: String) {
            SessionManager.idUsuario = uid.hashCode()
            SessionManager.uidUsuario = uid
            SessionManager.nombreUsuario = snap.child("nombre").value?.toString()
                ?: if (rol == "CONDUCTOR") "Conductor" else "Pasajero"
            SessionManager.correoUsuario = snap.child("email").value?.toString() ?: ""
            SessionManager.guardarRol(this, rol)
            irAPanel(
                if (rol == "CONDUCTOR") com.taxisign.pro.conductor.PanelConductorActivity::class.java
                else com.taxisign.pro.pasajero.PanelPasajeroActivity::class.java
            )
        }

        // El rol con el que inicio sesion manda; si no hay (instalaciones antiguas)
        // se busca primero como pasajero y luego como conductor.
        val rolGuardado = SessionManager.rolGuardado(this)
        val orden = when (rolGuardado) {
            "CONDUCTOR" -> listOf("CONDUCTOR")
            "PASAJERO" -> listOf("PASAJERO")
            else -> listOf("PASAJERO", "CONDUCTOR")
        }

        fun probar(indice: Int) {
            if (indice >= orden.size) {
                // La cuenta no tiene perfil del rol esperado: se cierra y se elige de nuevo
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                SessionManager.borrarRol(this)
                mostrarOpciones()
                return
            }
            val rol = orden[indice]
            val nodo = if (rol == "CONDUCTOR") "conductores" else "pasajeros"
            db.child("usuarios").child(nodo).child(uid).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists()) entrar(snap, rol) else probar(indice + 1)
                }
                .addOnFailureListener {
                    // Sin conexion: se muestran las opciones para no dejar la pantalla vacia
                    mostrarOpciones()
                }
        }
        probar(0)
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