package com.taxisign.pro.pasajero

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.InputStream

/**
 * EditarPerfilPasajeroActivity
 *
 * Edición rápida del perfil del pasajero con AUTO-GUARDADO en Firebase
 * (usuarios/pasajeros/{uid}). Cada cambio se guarda solo tras una breve
 * pausa de escritura (debounce). No sobreescribe dni ni tipoDiscapacidad,
 * que se editan en la pantalla de perfil completo.
 */
class EditarPerfilPasajeroActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }
    private val uid get() = auth.currentUser?.uid ?: ""

    private lateinit var edtNombre: EditText
    private lateinit var edtCorreo: EditText
    private lateinit var edtTelefono: EditText
    private lateinit var imgPerfil: ImageView

    private var fotoUri: Uri? = null
    private var fotoBase64Actual: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var pendiente: Runnable? = null

    private val seleccionarFoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                fotoUri = it
                imgPerfil.setImageURI(it)
                guardarConRetardo()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil_pasajero)

        edtNombre = findViewById(R.id.edt_nombre)
        edtCorreo = findViewById(R.id.edt_correo)
        edtTelefono = findViewById(R.id.edt_telefono)
        imgPerfil = findViewById(R.id.img_perfil)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_cambiar_foto).setOnClickListener {
            seleccionarFoto.launch("image/*")
        }

        // Carga inicial: SessionManager (instantáneo) + Firebase (completo)
        edtNombre.setText(SessionManager.nombreUsuario)
        edtCorreo.setText(SessionManager.correoUsuario)
        edtTelefono.setText(SessionManager.telefonoUsuario)
        cargarDesdeFirebase()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { guardarConRetardo() }
        }
        edtNombre.addTextChangedListener(watcher)
        edtCorreo.addTextChangedListener(watcher)
        edtTelefono.addTextChangedListener(watcher)

        // Botón guardar: guarda y sale
        findViewById<View>(R.id.btn_guardar).setOnClickListener {
            if (validar()) {
                guardarAhora(true)
                finish()
            }
        }
    }

    private fun cargarDesdeFirebase() {
        if (uid.isEmpty()) return
        dbRoot.child("usuarios").child("pasajeros").child(uid).get()
            .addOnSuccessListener { s ->
                if (!s.exists()) return@addOnSuccessListener
                edtNombre.setText(s.child("nombre").value?.toString() ?: "")
                edtCorreo.setText(
                    s.child("email").value?.toString()
                        ?: s.child("correo").value?.toString() ?: ""
                )
                edtTelefono.setText(s.child("telefono").value?.toString() ?: "")
                fotoBase64Actual = s.child("fotoPerfil").value?.toString() ?: ""
                mostrarFoto(fotoBase64Actual)
            }
    }

    private fun validar(): Boolean {
        if (edtNombre.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun guardarConRetardo() {
        pendiente?.let { handler.removeCallbacks(it) }
        pendiente = Runnable { guardarAhora(false) }
        handler.postDelayed(pendiente!!, 800)
    }

    private fun guardarAhora(mostrarToast: Boolean) {
        if (uid.isEmpty()) return
        val nombre = edtNombre.text.toString().trim()
        if (nombre.isEmpty()) return

        val correo = edtCorreo.text.toString().trim()
        val telefono = edtTelefono.text.toString().trim()

        val nuevaFoto = if (fotoUri != null) convertirUriABase64(fotoUri!!) else fotoBase64Actual

        val datos = hashMapOf<String, Any>(
            "nombre" to nombre,
            "email" to correo,
            "telefono" to telefono,
            "fotoPerfil" to nuevaFoto
        )

        dbRoot.child("usuarios").child("pasajeros").child(uid)
            .updateChildren(datos)
            .addOnSuccessListener {
                SessionManager.nombreUsuario = nombre
                SessionManager.correoUsuario = correo
                SessionManager.telefonoUsuario = telefono
                if (mostrarToast) {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (mostrarToast) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun mostrarFoto(base64: String) {
        try {
            if (base64.isEmpty()) return
            val limpio = base64
                .replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
            val bytes = Base64.decode(limpio, Base64.DEFAULT)
            imgPerfil.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        pendiente?.let { handler.removeCallbacks(it) }
    }
}
