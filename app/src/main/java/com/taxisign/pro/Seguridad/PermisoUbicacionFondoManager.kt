package com.taxisign.pro.Seguridad

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * PermisoUbicacionFondoManager
 *
 * Flujo de permisos de ubicacion del conductor:
 *   1. Aviso de uso de la ubicacion + consentimiento.
 *   2. ACCESS_FINE_LOCATION (permiso "mientras se usa la app").
 *
 * No se solicita ACCESS_BACKGROUND_LOCATION: la ubicacion se sigue
 * compartiendo con la pantalla bloqueada gracias a LocationForegroundService,
 * que se inicia con la app visible y muestra una notificacion permanente.
 */
class PermisoUbicacionFondoManager(
    private val activity: AppCompatActivity,
    private val alFinalizar: (concedido: Boolean) -> Unit
) {

    private var finalizado = false

    private val launcherFino = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        entregar(concedido)
    }

    /** Inicia el flujo: aviso de uso de ubicacion -> permiso "mientras se usa la app". */
    fun iniciar() {
        if (!PermisoUbicacionHelper.tieneConsentimiento(activity)) {
            PermisoUbicacionHelper.mostrarAvisoDestacado(activity) { aceptado ->
                if (!aceptado) {
                    entregar(false)
                } else {
                    continuar()
                }
            }
        } else {
            continuar()
        }
    }

    private fun continuar() {
        if (tienePermisoFino()) {
            entregar(true)
        } else {
            launcherFino.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun tienePermisoFino(): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Se mantiene por compatibilidad con AjustesConductor. Ya no se pide
     * ACCESS_BACKGROUND_LOCATION: el servicio en primer plano funciona con el
     * permiso "mientras se usa la app".
     */
    fun tienePermisoFondo(): Boolean = tienePermisoFino()

    private fun entregar(concedido: Boolean) {
        if (finalizado) return
        finalizado = true
        alFinalizar(concedido)
    }
}
