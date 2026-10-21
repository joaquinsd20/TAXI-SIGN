package com.taxisign.pro.conductor

import android.os.Bundle
import android.view.View
import android.widget.RatingBar
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

class ComentariosConductor : AppCompatActivity() {

    private val lista = mutableListOf<Resena>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentarios_conductor)

        val tvPromedio = findViewById<TextView>(R.id.tvPromedioGrande)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBarPromedio)
        val tvBase = findViewById<TextView>(R.id.tvBaseViajes)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloComentarios)
        val tvSin = findViewById<TextView>(R.id.tvSinComentarios)
        val recycler = findViewById<RecyclerView>(R.id.recyclerComentarios)

        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ResenasAdapter(lista)
        recycler.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            tvSin.text = "Inicia sesión para ver tus reseñas."
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("usuarios").child("conductores").child(uid).child("resenas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lista.clear()
                    var suma = 0
                    // Solo comentarios con texto
                    val conComentario = mutableListOf<Resena>()
                    for (c in snapshot.children) {
                        val r = c.getValue(Resena::class.java) ?: continue
                        suma += r.calificacion
                        if (r.comentario.isNotBlank()) conComentario.add(r)
                    }
                    val total = snapshot.childrenCount.toInt()

                    if (total == 0) {
                        tvPromedio.text = "0.0"
                        ratingBar.rating = 0f
                        tvBase.text = "Aún no tienes calificaciones"
                    } else {
                        val prom = suma.toFloat() / total
                        tvPromedio.text = "%.1f".format(prom)
                        ratingBar.rating = prom
                        tvBase.text = "Basado en $total viaje(s) calificado(s)"
                    }

                    conComentario.sortByDescending { it.timestamp }
                    lista.addAll(conComentario)
                    adapter.notifyDataSetChanged()

                    // Si no hay comentarios recientes, no mostramos esa sección
                    if (lista.isEmpty()) {
                        tvTitulo.visibility = View.GONE
                        recycler.visibility = View.GONE
                        tvSin.visibility = View.VISIBLE
                    } else {
                        tvTitulo.visibility = View.VISIBLE
                        recycler.visibility = View.VISIBLE
                        tvSin.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
