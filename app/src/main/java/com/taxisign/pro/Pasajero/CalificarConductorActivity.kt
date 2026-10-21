package com.taxisign.pro.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CalificarConductorActivity : AppCompatActivity() {

    private var calificacion = 0

    private var viajeId = ""
    private var uidConductor = ""
    private var nombreConductor = ""
    private var destino = ""
    private var tarifa = 0.0

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var txtNombreConductor: TextView
    private lateinit var edtComentario: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificar_conductor)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        uidConductor = intent.getStringExtra("ID_CONDUCTOR") ?: ""
        nombreConductor = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"
        destino = intent.getStringExtra("DESTINO") ?: ""
        tarifa = intent.getDoubleExtra("TARIFA", 0.0)

        txtNombreConductor = findViewById(R.id.txt_nombre_conductor)
        edtComentario = findViewById(R.id.edt_comentario)

        txtNombreConductor.text = nombreConductor

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnEnviar = findViewById<LinearLayout>(R.id.btn_enviar)

        val estrellas = listOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )

        ponerEstrellasEnGris(estrellas)

        btnBack.setOnClickListener {
            finish()
        }

        for (i in estrellas.indices) {
            estrellas[i].setOnClickListener {
                calificacion = i + 1
                actualizarEstrellas(estrellas, i)
            }
        }

        btnEnviar.setOnClickListener {
            if (calificacion == 0) {
                Toast.makeText(
                    this,
                    "Selecciona una calificación",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            guardarCalificacion()
        }
    }

    private fun actualizarEstrellas(
        estrellas: List<ImageView>,
        indexSeleccionado: Int
    ) {
        for (j in estrellas.indices) {
            val color = if (j <= indexSeleccionado) {
                R.color.yellow_primary
            } else {
                android.R.color.darker_gray
            }

            estrellas[j].setColorFilter(
                ContextCompat.getColor(this, color)
            )
        }
    }

    private fun ponerEstrellasEnGris(estrellas: List<ImageView>) {
        for (estrella in estrellas) {
            estrella.setColorFilter(
                ContextCompat.getColor(
                    this,
                    android.R.color.darker_gray
                )
            )
        }
    }

    private fun guardarCalificacion() {
        val uidPasajero = auth.currentUser?.uid ?: ""

        if (uidPasajero.isEmpty()) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (viajeId.isEmpty()) {
            Toast.makeText(this, "Error: viaje no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val comentario = edtComentario.text.toString().trim()
        val nombrePasajero = com.taxisign.pro.SessionManager.nombreUsuario.ifEmpty { "Pasajero" }

        val datosCalificacion = hashMapOf<String, Any>(
            "viajeId" to viajeId,
            "uidPasajero" to uidPasajero,
            "uidConductor" to uidConductor,
            "nombreConductor" to nombreConductor,
            "destino" to destino,
            "tarifa" to tarifa,
            "calificacion" to calificacion,
            "comentario" to comentario,
            "tipo" to "PASAJERO_A_CONDUCTOR",
            "timestamp" to System.currentTimeMillis()
        )

        // Reseña vista desde el conductor (para listar en su perfil)
        val resena = hashMapOf<String, Any>(
            "viajeId" to viajeId,
            "nombrePasajero" to nombrePasajero,
            "destino" to destino,
            "calificacion" to calificacion,
            "comentario" to comentario,
            "timestamp" to System.currentTimeMillis()
        )

        val updates = hashMapOf<String, Any>(
            "calificaciones/$viajeId/pasajeroAConductor" to datosCalificacion,
            "usuarios/conductores/$uidConductor/resenas/$viajeId" to resena,
            "viajes/$viajeId/calificacionConductor" to calificacion,
            "viajes/$viajeId/comentarioPasajeroAConductor" to comentario,
            "viajes/$viajeId/estadoCalificacionPasajero" to "calificado"
        )

        dbRoot.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Gracias por calificar al conductor",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, PanelPasajeroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Error al guardar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}