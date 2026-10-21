package com.taxisign.pro.conductor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class ResenasAdapter(
    private val lista: List<Resena>
) : RecyclerView.Adapter<ResenasAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tv_r_nombre)
        val estrellas: TextView = v.findViewById(R.id.tv_r_estrellas)
        val comentario: TextView = v.findViewById(R.id.tv_r_comentario)
        val fecha: TextView = v.findViewById(R.id.tv_r_fecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resena, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = lista[position]
        holder.nombre.text = r.nombrePasajero
        holder.estrellas.text = "★".repeat(r.calificacion.coerceIn(0, 5)) +
            "☆".repeat((5 - r.calificacion).coerceIn(0, 5))
        holder.comentario.text = if (r.comentario.isBlank()) "Sin comentario" else r.comentario
        holder.fecha.text = if (r.timestamp > 0) {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(r.timestamp))
        } else ""
    }

    override fun getItemCount() = lista.size
}
