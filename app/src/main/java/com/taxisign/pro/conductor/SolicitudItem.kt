package com.taxisign.pro.conductor

/** Una solicitud pendiente mostrada en la lista del conductor. */
data class SolicitudItem(
    val idSolicitud: String,
    val pasajeroId: String,
    val nombrePasajero: String,
    val telefonoPasajero: String,
    val tipoDiscapacidad: String,
    val direccionRecojo: String,
    val direccionDestino: String,
    val precio: String,
    val distancia: String,
    val tarifa: Double,
    val origenLat: Double,
    val origenLng: Double,
    val destinoLat: Double,
    val destinoLng: Double,
    val esSordoMudo: Boolean,
    val timestamp: Long
)
