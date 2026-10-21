package com.taxisign.pro.conductor

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ActivityGanancias
 *
 * Muestra TODO lo que el conductor ha ganado, leído de Firebase:
 *   usuarios/conductores/{uid}/ganancias
 * - Monto total acumulado
 * - Cantidad de viajes
 * - Detalle viaje por viaje (RecyclerView)
 */
class ActivityGanancias : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    private lateinit var tvMontoTotal: TextView
    private lateinit var tvTotalViajes: TextView
    private lateinit var tvHorasOnline: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var tvSinGanancias: TextView

    private val lista = mutableListOf<GananciaViaje>()
    private lateinit var adapter: GananciasAdapter

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ganancias)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val btnBack = findViewById<ImageButton>(R.id.btnBackGanancias)
        val btnVerHistorial = findViewById<Button>(R.id.btnVerHistorialDetallado)

        tvMontoTotal = findViewById(R.id.tvMontoTotalGanado)
        tvTotalViajes = findViewById(R.id.tvTotalViajes)
        tvHorasOnline = findViewById(R.id.tvHorasOnline)
        recycler = findViewById(R.id.recyclerGanancias)
        tvSinGanancias = findViewById(R.id.tvSinGanancias)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = GananciasAdapter(lista)
        recycler.adapter = adapter

        btnBack.setOnClickListener { vibrar(30); finish() }
        btnVerHistorial.setOnClickListener {
            vibrar(40)
            // El desglose ya se ve abajo; este botón refresca por si acaso
            cargarGanancias()
        }

        cargarGanancias()
    }

    private fun cargarGanancias() {
        if (uid.isEmpty()) {
            mostrarVacio(true)
            return
        }
        db.child("usuarios").child("conductores").child(uid).child("ganancias")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    var total = 0.0
                    for (child in snapshot.children) {
                        val g = child.getValue(GananciaViaje::class.java) ?: continue
                        lista.add(g)
                        total += g.monto
                    }
                    lista.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()

                    tvMontoTotal.text = "S/ ${"%.2f".format(total)}"
                    tvTotalViajes.text = lista.size.toString()
                    // Estimación simple: ~25 min de servicio por viaje
                    val horas = lista.size * 25.0 / 60.0
                    tvHorasOnline.text = "${"%.1f".format(horas)} h"

                    mostrarVacio(lista.isEmpty())
                }
                override fun onCancelled(error: DatabaseError) {
                    mostrarVacio(true)
                }
            })
    }

    private fun mostrarVacio(vacio: Boolean) {
        tvSinGanancias.visibility = if (vacio) View.VISIBLE else View.GONE
        recycler.visibility = if (vacio) View.GONE else View.VISIBLE
        if (vacio) {
            tvMontoTotal.text = "S/ 0.00"
            tvTotalViajes.text = "0"
            tvHorasOnline.text = "0.0 h"
        }
    }

    private fun vibrar(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(ms)
        }
    }
}
