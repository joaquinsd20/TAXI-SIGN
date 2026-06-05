package com.example.taxisign.pasajero

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.location.Geocoder
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText

import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.example.taxisign.ApiConfig

import com.example.taxisign.MensajesActivity
import com.example.taxisign.R
import com.example.taxisign.SessionManager
import com.example.taxisign.mapa.MapaBaseFragment

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
class PanelPasajeroActivity : AppCompatActivity() {

    private lateinit var etBusqueda: EditText
    private lateinit var recyclerResultados: RecyclerView
    private lateinit var adapter: DireccionAdapter
    private lateinit var placesClient: PlacesClient
    private val listaDirecciones =
        mutableListOf<Direccion>()
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
    private var destinoSeleccionado = ""
    private var destinoLatitud = 0.0
    private var destinoLongitud = 0.0
    private lateinit var txtDestino: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_pasajero)

        etBusqueda =
            findViewById(R.id.et_busqueda)

        recyclerResultados =
            findViewById(R.id.recyclerResultados)

        vibrator =
            getSystemService(
                VIBRATOR_SERVICE
            ) as? Vibrator

        mapaFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.mapaBaseFragment
                ) as MapaBaseFragment

        txtUsuario =
            findViewById(R.id.txtUsuario)

        btnMenu =
            findViewById(R.id.btn_menu)

        btnMiUbicacion =
            findViewById(R.id.btnMiUbicacion)

        layoutBuscar =
            findViewById(R.id.layout_buscar)

        chipCasa =
            findViewById(R.id.chip_casa)

        chipTrabajo =
            findViewById(R.id.chip_trabajo)

        btnPedirTaxi =
            findViewById(R.id.btn_pedir_taxi)

        tabViajes =
            findViewById(R.id.tab_viajes)

        tabMensajes =
            findViewById(R.id.tab_mensajes)

        tabPerfil =
            findViewById(R.id.tab_perfil)

        bottomContainer =
            findViewById(R.id.bottomContainer)

        panelBuscarDestino =
            findViewById(R.id.panelBuscarDestino)

        btnCerrarBusqueda =
            findViewById(R.id.btnCerrarBusqueda)
        txtDestino =
            findViewById(R.id.txtDestino)

        if (SessionManager.idUsuario == 0) {

            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        txtUsuario.text =
            "¡Hola, ${SessionManager.nombreUsuario}!"
        if (!Places.isInitialized()) {

            Places.initialize(
                applicationContext,
                "AIzaSyBXZDPoloKmzL9k0tDLBjfQ37hZ2kh87JE"
            )
        }

        placesClient =
            Places.createClient(this)
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

        findViewById<LinearLayout>(
            R.id.btn_confirmar_destino
        ).setOnClickListener {

            val destino =
                etBusqueda.text
                    .toString()
                    .trim()

            if (destino.isNotEmpty()) {

                obtenerCoordenadas(destino)
            }
        }
        btnMiUbicacion.setOnClickListener {

            vibrarConfirmacion()

            mapaFragment.moverAMiUbicacion()
        }

        btnMenu.setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Menú de usuario",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ABRIR PANEL BUSQUEDA

        layoutBuscar.setOnClickListener {

            vibrarConfirmacion()

            mostrarBusqueda()
        }

        // CERRAR PANEL BUSQUEDA

        btnCerrarBusqueda.setOnClickListener {

            vibrarConfirmacion()

            ocultarBusqueda()
        }

        chipCasa.setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Casa",
                Toast.LENGTH_SHORT
            ).show()
        }

        chipTrabajo.setOnClickListener {

            vibrarConfirmacion()

            Toast.makeText(
                this,
                "Trabajo",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnPedirTaxi.setOnClickListener {

            if (destinoSeleccionado.isEmpty()) {

                Toast.makeText(
                    this,
                    "Selecciona un destino",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val request =
                object : StringRequest(
                    Method.POST,
                    ApiConfig.CREAR_SOLICITUD,

                    { response ->

                        try {

                            val json =
                                JSONObject(response)

                            if (json.getBoolean("success")) {

                                val idSolicitud =
                                    json.getInt("id_solicitud")

                                val intent =
                                    Intent(
                                        this,
                                        BuscarConductorActivity::class.java
                                    )

                                intent.putExtra(
                                    "id_solicitud",
                                    idSolicitud
                                )

                                intent.putExtra(
                                    "destino",
                                    destinoSeleccionado
                                )

                                intent.putExtra(
                                    "destino_lat",
                                    destinoLatitud
                                )

                                intent.putExtra(
                                    "destino_lng",
                                    destinoLongitud
                                )

                                startActivity(intent)

                            } else {

                                Toast.makeText(
                                    this,
                                    "No se pudo crear la solicitud",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }
                    },

                    {

                        Toast.makeText(
                            this,
                            "Error de conexión",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                ) {

                    override fun getParams():
                            MutableMap<String, String> {

                        val params =
                            HashMap<String, String>()

                        params["id_pasajero"] =
                            SessionManager.idUsuario.toString()

                        params["origen_lat"] =
                            MapaBaseFragment.ultimaLatitud.toString()

                        params["origen_lng"] =
                            MapaBaseFragment.ultimaLongitud.toString()

                        params["destino"] =
                            destinoSeleccionado

                        params["destino_lat"] =
                            destinoLatitud.toString()

                        params["destino_lng"] =
                            destinoLongitud.toString()

                        return params
                    }
                }

            Volley
                .newRequestQueue(this)
                .add(request)
        }
        tabViajes.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HistorialViajesPasajeroActivity::class.java
                )
            )
        }

        tabMensajes.setOnClickListener {

            vibrarConfirmacion()

            startActivity(
                Intent(
                    this,
                    MensajesActivity::class.java
                )
            )
        }

        tabPerfil.setOnClickListener {

            vibrarConfirmacion()

            startActivity(
                Intent(
                    this,
                    PerfilPasajeroActivity::class.java
                )
            )
        }
    }

    private fun mostrarBusqueda() {

        bottomContainer.animate()
            .translationY(
                bottomContainer.height.toFloat()
            )
            .alpha(0f)
            .setDuration(250)
            .withEndAction {

                bottomContainer.visibility =
                    View.GONE

                panelBuscarDestino.visibility =
                    View.VISIBLE

                panelBuscarDestino.alpha = 0f

                panelBuscarDestino.translationY =
                    300f

                panelBuscarDestino.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
    }

    private fun ocultarBusqueda() {

        panelBuscarDestino.animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(250)
            .withEndAction {

                panelBuscarDestino.visibility =
                    View.GONE

                bottomContainer.visibility =
                    View.VISIBLE

                bottomContainer.alpha = 0f

                bottomContainer.translationY =
                    300f

                bottomContainer.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
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

        placesClient
            .findAutocompletePredictions(request)
            .addOnSuccessListener { response ->

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

            }
            .addOnFailureListener {

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
                destinoSeleccionado = direccion
                txtDestino.text =
                    direccion

                destinoLatitud =
                    location.latitude

                destinoLongitud =
                    location.longitude

                mapaFragment.moverMapaADestino(
                    location.latitude,
                    location.longitude,
                    direccion
                )

                ocultarBusqueda()
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
}