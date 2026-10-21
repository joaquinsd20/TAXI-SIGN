package com.taxisign.pro.pasajero

data class Direccion(

    val nombre: String,

    val latitud: Double,

    val longitud: Double,

    val placeId: String = ""
)