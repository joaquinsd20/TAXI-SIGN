package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CalificarPasajero : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar

    private var viajeId = ""
    private var uidPasajero = ""
    private var nombrePasajero = ""

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificar_pasajero)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        uidPasajero = intent.getStringExtra("PASAJERO_ID") ?: ""
        nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        ratingBar = findViewById(R.id.ratingPasajero)

        val btnEnviar =
            findViewById<Button>(R.id.btnEnviarCalificacion)

        btnEnviar.setOnClickListener {

            val puntos = ratingBar.rating.toInt()

            if (puntos == 0) {

                Toast.makeText(
                    this,
                    "Selecciona una calificación",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            guardarCalificacion(puntos)
        }
    }

    private fun guardarCalificacion(
        calificacion: Int
    ) {

        val uidConductor =
            auth.currentUser?.uid ?: ""

        if (uidConductor.isEmpty()) {

            Toast.makeText(
                this,
                "Conductor no autenticado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (viajeId.isEmpty()) {

            Toast.makeText(
                this,
                "Viaje no encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val datosCalificacion =
            hashMapOf<String, Any>(

                "viajeId" to viajeId,

                "uidConductor" to uidConductor,
                "uidPasajero" to uidPasajero,

                "nombrePasajero" to nombrePasajero,

                "calificacion" to calificacion,

                "tipo" to "CONDUCTOR_A_PASAJERO",

                "timestamp" to System.currentTimeMillis()
            )

        val resenaPasajero =
            hashMapOf<String, Any>(
                "viajeId" to viajeId,
                "nombreConductor" to com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Conductor" },
                "calificacion" to calificacion,
                "timestamp" to System.currentTimeMillis()
            )

        val updates =
            hashMapOf<String, Any>(

                "calificaciones/$viajeId/conductorAPasajero"
                        to datosCalificacion,

                "usuarios/pasajeros/$uidPasajero/resenas/$viajeId"
                        to resenaPasajero,

                "viajes/$viajeId/calificacionPasajero"
                        to calificacion,

                "viajes/$viajeId/estadoCalificacionConductor"
                        to "calificado"
            )

        dbRoot.updateChildren(updates)

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Pasajero calificado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                val intent =
                    Intent(
                        this,
                        PanelConductorActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }

            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}