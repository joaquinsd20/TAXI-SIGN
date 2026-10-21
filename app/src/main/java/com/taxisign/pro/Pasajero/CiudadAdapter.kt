package com.taxisign.pro.pasajero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class CiudadAdapter(
    private val lista: List<Ciudad>,
    private val onClick: (Ciudad) -> Unit
) : RecyclerView.Adapter<CiudadAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreCiudad)
        val tvDepartamento: TextView = view.findViewById(R.id.tvDepartamentoCiudad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ciudad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ciudad = lista[position]
        holder.tvNombre.text = ciudad.nombre
        holder.tvDepartamento.text = ciudad.departamento
        holder.itemView.setOnClickListener { onClick(ciudad) }
    }

    override fun getItemCount(): Int = lista.size
}
