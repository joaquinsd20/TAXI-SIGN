package com.taxisign.pro

import android.content.Context

object SessionManager {

    private const val PREFS_SESION = "taxisign_sesion"
    private const val KEY_ROL = "rol"

    private const val KEY_NOMBRE = "nombre"
    private const val KEY_CORREO = "correo"

    /**
     * Guarda el rol (y el nombre y correo actuales) con el que inicio sesion,
     * para que al volver a abrir la app entre directo a su panel.
     */
    fun guardarRol(context: Context, rol: String) {
        rolUsuario = rol
        context.getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE).edit()
            .putString(KEY_ROL, rol)
            .putString(KEY_NOMBRE, nombreUsuario)
            .putString(KEY_CORREO, correoUsuario)
            .apply()
    }

    /**
     * Recupera los datos de la sesion guardada en memoria. Se usa al abrir la
     * app (y si Android cerro el proceso en segundo plano) para no perder el
     * nombre, el rol ni el id del usuario.
     */
    fun restaurar(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE)
        uidUsuario = uid
        idUsuario = uid.hashCode()
        if (rolUsuario.isEmpty()) rolUsuario = prefs.getString(KEY_ROL, "") ?: ""
        if (nombreUsuario.isEmpty()) nombreUsuario = prefs.getString(KEY_NOMBRE, "") ?: ""
        if (correoUsuario.isEmpty()) correoUsuario = prefs.getString(KEY_CORREO, "") ?: ""
    }

    /** Rol guardado ("PASAJERO", "CONDUCTOR") o vacio si no hay sesion. */
    fun rolGuardado(context: Context): String =
        context.getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE)
            .getString(KEY_ROL, "") ?: ""

    fun borrarRol(context: Context) {
        rolUsuario = ""
        context.getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE)
            .edit().remove(KEY_ROL).remove(KEY_NOMBRE).remove(KEY_CORREO).apply()
    }

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