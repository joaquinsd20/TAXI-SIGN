package com.taxisign.pro

/**
 * Mensaje
 *
 * Modelo de datos para cada mensaje del chat.
 * Los campos tienen valores por defecto para que
 * Firebase pueda deserializar sin constructor explícito.
 */
data class Mensaje(
    val texto     : String = "",
    val emisorId  : String = "",   // UID de Firebase Auth
    val emisorRol : String = "",   // "PASAJERO" o "CONDUCTOR"
    val timestamp : Long   = 0L
)
