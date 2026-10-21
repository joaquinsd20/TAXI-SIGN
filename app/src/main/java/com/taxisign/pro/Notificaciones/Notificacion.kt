package com.taxisign.pro.notificaciones

/**
 * Notificacion
 *
 * Modelo de cada aviso guardado en Firebase: notificaciones/{uidDestino}/{push}
 * Campos con valor por defecto para que Firebase deserialice sin problemas.
 */
data class Notificacion(
    val id: String = "",
    val tipo: String = "INFO",      // VIAJE, LLEGADA, SEGURIDAD, INFO, GANANCIA
    val titulo: String = "",
    val mensaje: String = "",
    val timestamp: Long = 0L,
    val leida: Boolean = false,
    val paraUid: String = "",
    val rol: String = ""            // PASAJERO o CONDUCTOR
)
