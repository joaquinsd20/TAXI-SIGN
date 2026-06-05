package com.example.taxisign.conductor

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taxisign.R

class DocumentosConductor : AppCompatActivity() {

    // Variable de control para saber si estamos subiendo la Licencia o el SOAT
    private var tipoDocumentoActual: String = ""

    // Declaramos las variables de los textos para cambiar su estado dinámicamente
    private lateinit var tvEstadoLicencia: TextView
    private lateinit var tvEstadoSoat: TextView

    // Contrato nativo y moderno de Android para acceder a archivos/fotos del dispositivo
    private val lanzarSelectorArchivos = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { resultadoUri ->
            // Si el usuario seleccionó un archivo o foto con éxito, actualizamos la interfaz
            actualizarEstadoEnPantalla(resultadoUri)
        } ?: run {
            Toast.makeText(this, "Selección cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentos_conductor)

        // 1. Inicializar los componentes de texto de los estados (del XML)
        tvEstadoLicencia = findViewById(R.id.tvEstadoLicencia)
        tvEstadoSoat = findViewById(R.id.tvEstadoSoat)

        // 2. Inicializar los botones que ya tenías
        val btnBack = findViewById<ImageButton>(R.id.btnBackDocs)
        val btnLicencia = findViewById<ImageButton>(R.id.btnSubirLicencia)
        val btnSoat = findViewById<ImageButton>(R.id.btnSubirSoat)

        // Botón Volver
        btnBack.setOnClickListener {
            finish()
        }

        // Acciones reales para abrir el selector de archivos del dispositivo
        btnLicencia.setOnClickListener {
            tipoDocumentoActual = "LICENCIA"
            // "*/*" le permite al conductor escoger tanto fotos de la galería como archivos PDF
            lanzarSelectorArchivos.launch("*/*")
        }

        btnSoat.setOnClickListener {
            tipoDocumentoActual = "SOAT"
            lanzarSelectorArchivos.launch("*/*")
        }
    }

    /**
     * Lógica secundaria que cambia el texto y color del estado al recibir el archivo
     */
    private fun actualizarEstadoEnPantalla(uriArchivo: Uri) {
        // En esta sección ya cuentas con el parámetro 'uriArchivo' listo para subirlo a tu servidor o Firebase

        if (tipoDocumentoActual == "LICENCIA") {
            tvEstadoLicencia.text = "Estado: En Revisión ⏳"
            tvEstadoLicencia.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            Toast.makeText(this, "Licencia cargada de forma local", Toast.LENGTH_SHORT).show()
        } else if (tipoDocumentoActual == "SOAT") {
            tvEstadoSoat.text = "Estado: En Revisión ⏳"
            tvEstadoSoat.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            Toast.makeText(this, "SOAT cargado de forma local", Toast.LENGTH_SHORT).show()
        }
    }
}