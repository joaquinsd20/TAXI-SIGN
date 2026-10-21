package com.taxisign.pro.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.taxisign.pro.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

class MapaBaseFragment :
    Fragment(R.layout.fragment_mapa_base),
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    companion object {

        var ultimaLatitud = -8.111763

        var ultimaLongitud = -79.028687

        var ultimoZoom = 17f

        var destinoActual: LatLng? = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment =
            childFragmentManager
                .findFragmentById(R.id.map)
                    as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(
        googleMap: GoogleMap
    ) {

        mMap = googleMap

        mMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style_dark
            )
        )

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        mMap.setOnCameraIdleListener {

            val camera =
                mMap.cameraPosition

            ultimaLatitud =
                camera.target.latitude

            ultimaLongitud =
                camera.target.longitude

            ultimoZoom =
                camera.zoom
        }

        val posicion =
            LatLng(
                ultimaLatitud,
                ultimaLongitud
            )

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                posicion,
                ultimoZoom
            )
        )

        activarUbicacion()

        restaurarDestino()
    }

    private fun activarUbicacion() {

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap.isMyLocationEnabled = true
    }

    fun moverAMiUbicacion() {

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    requireActivity()
                )

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    ultimaLatitud =
                        location.latitude

                    ultimaLongitud =
                        location.longitude

                    ultimoZoom = 17f

                    val miUbicacion =
                        LatLng(
                            location.latitude,
                            location.longitude
                        )

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            miUbicacion,
                            17f
                        )
                    )
                }
            }
    }

    fun moverMapaADestino(
        latitud: Double,
        longitud: Double,
        direccion: String
    ) {

        ultimaLatitud = latitud
        ultimaLongitud = longitud
        ultimoZoom = 17f

        destinoActual =
            LatLng(
                latitud,
                longitud
            )

        mMap.clear()

        mMap.addMarker(
            MarkerOptions()
                .position(destinoActual!!)
                .title(direccion)
        )

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                destinoActual!!,
                17f
            )
        )
    }

    private fun restaurarDestino() {

        destinoActual?.let { destino ->

            mMap.addMarker(
                MarkerOptions()
                    .position(destino)
                    .title("Destino")
            )
        }
    }

    fun obtenerMapa(): GoogleMap {
        return mMap
    }
}