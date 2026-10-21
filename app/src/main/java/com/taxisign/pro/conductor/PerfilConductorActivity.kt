package com.taxisign.pro.conductor

import android.content.Context // 👈 IMPORTANTE: Para leer SharedPreferences
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.MainActivity
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager

class PerfilConductorActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator


    private lateinit var tvNombre: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_conductor)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // 1. Referencias de Información y Navegación
        val btnBack = findViewById<ImageButton>(R.id.btn_back_driver)
        val btnIrAEditarPerfil = findViewById<ImageButton>(R.id.btn_ir_a_editar)
        tvNombre = findViewById(R.id.tvNombrePerfil) // Inicialización vinculada
        val tvModeloAuto = findViewById<TextView>(R.id.tvAutoModelo)

        // 2. Referencias de Edición y Acción
        val etPlaca = findViewById<EditText>(R.id.et_placa)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_driver)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        cargarResenas()



        // 3. Lógica del botón Volver (Flecha arriba)
        btnBack.setOnClickListener {
            vibrarBreve()
            finish()
        }

        // 4. Lógica para abrir la pantalla de edición
        btnIrAEditarPerfil.setOnClickListener {
            vibrarBreve()
            val intent = Intent(this, ModificarDatosConductor::class.java)
            startActivity(intent)
        }

        // 5. Lógica de Guardar Datos del Vehículo
        btnGuardar.setOnClickListener {
            val placa = etPlaca.text.toString().trim().uppercase()

            if (placa.isNotEmpty() && placa.length >= 6) {
                vibrarBreve()
                Toast.makeText(this, "Vehículo [$placa] actualizado", Toast.LENGTH_SHORT).show()
                etPlaca.error = null
            } else {
                vibrarBreve()
                etPlaca.error = "Ingresa una placa válida (mín. 6 caracteres)"
            }
        }

        // 6. Lógica de Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            vibrarBreve()

            // Cerrar sesión REAL en Firebase (si no, la cuenta sigue activa y al volver
            // a abrir la app te reingresa automáticamente)
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

            // Limpiar datos locales de sesión
            val sharedPreferences = getSharedPreferences("TaxiSignPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().remove("nombre_conductor").apply()
            com.taxisign.pro.SessionManager.idUsuario = 0
            com.taxisign.pro.SessionManager.nombreUsuario = ""
            com.taxisign.pro.SessionManager.correoUsuario = ""
            com.taxisign.pro.SessionManager.rolUsuario = ""

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }

    // 🔄 SE EJECUTA SIEMPRE QUE REGRESAS A ESTA PANTALLA
    override fun onResume() {
        super.onResume()
        actualizarInterfazNombre()
    }

    private fun cargarResenas() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerResenas)
        val tvPromedio = findViewById<TextView>(R.id.tvRatingPromedio)
        val tvCount = findViewById<TextView>(R.id.tvRatingCount)

        val lista = mutableListOf<Resena>()
        val adapter = ResenasAdapter(lista)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recycler.adapter = adapter

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("usuarios").child("conductores").child(uid).child("resenas")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    lista.clear()
                    var suma = 0
                    for (child in snapshot.children) {
                        val r = child.getValue(Resena::class.java) ?: continue
                        lista.add(r)
                        suma += r.calificacion
                    }
                    lista.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()

                    if (lista.isEmpty()) {
                        tvPromedio.text = "⭐ 0.0"
                        tvCount.text = "Sin reseñas todavía"
                    } else {
                        val prom = suma.toDouble() / lista.size
                        tvPromedio.text = "⭐ %.1f".format(prom)
                        tvCount.text = "${lista.size} reseña(s)"
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun actualizarInterfazNombre() {
        // Leemos el valor fresco guardado en SharedPreferences
        val sharedPreferences = getSharedPreferences("TaxiSignPrefs", Context.MODE_PRIVATE)
        val nombreGuardado = sharedPreferences.getString("nombre_conductor", null)

        if (nombreGuardado != null) {
            // Sincronizamos SessionManager por si acaso lo usas en otras vistas
            SessionManager.nombreUsuario = nombreGuardado
            tvNombre.text = nombreGuardado
        } else {
            // Si por algún motivo está vacío, usa el de SessionManager o un fallback seguro
            tvNombre.text = SessionManager.nombreUsuario ?: "Conductor"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrarBreve() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}