package com.taxisign.pro.conductor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.InputStream

/**
 * ModificarDatosConductor
 *
 * Edita los datos personales del conductor.
 * ✦ Lee los datos actuales desde Firebase: usuarios/conductores/{uid}
 * ✦ AUTO-GUARDADO: cualquier cambio se guarda solo en Firebase (con un
 *   pequeño retardo "debounce" para no escribir en cada tecla).
 * ✦ Mantiene también el nombre en SharedPreferences porque
 *   PerfilConductorActivity lo usa como caché rápida.
 */
class ModificarDatosConductor : AppCompatActivity() {

    private lateinit var imgConductor: ImageView
    private var fotoSeleccionadaUri: Uri? = null
    private var fotoBase64Actual: String = ""

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }
    private val uid get() = auth.currentUser?.uid ?: ""

    // Debounce para el auto-guardado
    private val handler = Handler(Looper.getMainLooper())
    private var pendienteGuardar: Runnable? = null

    private lateinit var etNombre: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etCorreo: EditText

    private val seleccionarFotoGaleria =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                fotoSeleccionadaUri = it
                imgConductor.setImageURI(it)
                Toast.makeText(this, "Foto cargada", Toast.LENGTH_SHORT).show()
                guardarConRetardo() // también persistimos la foto
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_datos_conductor)

        val btnCancelar = findViewById<ImageButton>(R.id.btn_cancelar_edicion)
        val fabCambiarFoto = findViewById<FloatingActionButton>(R.id.fab_cambiar_foto)
        imgConductor = findViewById(R.id.img_editar_conductor)

        etNombre = findViewById(R.id.et_editar_nombre)
        etTelefono = findViewById(R.id.et_editar_telefono)
        etCorreo = findViewById(R.id.et_editar_correo)

        val btnGuardar = findViewById<Button>(R.id.btn_guardar_datos_personales)

        cargarDatosDesdeFirebase()

        btnCancelar.setOnClickListener { finish() }

        fabCambiarFoto.setOnClickListener {
            seleccionarFotoGaleria.launch("image/*")
        }

        // AUTO-GUARDADO al escribir
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { guardarConRetardo() }
        }
        etNombre.addTextChangedListener(watcher)
        etTelefono.addTextChangedListener(watcher)
        etCorreo.addTextChangedListener(watcher)

        // El botón sigue disponible para "guardar y salir"
        btnGuardar.setOnClickListener {
            if (validar()) {
                guardarAhora(mostrarToast = true)
                finish()
            }
        }
    }

    private fun cargarDatosDesdeFirebase() {
        if (uid.isEmpty()) return
        dbRoot.child("usuarios").child("conductores").child(uid).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) return@addOnSuccessListener
                etNombre.setText(snap.child("nombre").value?.toString() ?: "")
                etTelefono.setText(snap.child("telefono").value?.toString() ?: "")
                etCorreo.setText(
                    snap.child("email").value?.toString()
                        ?: snap.child("correo").value?.toString() ?: ""
                )
                fotoBase64Actual = snap.child("fotoPerfil").value?.toString() ?: ""
                mostrarFotoBase64(fotoBase64Actual)
            }
    }

    private fun validar(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        if (nombre.isEmpty() || telefono.isEmpty() || correo.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }
        if (telefono.length < 9) {
            Toast.makeText(this, "El teléfono debe tener al menos 9 dígitos", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /** Programa un guardado en 800 ms; si el usuario sigue escribiendo, se reinicia. */
    private fun guardarConRetardo() {
        pendienteGuardar?.let { handler.removeCallbacks(it) }
        pendienteGuardar = Runnable { guardarAhora(mostrarToast = false) }
        handler.postDelayed(pendienteGuardar!!, 800)
    }

    private fun guardarAhora(mostrarToast: Boolean) {
        if (uid.isEmpty()) return

        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        if (nombre.isEmpty()) return // evita guardar vacío en plena edición

        val nuevaFoto = if (fotoSeleccionadaUri != null) {
            convertirUriABase64(fotoSeleccionadaUri!!)
        } else fotoBase64Actual

        val datos = hashMapOf<String, Any>(
            "uid" to uid,
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to correo,
            "fotoPerfil" to nuevaFoto,
            "tipoUsuario" to "CONDUCTOR"
        )

        dbRoot.child("usuarios").child("conductores").child(uid)
            .updateChildren(datos)
            .addOnSuccessListener {
                // Caché local del nombre (la usa PerfilConductorActivity)
                getSharedPreferences("TaxiSignPrefs", Context.MODE_PRIVATE)
                    .edit().putString("nombre_conductor", nombre).apply()
                if (mostrarToast) {
                    Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (mostrarToast) {
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun convertirUriABase64(uri: Uri): String {
        return try {
            val input: InputStream? = contentResolver.openInputStream(uri)
            val bytes = input?.readBytes()
            input?.close()
            if (bytes != null) "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT)
            else fotoBase64Actual
        } catch (e: Exception) { fotoBase64Actual }
    }

    private fun mostrarFotoBase64(base64: String) {
        try {
            if (base64.isEmpty()) return
            val limpio = base64
                .replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
            val bytes = Base64.decode(limpio, Base64.DEFAULT)
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imgConductor.setImageBitmap(bmp)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        pendienteGuardar?.let { handler.removeCallbacks(it) }
    }
}
