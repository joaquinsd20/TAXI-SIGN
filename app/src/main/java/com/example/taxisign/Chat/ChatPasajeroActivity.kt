package com.example.taxisign

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class ChatPasajeroActivity : AppCompatActivity() {

    // ── Firebase: by lazy evita "Unresolved reference" ──────────
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("chats/$chatId/mensajes")
    }

    // ── Datos del Intent ─────────────────────────────────────────
    private lateinit var chatId: String

    // ── UI ───────────────────────────────────────────────────────
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var cardRespuestaConductor: CardView
    private lateinit var tvRespuestaConductor: TextView
    private lateinit var tvNombreConductor: TextView
    private lateinit var tvAvatarConductor: TextView
    private lateinit var indicadorNuevoMsg: View

    private lateinit var tabUbicacion: TextView
    private lateinit var tabInstrucciones: TextView
    private lateinit var tabPersonal: TextView

    private lateinit var gridUbicacion: View
    private lateinit var gridInstrucciones: View
    private lateinit var gridPersonal: View

    private lateinit var btnMensajeLibre: MaterialButton
    private lateinit var etMensajeLibre: EditText
    private lateinit var btnEnviarLibre: ImageButton

    // ── Adapter ──────────────────────────────────────────────────
    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: MensajesAdapter

    @Suppress("DEPRECATION")
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }

    // ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_pasajero)

        // 1. Leer Intent PRIMERO (database lazy depende de chatId)
        chatId = intent.getStringExtra("CHAT_ID") ?: run { finish(); return }
        val nombreConductor = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"

        bindViews()
        configurarHeader(nombreConductor)
        configurarRecycler()
        configurarTabs()
        configurarBotonesPredefinidos()
        configurarMensajeLibre()
        escucharMensajes()
    }

    // ── Binding ──────────────────────────────────────────────────

    private fun bindViews() {
        recyclerMensajes       = findViewById(R.id.recycler_mensajes)
        cardRespuestaConductor = findViewById(R.id.card_respuesta_conductor)
        tvRespuestaConductor   = findViewById(R.id.tv_respuesta_conductor)
        tvNombreConductor      = findViewById(R.id.tv_nombre_conductor)
        tvAvatarConductor      = findViewById(R.id.tv_avatar_conductor)
        indicadorNuevoMsg      = findViewById(R.id.indicador_nuevo_msg)

        tabUbicacion           = findViewById(R.id.tab_ubicacion)
        tabInstrucciones       = findViewById(R.id.tab_instrucciones)
        tabPersonal            = findViewById(R.id.tab_personal)

        gridUbicacion          = findViewById(R.id.grid_ubicacion)


        btnMensajeLibre        = findViewById(R.id.btn_mensaje_libre)
        etMensajeLibre         = findViewById(R.id.et_mensaje_libre)
        btnEnviarLibre         = findViewById(R.id.btn_enviar_libre)
    }

    // ── Header ───────────────────────────────────────────────────

    private fun configurarHeader(nombreConductor: String) {
        tvNombreConductor.text = nombreConductor
        tvAvatarConductor.text = nombreConductor.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        findViewById<ImageButton>(R.id.btn_volver).setOnClickListener { vibrar(); finish() }
    }

    // ── RecyclerView ─────────────────────────────────────────────

    private fun configurarRecycler() {
        adapter = MensajesAdapter(mensajes, auth.currentUser?.uid ?: "")
        recyclerMensajes.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerMensajes.adapter = adapter
    }

    // ── Tabs ─────────────────────────────────────────────────────

    private fun configurarTabs() {
        tabUbicacion.setOnClickListener    { cambiarTab(Tab.UBICACION) }
        tabInstrucciones.setOnClickListener { cambiarTab(Tab.INSTRUCCIONES) }
        tabPersonal.setOnClickListener     { cambiarTab(Tab.PERSONAL) }
        cambiarTab(Tab.UBICACION)
    }

    private fun cambiarTab(tab: Tab) {
        vibrar()
        gridUbicacion.visibility     = View.GONE
        gridInstrucciones.visibility = View.GONE
        gridPersonal.visibility      = View.GONE

        val amarillo = getColor(R.color.yellow_primary)
        val gris     = getColor(android.R.color.darker_gray)

        tabUbicacion.setTextColor(gris);     tabUbicacion.setBackgroundResource(R.drawable.bg_tab_inactivo)
        tabInstrucciones.setTextColor(gris); tabInstrucciones.setBackgroundResource(R.drawable.bg_tab_inactivo)
        tabPersonal.setTextColor(gris);      tabPersonal.setBackgroundResource(R.drawable.bg_tab_inactivo)

        when (tab) {
            Tab.UBICACION -> {
                gridUbicacion.visibility = View.VISIBLE
                tabUbicacion.setTextColor(amarillo)
                tabUbicacion.setBackgroundResource(R.drawable.bg_tab_activo)
            }
            Tab.INSTRUCCIONES -> {
                gridInstrucciones.visibility = View.VISIBLE
                tabInstrucciones.setTextColor(amarillo)
                tabInstrucciones.setBackgroundResource(R.drawable.bg_tab_activo)
            }
            Tab.PERSONAL -> {
                gridPersonal.visibility = View.VISIBLE
                tabPersonal.setTextColor(amarillo)
                tabPersonal.setBackgroundResource(R.drawable.bg_tab_activo)
            }
        }
    }

    // ── Botones predefinidos ──────────────────────────────────────

    private fun configurarBotonesPredefinidos() {
        val predefinidos = mapOf(
            R.id.btn_estoy_aqui     to "📍 ESTOY AQUÍ",
            R.id.btn_sigo_caminando to "🚶 SIGO CAMINANDO",

        )
        predefinidos.forEach { (id, texto) ->
            findViewById<MaterialButton>(id).setOnClickListener { vibrar(); enviarMensaje(texto) }
        }

    }

    // ── Mensaje libre ─────────────────────────────────────────────

    private fun configurarMensajeLibre() {
        btnMensajeLibre.setOnClickListener {
            vibrar()
            btnMensajeLibre.visibility = View.GONE
            etMensajeLibre.visibility  = View.VISIBLE
            btnEnviarLibre.visibility  = View.VISIBLE
            etMensajeLibre.requestFocus()
        }
        btnEnviarLibre.setOnClickListener {
            val texto = etMensajeLibre.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener
            vibrar()
            enviarMensaje("✏️ $texto")
            etMensajeLibre.setText("")
            etMensajeLibre.visibility  = View.GONE
            btnEnviarLibre.visibility  = View.GONE
            btnMensajeLibre.visibility = View.VISIBLE
        }
    }

    // ── Firebase: enviar ──────────────────────────────────────────

    private fun enviarMensaje(texto: String) {
        val mensaje = Mensaje(
            texto     = texto,
            emisorId  = auth.currentUser?.uid ?: "",
            emisorRol = "PASAJERO",
            timestamp = System.currentTimeMillis()
        )
        database.push().setValue(mensaje)
    }

    // ── Firebase: escuchar ────────────────────────────────────────

    private fun escucharMensajes() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(Mensaje::class.java) ?: return
                if (msg.emisorRol == "CONDUCTOR") {
                    mostrarRespuestaConductor(msg.texto)
                } else {
                    mensajes.add(msg)
                    adapter.notifyItemInserted(mensajes.size - 1)
                    recyclerMensajes.scrollToPosition(mensajes.size - 1)
                }
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ── Respuesta del conductor ───────────────────────────────────

    private fun mostrarRespuestaConductor(texto: String) {
        tvRespuestaConductor.text = texto
        cardRespuestaConductor.visibility = View.VISIBLE
        indicadorNuevoMsg.visibility = View.VISIBLE
        vibrarNotificacion()
        cardRespuestaConductor.postDelayed({
            cardRespuestaConductor.visibility = View.GONE
            indicadorNuevoMsg.visibility = View.GONE
        }, 8000)
    }

    // ── Vibración ─────────────────────────────────────────────────

    private fun vibrar() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(80)
    }

    private fun vibrarLargo() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
    }

    private fun vibrarNotificacion() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 100, 80, 100), -1)
    }

    private enum class Tab { UBICACION, INSTRUCCIONES, PERSONAL }
}
