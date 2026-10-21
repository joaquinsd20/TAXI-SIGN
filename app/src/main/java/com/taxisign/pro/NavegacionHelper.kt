package com.taxisign.pro

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng

/**
 * NavegacionHelper
 *
 * Obtiene la ruta REAL por carretera con sus pasos (turn-by-turn) usando la
 * Directions API de Google. Devuelve los pasos (instrucción + punto de maniobra
 * + distancia) y la lista de puntos para dibujar la línea de la ruta en el mapa.
 *
 * Requiere que la Directions API esté habilitada en la API key
 * (R.string.google_maps_key). Si falla, devuelve listas vacías y la pantalla
 * usa su texto de respaldo.
 */
object NavegacionHelper {

    data class PasoRuta(
        val instruccion: String,
        val maniobra: LatLng,
        val distanciaMetros: Int
    )

    fun obtenerRuta(
        context: Context,
        origen: LatLng,
        destino: LatLng,
        callback: (pasos: List<PasoRuta>, puntosRuta: List<LatLng>) -> Unit
    ) {
        val apiKey = try {
            context.getString(
                context.resources.getIdentifier("google_maps_key", "string", context.packageName)
            )
        } catch (e: Exception) { "" }

        if (apiKey.isEmpty()) { callback(emptyList(), emptyList()); return }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=${origen.latitude},${origen.longitude}" +
            "&destination=${destino.latitude},${destino.longitude}" +
            "&mode=driving&language=es&key=$apiKey"

        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { json ->
                try {
                    val pasos = mutableListOf<PasoRuta>()
                    val rutas = json.optJSONArray("routes")
                    if (rutas != null && rutas.length() > 0) {
                        val ruta0 = rutas.getJSONObject(0)
                        val leg = ruta0.getJSONArray("legs").getJSONObject(0)
                        val steps = leg.getJSONArray("steps")
                        for (i in 0 until steps.length()) {
                            val st = steps.getJSONObject(i)
                            val instr = limpiarHtml(st.optString("html_instructions", ""))
                            val dist = st.getJSONObject("distance").getInt("value")
                            val end = st.getJSONObject("end_location")
                            pasos.add(
                                PasoRuta(
                                    instruccion = instr,
                                    maniobra = LatLng(end.getDouble("lat"), end.getDouble("lng")),
                                    distanciaMetros = dist
                                )
                            )
                        }
                        val overview = ruta0.getJSONObject("overview_polyline").getString("points")
                        callback(pasos, decodificarPolyline(overview))
                    } else {
                        callback(emptyList(), emptyList())
                    }
                } catch (e: Exception) {
                    callback(emptyList(), emptyList())
                }
            },
            { callback(emptyList(), emptyList()) }
        )
        Volley.newRequestQueue(context).add(req)
    }

    private fun limpiarHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    /** Algoritmo oficial de Google para decodificar polylines codificadas. */
    private fun decodificarPolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}
