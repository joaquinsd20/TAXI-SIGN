package com.taxisign.pro.pasajero

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * SeleccionarCiudadActivity
 *
 * Pantalla que se muestra:
 *  - Justo después de crear la cuenta o iniciar sesión por primera vez
 *    (cuando el pasajero aún no tiene una ciudad configurada), y
 *  - Cuando el pasajero decide cambiar su ciudad desde el panel.
 *
 * Elegir la ciudad permite acotar el mapa y las búsquedas de lugares
 * (Google Places) a esa zona, haciendo que los resultados sean más
 * precisos y reales en vez de mostrar coincidencias de cualquier
 * parte del país.
 */
class SeleccionarCiudadActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001

    private lateinit var recyclerCiudades: RecyclerView
    private lateinit var etBuscarCiudad: EditText
    private lateinit var btnUsarGps: LinearLayout
    private lateinit var progressGps: ProgressBar
    private lateinit var adapter: CiudadAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    /** true cuando viene desde "Cambiar ciudad" en el panel (ya tenía una ciudad elegida). */
    private var esCambioDeCiudad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seleccionar_ciudad)

        esCambioDeCiudad = intent.getBooleanExtra("ES_CAMBIO", false)

        recyclerCiudades = findViewById(R.id.recyclerCiudades)
        etBuscarCiudad = findViewById(R.id.etBuscarCiudad)
        btnUsarGps = findViewById(R.id.btnUsarGps)
        progressGps = findViewById(R.id.progressGps)

        val btnBack = findViewById<ImageButton>(R.id.btnBackCiudad)
        if (esCambioDeCiudad) {
            btnBack.visibility = View.VISIBLE
            btnBack.setOnClickListener { finish() }
        }

        recyclerCiudades.layoutManager = LinearLayoutManager(this)
        adapter = CiudadAdapter(CiudadesPeru.LISTA) { ciudad -> confirmarCiudad(ciudad) }
        recyclerCiudades.adapter = adapter

        etBuscarCiudad.addTextChangedListener {
            val texto = it.toString().trim()
            val filtradas = if (texto.isEmpty()) {
                CiudadesPeru.LISTA
            } else {
                CiudadesPeru.LISTA.filter { c ->
                    c.nombre.contains(texto, ignoreCase = true) ||
                        c.departamento.contains(texto, ignoreCase = true)
                }
            }
            recyclerCiudades.adapter = CiudadAdapter(filtradas) { ciudad -> confirmarCiudad(ciudad) }
        }

        btnUsarGps.setOnClickListener {
            detectarCiudadPorGps()
        }
    }

    private fun detectarCiudadPorGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mostrarCargandoGps(true)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                mostrarCargandoGps(false)
                if (location != null) {
                    val ciudadDetectada = CiudadesPeru.masCercanaA(location.latitude, location.longitude)
                    Toast.makeText(
                        this,
                        "Detectamos que estás cerca de ${ciudadDetectada.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()
                    confirmarCiudad(ciudadDetectada)
                } else {
                    Toast.makeText(
                        this,
                        "No pudimos obtener tu ubicación. Elige tu ciudad de la lista",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                mostrarCargandoGps(false)
                Toast.makeText(this, "No se pudo acceder al GPS", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarCargandoGps(cargando: Boolean) {
        progressGps.visibility = if (cargando) View.VISIBLE else View.GONE
    }

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
            detectarCiudadPorGps()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(
                this,
                "Sin permiso de ubicación, elige tu ciudad manualmente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmarCiudad(ciudad: Ciudad) {
        // Actualizamos la sesión y el mapa inmediatamente
        SessionManager.ciudadActual = ciudad.nombre
        SessionManager.ciudadLat = ciudad.lat
        SessionManager.ciudadLng = ciudad.lng
        SessionManager.ciudadRadioKm = ciudad.radioKm

        com.taxisign.pro.mapa.MapaBaseFragment.ultimaLatitud = ciudad.lat
        com.taxisign.pro.mapa.MapaBaseFragment.ultimaLongitud = ciudad.lng
        com.taxisign.pro.mapa.MapaBaseFragment.ultimoZoom = 13f

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val datosCiudad = mapOf(
                "ciudad" to ciudad.nombre,
                "ciudadLat" to ciudad.lat,
                "ciudadLng" to ciudad.lng,
                "ciudadConfigurada" to true
            )
            dbRoot.child("usuarios").child("pasajeros").child(uid)
                .updateChildren(datosCiudad)
        }

        if (esCambioDeCiudad) {
            setResult(RESULT_OK)
            finish()
        } else {
            val intent = Intent(this, PanelPasajeroActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
