package com.taxisign.pro.pasajero

/**
 * Viaje
 *
 * Modelo para el historial. Se mantienen los 3 campos originales
 * (destino, fecha, estado) para no romper el código existente y se
 * agregan campos opcionales con valor por defecto para el detalle:
 *  - id: viajeId en Firebase (necesario para abrir el detalle)
 *  - nombreConductor / nombrePasajero: para mostrar AMBOS nombres
 */
data class Viaje(
    val destino: String = "",
    val fecha: String = "",
    val estado: String = "",
    val id: String = "",
    val nombreConductor: String = "",
    val nombrePasajero: String = "",
    val origen: String = "",
    val tarifa: Double = 0.0
)
