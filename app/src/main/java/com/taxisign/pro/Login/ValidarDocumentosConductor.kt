package com.taxisign.pro.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import java.io.InputStream

class ValidarDocumentosConductor : AppCompatActivity() {

    private lateinit var tvEstadoBrevete: TextView
    private lateinit var tvEstadoSoat: TextView
    private lateinit var tvEstadoAntecedentes: TextView

    private lateinit var btnSubirBrevete: Button
    private lateinit var btnSubirSoat: Button
    private lateinit var btnSubirAntecedentes: Button
    private lateinit var btnFinalizarRegistroConductor: Button

    private var uriBrevete: Uri? = null
    private var uriSoat: Uri? = null
    private var uriAntecedentes: Uri? = null

    // Datos recibidos de la pantalla anterior
    private var nombre: String = ""
    private var dni: String = ""
    private var telefono: String = ""
    private var placa: String = ""
    private var modeloVehiculo: String = ""
    private var colorVehiculo: String = ""
    private var email: String = ""
    private var password: String = ""

    // true si el conductor ya está autenticado vía Google (no se crea
    // usuario nuevo en Auth, se reutiliza el UID existente)
    private var esRegistroGoogle: Boolean = false
    private var uidGoogle: String = ""

    private var registroFallido = false

    // Instancias de Firebase (Solo Auth y Realtime Database, evitamos Storage)
    private val mAuth = FirebaseAuth.getInstance()
    private val mDatabase = FirebaseDatabase.getInstance().reference

    private val selectBreveteLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uriBrevete = it; actualizarUI(tvEstadoBrevete, it) }
    }

    private val selectSoatLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uriSoat = it; actualizarUI(tvEstadoSoat, it) }
    }

    private val selectAntecedentesLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uriAntecedentes = it; actualizarUI(tvEstadoAntecedentes, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.validar_documentos_conductor)

        // Recuperar datos de registro del Intent anterior
        esRegistroGoogle = intent.getBooleanExtra("LOGIN_GOOGLE", false)
        uidGoogle = intent.getStringExtra("EXTRA_UID_GOOGLE") ?: ""

        nombre = intent.getStringExtra("EXTRA_NOMBRE") ?: ""
        dni = intent.getStringExtra("EXTRA_DNI") ?: ""
        telefono = intent.getStringExtra("EXTRA_TELEFONO") ?: ""
        placa = intent.getStringExtra("EXTRA_PLACA") ?: ""
        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        password = intent.getStringExtra("EXTRA_PASSWORD") ?: ""
        modeloVehiculo = intent.getStringExtra("EXTRA_MODELO_VEHICULO") ?: ""
        colorVehiculo = intent.getStringExtra("EXTRA_COLOR_VEHICULO") ?: ""

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        tvEstadoBrevete = findViewById(R.id.tvEstadoBrevete)
        tvEstadoSoat = findViewById(R.id.tvEstadoSoat)
        tvEstadoAntecedentes = findViewById(R.id.tvEstadoAntecedentes)
        btnSubirBrevete = findViewById(R.id.btnSubirBrevete)
        btnSubirSoat = findViewById(R.id.btnSubirSoat)
        btnSubirAntecedentes = findViewById(R.id.btnSubirAntecedentes)
        btnFinalizarRegistroConductor = findViewById(R.id.btnFinalizarRegistroConductor)
    }

    private fun setupClickListeners() {
        btnSubirBrevete.setOnClickListener { selectBreveteLauncher.launch("*/*") }
        btnSubirSoat.setOnClickListener { selectSoatLauncher.launch("*/*") }
        btnSubirAntecedentes.setOnClickListener { selectAntecedentesLauncher.launch("*/*") }

        btnFinalizarRegistroConductor.setOnClickListener {
            if (uriBrevete != null && uriSoat != null && uriAntecedentes != null) {
                val faltanDatosBase = nombre.isEmpty() || dni.isEmpty() || placa.isEmpty() ||
                        modeloVehiculo.isEmpty() || colorVehiculo.isEmpty() || email.isEmpty()

                // En el registro tradicional (correo/contraseña) además se exige password.
                // En el registro con Google, el usuario ya está autenticado y no aplica.
                val faltanDatosCuenta = if (esRegistroGoogle) {
                    uidGoogle.isEmpty()
                } else {
                    password.isEmpty()
                }

                if (faltanDatosBase || faltanDatosCuenta) {
                    Toast.makeText(this, "Error: Faltan datos de registro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnFinalizarRegistroConductor.isEnabled = false
                btnFinalizarRegistroConductor.text = "REGISTRANDO..."
                registroFallido = false

                if (esRegistroGoogle) {
                    // El usuario ya existe en Firebase Auth (vía Google):
                    // reutilizamos su UID, no se crea una cuenta nueva.
                    procesarDocumentosABase64(uidGoogle)
                } else {
                    crearUsuarioConductor()
                }
            } else {
                Toast.makeText(this, "Selecciona los 3 documentos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearUsuarioConductor() {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid ?: ""
                    procesarDocumentosABase64(uid)
                } else {
                    val mensaje = if (task.exception is FirebaseAuthUserCollisionException) {
                        "Esta cuenta ya existe. Inicia sesión o usa otro correo."
                    } else {
                        task.exception?.localizedMessage ?: "Error al registrar credenciales"
                    }
                    restaurarBotonRegistro(mensaje)
                }
            }
    }

    // =========================================================================
    // BYPASS MAESTRO: Convertimos los archivos locales a Base64 (Texto Plano)
    // =========================================================================
    private fun procesarDocumentosABase64(uid: String) {
        btnFinalizarRegistroConductor.text = "PROCESANDO ARCHIVOS..."

        val base64Brevete = convertirUriABase64(uriBrevete!!)
        val base64Soat = convertirUriABase64(uriSoat!!)
        val base64Antecedentes = convertirUriABase64(uriAntecedentes!!)

        if (base64Brevete != null && base64Soat != null && base64Antecedentes != null) {
            val documentosMap = mapOf(
                "brevete" to "data:image/jpeg;base64,$base64Brevete",
                "soat" to "data:image/jpeg;base64,$base64Soat",
                "antecedentes" to "data:image/jpeg;base64,$base64Antecedentes"
            )
            guardarDatosEnDatabase(uid, documentosMap)
        } else {
            eliminarUsuarioCreado("Error crítico al procesar la codificación de tus documentos locales.")
        }
    }

    private fun convertirUriABase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun guardarDatosEnDatabase(uid: String, documentos: Map<String, String>) {
        if (registroFallido) return
        btnFinalizarRegistroConductor.text = "GUARDANDO EN NUBE..."

        val datos = mapOf(
            "uid" to uid,
            "nombre" to nombre,
            "dni" to dni,
            "telefono" to telefono,
            "placa" to placa,
            "email" to email,
            "modeloVehiculo" to modeloVehiculo,
            "colorVehiculo" to colorVehiculo,
            "documentos" to documentos,
            "estadoAprobacion" to "pendiente",
            "tipoUsuario" to "CONDUCTOR",
            "proveedor" to if (esRegistroGoogle) "google.com" else "password"
        )

        mDatabase.child("usuarios").child("conductores").child(uid).setValue(datos)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, EsperaAprobacionConductor::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    eliminarUsuarioCreado(task.exception?.localizedMessage ?: "Error al guardar datos")
                }
            }
    }

    private fun eliminarUsuarioCreado(error: String) {
        if (registroFallido) return
        registroFallido = true

        // En el flujo con Google, el usuario ya existía antes de llegar
        // a esta pantalla (no lo creamos nosotros), así que NO se borra
        // su cuenta de Firebase Auth si falla el guardado: simplemente
        // dejamos que reintente subir sus documentos.
        if (esRegistroGoogle) {
            restaurarBotonRegistro(error)
            return
        }

        mAuth.currentUser?.delete()?.addOnCompleteListener {
            restaurarBotonRegistro(error)
        } ?: run {
            restaurarBotonRegistro(error)
        }
    }

    private fun restaurarBotonRegistro(mensajeError: String) {
        btnFinalizarRegistroConductor.isEnabled = true
        btnFinalizarRegistroConductor.text = "FINALIZAR REGISTRO"
        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
    }

    private fun actualizarUI(tv: TextView, uri: Uri) {
        val nombreArchivo = obtenerNombreArchivo(uri)
        tv.text = "✓ $nombreArchivo"
        tv.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var resultado: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        resultado = it.getString(index)
                    }
                }
            }
        }
        if (resultado == null) {
            resultado = uri.path
            val cut = resultado?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                resultado = resultado?.substring(cut + 1)
            }
        }
        return resultado ?: "Documento"
    }
}