package com.taxisign.pro.Seguridad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * PermisoUbicacionFondoManager
 *
 * Orquesta la solicitud del permiso de ubicacion en segundo plano respetando
 * el orden que exige Android 11+ (API 30):
 *
 *   1. Aviso destacado + consentimiento expreso (requisito de Google Play).
 *   2. ACCESS_FINE_LOCATION (permiso "mientras se usa la app").
 *   3. ACCESS_BACKGROUND_LOCATION ("permitir todo el tiempo") en una
 *      segunda solicitud independiente, solo si el usuario acepto el aviso.
 *
 * En Android 10 y anteriores el sistema muestra la opcion "Permitir todo el
 * tiempo" dentro del dialogo de ACCESS_FINE_LOCATION, por lo que no hay una
 * solicitud separada.
 *
 * Uso (en la Activity, como campo para registrar el launcher a tiempo):
 *   private val permisoFondo = PermisoUbicacionFondoManager(this) { concedido -> ... }
 */
class PermisoUbicacionFondoManager(
    private val activity: AppCompatActivity,
    private val alFinalizar: (concedido: Boolean) -> Unit
) {

    private var finalizado = false

    private val launcherFondo = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        entregar(tienePermisoFondo())
    }

    private val launcherFino = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            pedirFondo()
        } else {
            entregar(false)
        }
    }

    /** Inicia el flujo: aviso destacado -> permiso fino -> permiso en segundo plano. */
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
        if (tienePermisoFondo()) {
            entregar(true)
            return
        }

        if (tienePermisoFino()) {
            pedirFondo()
        } else {
            launcherFino.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun pedirFondo() {
        if (tienePermisoFondo()) {
            entregar(true)
            return
        }

        // En Android 11+ el permiso de segundo plano se concede aparte.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            launcherFondo.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // Android 10 o anterior: el "permitir todo el tiempo" venia en el
            // dialogo del permiso fino, que ya se mostro. Si no se concedio,
            // no hay nada mas que pedir.
            entregar(false)
        }
    }

    fun tienePermisoFino(): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun tienePermisoFondo(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun entregar(concedido: Boolean) {
        if (finalizado) return
        finalizado = true
        alFinalizar(concedido)
    }
}
