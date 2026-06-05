package com.example.taxisign.pasajero

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taxisign.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.taxisign.mapa.MapaBaseFragment
class BuscarDestinoActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var etBusqueda: EditText

    private lateinit var recyclerResultados: RecyclerView

    private lateinit var adapter: DireccionAdapter

    private lateinit var placesClient: PlacesClient

    private val listaDirecciones =
        mutableListOf<Direccion>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.buscar_destino)

        // MAPA

        val mapFragment =
            supportFragmentManager.findFragmentById(
                R.id.map
            ) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // PLACES

        if (!Places.isInitialized()) {

            Places.initialize(
                applicationContext,
                "AIzaSyBXZDPoloKmzL9k0tDLBjfQ37hZ2kh87JE"
            )
        }

        placesClient =
            Places.createClient(this)

        // REFERENCIAS

        val btnBack =
            findViewById<ImageButton>(
                R.id.btn_back
            )

        etBusqueda =
            findViewById(
                R.id.et_busqueda
            )

        recyclerResultados =
            findViewById(
                R.id.recyclerResultados
            )

        val btnConfirmar =
            findViewById<LinearLayout>(
                R.id.btn_confirmar_destino
            )

        // RECYCLER

        recyclerResultados.layoutManager =
            LinearLayoutManager(this)

        adapter =
            DireccionAdapter(
                listaDirecciones
            ) { direccion ->

                etBusqueda.setText(
                    direccion.nombre
                )

                recyclerResultados.visibility =
                    View.GONE

                obtenerCoordenadas(
                    direccion.nombre
                )
            }

        recyclerResultados.adapter =
            adapter

        // ATRAS

        btnBack.setOnClickListener {

            finish()
        }

        // BUSCAR

        etBusqueda.addTextChangedListener {

            val texto =
                it.toString().trim()

            if (texto.length >= 2) {

                buscarDirecciones(texto)

            } else {

                listaDirecciones.clear()

                adapter.notifyDataSetChanged()

                recyclerResultados.visibility =
                    View.GONE
            }
        }

        // CONFIRMAR

        btnConfirmar.setOnClickListener {

            val destino =
                etBusqueda.text.toString().trim()

            if (destino.isEmpty()) {

                Toast.makeText(
                    this,
                    "Selecciona un destino",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            obtenerCoordenadas(destino)
        }
    }

    private fun buscarDirecciones(
        query: String
    ) {

        val token =
            AutocompleteSessionToken.newInstance()

        val request =
            FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setCountry("PE")
                .setTypeFilter(
                    TypeFilter.ESTABLISHMENT
                )
                .build()

        placesClient.findAutocompletePredictions(
            request
        ).addOnSuccessListener { response ->

            listaDirecciones.clear()

            for (prediction in response.autocompletePredictions) {

                listaDirecciones.add(

                    Direccion(

                        prediction
                            .getFullText(null)
                            .toString(),

                        0.0,

                        0.0
                    )
                )
            }

            adapter.notifyDataSetChanged()

            recyclerResultados.visibility =

                if (listaDirecciones.isNotEmpty())
                    View.VISIBLE
                else
                    View.GONE

        }.addOnFailureListener {

            Toast.makeText(
                this,
                "Error buscando direcciones",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun obtenerCoordenadas(
        direccion: String
    ) {

        val geocoder =
            Geocoder(this)

        try {

            val resultados =
                geocoder.getFromLocationName(
                    direccion,
                    1
                )

            if (!resultados.isNullOrEmpty()) {

                val location =
                    resultados[0]

                val intent =
                    Intent()

                intent.putExtra(
                    "latitud_destino",
                    location.latitude
                )

                intent.putExtra(
                    "longitud_destino",
                    location.longitude
                )

                intent.putExtra(
                    "direccion_destino",
                    direccion
                )

                setResult(
                    RESULT_OK,
                    intent
                )

                finish()
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    override fun onMapReady(
        googleMap: GoogleMap
    ) {

        mMap = googleMap

        mMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this,
                R.raw.map_style_dark
            )
        )

        val ultimaUbicacion =
            LatLng(
                MapaBaseFragment.ultimaLatitud,
                MapaBaseFragment.ultimaLongitud
            )

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                ultimaUbicacion,
                MapaBaseFragment.ultimoZoom
            )
        )
    }
}