package com.example.taxisign.pasajero

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.taxisign.ApiConfig
import com.example.taxisign.R
import com.example.taxisign.SessionManager
import org.json.JSONArray

class HistorialViajesPasajeroActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    private val listaViajes =
        mutableListOf<Viaje>()

    private lateinit var adapter:
            HistorialViajesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_historial_viajes_pasajero
        )

        val btnBack =
            findViewById<ImageButton>(
                R.id.btn_back
            )

        recycler =
            findViewById(
                R.id.recycler_historial
            )

        btnBack.setOnClickListener {

            finish()
        }

        recycler.layoutManager =
            LinearLayoutManager(this)

        adapter =
            HistorialViajesAdapter(
                listaViajes
            )

        recycler.adapter =
            adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {

        val request =
            object : StringRequest(
                Request.Method.POST,
                ApiConfig.OBTENER_VIAJES_PASAJERO,

                { response ->

                    try {

                        listaViajes.clear()

                        val jsonArray =
                            JSONArray(response)

                        for (i in 0 until jsonArray.length()) {

                            val obj =
                                jsonArray.getJSONObject(i)

                            listaViajes.add(

                                Viaje(

                                    obj.getString(
                                        "destino"
                                    ),

                                    obj.getString(
                                        "fecha"
                                    ),

                                    obj.getString(
                                        "estado"
                                    )
                                )
                            )
                        }

                        adapter.notifyDataSetChanged()

                    } catch (e: Exception) {

                        e.printStackTrace()

                        Toast.makeText(
                            this,
                            "Error cargando historial",
                            Toast.LENGTH_SHORT
                        ).show()
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

                    return hashMapOf(

                        "id_pasajero" to
                                SessionManager.idUsuario.toString()
                    )
                }
            }

        Volley.newRequestQueue(this)
            .add(request)
    }
}