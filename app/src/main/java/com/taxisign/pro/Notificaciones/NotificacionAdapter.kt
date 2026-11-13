package com.taxisign.pro.notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class NotificacionAdapter(
    private val lista: List<Notificacion>
) : RecyclerView.Adapter<NotificacionAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icono: ImageView = v.findViewById(R.id.ivNotificationIcon)
        val tipo: TextView = v.findViewById(R.id.tvNotificationType)
        val hora: TextView = v.findViewById(R.id.tvNotificationTime)
        val indicador: View = v.findViewById(R.id.unreadIndicator)
        val titulo: TextView = v.findViewById(R.id.tvNotificationTitle)
        val mensaje: TextView = v.findViewById(R.id.tvNotificationMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = lista[position]
        holder.tipo.text = etiquetaTipo(n.tipo)
        holder.titulo.text = n.titulo
        holder.mensaje.text = n.mensaje
        holder.hora.text = tiempoRelativo(n.timestamp)
        holder.indicador.visibility = if (n.leida) View.GONE else View.VISIBLE
        holder.mensaje.visibility = if (n.mensaje.isBlank()) View.GONE else View.VISIBLE
        holder.icono.setImageResource(iconoTipo(n.tipo))

        // Color por tipo de aviso (icono y etiqueta) y fondo resaltado si no se ha leido
        val color = android.graphics.Color.parseColor(colorTipo(n.tipo))
        holder.icono.imageTintList = android.content.res.ColorStateList.valueOf(color)
        holder.tipo.setTextColor(color)
        holder.itemView.setBackgroundResource(
            if (n.leida) R.drawable.bg_card_viaje else R.drawable.bg_card_noti_nueva
        )
    }

    private fun colorTipo(tipo: String) = when (tipo) {
        "SEGURIDAD" -> "#F87171"
        "LLEGADA" -> "#4ADE80"
        "GANANCIA" -> "#60A5FA"
        else -> "#FFD600"
    }

    override fun getItemCount() = lista.size

    private fun etiquetaTipo(tipo: String) = when (tipo) {
        "VIAJE" -> "Viaje"
        "LLEGADA" -> "Tu conductor"
        "SEGURIDAD" -> "Seguridad"
        "GANANCIA" -> "Ganancia"
        else -> "Aviso"
    }

    private fun iconoTipo(tipo: String) = when (tipo) {
        "SEGURIDAD" -> R.drawable.ic_warning
        "LLEGADA" -> R.drawable.ic_location_pin
        "VIAJE" -> R.drawable.ic_taxi
        "GANANCIA" -> R.drawable.ic_star
        else -> R.drawable.ic_notifications
    }

    private fun tiempoRelativo(ts: Long): String {
        if (ts <= 0) return ""
        val diff = System.currentTimeMillis() - ts
        val min = diff / 60000
        return when {
            min < 1 -> "Ahora"
            min < 60 -> "Hace $min min"
            min < 1440 -> "Hace ${min / 60} h"
            else -> "Hace ${min / 1440} d"
        }
    }
}
