package com.taxisign.pro.notificaciones

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.taxisign.pro.R
import com.taxisign.pro.notificaciones.Notificacion
import com.taxisign.pro.notificaciones.NotificacionAdapter
import com.taxisign.pro.notificaciones.NotificacionHelper

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var tabAll: LinearLayout
    private lateinit var tabUnread: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnMarkAllRead: ImageButton
    private lateinit var emptyState: LinearLayout

    private val todas = mutableListOf<Notificacion>()
    private val mostradas = mutableListOf<Notificacion>()
    private lateinit var adapter: NotificacionAdapter
    private var soloNoLeidas = false

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        inicializarVistas()
        configurarEventos()
        cargarNotificaciones()
    }

    private fun inicializarVistas() {
        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        tabAll = findViewById(R.id.tabAll)
        tabUnread = findViewById(R.id.tabUnread)
        btnBack = findViewById(R.id.btnBackNotifications)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        emptyState = findViewById(R.id.emptyState)

        recyclerNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificacionAdapter(mostradas)
        recyclerNotifications.adapter = adapter
    }

    private fun configurarEventos() {
        btnBack.setOnClickListener { finish() }

        btnMarkAllRead.setOnClickListener {
            NotificacionHelper.marcarTodasLeidas(uid)
            Toast.makeText(this, "Todas marcadas como leídas", Toast.LENGTH_SHORT).show()
        }

        tabAll.setOnClickListener { soloNoLeidas = false; aplicarFiltro() }
        tabUnread.setOnClickListener { soloNoLeidas = true; aplicarFiltro() }
    }

    private fun cargarNotificaciones() {
        if (uid.isEmpty()) {
            mostrarVacio(true)
            return
        }
        db.child("notificaciones").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    todas.clear()
                    for (child in snapshot.children) {
                        child.getValue(Notificacion::class.java)?.let { todas.add(it) }
                    }
                    // más recientes primero
                    todas.sortByDescending { it.timestamp }
                    aplicarFiltro()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@NotificationsActivity,
                        "Error al cargar avisos", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun aplicarFiltro() {
        mostradas.clear()
        mostradas.addAll(if (soloNoLeidas) todas.filter { !it.leida } else todas)
        adapter.notifyDataSetChanged()
        actualizarPestanas()
        mostrarVacio(mostradas.isEmpty())
    }

    /** Resalta la pestaña activa y muestra cuantos avisos faltan leer. */
    private fun actualizarPestanas() {
        val amarillo = R.drawable.bg_tab_activa
        tabAll.setBackgroundResource(if (soloNoLeidas) 0 else amarillo)
        tabUnread.setBackgroundResource(if (soloNoLeidas) amarillo else 0)
        findViewById<TextView>(R.id.tvTabAll).setTextColor(
            android.graphics.Color.parseColor(if (soloNoLeidas) "#9AA3B8" else "#111827")
        )
        findViewById<TextView>(R.id.tvTabUnread).setTextColor(
            android.graphics.Color.parseColor(if (soloNoLeidas) "#111827" else "#9AA3B8")
        )
        val sinLeer = todas.count { !it.leida }
        findViewById<TextView>(R.id.badgeUnreadCount).apply {
            text = if (sinLeer > 99) "99+" else sinLeer.toString()
            visibility = if (sinLeer > 0) View.VISIBLE else View.GONE
        }
        findViewById<TextView>(R.id.tvVacioTitulo).text =
            if (soloNoLeidas) "No tienes avisos sin leer" else "Todo al día"
    }

    private fun mostrarVacio(vacio: Boolean) {
        emptyState.visibility = if (vacio) View.VISIBLE else View.GONE
        recyclerNotifications.visibility = if (vacio) View.GONE else View.VISIBLE
    }
}