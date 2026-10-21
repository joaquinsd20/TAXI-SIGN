package com.taxisign.pro.pasajero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class LugarRecienteAdapter(
    private val lista: List<LugarReciente>,
    private val onClick: (LugarReciente) -> Unit
) : RecyclerView.Adapter<LugarRecienteAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDireccion: TextView = view.findViewById(R.id.tvRecienteDireccion)
        val tvFrecuencia: TextView = view.findViewById(R.id.tvRecienteFrecuencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar_reciente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lugar = lista[position]
        holder.tvDireccion.text = lugar.direccion
        holder.tvFrecuencia.text = if (lugar.vecesUsado > 1) {
            "Usado ${lugar.vecesUsado} veces"
        } else {
            "Búsqueda reciente"
        }
        holder.itemView.setOnClickListener { onClick(lugar) }
    }

    override fun getItemCount(): Int = lista.size
}
