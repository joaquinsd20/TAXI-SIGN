package com.taxisign.pro.pasajero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class DestinosFrecuentesAdapter(
    private val listaDestinos: List<DestinoFrecuente>
) : RecyclerView.Adapter<DestinosFrecuentesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtNombre: TextView = view.findViewById(R.id.txt_nombre)
        val txtDireccion: TextView = view.findViewById(R.id.txt_direccion)
        val btnSeleccionar: TextView = view.findViewById(R.id.btn_seleccionar)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destino_frecuente, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val destino = listaDestinos[position]

        holder.txtNombre.text = destino.nombre
        holder.txtDireccion.text = destino.direccion

        holder.btnSeleccionar.setOnClickListener {

            Toast.makeText(
                holder.itemView.context,
                "Destino seleccionado",
                Toast.LENGTH_SHORT
            ).show()

        }

    }

    override fun getItemCount(): Int {
        return listaDestinos.size
    }
}