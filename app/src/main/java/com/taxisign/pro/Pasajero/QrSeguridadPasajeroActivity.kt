package com.taxisign.pro.pasajero

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.taxisign.pro.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * QrSeguridadPasajeroActivity
 *
 * Muestra un QR con el código de seguridad del viaje. El conductor lo escanea.
 * Cuando el conductor valida (viajes/{id}/viajeIniciado == true) recién pasa a
 * DuranteViajePasajeroActivity. Estilo "PIN de Uber" pero con QR.
 */
class QrSeguridadPasajeroActivity : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private var viajeId = ""
    private var listener: ValueEventListener? = null
    private var yaNavego = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_seguridad_pasajero)

        viajeId = intent.getStringExtra("VIAJE_ID") ?: ""
        if (viajeId.isEmpty()) { finish(); return }

        prepararCodigoYqr()
        escucharValidacion()
    }

    /** Lee el código; si no existe, lo genera y guarda. Luego pinta el QR. */
    private fun prepararCodigoYqr() {
        val ref = db.child("viajes").child(viajeId).child("codigoSeguridad")
        ref.get().addOnSuccessListener { snap ->
            var codigo = snap.value?.toString() ?: ""
            if (codigo.isEmpty()) {
                codigo = (100000..999999).random().toString()
                ref.setValue(codigo)
            }
            findViewById<TextView>(R.id.tv_codigo).text = codigo
            pintarQr(codigo)
        }
    }

    private fun pintarQr(contenido: String) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val size = 600
            val matrix = writer.encode(contenido, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            findViewById<ImageView>(R.id.img_qr).setImageBitmap(bmp)
        } catch (_: Exception) { }
    }

    private fun escucharValidacion() {
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val iniciado = snapshot.getValue(Boolean::class.java) ?: false
                if (iniciado && !yaNavego) {
                    yaNavego = true
                    irADurante()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("viajes").child(viajeId).child("viajeIniciado")
            .addValueEventListener(listener!!)
    }

    private fun irADurante() {
        val intent = Intent(this, DuranteViajePasajeroActivity::class.java)
        intent.putExtras(this.intent.extras ?: Bundle()) // reenvía todos los extras
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let {
            if (viajeId.isNotEmpty())
                db.child("viajes").child(viajeId).child("viajeIniciado").removeEventListener(it)
        }
    }
}
