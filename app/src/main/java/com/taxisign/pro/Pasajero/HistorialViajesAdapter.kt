package com.taxisign.pro.pasajero

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class HistorialViajesAdapter(
    private val listaViajes: List<Viaje>
) : RecyclerView.Adapter<HistorialViajesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtDestino: TextView = view.findViewById(R.id.txt_destino)
        val txtFecha: TextView = view.findViewById(R.id.txt_fecha)
        val txtEstado: TextView = view.findViewById(R.id.txt_estado)
        val btnDetalle: TextView = view.findViewById(R.id.btn_detalle)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_viaje, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val viaje = listaViajes[position]

        holder.txtDestino.text = viaje.destino
        holder.txtFecha.text = viaje.fecha
        holder.txtEstado.text = viaje.estado

        holder.btnDetalle.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,
                DetalleViajeActivity::class.java
            ).apply {
                putExtra("VIAJE_ID", viaje.id)
                putExtra("ORIGEN", viaje.origen)
                putExtra("DESTINO", viaje.destino)
                putExtra("FECHA", viaje.fecha)
                putExtra("ESTADO", viaje.estado)
                putExtra("NOMBRE_CONDUCTOR", viaje.nombreConductor)
                putExtra("NOMBRE_PASAJERO", viaje.nombrePasajero)
                putExtra("TARIFA", viaje.tarifa)
            }

            holder.itemView.context.startActivity(intent)

        }

    }

    override fun getItemCount(): Int {
        return listaViajes.size
    }
}