package com.taxisign.pro.pasajero

data class OfertaConductor(
    val conductorId: String = "",
    val nombreConductor: String = "",
    val placa: String = "",
    val rating: Double = 0.0,
    val tarifa: Double = 0.0,
    val precioActual: Double = 0.0,
    val turno: String = "pasajero",
    val estado: String = "negociando",
    val timestamp: Long = 0L
) {
    fun precioEnMesa(): Double = if (precioActual > 0) precioActual else tarifa
}
