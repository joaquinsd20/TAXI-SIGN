package com.taxisign.pro.pasajero

/**
 * LugarReciente
 *
 * Cada vez que el pasajero confirma un destino real (uno que sí usó para
 * pedir un viaje, no solo lo que escribió), se guarda aquí. Funciona como
 * el historial de búsquedas de InDriver: si el pasajero va seguido a un
 * mismo lugar, ese lugar "sube" en la lista de sugerencias para que la
 * próxima búsqueda sea casi instantánea.
 *
 * Se guarda en: usuarios/pasajeros/{uid}/recientes/{idGenerado}
 */
data class LugarReciente(
    val id: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val vecesUsado: Int = 1,
    val ultimaVezUsado: Long = 0L
)
