package com.taxisign.pro.pasajero

import android.widget.TextView
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.InputStream

class PerfilPasajeroActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null

    private lateinit var ivPerfil: ImageView
    private lateinit var btnEditarFoto: ImageButton

    private var uriFotoPerfil: Uri? = null
    private var fotoBase64Actual: String = ""

    private val seleccionarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uriFotoPerfil = it
                ivPerfil.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_pasajero)

        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnGuardar = findViewById<LinearLayout>(R.id.btn_guardar)

        ivPerfil = findViewById(R.id.iv_perfil)
        btnEditarFoto = findViewById(R.id.btnEditarFoto)

        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etCorreo = findViewById<EditText>(R.id.et_correo)
        val etTelefono = findViewById<EditText>(R.id.et_telefono)
        val etDni = findViewById<EditText>(R.id.et_dni)
        val etTipoDiscapacidad = findViewById<EditText>(R.id.et_tipo_discapacidad)

        val mAuth = FirebaseAuth.getInstance()
        val mDatabase = FirebaseDatabase.getInstance().reference
        val uid = mAuth.currentUser?.uid ?: ""

        etNombre.setText(SessionManager.nombreUsuario)
        etCorreo.setText(SessionManager.correoUsuario)
        etTelefono.setText(SessionManager.telefonoUsuario)
        etDni.setText(SessionManager.dniUsuario)

        val tvNombreUsuario = findViewById<TextView>(R.id.tvNombreUsuario)
        tvNombreUsuario.text = SessionManager.nombreUsuario.ifEmpty { "Pasajero" }
        cargarRatingPasajero()

        if (uid.isNotEmpty()) {
            mDatabase.child("usuarios").child("pasajeros").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val nombre = snapshot.child("nombre").value?.toString() ?: ""
                        val correo = snapshot.child("email").value?.toString()
                            ?: snapshot.child("correo").value?.toString()
                            ?: ""
                        val telefono = snapshot.child("telefono").value?.toString() ?: ""
                        val dni = snapshot.child("dni").value?.toString() ?: ""
                        val tipoDiscapacidad =
                            snapshot.child("tipoDiscapacidad").value?.toString() ?: ""
                        val fotoPerfil =
                            snapshot.child("fotoPerfil").value?.toString() ?: ""

                        etNombre.setText(nombre)
                        tvNombreUsuario.text = nombre.ifEmpty { "Pasajero" }
                        etCorreo.setText(correo)
                        etTelefono.setText(telefono)
                        etDni.setText(dni)
                        etTipoDiscapacidad.setText(tipoDiscapacidad)

                        fotoBase64Actual = fotoPerfil
                        mostrarFotoBase64(fotoPerfil)

                        SessionManager.nombreUsuario = nombre
                        SessionManager.correoUsuario = correo
                        SessionManager.telefonoUsuario = telefono
                        SessionManager.dniUsuario = dni
                    }
                }
        }

        btnBack.setOnClickListener {
            vibrarBreve()
            finish()
        }

        btnEditarFoto.setOnClickListener {
            vibrarBreve()
            seleccionarFotoLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val tipoDiscapacidad = etTipoDiscapacidad.text.toString().trim()

            if (
                nombre.isEmpty() ||
                correo.isEmpty() ||
                telefono.isEmpty() ||
                dni.isEmpty() ||
                tipoDiscapacidad.isEmpty()
            ) {
                vibrarBreve()
                Toast.makeText(this, "Completa toda la información", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                vibrarBreve()
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (telefono.length < 9) {
                vibrarBreve()
                Toast.makeText(this, "Número de teléfono inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dni.length != 8) {
                vibrarBreve()
                Toast.makeText(this, "DNI inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vibrarBreve()

            val nuevaFotoBase64 = if (uriFotoPerfil != null) {
                convertirUriABase64(uriFotoPerfil!!)
            } else {
                fotoBase64Actual
            }

            if (uid.isNotEmpty()) {
                val esSordoMudo =
                    tipoDiscapacidad.contains("sordo", ignoreCase = true) ||
                            tipoDiscapacidad.contains("mudo", ignoreCase = true)

                val datos = hashMapOf<String, Any>(
                    "uid" to uid,
                    "nombre" to nombre,
                    "email" to correo,
                    "telefono" to telefono,
                    "dni" to dni,
                    "tipoDiscapacidad" to tipoDiscapacidad,
                    "esSordoMudo" to esSordoMudo,
                    "fotoPerfil" to nuevaFotoBase64,
                    "tipoUsuario" to "PASAJERO"
                )

                btnGuardar.isEnabled = false

                mDatabase.child("usuarios")
                    .child("pasajeros")
                    .child(uid)
                    .updateChildren(datos)
                    .addOnSuccessListener {
                        SessionManager.nombreUsuario = nombre
                        SessionManager.correoUsuario = correo
                        SessionManager.telefonoUsuario = telefono
                        SessionManager.dniUsuario = dni

                        Toast.makeText(
                            this,
                            "Perfil actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this, PanelPasajeroActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnGuardar.isEnabled = true
                        Toast.makeText(
                            this,
                            "Error al guardar: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(
                    this,
                    "Error: usuario no autenticado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun convertirUriABase64(uri: Uri): String {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT)
            } else {
                fotoBase64Actual
            }
        } catch (e: Exception) {
            fotoBase64Actual
        }
    }

    private fun cargarRatingPasajero() {
        val tv = findViewById<TextView>(R.id.tvRatingPasajero)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("usuarios").child("pasajeros").child(uid).child("resenas")
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    var suma = 0
                    var n = 0
                    for (c in snapshot.children) {
                        val cal = c.child("calificacion").getValue(Int::class.java) ?: continue
                        suma += cal; n++
                    }
                    tv.text = if (n == 0) "⭐ Sin calificaciones todavía"
                        else "⭐ %.1f  (%d viaje%s)".format(suma.toDouble() / n, n, if (n == 1) "" else "s")
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun mostrarFotoBase64(base64Data: String) {
        try {
            if (base64Data.isEmpty()) return

            val limpio = base64Data
                .replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")

            val bytes = Base64.decode(limpio, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            ivPerfil.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun vibrarBreve() {
        vibrator?.let {
            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        60,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(60)
            }
        }
    }
}