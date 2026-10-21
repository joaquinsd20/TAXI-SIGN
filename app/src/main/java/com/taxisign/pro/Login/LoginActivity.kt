package com.taxisign.pro.Login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.conductor.PanelConductorActivity
import com.taxisign.pro.pasajero.PanelPasajeroActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rolActual: String

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    android.util.Log.e("LoginActivity", "idToken es null — revisa default_web_client_id / SHA-1 en Firebase")
                    mostrarToast("Error: configuración de Google Sign-In incompleta")
                } else {
                    firebaseAuthWithGoogle(idToken, account.displayName, account.email)
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginActivity", "ApiException código: ${e.statusCode}")
                mostrarToast("Error con Google (código ${e.statusCode})")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        auth = FirebaseAuth.getInstance()
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        rolActual = intent.getStringExtra("ROL_SELECCIONADO") ?: "PASAJERO"

        // El client ID tipo "Web" se genera automáticamente desde
        // google-services.json como R.string.default_web_client_id,
        // siempre que exista un cliente OAuth tipo "Web application"
        // configurado en Firebase Console → Authentication → Google.
        val stringResourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
        val webClientId = if (stringResourceId != 0) {
            getString(stringResourceId)
        } else {
            ""
        }

        if (webClientId.isEmpty()) {
            android.util.Log.e("LoginActivity", "default_web_client_id no encontrado — revisa google-services.json")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupUI()
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.btnBackLogin).setOnClickListener { finish() }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                mostrarToast("Completa todos los campos")
                vibrar(100)
                return@setOnClickListener
            }

            autenticarConFirebase(email, pass, btnLogin)
        }

        findViewById<CardView>(R.id.btnGoogleLogin).setOnClickListener {
            iniciarFlujoGoogle()
        }

        findViewById<TextView>(R.id.tvForgotPass).setOnClickListener {
            recuperarContrasena(etEmail.text.toString().trim())
        }

        findViewById<TextView>(R.id.tvIrARegistro).setOnClickListener {
            irAPantallaRegistro()
        }
        findViewById<TextView>(R.id.tvRegistroAhora).setOnClickListener {
            irAPantallaRegistro()
        }
    }

    // ── Login con email/contraseña ──────────────────────────────

    private fun autenticarConFirebase(email: String, pass: String, btn: Button) {
        btn.isEnabled = false
        btn.text = "Iniciando sesión..."

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val nombreAuth = auth.currentUser?.displayName
                    val emailAuth = auth.currentUser?.email
                    obtenerDatosUsuarioYRedirigir(uid, nombreAuth, emailAuth, btn)
                } else {
                    mostrarToast("Error: " + (task.exception?.localizedMessage ?: "Credenciales inválidas"))
                    vibrar(150)
                    btn.isEnabled = true
                    btn.text = "ENTRAR"
                }
            }
    }

    // ── Busca el perfil del usuario; si no existe, lo CREA ───────

    private fun obtenerDatosUsuarioYRedirigir(
        uid: String,
        nombreFallback: String?,
        emailFallback: String?,
        btn: Button
    ) {
        val databaseRef = FirebaseDatabase.getInstance().reference

        databaseRef.child("usuarios").child("pasajeros").child(uid).get()
            .addOnSuccessListener { snapshotPasajero ->
                if (snapshotPasajero.exists()) {
                    if (rolActual != "PASAJERO") {
                        rechazarIngresoPorRol(
                            btn,
                            "Esta cuenta ya está registrada como pasajero. Ingresa desde la opción Pasajero."
                        )
                        return@addOnSuccessListener
                    }

                    val nombre = snapshotPasajero.child("nombre").value?.toString()
                        ?: nombreFallback
                        ?: "Pasajero"

                    val email = snapshotPasajero.child("email").value?.toString()
                        ?: emailFallback
                        ?: ""

                    guardarSesionYRedirigir(uid, nombre, email, "PASAJERO")
                    return@addOnSuccessListener
                }

                databaseRef.child("usuarios").child("conductores").child(uid).get()
                    .addOnSuccessListener { snapshotConductor ->
                        if (snapshotConductor.exists()) {
                            if (rolActual != "CONDUCTOR") {
                                rechazarIngresoPorRol(
                                    btn,
                                    "Esta cuenta ya está registrada como conductor. Ingresa desde la opción Conductor."
                                )
                                return@addOnSuccessListener
                            }

                            val nombre = snapshotConductor.child("nombre").value?.toString()
                                ?: nombreFallback
                                ?: "Conductor"

                            val email = snapshotConductor.child("email").value?.toString()
                                ?: emailFallback
                                ?: ""

                            guardarSesionYRedirigir(uid, nombre, email, "CONDUCTOR")
                        } else {
                            crearPerfilYRedirigir(uid, nombreFallback, emailFallback, btn)
                        }
                    }
                    .addOnFailureListener {
                        mostrarToast("Error al conectar con la base de datos de conductores")
                        btn.isEnabled = true
                        btn.text = "ENTRAR"
                    }
            }
            .addOnFailureListener {
                mostrarToast("Error al conectar con el servidor")
                btn.isEnabled = true
                btn.text = "ENTRAR"
            }
    }

    private fun rechazarIngresoPorRol(btn: Button, mensaje: String) {
        mostrarToast(mensaje)
        vibrar(150)
        auth.signOut()
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut()
        }
        btn.isEnabled = true
        btn.text = "ENTRAR"
    }

    // ── Crea el perfil inicial (primer login con Google) ────────

    private fun crearPerfilYRedirigir(
        uid: String,
        nombreFallback: String?,
        emailFallback: String?,
        btn: Button
    ) {
        val nombre = nombreFallback ?: auth.currentUser?.displayName ?: "Usuario"
        val email = emailFallback ?: auth.currentUser?.email ?: ""

        if (rolActual == "CONDUCTOR") {
            val intent = Intent(this, CrearCuentaConductor::class.java).apply {
                putExtra("LOGIN_GOOGLE", true)
                putExtra("EXTRA_UID_GOOGLE", uid)
                putExtra("EXTRA_NOMBRE", nombre)
                putExtra("EXTRA_EMAIL", email)
            }
            startActivity(intent)
            finish()
            return
        }

        val datosPasajero = mapOf(
            "uid" to uid,
            "nombre" to nombre,
            "email" to email,
            "tipoUsuario" to "PASAJERO",
            "ciudadConfigurada" to false,
            "proveedor" to (auth.currentUser?.providerData
                ?.firstOrNull { it.providerId != "firebase" }?.providerId ?: "google.com")
        )

        FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child("pasajeros")
            .child(uid)
            .setValue(datosPasajero)
            .addOnSuccessListener {
                guardarSesionYRedirigir(uid, nombre, email, "PASAJERO")
            }
            .addOnFailureListener {
                mostrarToast("Error al crear tu perfil. Intenta de nuevo.")
                btn.isEnabled = true
                btn.text = "ENTRAR"
            }
    }

    // ── Google Sign-In ────────────────────────────────────────────

    private fun firebaseAuthWithGoogle(idToken: String, displayName: String?, email: String?) {
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.isEnabled = false
        btnLogin.text = "Iniciando sesión..."

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    obtenerDatosUsuarioYRedirigir(uid, displayName, email, btnLogin)
                } else {
                    mostrarToast("Error en Firebase Auth con Google")
                    btnLogin.isEnabled = true
                    btnLogin.text = "ENTRAR"
                }
            }
    }

    private fun recuperarContrasena(emailIngresado: String) {
        // Si ya escribió un correo, lo usamos; si no, lo pedimos en un diálogo
        if (emailIngresado.isNotEmpty()) {
            enviarCorreoRecuperacion(emailIngresado)
            return
        }
        val input = EditText(this).apply {
            hint = "Tu correo electrónico"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Te enviaremos un enlace para restablecer tu contraseña.")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = input.text.toString().trim()
                if (correo.isEmpty()) mostrarToast("Ingresa tu correo")
                else enviarCorreoRecuperacion(correo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarCorreoRecuperacion(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarToast("Correo no válido")
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                mostrarToast("Te enviamos un enlace a $email para restablecer tu contraseña")
            }
            .addOnFailureListener { e ->
                mostrarToast("No se pudo enviar: ${e.message}")
            }
    }

    private fun guardarSesionYRedirigir(uid: String, nombre: String, correo: String, rol: String) {
        SessionManager.idUsuario = uid.hashCode()
        SessionManager.nombreUsuario = nombre
        SessionManager.correoUsuario = correo
        SessionManager.rolUsuario = rol

        // Guardar el PROVEEDOR real de autenticación (google.com o password)
        val proveedor = auth.currentUser?.providerData
            ?.firstOrNull { it.providerId != "firebase" }?.providerId ?: "password"
        val nodoRol = if (rol == "CONDUCTOR") "conductores" else "pasajeros"
        if (uid.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("usuarios").child(nodoRol).child(uid)
                .child("proveedor").setValue(proveedor)
        }

        if (rol == "CONDUCTOR") {
            val intent = Intent(this, PanelConductorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        // Pasajero: verificamos si ya configuró su ciudad antes de ir al panel
        if (uid.isEmpty()) {
            irAPanelPasajero()
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("usuarios").child("pasajeros").child(uid)
            .child("ciudadConfigurada").get()
            .addOnSuccessListener { snap ->
                val yaConfiguro = snap.getValue(Boolean::class.java) ?: false
                if (yaConfiguro) {
                    irAPanelPasajero()
                } else {
                    irASeleccionarCiudad()
                }
            }
            .addOnFailureListener {
                irAPanelPasajero()
            }
    }

    private fun irAPanelPasajero() {
        val intent = Intent(this, PanelPasajeroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun irASeleccionarCiudad() {
        val intent = Intent(this, com.taxisign.pro.pasajero.SeleccionarCiudadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun irAPantallaRegistro() {
        val claseDestino = if (rolActual == "CONDUCTOR") {
            CrearCuentaConductor::class.java
        } else {
            CrearCuentaPasajero::class.java
        }

        val intent = Intent(this, claseDestino).apply {
            putExtra("ROL_SELECCIONADO", rolActual)
        }
        startActivity(intent)
    }

    private fun iniciarFlujoGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun mostrarToast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun vibrar(d: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(d, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(d)
        }
    }
}