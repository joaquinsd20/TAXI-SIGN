package com.taxisign.pro.pasajero

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R
import java.util.Locale

class HistorialViajesAdapter(
    private val listaViajes: List<Viaje>
) : RecyclerView.Adapter<HistorialViajesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDestino: TextView = view.findViewById(R.id.txt_destino)
        val txtOrigen: TextView = view.findViewById(R.id.txt_origen)
        val txtFecha: TextView = view.findViewById(R.id.txt_fecha)
        val txtEstado: TextView = view.findViewById(R.id.txt_estado)
        val txtConductor: TextView = view.findViewById(R.id.txt_conductor)
        val txtTarifa: TextView = view.findViewById(R.id.txt_tarifa)
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
        holder.txtOrigen.text = viaje.origen.ifBlank { "Punto de recojo" }
        holder.txtFecha.text = viaje.fecha
        holder.txtConductor.text = viaje.nombreConductor.ifBlank { "Conductor" }
        holder.txtTarifa.text = if (viaje.tarifa > 0) {
            String.format(Locale.US, "S/ %.2f", viaje.tarifa)
        } else {
            "—"
        }

        // Estado con color: verde completado, rojo cancelado, amarillo en curso
        val (texto, colorTexto, colorFondo) = estiloEstado(viaje.estado)
        holder.txtEstado.text = texto
        holder.txtEstado.setTextColor(colorTexto)
        holder.txtEstado.backgroundTintList = ColorStateList.valueOf(colorFondo)

        val abrirDetalle = View.OnClickListener {
            val intent = Intent(holder.itemView.context, DetalleViajeActivity::class.java).apply {
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
        holder.itemView.setOnClickListener(abrirDetalle)
        holder.btnDetalle.setOnClickListener(abrirDetalle)
    }

    override fun getItemCount(): Int = listaViajes.size

    private fun estiloEstado(estado: String): Triple<String, Int, Int> {
        val e = estado.lowercase(Locale.getDefault())
        return when {
            "cancel" in e || "rechaz" in e ->
                Triple("Cancelado", Color.parseColor("#F87171"), Color.parseColor("#3A1E1E"))
            "final" in e || "complet" in e || "termin" in e ->
                Triple("Completado", Color.parseColor("#4ADE80"), Color.parseColor("#1A3D2A"))
            e.isBlank() || "descon" in e ->
                Triple("Sin estado", Color.parseColor("#9AA3B8"), Color.parseColor("#1E2740"))
            else ->
                Triple(estado.replaceFirstChar { it.uppercase() }, Color.parseColor("#FFD600"), Color.parseColor("#3A3410"))
        }
    }
}
