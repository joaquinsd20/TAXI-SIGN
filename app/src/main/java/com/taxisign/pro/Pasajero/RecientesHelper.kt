package com.taxisign.pro.pasajero

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

/**
 * RecientesHelper
 *
 * Maneja el historial de destinos recientes del pasajero, similar a
 * InDriver: cada destino real que confirma se guarda automáticamente,
 * y si lo vuelve a usar, solo se incrementa su contador en lugar de
 * crear un duplicado. Esto permite mostrar primero los lugares a los
 * que el pasajero va con más frecuencia.
 */
object RecientesHelper {

    private const val MAX_RECIENTES_GUARDADOS = 30

    private fun refRecientes(uid: String) =
        FirebaseDatabase.getInstance().reference
            .child("usuarios").child("pasajeros").child(uid).child("recientes")

    /** Genera un id estable a partir de la dirección, para no duplicar el mismo lugar. */
    private fun idParaDireccion(direccion: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(direccion.lowercase().trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Guarda (o actualiza) un destino confirmado como reciente.
     * Si ya existía, solo aumenta el contador de veces usado y refresca la fecha.
     */
    fun registrarDestinoUsado(uid: String, direccion: String, lat: Double, lng: Double) {
        if (uid.isEmpty() || direccion.isBlank()) return

        val id = idParaDireccion(direccion)
        val ref = refRecientes(uid).child(id)

        ref.get().addOnSuccessListener { snap ->
            val vecesPrevias = snap.child("vecesUsado").getValue(Int::class.java) ?: 0
            val lugar = LugarReciente(
                id = id,
                direccion = direccion,
                lat = lat,
                lng = lng,
                vecesUsado = vecesPrevias + 1,
                ultimaVezUsado = System.currentTimeMillis()
            )
            ref.setValue(lugar)
            limpiarExcedentes(uid)
        }
    }

    /**
     * Carga los lugares recientes ordenados por relevancia: primero los más usados,
     * y entre empates, los más recientes. Ideal para mostrarlos al abrir el buscador,
     * antes de que el pasajero escriba algo.
     */
    fun cargarRecientes(uid: String, onResult: (List<LugarReciente>) -> Unit) {
        if (uid.isEmpty()) {
            onResult(emptyList())
            return
        }
        refRecientes(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { it.getValue(LugarReciente::class.java) }
                    .sortedWith(compareByDescending<LugarReciente> { it.vecesUsado }
                        .thenByDescending { it.ultimaVezUsado })
                onResult(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    /** Si hay demasiados recientes guardados, elimina los menos usados/antiguos. */
    private fun limpiarExcedentes(uid: String) {
        refRecientes(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { it.getValue(LugarReciente::class.java) }
                if (lista.size <= MAX_RECIENTES_GUARDADOS) return

                val aEliminar = lista
                    .sortedWith(compareBy<LugarReciente> { it.vecesUsado }.thenBy { it.ultimaVezUsado })
                    .take(lista.size - MAX_RECIENTES_GUARDADOS)

                aEliminar.forEach { refRecientes(uid).child(it.id).removeValue() }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
