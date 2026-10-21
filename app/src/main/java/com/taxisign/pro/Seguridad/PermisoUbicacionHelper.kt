package com.taxisign.pro.Seguridad

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R

/**
 * PermisoUbicacionHelper
 *
 * Gestiona el CONSENTIMIENTO EXPRESO del usuario para el permiso de ubicacion
 * en segundo plano (ACCESS_BACKGROUND_LOCATION).
 *
 * Requisito de Google Play (politica de Datos de Usuario):
 * la app debe mostrar un AVISO DESTACADO (prominent disclosure) explicando
 * el uso de la ubicacion en segundo plano y obtener el consentimiento
 * explicito del usuario ANTES de solicitar el permiso del sistema.
 *
 * Este helper:
 *  1. Muestra el aviso destacado (dialogo a pantalla completa, no cancelable).
 *  2. Persiste la decision del usuario.
 *  3. Permite revocarla desde Ajustes.
 */
object PermisoUbicacionHelper {

    private const val PREFS_NAME = "taxisign_privacidad"
    private const val KEY_CONSENTIMIENTO = "consentimiento_ubicacion_fondo"
    private const val KEY_FECHA_CONSENTIMIENTO = "fecha_consentimiento_ubicacion_fondo"

    /** URL de la politica de privacidad (se puede cambiar por la definitiva). */
    const val URL_POLITICA_PRIVACIDAD =
        "https://joaquinsd20.github.io/TAXI-SIGN/privacy-policy.html"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** true si el usuario ya acepto el aviso destacado en alguna occasion. */
    fun tieneConsentimiento(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONSENTIMIENTO, false)

    /** Registra la aceptacion explicita del usuario. */
    fun registrarConsentimiento(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONSENTIMIENTO, true)
            .putLong(KEY_FECHA_CONSENTIMIENTO, System.currentTimeMillis())
            .apply()
    }

    /** Revoca el consentimiento (el usuario puede cambiar de opinion en cualquier momento). */
    fun revocarConsentimiento(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONSENTIMIENTO, false)
            .remove(KEY_FECHA_CONSENTIMIENTO)
            .apply()
    }

    fun fechaConsentimiento(context: Context): Long =
        prefs(context).getLong(KEY_FECHA_CONSENTIMIENTO, 0L)

    /**
     * Muestra el aviso destacado de ubicacion en segundo plano.
     *
     * @param activity Activity donde se muestra
     * @param alCerrar  callback con true si el usuario acepto, false si lo rechazo
     */
    fun mostrarAvisoDestacado(
        activity: AppCompatActivity,
        alCerrar: (aceptado: Boolean) -> Unit
    ) {
        val inflater = LayoutInflater.from(activity)
        val vista = inflater.inflate(R.layout.dialog_aviso_ubicacion_fondo, null)

        val btnAceptar = vista.findViewById<android.widget.Button>(R.id.btnAceptarUbicacionFondo)
        val btnRechazar = vista.findViewById<android.widget.Button>(R.id.btnRechazarUbicacionFondo)
        val linkPrivacidad = vista.findViewById<TextView>(R.id.tvLinkPoliticaPrivacidad)

        val dialog = AlertDialog.Builder(activity)
            .setView(vista)
            .setCancelable(false)
            .create()

        btnAceptar.setOnClickListener {
            registrarConsentimiento(activity)
            dialog.dismiss()
            alCerrar(true)
        }

        btnRechazar.setOnClickListener {
            dialog.dismiss()
            alCerrar(false)
        }

        linkPrivacidad.setOnClickListener {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(URL_POLITICA_PRIVACIDAD))
                )
            } catch (_: Exception) {
                // Si no hay navegador no se interrumpe el flujo.
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.94f).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
