package com.example.taxisign.conductor

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.taxisign.MainActivity
import com.example.taxisign.R
import com.example.taxisign.SessionManager
import com.example.taxisign.conductor.ModificarDatosConductor


class PerfilConductorActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Vinculación con tu archivo XML actualizado
        setContentView(R.layout.perfil_conductor)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // 2. Referencias de Información y Navegación
        val btnBack = findViewById<ImageButton>(R.id.btn_back_driver)
        val btnIrAEditarPerfil = findViewById<ImageButton>(R.id.btn_ir_a_editar) // NUEVO BOTÓN DETECTADO
        val tvNombre = findViewById<TextView>(R.id.tvNombrePerfil)
        val tvModeloAuto = findViewById<TextView>(R.id.tvAutoModelo)

        // 3. Referencias de Edición y Acción (Sincronizadas con el diseño neón)
        val etPlaca = findViewById<EditText>(R.id.et_placa)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_driver)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        // 4. Cargar datos iniciales
        tvNombre.text = SessionManager.nombreUsuario ?: "com/example/taxisign/conductor"
        // Aquí podrías setear el modelo si lo tienes en el SessionManager
        // tvModeloAuto.text = SessionManager.modeloAuto

        // 5. Lógica del botón Volver (Flecha arriba)
        btnBack.setOnClickListener {
            vibrarBreve()
            finish()
        }

        // ══════════════════════════════════════════
        //  LÓGICA DEL NUEVO BOTÓN DE EDICIÓN
        // ══════════════════════════════════════════
        btnIrAEditarPerfil.setOnClickListener {
            vibrarBreve()
            // Abre la pantalla para modificar foto, nombre, teléfono y contraseña
            val intent = Intent(this, ModificarDatosConductor::class.java)
            startActivity(intent)
        }

        // 6. Lógica de Guardar Datos del Vehículo
        btnGuardar.setOnClickListener {
            val placa = etPlaca.text.toString().trim().uppercase()

            if (placa.isNotEmpty() && placa.length >= 6) {
                vibrarBreve()
                // Aquí iría tu lógica para guardar en la base de datos o SessionManager
                Toast.makeText(this, "Vehículo [$placa] actualizado", Toast.LENGTH_SHORT).show()
                etPlaca.error = null
            } else {
                vibrarBreve()
                etPlaca.error = "Ingresa una placa válida (mín. 6 caracteres)"
            }
        }

        // 7. Lógica de Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            vibrarBreve()

            // Opcional: limpiar sesión
            // SessionManager.logout()

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            // Ir a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrarBreve() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}