package com.taxisign.pro

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes

/**
 * Punto unico de acceso a Google Places.
 *
 * Usa la API "Places API (New)": los proyectos de Google Cloud creados desde
 * marzo de 2025 ya no pueden activar la Places API antigua, y con ella la
 * busqueda de lugares fallaba siempre.
 */
object PlacesHelper {

    private const val TAG = "PlacesHelper"

    /**
     * Pasa a false cuando Google rechaza la busqueda (por ejemplo, sin
     * facturacion activa). Desde ese momento la app usa directamente la
     * busqueda gratuita y no espera a que Google falle en cada letra.
     */
    @Volatile
    var placesDisponible = true

    /** true si el error indica que Google Places no esta habilitado para esta app. */
    fun esRechazoDeServicio(error: Exception): Boolean {
        val codigo = (error as? ApiException)?.statusCode
        return codigo == PlacesStatusCodes.REQUEST_DENIED ||
            codigo == PlacesStatusCodes.OVER_QUERY_LIMIT ||
            codigo == CommonStatusCodes.API_NOT_CONNECTED ||
            codigo == CommonStatusCodes.DEVELOPER_ERROR
    }

    fun cliente(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                context.applicationContext,
                context.getString(R.string.google_maps_key)
            )
        }
        return Places.createClient(context)
    }

    /** Mensaje entendible para el usuario; el detalle tecnico queda en Logcat. */
    fun mensajeError(error: Exception): String {
        val codigo = (error as? ApiException)?.statusCode
        Log.e(TAG, "Error de Google Places (codigo=$codigo): ${error.message}", error)
        return when (codigo) {
            CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT ->
                "Sin conexión a internet. Revisa tus datos o Wi-Fi."
            PlacesStatusCodes.REQUEST_DENIED ->
                "La búsqueda de lugares no está disponible en este momento. Intenta más tarde."
            PlacesStatusCodes.OVER_QUERY_LIMIT ->
                "Hay muchas búsquedas en este momento. Intenta en unos minutos."
            else -> "No se pudo buscar el lugar. Intenta de nuevo."
        }
    }
}
