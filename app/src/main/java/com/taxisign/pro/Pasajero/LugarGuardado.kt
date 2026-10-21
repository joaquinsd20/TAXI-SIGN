package com.taxisign.pro.pasajero

/**
 * LugarGuardado
 * tipo: "CASA", "TRABAJO" (trabajo/colegio/universidad) o "FRECUENTE"
 */
data class LugarGuardado(
    val id: String = "",
    val tipo: String = "FRECUENTE",
    val etiqueta: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
