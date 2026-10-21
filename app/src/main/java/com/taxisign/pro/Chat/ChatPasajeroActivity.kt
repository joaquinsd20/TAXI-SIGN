package com.taxisign.pro

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatPasajeroActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var chatId: String
    private lateinit var database: DatabaseReference
    private var mensajesListener: ChildEventListener? = null

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

    private lateinit var switchModoSenas: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchModoVoz: androidx.appcompat.widget.SwitchCompat
    private lateinit var btnVerSenas: MaterialButton

    // Preferencia: si el pasajero prefiere ver TODO en señas automáticamente
    private var modoSenasAuto = false
    // Preferencia: si el teléfono "habla" por el pasajero (texto a voz)
    private var modoVoz = false
    private var tts: android.speech.tts.TextToSpeech? = null
    // Último mensaje recibido del conductor (para el botón "Ver en señas")
    private var ultimoMensajeConductor = ""

    private val prefs by lazy {
        getSharedPreferences("TaxiSignPrefs", MODE_PRIVATE)
    }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: MensajesAdapter

    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_pasajero)

        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        val nombreConductor = intent.getStringExtra("NOMBRE_CONDUCTOR") ?: "Conductor"

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Error: chat no disponible", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(chatId)
            .child("mensajes")

        bindViews()
        configurarHeader(nombreConductor)
        configurarRecycler()
        configurarTabs()
        configurarBotonesPredefinidos()
        configurarMensajeLibre()
        configurarModoSenas()
        configurarModoVoz()
        escucharMensajes()
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun bindViews() {
        recyclerMensajes = findViewById(R.id.recycler_mensajes)
        cardRespuestaConductor = findViewById(R.id.card_respuesta_conductor)
        tvRespuestaConductor = findViewById(R.id.tv_respuesta_conductor)
        tvNombreConductor = findViewById(R.id.tv_nombre_conductor)
        tvAvatarConductor = findViewById(R.id.tv_avatar_conductor)
        indicadorNuevoMsg = findViewById(R.id.indicador_nuevo_msg)

        tabUbicacion = findViewById(R.id.tab_ubicacion)
        tabInstrucciones = findViewById(R.id.tab_instrucciones)
        tabPersonal = findViewById(R.id.tab_personal)

        gridUbicacion = findViewById(R.id.grid_ubicacion)
        gridInstrucciones = findViewById(R.id.grid_instrucciones)
        gridPersonal = findViewById(R.id.grid_personal)

        btnMensajeLibre = findViewById(R.id.btn_mensaje_libre)
        etMensajeLibre = findViewById(R.id.et_mensaje_libre)
        btnEnviarLibre = findViewById(R.id.btn_enviar_libre)

        switchModoSenas = findViewById(R.id.switch_modo_senas)
        switchModoVoz = findViewById(R.id.switch_modo_voz)
        btnVerSenas = findViewById(R.id.btn_ver_senas)
    }

    private fun configurarHeader(nombreConductor: String) {
        tvNombreConductor.text = nombreConductor
        tvAvatarConductor.text = nombreConductor.firstOrNull()?.uppercaseChar()?.toString() ?: "C"

        findViewById<ImageButton>(R.id.btn_volver).setOnClickListener {
            vibrar()
            finish()
        }
    }

    private fun configurarRecycler() {
        adapter = MensajesAdapter(mensajes, auth.currentUser?.uid ?: "")
        recyclerMensajes.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerMensajes.adapter = adapter
    }

    private fun configurarTabs() {
        tabUbicacion.setOnClickListener { cambiarTab(Tab.UBICACION) }
        tabInstrucciones.setOnClickListener { cambiarTab(Tab.INSTRUCCIONES) }
        tabPersonal.setOnClickListener { cambiarTab(Tab.PERSONAL) }

        cambiarTab(Tab.UBICACION)
    }

    private fun cambiarTab(tab: Tab) {
        vibrar()

        gridUbicacion.visibility = View.GONE
        gridInstrucciones.visibility = View.GONE
        gridPersonal.visibility = View.GONE

        val amarillo = getColor(R.color.yellow_primary)
        val gris = getColor(android.R.color.darker_gray)

        tabUbicacion.setTextColor(gris)
        tabInstrucciones.setTextColor(gris)
        tabPersonal.setTextColor(gris)

        when (tab) {
            Tab.UBICACION -> {
                gridUbicacion.visibility = View.VISIBLE
                tabUbicacion.setTextColor(amarillo)
            }

            Tab.INSTRUCCIONES -> {
                gridInstrucciones.visibility = View.VISIBLE
                tabInstrucciones.setTextColor(amarillo)
            }

            Tab.PERSONAL -> {
                gridPersonal.visibility = View.VISIBLE
                tabPersonal.setTextColor(amarillo)
            }
        }
    }

    private fun configurarBotonesPredefinidos() {
        val predefinidos = mapOf(
            R.id.btn_estoy_aqui to "📍 Estoy aquí",
            R.id.btn_sigo_caminando to "🚶 Sigo caminando",
            R.id.btn_esperame to "⏳ Espérame un momento",
            R.id.btn_estoy_saliendo to "🚪 Estoy saliendo",
            R.id.btn_no_puedo_hablar to "🤟 No puedo hablar, por favor usa el chat",
            R.id.btn_usa_chat to "💬 Por favor comunícate conmigo por chat"
        )

        predefinidos.forEach { (id, texto) ->
            findViewById<MaterialButton>(id).setOnClickListener {
                vibrar()
                enviarMensaje(texto)
            }
        }
    }

    private fun configurarMensajeLibre() {

        btnMensajeLibre.setOnClickListener {

            vibrar()

            gridUbicacion.visibility = View.GONE
            gridInstrucciones.visibility = View.GONE
            gridPersonal.visibility = View.GONE

            btnMensajeLibre.visibility = View.GONE
            etMensajeLibre.visibility = View.VISIBLE
            btnEnviarLibre.visibility = View.VISIBLE

            etMensajeLibre.requestFocus()
        }

        btnEnviarLibre.setOnClickListener {

            val texto = etMensajeLibre.text.toString().trim()

            if (texto.isEmpty()) {

                Toast.makeText(
                    this,
                    "Escribe un mensaje",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            vibrar()

            enviarMensaje(texto)

            etMensajeLibre.setText("")

            // Ocultar teclado
            val imm =
                getSystemService(INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager

            imm.hideSoftInputFromWindow(
                etMensajeLibre.windowToken,
                0
            )

            etMensajeLibre.clearFocus()

            etMensajeLibre.visibility = View.GONE
            btnEnviarLibre.visibility = View.GONE
            btnMensajeLibre.visibility = View.VISIBLE

            cambiarTab(Tab.UBICACION)
        }
    }

    private fun configurarModoSenas() {
        // 1) Carga inmediata desde preferencias locales
        modoSenasAuto = prefs.getBoolean("modo_senas_auto", false)
        switchModoSenas.isChecked = modoSenasAuto

        // 2) Sincroniza con Firebase (fuente de verdad del perfil del pasajero)
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            dbRoot.child("usuarios").child("pasajeros").child(uid)
                .child("preferenciaSenas").get()
                .addOnSuccessListener { snap ->
                    val valor = snap.getValue(Boolean::class.java)
                    if (valor != null) {
                        modoSenasAuto = valor
                        switchModoSenas.isChecked = valor
                        prefs.edit().putBoolean("modo_senas_auto", valor).apply()
                    }
                }
        }

        // 3) Al cambiar el switch: guarda local + Firebase (auto-guardado)
        switchModoSenas.setOnCheckedChangeListener { _, isChecked ->
            modoSenasAuto = isChecked
            vibrar()
            prefs.edit().putBoolean("modo_senas_auto", isChecked).apply()

            val u = auth.currentUser?.uid
            if (!u.isNullOrEmpty()) {
                dbRoot.child("usuarios").child("pasajeros").child(u)
                    .child("preferenciaSenas").setValue(isChecked)
            }

            if (isChecked) {
                Toast.makeText(this, "Modo señas activado", Toast.LENGTH_SHORT).show()
                // Si ya hay un mensaje del conductor, muéstralo en señas al instante
                if (ultimoMensajeConductor.isNotEmpty()) mostrarEnSenas(ultimoMensajeConductor)
            } else {
                Toast.makeText(this, "Modo señas desactivado", Toast.LENGTH_SHORT).show()
            }
        }

        // 4) Botón manual "Ver en señas" (siempre disponible)
        btnVerSenas.setOnClickListener {
            vibrar()
            if (ultimoMensajeConductor.isEmpty()) {
                Toast.makeText(this, "Aún no hay mensaje del conductor", Toast.LENGTH_SHORT).show()
            } else {
                mostrarEnSenas(ultimoMensajeConductor)
            }
        }
    }

    private fun mostrarEnSenas(texto: String) {
        SeñasBottomSheet.newInstance(texto)
            .show(supportFragmentManager, "senas")
    }

    private fun configurarModoVoz() {
        // Inicializa el motor de voz en español
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale("es", "ES")
            }
        }

        // Carga preferencia local + Firebase
        modoVoz = prefs.getBoolean("modo_voz", false)
        switchModoVoz.isChecked = modoVoz

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            dbRoot.child("usuarios").child("pasajeros").child(uid)
                .child("preferenciaVoz").get()
                .addOnSuccessListener { snap ->
                    snap.getValue(Boolean::class.java)?.let {
                        modoVoz = it
                        switchModoVoz.isChecked = it
                        prefs.edit().putBoolean("modo_voz", it).apply()
                    }
                }
        }

        switchModoVoz.setOnCheckedChangeListener { _, isChecked ->
            modoVoz = isChecked
            vibrar()
            prefs.edit().putBoolean("modo_voz", isChecked).apply()
            auth.currentUser?.uid?.let { u ->
                dbRoot.child("usuarios").child("pasajeros").child(u)
                    .child("preferenciaVoz").setValue(isChecked)
            }
            if (isChecked) {
                Toast.makeText(this, "El teléfono hablará por ti", Toast.LENGTH_SHORT).show()
                hablar("Hola, voy a comunicarme contigo por voz")
            }
        }
    }

    /** Convierte texto a voz (el pasajero "habla" al conductor oyente). */
    private fun hablar(texto: String) {
        if (texto.isBlank()) return
        // Quitamos emojis para que no los lea
        val limpio = texto.replace(Regex("[^\\p{L}\\p{N} ,.?!¿¡]"), "").trim()
        if (limpio.isEmpty()) return
        tts?.speak(limpio, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "msg")
    }

    private fun enviarMensaje(texto: String) {
        val uid = auth.currentUser?.uid ?: ""

        if (uid.isEmpty()) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val mensaje = Mensaje(
            texto = texto,
            emisorId = uid,
            emisorRol = "PASAJERO",
            timestamp = System.currentTimeMillis()
        )

        // Si el modo voz está activo, el teléfono "habla" por el pasajero
        if (modoVoz) hablar(texto)

        database.push().setValue(mensaje)
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escucharMensajes() {
        mensajesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(Mensaje::class.java) ?: return

                mensajes.add(msg)
                adapter.notifyItemInserted(mensajes.size - 1)
                recyclerMensajes.scrollToPosition(mensajes.size - 1)

                if (msg.emisorRol == "CONDUCTOR") {
                    ultimoMensajeConductor = msg.texto
                    mostrarRespuestaConductor(msg.texto)
                    // Aviso visual: flash de cámara (para personas sordas)
                    com.taxisign.pro.notificaciones.NotificacionHelper.flashCamara(this@ChatPasajeroActivity)
                    // Si el pasajero eligió "no entiendo el texto", lo traducimos
                    if (modoSenasAuto) mostrarEnSenas(msg.texto)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        database.addChildEventListener(mensajesListener!!)
    }

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

    private fun vibrar() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    80,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    private fun vibrarNotificacion() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 80, 100),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 80, 100), -1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        tts?.stop()
        tts?.shutdown()

        mensajesListener?.let {
            database.removeEventListener(it)
        }
    }

    private enum class Tab {
        UBICACION,
        INSTRUCCIONES,
        PERSONAL
    }
}