package com.example.taxisign.conductor

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.taxisign.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ModificarDatosConductor : AppCompatActivity() {

    private lateinit var imgConductor: ImageView
    private var fotoSeleccionadaUri: Uri? = null

    // Selector de galería moderno (Photo Picker / Galería nativa)
    private val seleccionarFotoGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fotoSeleccionadaUri = it
            imgConductor.setImageURI(it) // Muestra la foto seleccionada en el círculo de inmediato
            Toast.makeText(this, "Foto cargada localmente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_datos_conductor)

        // 1. Inicializar vistas de control de la barra superior e imagen
        val btnCancelar = findViewById<ImageButton>(R.id.btn_cancelar_edicion)
        val fabCambiarFoto = findViewById<FloatingActionButton>(R.id.fab_cambiar_foto)
        imgConductor = findViewById(R.id.img_editar_conductor)

        // 2. Inicializar campos del formulario
        val etNombre = findViewById<EditText>(R.id.et_editar_nombre)
        val etTelefono = findViewById<EditText>(R.id.et_editar_telefono)
        val etCorreo = findViewById<EditText>(R.id.et_editar_correo)
        val etPassActual = findViewById<EditText>(R.id.et_pass_actual)
        val etPassNueva = findViewById<EditText>(R.id.et_pass_nueva)

        // 3. Inicializar el botón de acción principal
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_datos_personales)

        // BOTÓN REGRESAR / CANCELAR: Vuelve a la pantalla anterior sin guardar nada
        btnCancelar.setOnClickListener {
            finish()
        }

        // ACCIÓN CAMBIAR FOTO: Lanza el selector para buscar imágenes en el dispositivo
        fabCambiarFoto.setOnClickListener {
            seleccionarFotoGaleria.launch("image/*")
        }

        // BOTÓN GUARDAR CAMBIOS: Valida el formulario completo antes de procesar
        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val passActual = etPassActual.text.toString()
            val passNueva = etPassNueva.text.toString()

            // Validación de campos vacíos obligatorios
            if (nombre.isEmpty() || telefono.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos personales", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación básica de la longitud del teléfono
            if (telefono.length < 9) {
                Toast.makeText(this, "El teléfono debe tener al menos 9 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lógica y validación cruzada para cambio de contraseña
            if (passNueva.isNotEmpty()) {
                if (passActual.isEmpty()) {
                    Toast.makeText(this, "Introduce tu contraseña actual para poder cambiarla", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (passNueva.length < 6) {
                    Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (passActual == passNueva) {
                    Toast.makeText(this, "La nueva contraseña no puede ser igual a la actual", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // --- PROCESO DE GUARDADO EXITOSO ---
            // Aquí es donde enviarías 'fotoSeleccionadaUri', los textos y contraseñas a Firebase o tu API
            if (passNueva.isNotEmpty()) {
                Toast.makeText(this, "Datos personales y contraseña actualizados con éxito", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Datos personales actualizados con éxito", Toast.LENGTH_LONG).show()
            }

            // Cierra la pantalla y regresa al Perfil de manera limpia
            finish()
        }
    }
}