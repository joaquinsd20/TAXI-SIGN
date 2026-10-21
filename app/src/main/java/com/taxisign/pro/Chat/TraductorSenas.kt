package com.taxisign.pro

import android.content.Context

/**
 * TraductorSenas
 *
 * Convierte texto (lo que escribe el CONDUCTOR) en una secuencia de señas
 * para el PASAJERO sordo/mudo.
 *
 * Estrategia (respeta los recursos que ya existen en el proyecto):
 *  1. Si existe una seña de PALABRA completa (drawable "sena_palabra_<palabra>"
 *     o "raw/sena_<palabra>") se usa esa imagen/recurso directamente.
 *  2. Si no existe, se hace DACTILOLOGÍA: se deletrea la palabra letra por
 *     letra usando los drawables que ya están en el proyecto
 *     (sena_a, sena_b, ... sena_z, sena_nn para la Ñ).
 *
 * No depende de internet ni de librerías nuevas.
 */
object TraductorSenas {

    /** Una letra individual con su recurso de seña (0 = sin imagen). */
    data class Letra(
        val caracter: Char,
        val drawableResId: Int
    )

    /**
     * Un "token" es una palabra completa.
     * - Si [drawablePalabra] != 0  -> la palabra tiene una seña propia (una sola imagen).
     * - Si no                      -> se deletrea con [letras] (dactilología).
     */
    data class TokenSena(
        val palabra: String,
        val letras: List<Letra>,
        val drawablePalabra: Int = 0
    ) {
        val esPalabraCompleta: Boolean get() = drawablePalabra != 0
    }

    /**
     * Traduce un texto libre a una lista de palabras-seña.
     * Limpia emojis y signos antes de procesar.
     */
    fun traducir(context: Context, texto: String): List<TokenSena> {
        val limpio = texto
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")   // fuera emojis/símbolos
            .trim()
            .replace(Regex("\\s+"), " ")

        if (limpio.isEmpty()) return emptyList()

        return limpio.split(" ").mapNotNull { palabra ->
            if (palabra.isBlank()) return@mapNotNull null

            // 1) ¿La palabra tiene una seña propia?
            val resPalabra = resolverDrawablePalabra(context, palabra)
            if (resPalabra != 0) {
                return@mapNotNull TokenSena(
                    palabra = palabra,
                    letras = emptyList(),
                    drawablePalabra = resPalabra
                )
            }

            // 2) Dactilología: letra por letra
            val letras = palabra.uppercase().map { c ->
                Letra(
                    caracter = c,
                    drawableResId = resolverDrawableLetra(context, c)
                )
            }
            TokenSena(palabra = palabra, letras = letras)
        }
    }

    /** Devuelve el resId del drawable de una letra (o 0 si no existe). */
    private fun resolverDrawableLetra(context: Context, letra: Char): Int {
        val nombre = when (letra.uppercaseChar()) {
            'Ñ' -> "sena_nn"
            else -> "sena_${letra.lowercaseChar()}"
        }
        return context.resources.getIdentifier(
            nombre, "drawable", context.packageName
        )
    }

    /**
     * Busca una seña de palabra completa. Permite ampliar el diccionario
     * en el futuro solo agregando drawables llamados "sena_palabra_hola", etc.
     * (Hoy no hay ninguno, así que normalmente devuelve 0 y se usa dactilología.)
     */
    private fun resolverDrawablePalabra(context: Context, palabra: String): Int {
        val clave = palabra.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "nn")
        return context.resources.getIdentifier(
            "sena_palabra_$clave", "drawable", context.packageName
        )
    }
}
