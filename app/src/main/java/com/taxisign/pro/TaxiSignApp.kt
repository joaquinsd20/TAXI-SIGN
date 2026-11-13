package com.taxisign.pro

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Application de TaxiSign.
 *
 * Con targetSdk 35+ Android dibuja todas las pantallas "edge-to-edge" (detras
 * de la barra de estado y de la barra de navegacion). Como las pantallas de la
 * app no manejan esos insets, aqui se aplica un relleno comun a cada Activity
 * para que ningun encabezado quede bajo la barra de estado, ningun boton bajo
 * la barra de gestos y el contenido suba cuando aparece el teclado.
 */
class TaxiSignApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Si Android cerro el proceso, recupera el nombre/rol del usuario con sesion abierta
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let {
            SessionManager.restaurar(this, it.uid)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = aplicarInsets(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun aplicarInsets(activity: Activity) {
        val contenido = activity.findViewById<View>(android.R.id.content) ?: return
        if (contenido.getTag(R.id.tag_insets_aplicados) == true) return
        contenido.setTag(R.id.tag_insets_aplicados, true)

        ViewCompat.setOnApplyWindowInsetsListener(contenido) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                maxOf(barras.bottom, teclado.bottom)
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(contenido)
    }
}
