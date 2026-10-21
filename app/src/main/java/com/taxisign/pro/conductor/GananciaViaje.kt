package com.taxisign.pro.conductor

data class GananciaViaje(
    val viajeId: String = "",
    val monto: Double = 0.0,
    val pasajero: String = "",
    val destino: String = "",
    val timestamp: Long = 0L
)
