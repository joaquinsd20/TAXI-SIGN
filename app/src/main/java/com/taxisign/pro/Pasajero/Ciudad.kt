package com.taxisign.pro.pasajero

/**
 * Ciudad
 *
 * Representa una ciudad soportada por TaxiSign. Al elegir una ciudad,
 * el mapa se centra en sus coordenadas y las búsquedas de Google Places
 * quedan acotadas a esa zona (mediante un sesgo geográfico/bounds),
 * haciendo que los resultados sean más relevantes y reales para el
 * pasajero en lugar de devolver lugares de cualquier parte del país.
 */
data class Ciudad(
    val nombre: String,
    val departamento: String,
    val lat: Double,
    val lng: Double,
    val radioKm: Double = 25.0
)

object CiudadesPeru {

    val LISTA = listOf(
        Ciudad("Trujillo", "La Libertad", -8.111763, -79.028687, 20.0),
        Ciudad("Lima", "Lima", -12.046374, -77.042793, 35.0),
        Ciudad("Arequipa", "Arequipa", -16.409047, -71.537451, 25.0),
        Ciudad("Chiclayo", "Lambayeque", -6.776932, -79.844017, 20.0),
        Ciudad("Piura", "Piura", -5.194490, -80.632820, 20.0),
        Ciudad("Cusco", "Cusco", -13.531950, -71.967463, 20.0),
        Ciudad("Chimbote", "Áncash", -9.084680, -78.578850, 15.0),
        Ciudad("Huancayo", "Junín", -12.065450, -75.204170, 15.0),
        Ciudad("Tacna", "Tacna", -18.014670, -70.253334, 15.0),
        Ciudad("Iquitos", "Loreto", -3.749120, -73.253410, 15.0),
        Ciudad("Cajamarca", "Cajamarca", -7.163700, -78.500000, 15.0),
        Ciudad("Pucallpa", "Ucayali", -8.379170, -74.553030, 15.0)
    )

    /** Busca la ciudad soportada más cercana a una coordenada (útil con el GPS). */
    fun masCercanaA(lat: Double, lng: Double): Ciudad {
        return LISTA.minByOrNull { ciudad ->
            distanciaAproximadaKm(lat, lng, ciudad.lat, ciudad.lng)
        } ?: LISTA.first()
    }

    fun porNombre(nombre: String): Ciudad? =
        LISTA.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }

    /** Distancia aproximada en km entre dos coordenadas (fórmula de Haversine). */
    private fun distanciaAproximadaKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val radioTierra = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return radioTierra * c
    }
}
