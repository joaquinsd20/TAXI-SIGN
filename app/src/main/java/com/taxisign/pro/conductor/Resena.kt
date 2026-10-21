package com.taxisign.pro.conductor

data class Resena(
    val viajeId: String = "",
    val nombrePasajero: String = "",
    val destino: String = "",
    val calificacion: Int = 0,
    val comentario: String = "",
    val timestamp: Long = 0L
)
