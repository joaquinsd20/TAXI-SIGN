package com.taxisign.pro.pasajero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class OfertasAdapter(
    private val lista: List<OfertaConductor>,
    private val onElegir: (OfertaConductor) -> Unit
) : RecyclerView.Adapter<OfertasAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tv_of_nombre)
        val rating: TextView = v.findViewById(R.id.tv_of_rating)
        val placa: TextView = v.findViewById(R.id.tv_of_placa)
        val tarifa: TextView = v.findViewById(R.id.tv_of_tarifa)
        val btn: TextView = v.findViewById(R.id.btn_of_elegir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_oferta_conductor, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = lista[position]
        holder.nombre.text = o.nombreConductor
        holder.rating.text = if (o.rating > 0) "⭐ %.1f".format(o.rating) else "⭐ Nuevo"
        holder.placa.text = o.placa
        val precio = o.precioEnMesa()
        holder.tarifa.text = if (precio > 0) "S/ %.2f".format(precio) else ""
        holder.btn.text = if (o.turno == "pasajero") "RESPONDER" else "ESPERANDO"
        holder.btn.setOnClickListener { onElegir(o) }
        holder.itemView.setOnClickListener { onElegir(o) }
    }

    override fun getItemCount() = lista.size
}
