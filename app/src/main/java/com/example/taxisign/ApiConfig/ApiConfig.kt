package com.example.taxisign

object ApiConfig {

    private const val BASE_URL =
        "http://192.168.100.12/taxisign_db/"

    const val REGISTER =
        BASE_URL + "registrar.php"
    const val LOGIN =
        BASE_URL + "login.php"
    const val CREAR_SOLICITUD =
        BASE_URL + "crear_solicitud.php"
    const val VERIFICAR_ESTADO =
        BASE_URL + "verificar_estado.php"
    const val OBTENER_SOLICITUDES =
        BASE_URL + "obtener_solicitudes.php"
    const val ACEPTAR_SOLICITUD =
        BASE_URL + "aceptar_solicitud.php"
    const val CANCELAR_SOLICITUD =
        BASE_URL + "cancelar_solicitud.php"

    const val OBTENER_VIAJES_PASAJERO =
        BASE_URL + "obtener_viajes_pasajero.php"
}