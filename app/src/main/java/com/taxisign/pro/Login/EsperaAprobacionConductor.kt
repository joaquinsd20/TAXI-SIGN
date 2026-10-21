package com.taxisign.pro.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.conductor.PanelConductorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EsperaAprobacionConductor : AppCompatActivity() {

    private lateinit var btnRevisarEstado: Button
    private lateinit var btnSoporte: TextView

    // Vistas de los estados de documentos
    private lateinit var statusBrevete: TextView
    private lateinit var statusSoat: TextView
    private lateinit var statusAntecedentes: TextView

    private val mAuth = FirebaseAuth.getInstance()
    private val mDatabase = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CORREGIDO: Enlazamos con el nombre real de tu archivo XML
        setContentView(R.layout.espera_aprobacion_documentos)

        // Inicialización de vistas
        btnRevisarEstado = findViewById(R.id.btnRevisarEstado)
        btnSoporte = findViewById(R.id.btnSoporte)
        statusBrevete = findViewById(R.id.statusBrevete)
        statusSoat = findViewById(R.id.statusSoat)
        statusAntecedentes = findViewById(R.id.statusAntecedentes)

        // Botón para consultar manualmente a Firebase
        btnRevisarEstado.setOnClickListener {
            comprobarEstadoAprobacionReal()
        }

        // Botón para soporte WhatsApp
        btnSoporte.setOnClickListener {
            abrirSoporteWhatsApp()
        }

        // Bloquear botón físico de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@EsperaAprobacionConductor,
                    "Tus documentos están en validación. No puedes retroceder.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Activa la escucha automática: si lo apruebas en Firebase, el celular avanza solo
        escucharAprobacionEnTiempoReal()
    }

    private fun comprobarEstadoAprobacionReal() {
        val uid = mAuth.currentUser?.uid ?: return
        btnRevisarEstado.isEnabled = false
        btnRevisarEstado.text = "Consultando servidor..."

        mDatabase.child("usuarios").child("conductores").child(uid).get()
            .addOnSuccessListener { snapshot ->
                btnRevisarEstado.isEnabled = true
                btnRevisarEstado.text = "REVISAR ESTADO"

                if (snapshot.exists()) {
                    val estado = snapshot.child("estadoAprobacion").value?.toString() ?: "pendiente"

                    if (estado.lowercase() == "aprobado") {
                        val nombre = snapshot.child("nombre").value?.toString() ?: "Conductor"
                        val email = snapshot.child("email").value?.toString() ?: ""

                        actualizarUIYRedirigir(uid, nombre, email)
                    } else {
                        Toast.makeText(this, "Tu estado actual sigue siendo PENDIENTE.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "No se encontraron tus datos de registro.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                btnRevisarEstado.isEnabled = true
                btnRevisarEstado.text = "REVISAR ESTADO"
                Toast.makeText(this, "Error de conexión al verificar el estado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escucharAprobacionEnTiempoReal() {
        val uid = mAuth.currentUser?.uid ?: return

        mDatabase.child("usuarios").child("conductores").child(uid).child("estadoAprobacion")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estado = snapshot.value?.toString() ?: "pendiente"
                    if (estado.lowercase() == "aprobado") {
                        // Descargar perfil para guardar sesión antes de redirigir
                        mDatabase.child("usuarios").child("conductores").child(uid).get()
                            .addOnSuccessListener { userSnapshot ->
                                val nombre = userSnapshot.child("nombre").value?.toString() ?: "Conductor"
                                val email = userSnapshot.child("email").value?.toString() ?: ""
                                actualizarUIYRedirigir(uid, nombre, email)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejo silencioso
                }
            })
    }

    private fun actualizarUIYRedirigir(uid: String, nombre: String, email: String) {
        val colorVerde = ContextCompat.getColor(this, android.R.color.holo_green_light)

        statusBrevete.text = "✓ Aprobado"
        statusBrevete.setTextColor(colorVerde)
        statusSoat.text = "✓ Aprobado"
        statusSoat.setTextColor(colorVerde)
        statusAntecedentes.text = "✓ Aprobado"
        statusAntecedentes.setTextColor(colorVerde)

        Toast.makeText(this, "¡Felicidades! Tus documentos han sido aprobados.", Toast.LENGTH_LONG).show()

        // Espera un momento para que el chofer vea los checks verdes
        btnRevisarEstado.postDelayed({
            // Guardar datos en el SessionManager local
            SessionManager.idUsuario = uid.hashCode()
            SessionManager.nombreUsuario = nombre
            SessionManager.correoUsuario = email
            SessionManager.rolUsuario = "CONDUCTOR"

            val intent = Intent(this, PanelConductorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun abrirSoporteWhatsApp() {
        val numeroSoporte = "51954115555"
        val mensaje = "Hola TaxiSign Trujillo, acabo de subir mis documentos para el registro de conductor y me gustaría agilizar mi proceso de validación."

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$numeroSoporte&text=${Uri.encode(mensaje)}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No tienes WhatsApp instalado en tu celular", Toast.LENGTH_SHORT).show()
        }
    }
}