package com.taxisign.pro.Seguridad

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Abre la politica de privacidad publicada en GitHub Pages.
 * Se usa desde la pantalla de inicio, los ajustes de pasajero y conductor,
 * Soporte y el aviso de ubicacion, para que siempre este a un toque.
 */
object PoliticaPrivacidad {

    fun abrir(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(PermisoUbicacionHelper.URL_POLITICA_PRIVACIDAD))
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Política de privacidad: ${PermisoUbicacionHelper.URL_POLITICA_PRIVACIDAD}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
