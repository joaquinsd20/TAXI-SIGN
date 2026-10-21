package com.taxisign.pro.pasajero

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.taxisign.pro.R

class HistorialViajesPasajeroActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    private val listaViajes =
        mutableListOf<Viaje>()

    private lateinit var adapter:
            HistorialViajesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_historial_viajes_pasajero
        )

        val btnBack =
            findViewById<ImageButton>(
                R.id.btn_back
            )

        recycler =
            findViewById(
                R.id.recycler_historial
            )

        btnBack.setOnClickListener {

            finish()
        }

        recycler.layoutManager =
            LinearLayoutManager(this)

        adapter =
            HistorialViajesAdapter(
                listaViajes
            )

        recycler.adapter =
            adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        val mAuth = FirebaseAuth.getInstance()
        val mDatabase = FirebaseDatabase.getInstance().reference
        val uid = mAuth.currentUser?.uid ?: ""

        if (uid.isNotEmpty()) {
            mDatabase.child("viajes")
                .orderByChild("pasajeroId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        listaViajes.clear()
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val destino = child.child("destino").value?.toString() ?: "Sin destino"
                                val estado = child.child("estado").value?.toString() ?: "Desconocido"
                                val timestamp = child.child("timestamp").value as? Long ?: 0L

                                val fecha = if (timestamp > 0) {
                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(timestamp))
                                } else {
                                    "Sin fecha"
                                }

                                // viajeId: usamos el campo guardado o, si falta, la propia key
                                val id = child.child("viajeId").value?.toString()
                                    ?: child.key ?: ""
                                val nombreConductor =
                                    child.child("nombreConductor").value?.toString() ?: "Conductor"
                                val nombrePasajero =
                                    child.child("nombrePasajero").value?.toString() ?: "Pasajero"
                                val origen =
                                    child.child("origen").value?.toString()
                                        ?: child.child("direccionRecojo").value?.toString() ?: ""
                                val tarifa =
                                    child.child("tarifa").getValue(Double::class.java) ?: 0.0

                                listaViajes.add(
                                    Viaje(
                                        destino = destino,
                                        fecha = fecha,
                                        estado = estado,
                                        id = id,
                                        nombreConductor = nombreConductor,
                                        nombrePasajero = nombrePasajero,
                                        origen = origen,
                                        tarifa = tarifa
                                    )
                                )
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@HistorialViajesPasajeroActivity,
                            "Error cargando historial: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {
            Toast.makeText(this, "Inicia sesión para ver tu historial", Toast.LENGTH_SHORT).show()
        }
    }
}