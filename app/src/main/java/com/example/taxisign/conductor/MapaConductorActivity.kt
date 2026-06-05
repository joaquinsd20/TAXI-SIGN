package com.example.taxisign.conductor

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taxisign.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapaConductorActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var primeraCarga = true

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout._mapa_conductor)   // mismo layout que tenías

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<FloatingActionButton>(R.id.btn_centrar_mapa).setOnClickListener {
            centrarEnMiUbicacion()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // Estilo oscuro
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
            )
            if (!success) Toast.makeText(this, "Error en estilo del mapa", Toast.LENGTH_SHORT).show()
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        }

        verificarPermisosYIniciar()
    }

    // ── Permisos ──────────────────────────────────────────────────

    private fun verificarPermisosYIniciar() {
        if (tienePermiso()) {
            activarCapaUbicacion()
            iniciarActualizaciones()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun tienePermiso() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // ── Punto azul en el mapa ─────────────────────────────────────

    private fun activarCapaUbicacion() {
        if (!tienePermiso()) return
        try { mMap.isMyLocationEnabled = true }
        catch (e: SecurityException) { e.printStackTrace() }
    }

    // ── Actualizaciones continuas con FusedLocation ───────────────

    private fun iniciarActualizaciones() {
        if (!tienePermiso()) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(2000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val posicion = LatLng(location.latitude, location.longitude)

                // Solo la primera vez mover la cámara automáticamente
                if (primeraCarga) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16f))
                    primeraCarga = false
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    // ── Centrar manualmente ───────────────────────────────────────

    private fun centrarEnMiUbicacion() {
        if (!tienePermiso()) { verificarPermisosYIniciar(); return }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), 17f
                        )
                    )
                } else {
                    Toast.makeText(this, "GPS buscando señal...", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    // ── Resultado del permiso ─────────────────────────────────────

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            activarCapaUbicacion()
            iniciarActualizaciones()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        if (::mMap.isInitialized && tienePermiso()) iniciarActualizaciones()
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}