package com.taxisign.pro.pasajero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R

class DireccionAdapter(

    private val lista: List<Direccion>,

    private val onClick: (Direccion) -> Unit

) : RecyclerView.Adapter<DireccionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val txtDireccion: TextView =
            view.findViewById(R.id.txtDireccion)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_direccion,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val direccion =
            lista[position]

        holder.txtDireccion.text =
            direccion.nombre

        holder.itemView.setOnClickListener {

            onClick(direccion)
        }
    }

    override fun getItemCount(): Int {

        return lista.size
    }
}