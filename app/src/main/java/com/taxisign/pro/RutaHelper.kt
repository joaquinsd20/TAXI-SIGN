package com.taxisign.pro

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng

/**
 * RutaHelper
 *
 * Calcula distancia (km) y tiempo (min) por CARRETERA usando la Directions API
 * de Google (con Volley). Si falla o no hay internet/clave, cae automáticamente
 * al cálculo en línea recta para no romper la app.
 *
 * Nota: requiere que la Directions API esté habilitada en la misma API key de
 * Google Maps (R.string.google_maps_key). Si no lo está, siempre usará el
 * estimado en línea recta (igual funciona, solo menos preciso).
 */
object RutaHelper {

    /**
     * @param callback (km, minutos, esRutaReal) — esRutaReal=false si fue estimado.
     */
    fun calcular(
        context: Context,
        origen: LatLng,
        destino: LatLng,
        callback: (km: Double, minutos: Int, esRutaReal: Boolean) -> Unit
    ) {
        val (kmRecta, minRecta) = lineaRecta(origen, destino)

        val apiKey = try {
            context.getString(
                context.resources.getIdentifier("google_maps_key", "string", context.packageName)
            )
        } catch (e: Exception) { "" }

        if (apiKey.isEmpty()) {
            callback(kmRecta, minRecta, false)
            return
        }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=${origen.latitude},${origen.longitude}" +
            "&destination=${destino.latitude},${destino.longitude}" +
            "&mode=driving&language=es&key=$apiKey"

        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { json ->
                try {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val leg = routes.getJSONObject(0)
                            .getJSONArray("legs").getJSONObject(0)
                        val metros = leg.getJSONObject("distance").getInt("value")
                        val segundos = leg.getJSONObject("duration").getInt("value")
                        val km = metros / 1000.0
                        val min = Math.ceil(segundos / 60.0).toInt().coerceAtLeast(1)
                        callback(km, min, true)
                    } else {
                        callback(kmRecta, minRecta, false)
                    }
                } catch (e: Exception) {
                    callback(kmRecta, minRecta, false)
                }
            },
            {
                callback(kmRecta, minRecta, false)
            }
        )
        Volley.newRequestQueue(context).add(req)
    }

    private fun lineaRecta(o: LatLng, d: LatLng): Pair<Double, Int> {
        val res = FloatArray(1)
        android.location.Location.distanceBetween(o.latitude, o.longitude, d.latitude, d.longitude, res)
        val km = res[0] / 1000.0
        val min = Math.ceil((km / 25.0) * 60.0).toInt().coerceAtLeast(1)
        return Pair(km, min)
    }
}
