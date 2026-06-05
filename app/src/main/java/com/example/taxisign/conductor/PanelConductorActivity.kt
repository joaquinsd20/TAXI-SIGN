package com.example.taxisign.conductor

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taxisign.R
import com.example.taxisign.Soporte
// Asegúrate de que esta clase exista en tu carpeta/paquete 'conductor'
import com.example.taxisign.conductor.BuscarPasajero

class PanelConductorActivity : AppCompatActivity() {

    private var isOnline = false
    private lateinit var vibrator: Vibrator

    // AGREGADO: Handler y Runnable para controlar el retraso antes de buscar pasajeros
    private val handlerSolicitud = Handler(Looper.getMainLooper())
    private lateinit var runnableBuscarPasajero: Runnable

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_conductor)

        // Inicializamos el vibrador para dar feedback táctil a los botones
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // --- 1. REFERENCIAS DE LOS BOTONES ---

        // Superiores
        val btnPerfil = findViewById<CardView>(R.id.btnIrPerfil)
        val btnGananciasMoneda = findViewById<CardView>(R.id.btnIrGanancias)
        val tvReseñas = findViewById<TextView>(R.id.btnIrComentarios)

        // Menú Lateral (Iconos)
        val btnDocumentos = findViewById<CardView>(R.id.btnIrDocumentos)
        val btnSoporte = findViewById<CardView>(R.id.btnIrSoporte)
        val btnAjustes = findViewById<CardView>(R.id.btnIrAjustes)

        // Panel Inferior y Estado
        val btnToggleStatus = findViewById<Button>(R.id.btnToggleStatus)
        val tvStatusLabel = findViewById<TextView>(R.id.tvStatusLabel)
        val tvGananciaHoy = findViewById<TextView>(R.id.tvGananciaHoy)
        val tvViajesHoy = findViewById<TextView>(R.id.tvViajesHoy)

        // --- 2. CONFIGURACIÓN DE NAVEGACIÓN FUNCIONAL ---

        // Ir al Perfil (Click en la foto/nombre)
        btnPerfil.setOnClickListener {
            navegarA(PerfilConductorActivity::class.java)
        }

        // Ir a Ganancias Detalladas (Click en la moneda amarilla)
        btnGananciasMoneda.setOnClickListener {
            navegarA(ActivityGanancias::class.java)
        }

        // Ir a Comentarios/Reputación (Click en las estrellas)
        tvReseñas.setOnClickListener {
            navegarA(ComentariosConductor::class.java)
        }

        // Ir a Gestión de Documentos (Icono de Carnet 🪪)
        btnDocumentos.setOnClickListener {
            navegarA(DocumentosConductor::class.java)
        }

        // Ir a Soporte/Ayuda (Icono de Audífonos 🎧)
        btnSoporte.setOnClickListener {
            navegarA(Soporte::class.java)
        }

        // Ir a Ajustes (Icono de Engranaje ⚙️)
        btnAjustes.setOnClickListener {
            navegarA(AjustesConductor::class.java)
        }

        // --- 3. AGREGADO: ACCIÓN AUTOMÁTICA DE BÚSQUEDA ---
        runnableBuscarPasajero = Runnable {
            if (isOnline) {
                vibrarDispositivo(150) // Vibración de aviso
                Toast.makeText(this, "Iniciando radar de viaje...", Toast.LENGTH_SHORT).show()

                // Abre la pantalla de BuscarPasajero que querías
                val intentBuscar = Intent(this, BuscarPasajero::class.java)
                startActivity(intentBuscar)
            }
        }

        // --- 4. LÓGICA DEL BOTÓN DE ESTADO (TURNO) ---

        btnToggleStatus.setOnClickListener {
            vibrarDispositivo(50)
            if (!isOnline) {
                // Conectar
                isOnline = true
                btnToggleStatus.text = "FINALIZAR TURNO"
                btnToggleStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF4B4B"))
                btnToggleStatus.setTextColor(Color.WHITE)

                tvStatusLabel.text = "CONECTADO - BUSCANDO PASAJEROS"
                tvStatusLabel.setTextColor(Color.parseColor("#4CAF50"))

                // Valores de ejemplo al conectar
                tvGananciaHoy.text = "S/ 25.00"
                tvViajesHoy.text = "2"

                Toast.makeText(this, "Modo Conductor Activo", Toast.LENGTH_SHORT).show()

                // AGREGADO: Tras 3 segundos (3000ms) ejecuta la redirección automática a BuscarPasajero
                handlerSolicitud.postDelayed(runnableBuscarPasajero, 3000)

            } else {
                // Desconectar
                isOnline = false
                btnToggleStatus.text = "INICIAR TURNO"
                btnToggleStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD600"))
                btnToggleStatus.setTextColor(Color.parseColor("#151B31"))

                tvStatusLabel.text = "ESTÁS DESCONECTADO"
                tvStatusLabel.setTextColor(Color.parseColor("#B8C1D9"))

                Toast.makeText(this, "Has salido de línea", Toast.LENGTH_SHORT).show()

                // AGREGADO: Si se desconecta antes de los 3 segundos, cancela la redirección
                handlerSolicitud.removeCallbacks(runnableBuscarPasajero)
            }
        }
    }

    /**
     * Función auxiliar para navegar de forma limpia y con vibración
     */
    private fun navegarA(claseDestino: Class<*>) {
        vibrarDispositivo(30)
        val intent = Intent(this, claseDestino)
        startActivity(intent)
    }

    /**
     * Vibración para dar seguridad al usuario al tocar botones
     */
    private fun vibrarDispositivo(milisegundos: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(milisegundos)
        }
    }

    // AGREGADO: Evita problemas si el usuario sale de la app de imprevisto
    override fun onPause() {
        super.onPause()
        handlerSolicitud.removeCallbacks(runnableBuscarPasajero)
    }
}