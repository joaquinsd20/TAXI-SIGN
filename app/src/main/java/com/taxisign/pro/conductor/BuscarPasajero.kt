package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.database.*
import com.taxisign.pro.R

/**
 * BuscarPasajero
 *
 * Ahora muestra una LISTA de solicitudes pendientes para que el conductor
 * elija la que le convenga (antes agarraba la primera automáticamente).
 * Al tocar una, abre SolicitudDetalleConductor para aceptarla.
 */
class BuscarPasajero : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val mDatabase by lazy { FirebaseDatabase.getInstance().reference }
    private var solicitudesListener: ValueEventListener? = null

    private val solicitudes = mutableListOf<SolicitudItem>()
    private lateinit var adapter: SolicitudesAdapter

    private lateinit var tvSin: TextView
    private var progress: View? = null

    companion object {
        private const val LIMITE_SOLICITUD_MS = 5 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_pasajeros)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        tvSin = findViewById(R.id.tv_sin_solicitudes)
        progress = findViewById(R.id.progress_radar)

        val recycler = findViewById<RecyclerView>(R.id.recycler_solicitudes)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SolicitudesAdapter(solicitudes) { abrirSolicitudDetalle(it) }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btn_cancelar_busqueda).setOnClickListener {
            detenerRadar()
            finish()
        }

        iniciarRadarRealtime()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
        val trujillo = LatLng(-8.1116, -79.0287)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 14f))
    }

    private fun iniciarRadarRealtime() {
        if (solicitudesListener != null) return

        solicitudesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ahora = System.currentTimeMillis()
                val previas = solicitudes.size
                solicitudes.clear()

                for (snap in snapshot.children) {
                    val estado = snap.child("estado").value?.toString() ?: ""
                    if (estado != "pendiente") continue
                    val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L
                    if (timestamp == 0L || (ahora - timestamp) > LIMITE_SOLICITUD_MS) continue

                    solicitudes.add(mapearSolicitud(snap))
                }

                solicitudes.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()

                progress?.visibility = if (solicitudes.isEmpty()) View.VISIBLE else View.GONE
                tvSin.text = if (solicitudes.isEmpty())
                    "Buscando solicitudes cercanas..." else "${solicitudes.size} solicitud(es) disponible(s)"

                // Vibra solo cuando llega una nueva
                if (solicitudes.size > previas) vibrateNotification()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BuscarPasajero,
                    "Error de conexión al radar: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mDatabase.child("solicitudes")
            .orderByChild("estado")
            .equalTo("pendiente")
            .addValueEventListener(solicitudesListener!!)
    }

    private fun mapearSolicitud(snap: DataSnapshot): SolicitudItem {
        val tipoDiscapacidad = snap.child("tipoDiscapacidad").value?.toString() ?: ""
        val esSordoMudo = snap.child("esSordoMudo").getValue(Boolean::class.java)
            ?: (tipoDiscapacidad.contains("sordo", true) || tipoDiscapacidad.contains("mudo", true))

        return SolicitudItem(
            idSolicitud = snap.key ?: "",
            pasajeroId = snap.child("idPasajero").value?.toString() ?: "",
            nombrePasajero = snap.child("nombrePasajero").value?.toString() ?: "Pasajero",
            telefonoPasajero = snap.child("telefonoPasajero").value?.toString() ?: "",
            tipoDiscapacidad = tipoDiscapacidad,
            direccionRecojo = snap.child("direccionRecojo").value?.toString()
                ?: snap.child("origen").value?.toString() ?: "Ubicación del pasajero",
            direccionDestino = snap.child("direccionDestino").value?.toString()
                ?: snap.child("destino").value?.toString() ?: "Destino",
            precio = snap.child("precio").value?.toString() ?: "Por calcular",
            distancia = snap.child("distancia").value?.toString() ?: "Por calcular",
            tarifa = snap.child("tarifa").getValue(Double::class.java) ?: 0.0,
            origenLat = snap.child("origenLat").getValue(Double::class.java) ?: 0.0,
            origenLng = snap.child("origenLng").getValue(Double::class.java) ?: 0.0,
            destinoLat = snap.child("destinoLat").getValue(Double::class.java) ?: 0.0,
            destinoLng = snap.child("destinoLng").getValue(Double::class.java) ?: 0.0,
            esSordoMudo = esSordoMudo,
            timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L
        )
    }

    private fun abrirSolicitudDetalle(s: SolicitudItem) {
        vibrateNotification()
        val intent = Intent(this, SolicitudDetalleConductor::class.java).apply {
            putExtra("ID_SOLICITUD", s.idSolicitud)
            putExtra("PASAJERO_ID", s.pasajeroId)
            putExtra("NOMBRE_PASAJERO", s.nombrePasajero)
            putExtra("TELEFONO_PASAJERO", s.telefonoPasajero)
            putExtra("TIPO_DISCAPACIDAD", s.tipoDiscapacidad)
            putExtra("DIRECCION_RECOJO", s.direccionRecojo)
            putExtra("DIRECCION_DESTINO", s.direccionDestino)
            putExtra("PRECIO", s.precio)
            putExtra("DISTANCIA", s.distancia)
            putExtra("TARIFA", s.tarifa)
            putExtra("LAT_RECOJO", s.origenLat)
            putExtra("LNG_RECOJO", s.origenLng)
            putExtra("DESTINO_LAT", s.destinoLat)
            putExtra("DESTINO_LNG", s.destinoLng)
            putExtra("ES_SORDO_MUDO", s.esSordoMudo)
        }
        detenerRadar()
        startActivity(intent)
        finish()
    }

    private fun detenerRadar() {
        solicitudesListener?.let {
            mDatabase.child("solicitudes")
                .orderByChild("estado")
                .equalTo("pendiente")
                .removeEventListener(it)
            solicitudesListener = null
        }
    }

    private fun vibrateNotification() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") it.vibrate(400)
            }
        }
    }

    override fun onDestroy() {
        detenerRadar()
        super.onDestroy()
    }
}
