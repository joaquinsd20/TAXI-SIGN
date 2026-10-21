package com.taxisign.pro.pasajero

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.mapa.MapaBaseFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.FirebaseDatabase

class BuscarDestinoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var etBusqueda: EditText
    private lateinit var recyclerResultados: RecyclerView
    private lateinit var adapter: DireccionAdapter
    private lateinit var placesClient: PlacesClient
    private val listaDirecciones = mutableListOf<Direccion>()

    private val mDatabase = FirebaseDatabase.getInstance().reference
    private var viajeId: String? = null // Podría ser el idSolicitud

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_destino)

        // Recibimos el ID del viaje/solicitud desde el PanelPasajero
        viajeId = intent.getStringExtra("VIAJE_ID")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        inicializarVistas()
    }

    private fun inicializarVistas() {
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        etBusqueda = findViewById(R.id.et_busqueda)
        recyclerResultados = findViewById(R.id.recyclerResultados)
        val btnConfirmar = findViewById<LinearLayout>(R.id.btn_confirmar_destino)

        recyclerResultados.layoutManager = LinearLayoutManager(this)
        adapter = DireccionAdapter(listaDirecciones) { direccion ->
            etBusqueda.setText(direccion.nombre)
            recyclerResultados.visibility = View.GONE
            obtenerCoordenadasDireccion(direccion)
        }
        recyclerResultados.adapter = adapter

        btnBack.setOnClickListener { finish() }

        etBusqueda.addTextChangedListener {
            val texto = it.toString().trim()
            if (texto.length >= 2) buscarDirecciones(texto)
            else {
                listaDirecciones.clear()
                adapter.notifyDataSetChanged()
                recyclerResultados.visibility = View.GONE
            }
        }

        btnConfirmar.setOnClickListener {
            val destino = etBusqueda.text.toString().trim()
            if (destino.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa una dirección", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            obtenerCoordenadas(destino)
        }
    }

    private fun buscarDirecciones(query: String) {
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .setCountry("PE")
            .setLocationBias(RectangularBounds.newInstance(bondsCiudadActual()))
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                listaDirecciones.clear()
                for (prediction in response.autocompletePredictions) {
                    listaDirecciones.add(
                        Direccion(
                            prediction.getFullText(null).toString(),
                            0.0,
                            0.0,
                            prediction.placeId
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                recyclerResultados.visibility = if (listaDirecciones.isNotEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                listaDirecciones.clear()
                adapter.notifyDataSetChanged()
                recyclerResultados.visibility = View.GONE
                Toast.makeText(
                    this,
                    "No se pudieron cargar direcciones. Revisa tu conexión o la configuración de la API de Maps en la consola de Google.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /** Usa la Place de Google para obtener las coordenadas exactas, con respaldo del Geocoder. */
    private fun obtenerCoordenadasDireccion(direccion: Direccion) {
        if (direccion.placeId.isNotBlank()) {
            val request = FetchPlaceRequest.newInstance(
                direccion.placeId,
                listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
            )
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val lat = place.latLng?.latitude ?: 0.0
                    val lng = place.latLng?.longitude ?: 0.0
                    val nombre = place.address ?: direccion.nombre
                    finalizarDestino(nombre, lat, lng)
                }
                .addOnFailureListener {
                    obtenerCoordenadas(direccion.nombre)
                }
        } else {
            obtenerCoordenadas(direccion.nombre)
        }
    }

    /** Sesgo geográfico alrededor de la ciudad activa del pasajero (resultados más reales). */
    private fun bondsCiudadActual(): LatLngBounds {
        val lat = if (SessionManager.ciudadLat != 0.0) SessionManager.ciudadLat else MapaBaseFragment.ultimaLatitud
        val lng = if (SessionManager.ciudadLng != 0.0) SessionManager.ciudadLng else MapaBaseFragment.ultimaLongitud
        val radioKm = if (SessionManager.ciudadRadioKm > 0) SessionManager.ciudadRadioKm else 25.0

        val deltaLat = radioKm / 111.0
        val deltaLng = radioKm / (111.0 * Math.cos(Math.toRadians(lat)).coerceAtLeast(0.1))

        return LatLngBounds(
            LatLng(lat - deltaLat, lng - deltaLng),
            LatLng(lat + deltaLat, lng + deltaLng)
        )
    }

    private fun obtenerCoordenadas(direccion: String) {
        val geocoder = Geocoder(this)
        try {
            val resultados = geocoder.getFromLocationName(direccion, 1)
            if (!resultados.isNullOrEmpty()) {
                val loc = resultados[0]
                finalizarDestino(direccion, loc.latitude, loc.longitude)
            } else {
                Toast.makeText(this, "No se pudo encontrar la ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error de red, verifica tu conexión", Toast.LENGTH_SHORT).show()
        }
    }

    /** Finaliza el flujo: actualiza Firebase si hay viaje activo, guarda el reciente y devuelve el resultado. */
    private fun finalizarDestino(direccion: String, lat: Double, lng: Double) {
        // Si ya existe una solicitud activa, actualizamos Firebase en tiempo real
        if (!viajeId.isNullOrEmpty()) {
            val updates = mapOf(
                "destino" to direccion,
                "destinoLat" to lat,
                "destinoLng" to lng
            )
            mDatabase.child("solicitudes").child(viajeId!!).updateChildren(updates)
        }

        // Guardamos automáticamente este destino real como búsqueda
        // reciente del pasajero (igual que InDriver).
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            RecientesHelper.registrarDestinoUsado(uid, direccion, lat, lng)
        }

        // Devolvemos el resultado al panel principal para cerrar el flujo
        val intent = Intent()
        intent.putExtra("destino", direccion)
        intent.putExtra("lat", lat)
        intent.putExtra("lng", lng)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Asegúrate de tener el archivo raw/map_style_dark.json
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))

        val lat = MapaBaseFragment.ultimaLatitud
        val lng = MapaBaseFragment.ultimaLongitud
        if (lat != 0.0 && lng != 0.0) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
        }
    }
}