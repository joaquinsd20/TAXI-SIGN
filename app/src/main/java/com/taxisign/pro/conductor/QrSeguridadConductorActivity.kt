package com.taxisign.pro.conductor

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * QrSeguridadConductorActivity
 *
 * El conductor escanea el QR del pasajero (o ingresa el código manualmente).
 * Si coincide con viajes/{id}/codigoSeguridad, marca viajeIniciado=true y
 * estado="en_viaje", y recién pasa a DuranteViajeConductor.
 */
class QrSeguridadConductorActivity : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private var viajeId = ""
    private var codigoEsperado = ""

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            validar(result.contents.trim())
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_seguridad_conductor)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        if (viajeId.isEmpty()) { finish(); return }

        // Cargar el código esperado
        db.child("viajes").child(viajeId).child("codigoSeguridad").get()
            .addOnSuccessListener { codigoEsperado = it.value?.toString() ?: "" }

        findViewById<MaterialButton>(R.id.btn_escanear).setOnClickListener {
            val opciones = ScanOptions().apply {
                setPrompt("Apunta al QR del pasajero")
                setBeepEnabled(true)
                setOrientationLocked(true)
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            }
            scanLauncher.launch(opciones)
        }

        findViewById<MaterialButton>(R.id.btn_validar_manual).setOnClickListener {
            val manual = findViewById<EditText>(R.id.et_codigo_manual).text.toString().trim()
            if (manual.isEmpty()) {
                Toast.makeText(this, "Ingresa el código", Toast.LENGTH_SHORT).show()
            } else {
                validar(manual)
            }
        }
    }

    private fun validar(codigo: String) {
        if (codigoEsperado.isEmpty()) {
            // Por si aún no cargó; lo leemos de nuevo
            db.child("viajes").child(viajeId).child("codigoSeguridad").get()
                .addOnSuccessListener {
                    codigoEsperado = it.value?.toString() ?: ""
                    comparar(codigo)
                }
        } else {
            comparar(codigo)
        }
    }

    private fun comparar(codigo: String) {
        if (codigo == codigoEsperado && codigoEsperado.isNotEmpty()) {
            val updates = hashMapOf<String, Any>(
                "viajes/$viajeId/viajeIniciado" to true,
                "viajes/$viajeId/estado" to "en_viaje"
            )
            db.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(this, "¡Verificado! Iniciando viaje", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DuranteViajeConductor::class.java)
                intent.putExtras(this.intent.extras ?: Bundle())
                startActivity(intent)
                finish()
            }
        } else {
            Toast.makeText(this, "El código no coincide. Verifica con el pasajero.", Toast.LENGTH_LONG).show()
        }
    }
}
