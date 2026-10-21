package com.taxisign.pro.pasajero

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taxisign.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * LugaresActivity
 *
 * El pasajero guarda Casa, Trabajo/Colegio/Universidad y lugares frecuentes.
 * Todo se guarda en Firebase: usuarios/pasajeros/{uid}/lugares/{id}
 *
 * Al tocar un lugar, devuelve su dirección a la pantalla anterior (para
 * usarla como destino). Mantenerlo simple y autocontenido.
 */
class LugaresActivity : AppCompatActivity() {

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private fun refLugares() = db.child("usuarios").child("pasajeros").child(uid).child("lugares")

    private val frecuentes = mutableListOf<LugarGuardado>()
    private lateinit var adapter: FrecuentesAdapter

    private lateinit var tvDirCasa: TextView
    private lateinit var tvDirTrabajo: TextView
    private lateinit var tvSinFrecuentes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lugares)

        tvDirCasa = findViewById(R.id.tv_dir_casa)
        tvDirTrabajo = findViewById(R.id.tv_dir_trabajo)
        tvSinFrecuentes = findViewById(R.id.tv_sin_frecuentes)

        findViewById<ImageButton>(R.id.btn_back_lugares).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.card_casa).setOnClickListener {
            editarFijo("CASA", "Casa")
        }
        findViewById<LinearLayout>(R.id.card_trabajo).setOnClickListener {
            editarFijo("TRABAJO", "Trabajo / Estudios")
        }
        findViewById<View>(R.id.btn_agregar_frecuente).setOnClickListener {
            agregarFrecuente()
        }

        val recycler = findViewById<RecyclerView>(R.id.recycler_frecuentes)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = FrecuentesAdapter(
            frecuentes,
            onClick = { lugar -> devolverLugar(lugar) },
            onDelete = { lugar -> eliminar(lugar) }
        )
        recycler.adapter = adapter

        cargar()
    }

    private fun cargar() {
        if (uid.isEmpty()) return
        refLugares().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                frecuentes.clear()
                var casa: LugarGuardado? = null
                var trabajo: LugarGuardado? = null
                for (child in snapshot.children) {
                    val l = child.getValue(LugarGuardado::class.java) ?: continue
                    when (l.tipo) {
                        "CASA" -> casa = l
                        "TRABAJO" -> trabajo = l
                        else -> frecuentes.add(l)
                    }
                }
                tvDirCasa.text = casa?.direccion?.ifEmpty { "Toca para agregar tu casa" }
                    ?: "Toca para agregar tu casa"
                tvDirTrabajo.text = trabajo?.direccion?.ifEmpty { "Toca para agregar" }
                    ?: "Toca para agregar"
                adapter.notifyDataSetChanged()
                tvSinFrecuentes.visibility = if (frecuentes.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /** Casa y Trabajo usan id fijo = tipo, para que siempre haya uno solo. */
    private fun editarFijo(tipo: String, titulo: String) {
        if (uid.isEmpty()) {
            Toast.makeText(this, "Inicia sesión para guardar lugares", Toast.LENGTH_SHORT).show()
            return
        }
        dialogoLugar(titulo, etiquetaInicial = titulo) { etiqueta, direccion ->
            val (lat, lng) = geocodificar(direccion)
            val lugar = LugarGuardado(
                id = tipo, tipo = tipo, etiqueta = etiqueta, direccion = direccion,
                lat = lat, lng = lng
            )
            refLugares().child(tipo).setValue(lugar)
            Toast.makeText(this, "$titulo guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun agregarFrecuente() {
        if (uid.isEmpty()) {
            Toast.makeText(this, "Inicia sesión para guardar lugares", Toast.LENGTH_SHORT).show()
            return
        }
        dialogoLugar("Nuevo lugar frecuente", etiquetaInicial = "") { etiqueta, direccion ->
            val (lat, lng) = geocodificar(direccion)
            val ref = refLugares().push()
            val lugar = LugarGuardado(
                id = ref.key ?: "", tipo = "FRECUENTE",
                etiqueta = etiqueta.ifEmpty { "Lugar" }, direccion = direccion,
                lat = lat, lng = lng
            )
            ref.setValue(lugar)
            Toast.makeText(this, "Lugar agregado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminar(lugar: LugarGuardado) {
        if (lugar.id.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Quitar \"${lugar.etiqueta}\" de tus lugares?")
            .setPositiveButton("Eliminar") { _, _ -> refLugares().child(lugar.id).removeValue() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun devolverLugar(lugar: LugarGuardado) {
        val data = android.content.Intent().apply {
            putExtra("LUGAR_DIRECCION", lugar.direccion)
            putExtra("LUGAR_LAT", lugar.lat)
            putExtra("LUGAR_LNG", lugar.lng)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    /** Diálogo simple con etiqueta + dirección (texto). */
    private fun geocodificar(direccion: String): Pair<Double, Double> {
        return try {
            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
            val res = geocoder.getFromLocationName(direccion, 1)
            val loc = res?.firstOrNull()
            if (loc != null) Pair(loc.latitude, loc.longitude) else Pair(0.0, 0.0)
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }

    private fun dialogoLugar(
        titulo: String,
        etiquetaInicial: String,
        onOk: (etiqueta: String, direccion: String) -> Unit
    ) {
        val cont = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etEtiqueta = EditText(this).apply {
            hint = "Nombre (ej: Casa de mamá)"
            setText(etiquetaInicial)
        }
        val etDireccion = EditText(this).apply { hint = "Dirección o referencia" }
        cont.addView(etEtiqueta)
        cont.addView(etDireccion)

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(cont)
            .setPositiveButton("Guardar") { _, _ ->
                val dir = etDireccion.text.toString().trim()
                if (dir.isEmpty()) {
                    Toast.makeText(this, "Escribe una dirección", Toast.LENGTH_SHORT).show()
                } else {
                    onOk(etEtiqueta.text.toString().trim(), dir)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Adapter interno de lugares frecuentes
    private class FrecuentesAdapter(
        val items: List<LugarGuardado>,
        val onClick: (LugarGuardado) -> Unit,
        val onDelete: (LugarGuardado) -> Unit
    ) : RecyclerView.Adapter<FrecuentesAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val etiqueta: TextView = v.findViewById(R.id.tv_lf_etiqueta)
            val direccion: TextView = v.findViewById(R.id.tv_lf_direccion)
            val borrar: ImageButton = v.findViewById(R.id.btn_lf_borrar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lugar_frecuente, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val l = items[position]
            holder.etiqueta.text = l.etiqueta
            holder.direccion.text = l.direccion
            holder.itemView.setOnClickListener { onClick(l) }
            holder.borrar.setOnClickListener { onDelete(l) }
        }

        override fun getItemCount() = items.size
    }
}
