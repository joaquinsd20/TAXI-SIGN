package com.taxisign.pro.conductor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class SolicitudesAdapter(
    private val lista: List<SolicitudItem>,
    private val onAceptar: (SolicitudItem) -> Unit
) : RecyclerView.Adapter<SolicitudesAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.tv_s_nombre)
        val badge: TextView = v.findViewById(R.id.tv_s_badge)
        val ruta: TextView = v.findViewById(R.id.tv_s_ruta)
        val precio: TextView = v.findViewById(R.id.tv_s_precio)
        val btn: TextView = v.findViewById(R.id.btn_s_ver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = lista[position]
        holder.nombre.text = s.nombrePasajero
        holder.badge.visibility = if (s.esSordoMudo) View.VISIBLE else View.GONE
        holder.ruta.text = "📍 ${s.direccionRecojo}\n🏁 ${s.direccionDestino}"
        val precioTexto = if (s.precio.isNotBlank() && s.precio != "Por calcular")
            s.precio else "S/ %.2f".format(s.tarifa)
        val distanciaTexto = if (s.distancia.isNotBlank() && s.distancia != "Por calcular")
            " • ${s.distancia}" else ""
        holder.precio.text = "$precioTexto$distanciaTexto"
        val accion = { onAceptar(s) }
        holder.itemView.setOnClickListener { accion() }
        holder.btn.setOnClickListener { accion() }
    }

    override fun getItemCount() = lista.size
}
