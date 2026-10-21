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
        val detalles: View = v.findViewById(R.id.detailsContainer)
        val botones: View = v.findViewById(R.id.actionButtonsContainer)
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
        // Estos bloques son opcionales en cada aviso; los ocultamos por defecto
        holder.detalles.visibility = View.GONE
        holder.botones.visibility = View.GONE
        holder.icono.setImageResource(iconoTipo(n.tipo))
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
        "SEGURIDAD" -> android.R.drawable.ic_dialog_alert
        "LLEGADA" -> android.R.drawable.ic_menu_mylocation
        "GANANCIA" -> android.R.drawable.ic_menu_save
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
