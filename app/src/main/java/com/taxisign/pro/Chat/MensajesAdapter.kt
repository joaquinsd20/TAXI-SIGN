package com.taxisign.pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MensajesAdapter(
    private val mensajes: List<Mensaje>,
    private val miUid: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_MIO  = 0
        private const val TIPO_OTRO = 1
        private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int =
        if (mensajes[position].emisorId == miUid) TIPO_MIO else TIPO_OTRO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_MIO) {
            MioViewHolder(inflater.inflate(R.layout.item_mensaje_mio, parent, false))
        } else {
            OtroViewHolder(inflater.inflate(R.layout.item_mensaje_otro, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = mensajes[position]
        val hora = formatoHora.format(Date(msg.timestamp))
        when (holder) {
            is MioViewHolder  -> holder.bind(msg.texto, hora)
            is OtroViewHolder -> holder.bind(msg.texto, hora, msg.emisorRol)
        }
    }

    override fun getItemCount() = mensajes.size

    // ── Burbuja propia (derecha) ──────────────────────────────────
    class MioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMensaje: TextView = view.findViewById(R.id.tv_mensaje)
        private val tvHora: TextView    = view.findViewById(R.id.tv_hora)

        fun bind(texto: String, hora: String) {
            tvMensaje.text = texto
            tvHora.text    = hora
        }
    }

    // ── Burbuja del otro (izquierda) ──────────────────────────────
    // IDs coinciden exactamente con item_mensaje_otro.xml:
    //   @+id/tv_avatar_otro, @+id/tv_nombre_otro, @+id/tv_mensaje, @+id/tv_hora
    class OtroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMensaje: TextView = view.findViewById(R.id.tv_mensaje)
        private val tvHora: TextView    = view.findViewById(R.id.tv_hora)
        private val tvNombreOtro: TextView = view.findViewById(R.id.tv_nombre_otro)
        private val tvAvatarOtro: TextView = view.findViewById(R.id.tv_avatar_otro)

        fun bind(texto: String, hora: String, rol: String) {
            tvMensaje.text    = texto
            tvHora.text       = hora
            val nombre        = if (rol == "CONDUCTOR") "Conductor" else "Pasajero"
            tvNombreOtro.text = nombre
            tvAvatarOtro.text = nombre.first().uppercaseChar().toString()
        }
    }
}
