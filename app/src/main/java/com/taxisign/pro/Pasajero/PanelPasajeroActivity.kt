package com.taxisign.pro.pasajero

import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.notificaciones.NotificationsActivity
import com.taxisign.pro.R
import com.taxisign.pro.SessionManager
import com.taxisign.pro.Login.LoginActivity
import com.taxisign.pro.mapa.MapaBaseFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PanelPasajeroActivity : AppCompatActivity() {

    private lateinit var etBusqueda: EditText
    private lateinit var recyclerResultados: RecyclerView
    private lateinit var recyclerRecientes: RecyclerView
    private lateinit var tvTituloRecientes: TextView
    private lateinit var adapter: DireccionAdapter
    private lateinit var placesClient: PlacesClient
    private val listaDirecciones = mutableListOf<Direccion>()

    private var vibrator: Vibrator? = null
    private lateinit var mapaFragment: MapaBaseFragment

    private lateinit var txtUsuario: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnMiUbicacion: ImageButton
    private lateinit var layoutBuscar: LinearLayout
    private lateinit var chipCasa: LinearLayout
    private lateinit var chipTrabajo: LinearLayout
    private lateinit var btnPedirTaxi: LinearLayout
    private lateinit var tabViajes: LinearLayout
    private lateinit var tabMensajes: LinearLayout
    private lateinit var tabPerfil: LinearLayout
    private lateinit var bottomContainer: LinearLayout
    private lateinit var panelBuscarDestino: LinearLayout
    private lateinit var btnCerrarBusqueda: ImageButton
    private lateinit var txtDestino: TextView
    private lateinit var btnCiudadActual: LinearLayout
    private lateinit var txtCiudadActual: TextView

    private var destinoSeleccionado = ""
    private var destinoLatitud = 0.0
    private var destinoLongitud = 0.0

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRoot by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_pasajero)

        iniciarNotificaciones()

        etBusqueda = findViewById(R.id.et_busqueda)
        recyclerResultados = findViewById(R.id.recyclerResultados)
        recyclerRecientes = findViewById(R.id.recyclerRecientes)
        tvTituloRecientes = findViewById(R.id.tvTituloRecientes)
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        mapaFragment =
            supportFragmentManager.findFragmentById(R.id.mapaBaseFragment) as MapaBaseFragment

        txtUsuario = findViewById(R.id.txtUsuario)
        btnMenu = findViewById(R.id.btn_menu)
        btnMiUbicacion = findViewById(R.id.btnMiUbicacion)
        layoutBuscar = findViewById(R.id.layout_buscar)
        chipCasa = findViewById(R.id.chip_casa)
        chipTrabajo = findViewById(R.id.chip_trabajo)
        btnPedirTaxi = findViewById(R.id.btn_pedir_taxi)
        tabViajes = findViewById(R.id.tab_viajes)
        tabMensajes = findViewById(R.id.tab_notificaciones)
        tabPerfil = findViewById(R.id.tab_perfil)
        bottomContainer = findViewById(R.id.bottomContainer)
        panelBuscarDestino = findViewById(R.id.panelBuscarDestino)
        btnCerrarBusqueda = findViewById(R.id.btnCerrarBusqueda)
        txtDestino = findViewById(R.id.txtDestino)
        btnCiudadActual = findViewById(R.id.btnCiudadActual)
        txtCiudadActual = findViewById(R.id.txtCiudadActual)

        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            irLogin()
            return
        }

        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyBXZDPoloKmzL9k0tDLBjfQ37hZ2kh87JE"
            )
        }

        placesClient = Places.createClient(this)

        recyclerResultados.layoutManager = LinearLayoutManager(this)
        adapter = DireccionAdapter(listaDirecciones) { direccion ->
            etBusqueda.setText(direccion.nombre)
            recyclerResultados.visibility = View.GONE
            ocultarTeclado()
            obtenerCoordenadasDireccion(direccion)
        }
        recyclerResultados.adapter = adapter

        recyclerRecientes.layoutManager = LinearLayoutManager(this)

        etBusqueda.addTextChangedListener {
            val texto = it.toString().trim()

            if (texto.length >= 2) {
                ocultarRecientes()
                buscarDirecciones(texto)
            } else {
                listaDirecciones.clear()
                adapter.notifyDataSetChanged()
                recyclerResultados.visibility = View.GONE
                mostrarRecientesSiHay()
            }
        }

        findViewById<LinearLayout>(R.id.btn_confirmar_destino).setOnClickListener {
            val destino = etBusqueda.text.toString().trim()

            if (destino.isNotEmpty()) {
                ocultarTeclado()
                obtenerCoordenadas(destino)
            }
        }

        btnMiUbicacion.setOnClickListener {
            vibrarConfirmacion()
            mapaFragment.moverAMiUbicacion()
        }

        btnCiudadActual.setOnClickListener {
            vibrarConfirmacion()
            cambiarCiudadLauncher.launch(
                Intent(this, SeleccionarCiudadActivity::class.java)
                    .putExtra("ES_CAMBIO", true)
            )
        }

        btnMenu.setOnClickListener {
            vibrarConfirmacion()
            mostrarMenuUsuario()
        }

        layoutBuscar.setOnClickListener {
            vibrarConfirmacion()
            mostrarBusqueda()
        }

        btnCerrarBusqueda.setOnClickListener {
            vibrarConfirmacion()
            ocultarBusqueda()
        }

        chipCasa.setOnClickListener {
            vibrarConfirmacion()
            usarLugarGuardado("CASA")
        }

        chipTrabajo.setOnClickListener {
            vibrarConfirmacion()
            usarLugarGuardado("TRABAJO")
        }

        findViewById<LinearLayout>(R.id.btn_mis_lugares).setOnClickListener {
            vibrarConfirmacion()
            lugaresLauncher.launch(Intent(this, LugaresActivity::class.java))
        }

        btnPedirTaxi.setOnClickListener {
            if (destinoSeleccionado.isEmpty()) {
                Toast.makeText(this, "Selecciona un destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vibrarConfirmacion()

            val origenReal = obtenerDireccionDeCoords(
                MapaBaseFragment.ultimaLatitud,
                MapaBaseFragment.ultimaLongitud
            )

            val intent = Intent(this, ConfirmacionViajeActivity::class.java).apply {
                putExtra("origen", origenReal)
                putExtra("destino", destinoSeleccionado)
                putExtra("origenLat", MapaBaseFragment.ultimaLatitud)
                putExtra("origenLng", MapaBaseFragment.ultimaLongitud)
                putExtra("destinoLat", destinoLatitud)
                putExtra("destinoLng", destinoLongitud)
                putExtra("metodo_pago", "Efectivo")
            }

            startActivity(intent)
        }

        tabViajes.setOnClickListener {
            startActivity(Intent(this, HistorialViajesPasajeroActivity::class.java))
        }

        tabMensajes.setOnClickListener {
            vibrarConfirmacion()
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        tabPerfil.setOnClickListener {
            vibrarConfirmacion()
            startActivity(Intent(this, PerfilPasajeroActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosUsuarioDesdeFirebase()
    }

    private var notiListener: com.google.firebase.database.ChildEventListener? = null
    private var notiUid: String = ""

    private fun iniciarNotificaciones() {
        val uid = auth.currentUser?.uid ?: return
        notiUid = uid
        com.taxisign.pro.notificaciones.NotificacionHelper.crearCanal(this)

        // Permiso de notificaciones en Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9911)
            }
        }

        // El pasajero suele ser sordo → activamos flash
        notiListener = com.taxisign.pro.notificaciones.NotificacionHelper.escuchar(
            context = this, miUid = uid, flash = true
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        com.taxisign.pro.notificaciones.NotificacionHelper.dejarDeEscuchar(
            notiUid, notiListener
        )
    }

    private fun cargarDatosUsuarioDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: return

        dbRoot.child("usuarios")
            .child("pasajeros")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    txtUsuario.text = "¡Hola, Usuario!"
                    return@addOnSuccessListener
                }

                val nombre = snapshot.child("nombre").value?.toString()
                    ?: auth.currentUser?.displayName
                    ?: "Usuario"

                val correo = snapshot.child("email").value?.toString()
                    ?: auth.currentUser?.email
                    ?: ""

                val telefono = snapshot.child("telefono").value?.toString() ?: ""
                val dni = snapshot.child("dni").value?.toString() ?: ""
                val tipoDiscapacidad =
                    snapshot.child("tipoDiscapacidad").value?.toString() ?: ""

                txtUsuario.text = "¡Hola, $nombre!"

                SessionManager.nombreUsuario = nombre
                SessionManager.correoUsuario = correo
                SessionManager.telefonoUsuario = telefono
                SessionManager.dniUsuario = dni
                SessionManager.tipoDiscapacidadUsuario = tipoDiscapacidad
                SessionManager.esSordoMudo = tipoDiscapacidad.equals("Sordo", ignoreCase = true) ||
                        tipoDiscapacidad.equals("Mudo", ignoreCase = true) ||
                        tipoDiscapacidad.equals("Sordomudo", ignoreCase = true) ||
                        tipoDiscapacidad.contains("sordo", ignoreCase = true) ||
                        tipoDiscapacidad.contains("mudo", ignoreCase = true)

                val ciudad = snapshot.child("ciudad").value?.toString() ?: ""
                if (ciudad.isNotEmpty()) {
                    val ciudadLat = snapshot.child("ciudadLat").getValue(Double::class.java) ?: 0.0
                    val ciudadLng = snapshot.child("ciudadLng").getValue(Double::class.java) ?: 0.0
                    SessionManager.ciudadActual = ciudad
                    SessionManager.ciudadLat = ciudadLat
                    SessionManager.ciudadLng = ciudadLng
                    txtCiudadActual.text = ciudad
                } else {
                    txtCiudadActual.text = "Elegir ciudad"
                }
            }
            .addOnFailureListener {
                txtUsuario.text = "¡Hola, ${SessionManager.nombreUsuario}!"
            }
    }

    private val cambiarCiudadLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            txtCiudadActual.text = SessionManager.ciudadActual.ifEmpty { "Elegir ciudad" }
            Toast.makeText(this, "Ciudad actualizada a ${SessionManager.ciudadActual}", Toast.LENGTH_SHORT).show()
        }
    }

    private val lugaresLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val direccion = result.data?.getStringExtra("LUGAR_DIRECCION") ?: ""
            if (direccion.isNotEmpty()) {
                etBusqueda.setText(direccion)
                etBusqueda.setSelection(direccion.length)
                buscarDirecciones(direccion) // que Places resuelva las coordenadas
            }
        }
    }

    private fun usarLugarGuardado(tipo: String) {
        val uid = auth.currentUser?.uid ?: return
        dbRoot.child("usuarios").child("pasajeros").child(uid)
            .child("lugares").child(tipo).get()
            .addOnSuccessListener { snap ->
                val dir = snap.child("direccion").value?.toString() ?: ""
                val lat = snap.child("lat").getValue(Double::class.java) ?: 0.0
                val lng = snap.child("lng").getValue(Double::class.java) ?: 0.0

                if (dir.isEmpty()) {
                    val cual = if (tipo == "CASA") "tu casa" else "tu trabajo/estudios"
                    Toast.makeText(this, "Aún no has guardado $cual en \"Mis lugares\"", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (lat != 0.0 || lng != 0.0) {
                    // Marcar directamente en el mapa la ubicación exacta guardada
                    destinoSeleccionado = dir
                    txtDestino.text = dir
                    destinoLatitud = lat
                    destinoLongitud = lng
                    mapaFragment.moverMapaADestino(lat, lng, dir)
                    ocultarBusqueda()
                    ocultarTeclado()
                    Toast.makeText(this, "Destino: $dir", Toast.LENGTH_SHORT).show()
                    // No se registra como "reciente": ya vive en Mis lugares (Casa/Trabajo)
                } else {
                    // Sin coordenadas guardadas: resolver por texto
                    mostrarBusqueda()
                    etBusqueda.setText(dir)
                    etBusqueda.setSelection(dir.length)
                    buscarDirecciones(dir)
                }
            }
    }

    private fun obtenerDireccionDeCoords(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "Mi ubicación actual"
        return try {
            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
            val res = geocoder.getFromLocation(lat, lng, 1)
            val dir = res?.firstOrNull()
            if (dir != null) {
                val linea = dir.getAddressLine(0)
                if (!linea.isNullOrEmpty()) linea
                else listOfNotNull(dir.thoroughfare, dir.subLocality, dir.locality)
                    .joinToString(", ").ifEmpty { "Mi ubicación actual" }
            } else "Mi ubicación actual"
        } catch (e: Exception) {
            "Mi ubicación actual"
        }
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(etBusqueda.windowToken, 0)
        etBusqueda.clearFocus()
    }

    private fun mostrarMenuUsuario() {
        val popup = PopupMenu(this, btnMenu)

        popup.menu.add("Mi perfil")
        popup.menu.add("Mis viajes")
        popup.menu.add("Mis lugares")
        popup.menu.add("Cambiar ciudad")
        popup.menu.add("Notificaciones")
        popup.menu.add("Configuración")
        popup.menu.add("Seguridad y contraseña")
        popup.menu.add("Cerrar sesión")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Mi perfil" -> {
                    startActivity(Intent(this, PerfilPasajeroActivity::class.java))
                    true
                }

                "Mis viajes" -> {
                    startActivity(Intent(this, HistorialViajesPasajeroActivity::class.java))
                    true
                }

                "Mis lugares" -> {
                    lugaresLauncher.launch(Intent(this, LugaresActivity::class.java))
                    true
                }

                "Cambiar ciudad" -> {
                    cambiarCiudadLauncher.launch(
                        Intent(this, SeleccionarCiudadActivity::class.java)
                            .putExtra("ES_CAMBIO", true)
                    )
                    true
                }

                "Notificaciones" -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    true
                }

                "Configuración" -> {
                    startActivity(Intent(this, AjustesPasajeroActivity::class.java))
                    true
                }

                "Seguridad y contraseña" -> {
                    startActivity(Intent(this, com.taxisign.pro.Seguridad.SeguridadContraseña::class.java))
                    true
                }

                "Cerrar sesión" -> {
                    cerrarSesion()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    private fun cerrarSesion() {
        // Quitar listeners ANTES de cerrar sesión (evita errores de permiso de Firebase)
        com.taxisign.pro.notificaciones.NotificacionHelper.dejarDeEscuchar(notiUid, notiListener)
        notiListener = null

        auth.signOut()

        SessionManager.idUsuario = 0
        SessionManager.nombreUsuario = ""
        SessionManager.correoUsuario = ""
        SessionManager.telefonoUsuario = ""
        SessionManager.dniUsuario = ""
        SessionManager.tipoDiscapacidadUsuario = ""

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        irLogin()
    }

    private fun irLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarBusqueda() {
        findViewById<View>(R.id.topBar).visibility = View.GONE
        btnMiUbicacion.visibility = View.GONE

        bottomContainer.animate()
            .translationY(bottomContainer.height.toFloat())
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                bottomContainer.visibility = View.GONE
                panelBuscarDestino.visibility = View.VISIBLE
                panelBuscarDestino.alpha = 0f
                panelBuscarDestino.translationY = 300f

                panelBuscarDestino.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .start()

                // Al abrir el buscador, sin nada escrito aún, mostramos
                // primero los lugares recientes (como InDriver) para que
                // el pasajero no tenga que volver a escribir destinos
                // a los que ya fue antes.
                if (etBusqueda.text.isNullOrBlank()) {
                    mostrarRecientesSiHay()
                }
            }
            .start()
    }
    private fun ocultarBusqueda() {
        panelBuscarDestino.animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                panelBuscarDestino.visibility = View.GONE

                findViewById<View>(R.id.topBar).visibility = View.VISIBLE
                btnMiUbicacion.visibility = View.VISIBLE

                bottomContainer.visibility = View.VISIBLE
                bottomContainer.alpha = 0f
                bottomContainer.translationY = 300f

                bottomContainer.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
    }

    /** Carga y muestra los lugares recientes del pasajero, si tiene alguno guardado. */
    private fun mostrarRecientesSiHay() {
        val uid = auth.currentUser?.uid ?: return
        RecientesHelper.cargarRecientes(uid) { recientes ->
            if (recientes.isEmpty()) {
                tvTituloRecientes.visibility = View.GONE
                recyclerRecientes.visibility = View.GONE
                return@cargarRecientes
            }

            tvTituloRecientes.visibility = View.VISIBLE
            recyclerRecientes.visibility = View.VISIBLE
            recyclerRecientes.adapter = LugarRecienteAdapter(recientes) { lugar ->
                etBusqueda.setText(lugar.direccion)
                ocultarTeclado()

                if (lugar.lat != 0.0 || lugar.lng != 0.0) {
                    confirmarDestino(lugar.direccion, lugar.lat, lugar.lng)
                } else {
                    obtenerCoordenadas(lugar.direccion)
                }
            }
        }
    }

    private fun ocultarRecientes() {
        tvTituloRecientes.visibility = View.GONE
        recyclerRecientes.visibility = View.GONE
    }

    /**
     * Construye el sesgo geográfico (bounds) alrededor de la ciudad activa del
     * pasajero, para que Google Places priorice resultados reales y cercanos
     * en vez de devolver lugares de cualquier parte del Perú.
     */
    private fun bondsCiudadActual(): LatLngBounds {
        val lat = if (SessionManager.ciudadLat != 0.0) SessionManager.ciudadLat else MapaBaseFragment.ultimaLatitud
        val lng = if (SessionManager.ciudadLng != 0.0) SessionManager.ciudadLng else MapaBaseFragment.ultimaLongitud
        val radioKm = if (SessionManager.ciudadRadioKm > 0) SessionManager.ciudadRadioKm else 25.0

        // Conversión aproximada de km a grados de latitud/longitud
        val deltaLat = radioKm / 111.0
        val deltaLng = radioKm / (111.0 * Math.cos(Math.toRadians(lat)).coerceAtLeast(0.1))

        return LatLngBounds(
            LatLng(lat - deltaLat, lng - deltaLng),
            LatLng(lat + deltaLat, lng + deltaLng)
        )
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
                            nombre = prediction.getFullText(null).toString(),
                            latitud = 0.0,
                            longitud = 0.0,
                            placeId = prediction.placeId
                        )
                    )
                }

                adapter.notifyDataSetChanged()

                recyclerResultados.visibility =
                    if (listaDirecciones.isNotEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error buscando direcciones: revisa tu conexión y la API de Places en la consola de Google", Toast.LENGTH_LONG).show()
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
                    confirmarDestino(nombre, lat, lng)
                }
                .addOnFailureListener {
                    obtenerCoordenadas(direccion.nombre)
                }
        } else {
            obtenerCoordenadas(direccion.nombre)
        }
    }

    private fun obtenerCoordenadas(direccion: String) {
        val geocoder = Geocoder(this)

        try {
            val resultados = geocoder.getFromLocationName(direccion, 1)

            if (!resultados.isNullOrEmpty()) {
                val location = resultados[0]
                confirmarDestino(direccion, location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "No se pudo encontrar esa dirección", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(
                this,
                "No se pudo obtener la ubicación",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Aplica el destino seleccionado al mapa y lo guarda como búsqueda reciente. */
    private fun confirmarDestino(direccion: String, lat: Double, lng: Double) {
        destinoSeleccionado = direccion
        txtDestino.text = direccion
        destinoLatitud = lat
        destinoLongitud = lng

        mapaFragment.moverMapaADestino(lat, lng, direccion)
        ocultarBusqueda()

        // Guardamos automáticamente este destino real como "reciente",
        // igual que InDriver: si el pasajero repite el lugar, la próxima
        // vez aparecerá primero en su lista de búsquedas.
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            RecientesHelper.registrarDestinoUsado(uid, direccion, lat, lng)
        }
    }

    private fun vibrarConfirmacion() {
        vibrator?.let {
            if (!it.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        60,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(60)
            }
        }
    }
}