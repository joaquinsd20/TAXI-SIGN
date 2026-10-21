package com.taxisign.pro

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * SeñasBottomSheet
 *
 * Muestra un texto convertido a lengua de señas para el pasajero.
 * - Agrupa por PALABRAS (cada palabra en su propia fila).
 * - Usa TraductorSenas: seña de palabra completa si existe, o dactilología.
 * - Botón "▶ Reproducir": resalta letra por letra con vibración, para que
 *   sea más fácil de seguir para una persona sorda.
 */
class SeñasBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(mensaje: String): SeñasBottomSheet {
            return SeñasBottomSheet().apply {
                arguments = Bundle().apply { putString("mensaje", mensaje) }
            }
        }
    }

    private val vibrator by lazy {
        requireContext().getSystemService(Vibrator::class.java)
    }

    // Guardamos las vistas de letra en orden para la reproducción secuencial
    private val itemsLetra = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 32, 40, 48)
            setBackgroundColor(0xFF1A1A2E.toInt())
        }

        val mensaje = arguments?.getString("mensaje") ?: ""

        // Título
        root.addView(TextView(requireContext()).apply {
            text = "🤟 En lengua de señas"
            textSize = 18f
            setTextColor(0xFFFFD600.toInt())
            setPadding(0, 0, 0, 8)
        })

        // El texto original (para quien sí lee algunas palabras)
        root.addView(TextView(requireContext()).apply {
            text = "“$mensaje”"
            textSize = 14f
            setTextColor(0xFFAAB1CB.toInt())
            setPadding(0, 0, 0, 20)
        })

        val tokens = TraductorSenas.traducir(requireContext(), mensaje)

        if (tokens.isEmpty()) {
            root.addView(TextView(requireContext()).apply {
                text = "No hay nada que mostrar."
                setTextColor(0xFFFFFFFF.toInt())
            })
            return root
        }

        // Contenedor scrollable vertical de palabras
        val contenedorPalabras = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        tokens.forEach { token ->
            contenedorPalabras.addView(crearFilaPalabra(token))
        }

        val scrollV = androidx.core.widget.NestedScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        scrollV.addView(contenedorPalabras)
        root.addView(scrollV)

        // Botón reproducir
        root.addView(TextView(requireContext()).apply {
            text = "▶  Reproducir seña por seña"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFF1A1A2E.toInt())
            setBackgroundColor(0xFFFFD600.toInt())
            setPadding(24, 24, 24, 24)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 20
            layoutParams = lp
            setOnClickListener { reproducirSecuencia() }
        })

        return root
    }

    /** Crea una fila: etiqueta de la palabra + sus señas (palabra o letras). */
    private fun crearFilaPalabra(token: TraductorSenas.TokenSena): View {
        val fila = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }

        fila.addView(TextView(requireContext()).apply {
            text = token.palabra.uppercase()
            textSize = 13f
            setTextColor(0xFFFFD600.toInt())
            setPadding(0, 0, 0, 4)
        })

        val letrasContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        if (token.esPalabraCompleta) {
            // Seña de palabra completa (una sola imagen)
            val item = construirItem(token.palabra, token.drawablePalabra)
            itemsLetra.add(item)
            letrasContainer.addView(item)
        } else {
            token.letras.forEach { letra ->
                val item = construirItem(
                    letra.caracter.toString(),
                    letra.drawableResId
                )
                itemsLetra.add(item)
                letrasContainer.addView(item)
            }
        }

        val scroll = HorizontalScrollView(requireContext())
        scroll.addView(letrasContainer)
        fila.addView(scroll)
        return fila
    }

    private fun construirItem(etiqueta: String, resId: Int): View {
        val item = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
        }
        val img = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(110, 130)
            scaleType = ImageView.ScaleType.FIT_CENTER
            if (resId != 0) setImageResource(resId)
            else setImageResource(android.R.drawable.ic_menu_help)
        }
        val texto = TextView(requireContext()).apply {
            text = etiqueta
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        }
        item.addView(img)
        item.addView(texto)
        return item
    }

    /** Resalta cada seña en orden, con una pequeña vibración por seña. */
    private fun reproducirSecuencia() {
        if (itemsLetra.isEmpty()) return
        itemsLetra.forEachIndexed { index, item ->
            item.postDelayed({
                // apaga el anterior
                if (index > 0) itemsLetra[index - 1].setBackgroundColor(0x00000000)
                item.setBackgroundColor(0x33FFD600)
                vibrar()
                // al terminar, limpia el último
                if (index == itemsLetra.lastIndex) {
                    item.postDelayed({ item.setBackgroundColor(0x00000000) }, 700)
                }
            }, index * 700L)
        }
    }

    private fun vibrar() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(60)
        }
    }
}
