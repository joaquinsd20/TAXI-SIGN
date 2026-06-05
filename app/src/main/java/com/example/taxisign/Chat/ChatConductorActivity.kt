package com.example.taxisign

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ChatConductorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatConductor"
    }

    private lateinit var chatId: String

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("chats/$chatId/mensajes")
    }

    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var tvNombrePasajero: TextView
    private lateinit var tvAvatarPasajero: TextView
    private lateinit var btnOk: MaterialButton
    private lateinit var btnEnseguida: MaterialButton
    private lateinit var btnDondeEstas: MaterialButton
    private lateinit var btnEntendido: MaterialButton
    private lateinit var btnNoEncuentro: MaterialButton
    private lateinit var btnLlegue: MaterialButton
    private lateinit var btnEscribir: MaterialButton
    private lateinit var etMensajeLibre: EditText
    private lateinit var btnEnviarLibre: ImageButton

    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: MensajesAdapter

    @Suppress("DEPRECATION")
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_conductor)

        val id = intent.getStringExtra("CHAT_ID")
        if (id.isNullOrEmpty()) {
            Log.e(TAG, "CHAT_ID vacío — cerrando")
            Toast.makeText(this, "Error: chat no disponible", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        chatId = id

        // ── Log de diagnóstico de sesión ──────────────────────────
        val user = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "Usuario actual: ${user?.uid} / email: ${user?.email}")
        if (user == null) {
            Log.e(TAG, "Sin sesión activa en Firebase Auth")
        }

        val nombrePasajero = intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        bindViews()
        configurarHeader(nombrePasajero)
        configurarRecycler()
        configurarRespuestasRapidas()
        configurarMensajeLibre()
        escucharMensajes()
    }

    private fun bindViews() {
        recyclerMensajes = findViewById(R.id.recycler_mensajes)
        tvNombrePasajero = findViewById(R.id.tv_nombre_pasajero)
        tvAvatarPasajero = findViewById(R.id.tv_avatar_pasajero)
        btnOk            = findViewById(R.id.btn_ok)
        btnEnseguida     = findViewById(R.id.btn_enseguida)
        btnDondeEstas    = findViewById(R.id.btn_donde_estas)
        btnEntendido     = findViewById(R.id.btn_entendido)
        btnNoEncuentro   = findViewById(R.id.btn_no_encuentro)
        btnLlegue        = findViewById(R.id.btn_llegue)
        btnEscribir      = findViewById(R.id.btn_escribir)
        etMensajeLibre   = findViewById(R.id.et_mensaje_libre)
        btnEnviarLibre   = findViewById(R.id.btn_enviar_libre)
    }

    private fun configurarHeader(nombrePasajero: String) {
        tvNombrePasajero.text = nombrePasajero
        tvAvatarPasajero.text = nombrePasajero.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        findViewById<ImageButton>(R.id.btn_volver).setOnClickListener { vibrar(); finish() }
    }

    private fun configurarRecycler() {
        // Usar uid si hay sesión, sino usar string vacío (modo demo)
        val uid = auth.currentUser?.uid ?: ""
        adapter = MensajesAdapter(mensajes, uid)
        recyclerMensajes.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerMensajes.adapter = adapter
    }

    private fun configurarRespuestasRapidas() {
        mapOf(
            btnOk          to "👍 OK",
            btnEnseguida   to "⏱️ ENSEGUIDA VOY",
            btnDondeEstas  to "📍 ¿DÓNDE ESTÁS?",
            btnEntendido   to "✅ ENTENDIDO",
            btnNoEncuentro to "❓ NO TE ENCUENTRO",
            btnLlegue      to "🚕 YA LLEGUÉ"
        ).forEach { (btn, texto) ->
            btn.setOnClickListener {
                Log.d(TAG, "Botón tocado: $texto")
                vibrar()
                enviarMensaje(texto)
            }
        }
    }

    private fun configurarMensajeLibre() {
        btnEscribir.setOnClickListener {
            vibrar()
            btnEscribir.visibility    = View.GONE
            etMensajeLibre.visibility = View.VISIBLE
            btnEnviarLibre.visibility = View.VISIBLE
            etMensajeLibre.requestFocus()
        }
        btnEnviarLibre.setOnClickListener {
            val texto = etMensajeLibre.text.toString().trim()
            if (texto.isEmpty()) {
                Toast.makeText(this, "Escribe algo primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vibrar()
            enviarMensaje(texto)
            etMensajeLibre.setText("")
            etMensajeLibre.visibility = View.GONE
            btnEnviarLibre.visibility = View.GONE
            btnEscribir.visibility    = View.VISIBLE
        }
    }

    private fun enviarMensaje(texto: String) {
        // ── Si no hay sesión usar uid temporal para pruebas ──────
        val uid = auth.currentUser?.uid ?: run {
            Log.w(TAG, "Sin sesión — usando uid temporal para prueba")
            "conductor_sin_sesion"
        }

        val mensaje = Mensaje(
            texto     = texto,
            emisorId  = uid,
            emisorRol = "CONDUCTOR",
            timestamp = System.currentTimeMillis()
        )

        database.push().setValue(mensaje)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Enviado: $texto")
                Toast.makeText(this, "✓ Enviado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "✗ Error al enviar: ${e.message}")
                Toast.makeText(this, "Error Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun escucharMensajes() {
        Log.d(TAG, "Escuchando: chats/$chatId/mensajes")
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(Mensaje::class.java) ?: return
                Log.d(TAG, "Recibido: ${msg.texto} [${msg.emisorRol}]")
                mensajes.add(msg)
                adapter.notifyItemInserted(mensajes.size - 1)
                recyclerMensajes.scrollToPosition(mensajes.size - 1)
                if (msg.emisorRol == "PASAJERO") {
                    if (msg.texto.contains("SOS")) vibrarEmergencia() else vibrarNotificacion()
                }
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase cancelado: ${error.message}")
                Toast.makeText(this@ChatConductorActivity,
                    "Error Firebase: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun vibrar() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(80)
    }

    private fun vibrarNotificacion() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 100, 80, 100), -1)
    }

    private fun vibrarEmergencia() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 300, 150, 300, 150, 300), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 300, 150, 300, 150, 300), -1)
    }
}