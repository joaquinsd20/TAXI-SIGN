package com.taxisign.pro

object SessionManager {

    var uidUsuario: String = ""

    var idUsuario: Int = 0
    var nombreUsuario: String = ""
    var correoUsuario: String = ""
    var passwordUsuario: String = ""
    var rolUsuario: String = ""

    var telefonoUsuario: String = ""
    var dniUsuario: String = ""

    // TaxiSign - accesibilidad
    var tipoDiscapacidadUsuario: String = ""
    var esSordoMudo: Boolean = false
    var metodoComunicacion: String = ""

    // TaxiSign - ciudad activa del pasajero
    var ciudadActual: String = ""
    var ciudadLat: Double = 0.0
    var ciudadLng: Double = 0.0
    var ciudadRadioKm: Double = 25.0

    // Perfil
    var fotoPerfil: String = ""

    fun limpiarSesion() {
        uidUsuario = ""
        idUsuario = 0
        nombreUsuario = ""
        correoUsuario = ""
        passwordUsuario = ""
        rolUsuario = ""
        telefonoUsuario = ""
        dniUsuario = ""
        tipoDiscapacidadUsuario = ""
        esSordoMudo = false
        metodoComunicacion = ""
        ciudadActual = ""
        ciudadLat = 0.0
        ciudadLng = 0.0
        ciudadRadioKm = 25.0
        fotoPerfil = ""
    }
}