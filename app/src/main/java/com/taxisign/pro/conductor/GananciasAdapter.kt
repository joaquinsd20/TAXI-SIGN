package com.taxisign.pro.conductor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class GananciasAdapter(
    private val lista: List<GananciaViaje>
) : RecyclerView.Adapter<GananciasAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val destino: TextView = v.findViewById(R.id.tv_g_destino)
        val pasajero: TextView = v.findViewById(R.id.tv_g_pasajero)
        val fecha: TextView = v.findViewById(R.id.tv_g_fecha)
        val monto: TextView = v.findViewById(R.id.tv_g_monto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ganancia, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = lista[position]
        holder.destino.text = g.destino
        holder.pasajero.text = "Pasajero: ${g.pasajero}"
        holder.monto.text = "S/ %.2f".format(g.monto)
        holder.fecha.text = if (g.timestamp > 0) {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(g.timestamp))
        } else ""
    }

    override fun getItemCount() = lista.size
}
