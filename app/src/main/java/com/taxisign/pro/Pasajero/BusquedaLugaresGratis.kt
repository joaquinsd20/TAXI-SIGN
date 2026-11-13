package com.taxisign.pro.pasajero

import android.app.Activity
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLngBounds
import java.util.Locale

/**
 * Busqueda de lugares gratuita, usada cuando Google Places no esta disponible
 * (por ejemplo, sin cuenta de facturacion activa en Google Cloud).
 *
 * 1. Photon (OpenStreetMap, https://photon.komoot.io): autocompletado gratis.
 * 2. Geocoder de Android: respaldo si Photon falla o no encuentra nada.
 *
 * Los resultados ya traen latitud y longitud, por lo que no hace falta una
 * segunda consulta al elegir uno.
 */
object BusquedaLugaresGratis {

    private const val TAG = "BusquedaLugaresGratis"
    private const val MAX_RESULTADOS = 8

    fun buscar(
        activity: Activity,
        query: String,
        bounds: LatLngBounds,
        alTerminar: (List<Direccion>) -> Unit
    ) {
        buscarEnPhoton(activity, query, bounds) { resultados ->
            if (resultados.isNotEmpty()) {
                alTerminar(resultados)
            } else {
                buscarEnGeocoder(activity, query, bounds, alTerminar)
            }
        }
    }

    private fun buscarEnPhoton(
        activity: Activity,
        query: String,
        bounds: LatLngBounds,
        alTerminar: (List<Direccion>) -> Unit
    ) {
        val centro = bounds.center
        val url = Uri.parse("https://photon.komoot.io/api/").buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("lat", centro.latitude.toString())
            .appendQueryParameter("lon", centro.longitude.toString())
            .appendQueryParameter("limit", "15")
            .appendQueryParameter(
                "bbox",
                "${bounds.southwest.longitude},${bounds.southwest.latitude}," +
                    "${bounds.northeast.longitude},${bounds.northeast.latitude}"
            )
            .build()
            .toString()

        val request = object : JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { json ->
                val lista = mutableListOf<Direccion>()
                val features = json.optJSONArray("features")
                if (features != null) {
                    for (i in 0 until features.length()) {
                        val feature = features.optJSONObject(i) ?: continue
                        val props = feature.optJSONObject("properties") ?: continue
                        if (!props.optString("countrycode").equals("PE", ignoreCase = true)) continue
                        val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                        val nombre = armarNombre(props)
                        if (nombre.isBlank() || lista.any { it.nombre == nombre }) continue
                        lista.add(Direccion(nombre, coords.optDouble(1), coords.optDouble(0)))
                        if (lista.size >= MAX_RESULTADOS) break
                    }
                }
                alTerminar(lista)
            },
            { error ->
                Log.w(TAG, "Photon no respondio: ${error.message}")
                alTerminar(emptyList())
            }
        ) {
            // Photon pide identificar la app en el User-Agent (uso justo).
            override fun getHeaders(): MutableMap<String, String> =
                mutableMapOf("User-Agent" to "TaxiSign-Android/1.0 (taxisign@hotmail.com)")
        }
        request.retryPolicy = DefaultRetryPolicy(6000, 0, 1f)
        request.tag = TAG
        Volley.newRequestQueue(activity.applicationContext).add(request)
    }

    private fun armarNombre(props: org.json.JSONObject): String {
        fun campo(nombre: String) = props.optString(nombre).takeIf { it.isNotBlank() }
        val calle = listOfNotNull(campo("street"), campo("housenumber")).joinToString(" ")
        return listOfNotNull(
            campo("name"),
            calle.takeIf { it.isNotBlank() },
            campo("district") ?: campo("locality"),
            campo("city")
        ).distinct().joinToString(", ")
    }

    @Suppress("DEPRECATION")
    private fun buscarEnGeocoder(
        activity: Activity,
        query: String,
        bounds: LatLngBounds,
        alTerminar: (List<Direccion>) -> Unit
    ) {
        Thread {
            val lista = try {
                Geocoder(activity, Locale("es", "PE")).getFromLocationName(
                    query, MAX_RESULTADOS,
                    bounds.southwest.latitude, bounds.southwest.longitude,
                    bounds.northeast.latitude, bounds.northeast.longitude
                ).orEmpty().map { dir ->
                    Direccion(
                        dir.getAddressLine(0) ?: dir.featureName ?: query,
                        dir.latitude,
                        dir.longitude
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder fallo: ${e.message}")
                emptyList()
            }
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) alTerminar(lista)
            }
        }.start()
    }
}
