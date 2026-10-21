package com.taxisign.pro.conductor

data class SolicitudViaje(
    val idSolicitud: String = "",
    val pasajeroId: String = "",
    val nombrePasajero: String = "",
    val telefonoPasajero: String = "",
    val direccionRecojo: String = "",
    val direccionDestino: String = "",
    val precio: String = "",
    val tarifa: Double = 0.0,
    val origenLat: Double = 0.0,
    val origenLng: Double = 0.0,
    val destinoLat: Double = 0.0,
    val destinoLng: Double = 0.0,
    val esSordoMudo: Boolean = false,
    val timestamp: Long = 0L
)