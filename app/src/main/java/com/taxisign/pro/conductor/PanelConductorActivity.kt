package com.taxisign.pro.conductor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.Seguridad.Soporte
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PanelConductorActivity : AppCompatActivity(), OnMapReadyCallback {

    private var isOnline = false
    private lateinit var vibrator: Vibrator
    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var tvNombreConductor: TextView
    private lateinit var btnToggleStatus: Button
    private lateinit var tvStatusLabel: TextView
    private lateinit var tvGananciaHoy: TextView
    private lateinit var tvViajesHoy: TextView

    // GPS y Firebase en Tiempo Real
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var ultimaUbicacionConductor: Location? = null

    private val mAuth = FirebaseAuth.getInstance()
    private val mDatabase = FirebaseDatabase.getInstance().reference

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_conductor)

        iniciarNotificaciones()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initViews()
        setupClickListeners()
        setupGpsTracking()
    }

    private fun initViews() {
        tvNombreConductor = findViewById(R.id.tvNombreConductor)
        btnToggleStatus = findViewById(R.id.btnToggleStatus)
        tvStatusLabel = findViewById(R.id.tvStatusLabel)
        tvGananciaHoy = findViewById(R.id.tvGananciaHoy)
        tvViajesHoy = findViewById(R.id.tvViajesHoy)
        cargarGananciasHoy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        findViewById<CardView>(R.id.btnIrPerfil).setOnClickListener { navegarA(PerfilConductorActivity::class.java) }
        findViewById<CardView>(R.id.btnIrGanancias).setOnClickListener { navegarA(ActivityGanancias::class.java) }
        findViewById<TextView>(R.id.btnIrComentarios).setOnClickListener { navegarA(ComentariosConductor::class.java) }
        findViewById<CardView>(R.id.btnIrDocumentos).setOnClickListener { navegarA(DocumentosConductor::class.java) }
        findViewById<CardView>(R.id.btnIrSoporte).setOnClickListener { navegarA(Soporte::class.java) }
        findViewById<CardView>(R.id.btnIrAjustes).setOnClickListener { navegarA(AjustesConductor::class.java) }

        findViewById<FloatingActionButton>(R.id.fabCentrarUbicacion).setOnClickListener {
            vibrarDispositivo(40)
            animarHaciaUbicacionActual()
        }

        btnToggleStatus.setOnClickListener {
            vibrarDispositivo(50)
            if (!isOnline) {
                conectarConductor()
            } else {
                desconectarConductor()
            }
        }
    }

    // Configuración del GPS militar en bucle continuo de alta precisión
    private fun setupGpsTracking() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    ultimaUbicacionConductor = location

                    // Si estamos conectados, subimos las coordenadas constantemente a Firebase
                    if (isOnline) {
                        subirUbicacionAFirebase(location)
                    }
                }
            }
        }
    }

    private fun conectarConductor() {
        isOnline = true
        btnToggleStatus.text = "FINALIZAR TURNO"
        btnToggleStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF4B4B"))
        btnToggleStatus.setTextColor(Color.WHITE)
        tvStatusLabel.text = "CONECTADO - BUSCANDO PASAJEROS EN TRUJILLO"
        tvStatusLabel.setTextColor(Color.parseColor("#4CAF50"))

        Toast.makeText(this, "Conectando al radar de viajes de Trujillo...", Toast.LENGTH_SHORT).show()

        // Empezar a rastrear GPS de forma continua
        iniciarRastreoGps()

        // CORREGIDO: Redirigir de inmediato a la pantalla del Radar de Viajes Activo (BuscarPasajero)
        val intent = Intent(this, BuscarPasajero::class.java)
        startActivity(intent)
    }

    private fun desconectarConductor() {
        isOnline = false
        btnToggleStatus.text = "INICIAR TURNO"
        btnToggleStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD600"))
        btnToggleStatus.setTextColor(Color.parseColor("#151B31"))
        tvStatusLabel.text = "ESTÁS DESCONECTADO"
        tvStatusLabel.setTextColor(Color.parseColor("#B8C1D9"))

        Toast.makeText(this, "Has salido de línea", Toast.LENGTH_SHORT).show()

        detenerRastreoGps()
        removerConductorDisponible()
    }

    private fun subirUbicacionAFirebase(location: Location) {
        val uid = mAuth.currentUser?.uid ?: return
        val datosConector = mapOf(
            "uid" to uid,
            "nombre" to (SessionManager.nombreUsuario ?: "Conductor"),
            "latitud" to location.latitude,
            "longitud" to location.longitude,
            "estado" to "disponible",
            "timestamp" to System.currentTimeMillis()
        )
        // Guardamos las coordenadas vivas para que los pasajeros nos vean en el mapa
        mDatabase.child("conductores_disponibles").child(uid).setValue(datosConector)
    }

    private fun removerConductorDisponible() {
        val uid = mAuth.currentUser?.uid ?: return
        mDatabase.child("conductores_disponibles").child(uid).removeValue()
    }

    private fun iniciarRastreoGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun detenerRastreoGps() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        cargarNombreConductor()
        cargarRatingConductor()
        if (isOnline) {
            iniciarRastreoGps()
        }
    }

    private fun cargarNombreConductor() {
        val nombreGuardado = SessionManager.nombreUsuario
        if (!nombreGuardado.isNullOrEmpty()) {
            tvNombreConductor.text = "Hola, $nombreGuardado"
        } else {
            tvNombreConductor.text = "Hola, Conductor"
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
        )

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = true

        val trujillo = LatLng(-8.1116, -79.0287)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(trujillo, 14f))

        verificarPermisosUbicacion()
    }

    private fun verificarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            activarCapaUbicacionNativa()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun activarCapaUbicacionNativa() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
    }

    private fun animarHaciaUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            verificarPermisosUbicacion()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                ultimaUbicacionConductor = location
                val miUbicacion = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 16.5f))
            } else {
                Toast.makeText(this, "Enciende tu GPS para centrar tu ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activarCapaUbicacionNativa()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navegarA(claseDestino: Class<*>) {
        vibrarDispositivo(30)
        startActivity(Intent(this, claseDestino))
    }

    private fun vibrarDispositivo(milisegundos: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milisegundos)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isOnline) {
            detenerRastreoGps()
        }
    }

    private fun cargarRatingConductor() {
        val uid = mAuth.currentUser?.uid ?: return
        val tv = findViewById<TextView>(R.id.btnIrComentarios)
        mDatabase.child("usuarios").child("conductores").child(uid).child("resenas")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    var suma = 0; var n = 0
                    for (c in snapshot.children) {
                        val cal = c.child("calificacion").getValue(Int::class.java) ?: continue
                        suma += cal; n++
                    }
                    tv.text = if (n == 0) "Ver reseñas"
                        else "⭐ %.1f (%d) · Ver reseñas".format(suma.toDouble() / n, n)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun cargarGananciasHoy() {
        val uid = mAuth.currentUser?.uid ?: return

        // Inicio del día de hoy (medianoche)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val inicioDia = cal.timeInMillis

        mDatabase.child("usuarios").child("conductores").child(uid).child("ganancias")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    var total = 0.0
                    var viajes = 0
                    for (child in snapshot.children) {
                        val ts = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        if (ts >= inicioDia) {
                            total += child.child("monto").getValue(Double::class.java) ?: 0.0
                            viajes++
                        }
                    }
                    tvGananciaHoy.text = "S/ %.2f".format(total)
                    tvViajesHoy.text = viajes.toString()
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private var notiListener: com.google.firebase.database.ChildEventListener? = null
    private var notiUid: String = ""

    private fun iniciarNotificaciones() {
        val uid = mAuth.currentUser?.uid ?: return
        notiUid = uid
        com.taxisign.pro.notificaciones.NotificacionHelper.crearCanal(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9912)
            }
        }
        notiListener = com.taxisign.pro.notificaciones.NotificacionHelper.escuchar(
            context = this, miUid = uid, flash = false
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        removerConductorDisponible()
        com.taxisign.pro.notificaciones.NotificacionHelper.dejarDeEscuchar(
            notiUid, notiListener
        )
    }
}