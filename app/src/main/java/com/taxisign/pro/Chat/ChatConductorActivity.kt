package com.taxisign.pro

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
import com.google.firebase.database.*

class ChatConductorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatConductor"
    }

    private lateinit var chatId: String

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance()
            .getReference("chats/$chatId/mensajes")
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

    private var switchModoSenas: androidx.appcompat.widget.SwitchCompat? = null
    private var switchModoVoz: androidx.appcompat.widget.SwitchCompat? = null

    private var modoSenasAuto = false
    private var modoVoz = false
    private var tts: android.speech.tts.TextToSpeech? = null
    private var ultimoMensajePasajero = ""

    private val prefs by lazy {
        getSharedPreferences("TaxiSignPrefs", MODE_PRIVATE)
    }

    private val dbRoot by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private val mensajes = mutableListOf<Mensaje>()
    private lateinit var adapter: MensajesAdapter

    @Suppress("DEPRECATION")
    private val vibrator by lazy {
        getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_conductor)

        val id = intent.getStringExtra("CHAT_ID")

        if (id.isNullOrEmpty()) {
            Log.e(TAG, "CHAT_ID vacío — cerrando")
            Toast.makeText(
                this,
                "Error: chat no disponible",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        chatId = id

        val user = auth.currentUser

        Log.d(
            TAG,
            "Usuario actual: ${user?.uid} / email: ${user?.email}"
        )

        if (user == null) {
            Log.e(TAG, "Sin sesión activa en Firebase Auth")
        }

        val nombrePasajero =
            intent.getStringExtra("NOMBRE_PASAJERO") ?: "Pasajero"

        bindViews()
        configurarHeader(nombrePasajero)
        configurarRecycler()
        configurarRespuestasRapidas()
        configurarMensajeLibre()
        configurarAccesibilidad()
        escucharMensajes()

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun bindViews() {
        recyclerMensajes = findViewById(R.id.recycler_mensajes)
        tvNombrePasajero = findViewById(R.id.tv_nombre_pasajero)
        tvAvatarPasajero = findViewById(R.id.tv_avatar_pasajero)

        btnOk = findViewById(R.id.btn_ok)
        btnEnseguida = findViewById(R.id.btn_enseguida)
        btnDondeEstas = findViewById(R.id.btn_donde_estas)
        btnEntendido = findViewById(R.id.btn_entendido)
        btnNoEncuentro = findViewById(R.id.btn_no_encuentro)
        btnLlegue = findViewById(R.id.btn_llegue)

        btnEscribir = findViewById(R.id.btn_escribir)
        etMensajeLibre = findViewById(R.id.et_mensaje_libre)
        btnEnviarLibre = findViewById(R.id.btn_enviar_libre)

        switchModoSenas = findViewById(R.id.switch_modo_senas)
        switchModoVoz = findViewById(R.id.switch_modo_voz)
    }

    private fun configurarHeader(nombrePasajero: String) {
        tvNombrePasajero.text = nombrePasajero
        tvAvatarPasajero.text =
            nombrePasajero.firstOrNull()?.uppercaseChar()?.toString() ?: "P"

        findViewById<ImageButton>(R.id.btn_volver).setOnClickListener {
            vibrar()
            finish()
        }
    }

    private fun configurarRecycler() {
        val uid = auth.currentUser?.uid ?: ""

        adapter = MensajesAdapter(
            mensajes,
            uid
        )

        recyclerMensajes.layoutManager =
            LinearLayoutManager(this).apply {
                stackFromEnd = true
            }

        recyclerMensajes.adapter = adapter
    }

    private fun configurarRespuestasRapidas() {
        mapOf(
            btnOk to "👍 OK",
            btnEnseguida to "⏱️ ENSEGUIDA VOY",
            btnDondeEstas to "📍 ¿DÓNDE ESTÁS?",
            btnEntendido to "✅ ENTENDIDO",
            btnNoEncuentro to "❓ NO TE ENCUENTRO",
            btnLlegue to "🚕 YA LLEGUÉ"
        ).forEach { (btn, texto) ->

            btn.setOnClickListener {
                Log.d(TAG, "Botón tocado: $texto")
                vibrar()
                enviarMensaje(texto)
            }

            btn.setOnLongClickListener {
                vibrar()

                val textoLimpio =
                    texto.replace(
                        Regex("[^\\p{L}\\p{N} ]"),
                        ""
                    ).trim()

                SeñasBottomSheet
                    .newInstance(textoLimpio)
                    .show(
                        supportFragmentManager,
                        "senas"
                    )

                true
            }
        }
    }

    private fun configurarMensajeLibre() {
        btnEscribir.setOnClickListener {
            vibrar()

            findViewById<View>(
                R.id.contenedor_respuestas_rapidas
            ).visibility = View.GONE

            btnEscribir.visibility = View.GONE
            etMensajeLibre.visibility = View.VISIBLE
            btnEnviarLibre.visibility = View.VISIBLE

            etMensajeLibre.requestFocus()
        }

        btnEnviarLibre.setOnClickListener {
            val texto =
                etMensajeLibre.text.toString().trim()

            if (texto.isEmpty()) {
                Toast.makeText(
                    this,
                    "Escribe algo primero",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibrar()
            enviarMensaje(texto)

            etMensajeLibre.setText("")
            etMensajeLibre.visibility = View.GONE
            btnEnviarLibre.visibility = View.GONE
            btnEscribir.visibility = View.VISIBLE

            findViewById<View>(
                R.id.contenedor_respuestas_rapidas
            ).visibility = View.VISIBLE
        }

        btnEnviarLibre.setOnLongClickListener {
            val texto =
                etMensajeLibre.text.toString().trim()

            if (texto.isEmpty()) {
                Toast.makeText(
                    this,
                    "Escribe algo primero",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnLongClickListener true
            }

            vibrar()

            SeñasBottomSheet
                .newInstance(texto)
                .show(
                    supportFragmentManager,
                    "senas"
                )

            true
        }
    }

    private fun configurarAccesibilidad() {
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale("es", "ES")
            }
        }

        modoSenasAuto =
            prefs.getBoolean("cond_modo_senas", false)

        modoVoz =
            prefs.getBoolean("cond_modo_voz", false)

        switchModoSenas?.isChecked = modoSenasAuto
        switchModoVoz?.isChecked = modoVoz

        val uid = auth.currentUser?.uid

        if (!uid.isNullOrEmpty()) {
            dbRoot.child("usuarios")
                .child("conductores")
                .child(uid)
                .get()
                .addOnSuccessListener { snapshot ->

                    snapshot.child("preferenciaSenas")
                        .getValue(Boolean::class.java)
                        ?.let {
                            modoSenasAuto = it
                            switchModoSenas?.isChecked = it
                        }

                    snapshot.child("preferenciaVoz")
                        .getValue(Boolean::class.java)
                        ?.let {
                            modoVoz = it
                            switchModoVoz?.isChecked = it
                        }
                }
        }

        switchModoSenas?.setOnCheckedChangeListener { _, isChecked ->
            modoSenasAuto = isChecked
            vibrar()

            prefs.edit()
                .putBoolean("cond_modo_senas", isChecked)
                .apply()

            auth.currentUser?.uid?.let { uidConductor ->
                dbRoot.child("usuarios")
                    .child("conductores")
                    .child(uidConductor)
                    .child("preferenciaSenas")
                    .setValue(isChecked)
            }

            if (isChecked && ultimoMensajePasajero.isNotEmpty()) {
                mostrarEnSenas(ultimoMensajePasajero)
            }
        }

        switchModoVoz?.setOnCheckedChangeListener { _, isChecked ->
            modoVoz = isChecked
            vibrar()

            prefs.edit()
                .putBoolean("cond_modo_voz", isChecked)
                .apply()

            auth.currentUser?.uid?.let { uidConductor ->
                dbRoot.child("usuarios")
                    .child("conductores")
                    .child(uidConductor)
                    .child("preferenciaVoz")
                    .setValue(isChecked)
            }

            if (isChecked) {
                Toast.makeText(
                    this,
                    "Leeré en voz alta los mensajes del pasajero",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarEnSenas(texto: String) {
        SeñasBottomSheet
            .newInstance(texto)
            .show(
                supportFragmentManager,
                "senas"
            )
    }

    private fun hablar(texto: String) {
        if (texto.isBlank()) return

        val limpio =
            texto.replace(
                Regex("[^\\p{L}\\p{N} ,.?!¿¡]"),
                ""
            ).trim()

        if (limpio.isEmpty()) return

        tts?.speak(
            limpio,
            android.speech.tts.TextToSpeech.QUEUE_FLUSH,
            null,
            "msg"
        )
    }

    private fun enviarMensaje(texto: String) {
        val uid =
            auth.currentUser?.uid ?: run {
                Log.w(
                    TAG,
                    "Sin sesión — usando uid temporal para prueba"
                )
                "conductor_sin_sesion"
            }

        val mensaje = Mensaje(
            texto = texto,
            emisorId = uid,
            emisorRol = "CONDUCTOR",
            timestamp = System.currentTimeMillis()
        )

        database.push()
            .setValue(mensaje)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Enviado: $texto")
                Toast.makeText(
                    this,
                    "✓ Enviado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Log.e(
                    TAG,
                    "✗ Error al enviar: ${error.message}"
                )
                Toast.makeText(
                    this,
                    "Error Firebase: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun escucharMensajes() {
        Log.d(
            TAG,
            "Escuchando: chats/$chatId/mensajes"
        )

        database.addChildEventListener(
            object : ChildEventListener {

                override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                    val msg =
                        snapshot.getValue(Mensaje::class.java)
                            ?: return

                    Log.d(
                        TAG,
                        "Recibido: ${msg.texto} [${msg.emisorRol}]"
                    )

                    mensajes.add(msg)
                    adapter.notifyItemInserted(mensajes.size - 1)
                    recyclerMensajes.scrollToPosition(mensajes.size - 1)

                    if (msg.emisorRol == "PASAJERO") {
                        ultimoMensajePasajero = msg.texto

                        if (msg.texto.contains("SOS")) {
                            vibrarEmergencia()
                        } else {
                            vibrarNotificacion()
                        }

                        if (modoSenasAuto) {
                            mostrarEnSenas(msg.texto)
                        }

                        if (modoVoz) {
                            hablar(msg.texto)
                        }
                    }
                }

                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {}

                override fun onChildRemoved(
                    snapshot: DataSnapshot
                ) {}

                override fun onChildMoved(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {}

                override fun onCancelled(
                    error: DatabaseError
                ) {
                    Log.e(
                        TAG,
                        "Firebase cancelado: ${error.message}"
                    )

                    Toast.makeText(
                        this@ChatConductorActivity,
                        "Error Firebase: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
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
            vibrator.vibrate(
                longArrayOf(0, 100, 80, 100),
                -1
            )
        }
    }

    private fun vibrarEmergencia() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(
                        0,
                        300,
                        150,
                        300,
                        150,
                        300
                    ),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                longArrayOf(
                    0,
                    300,
                    150,
                    300,
                    150,
                    300
                ),
                -1
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        tts?.stop()
        tts?.shutdown()
    }
}