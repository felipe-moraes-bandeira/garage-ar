package com.ifpr.androidapptemplatear.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

const val DEFAULT_MODEL = "esportivo.glb"

/** pikachu.glb -> Pikachu | cadeira_escolar.glb -> Cadeira escolar */
fun friendlyModelName(file: String): String =
    file.substringBeforeLast('.')
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/** Modelos importados pelo usuário ficam na raiz de filesDir. */
fun importedModels(context: Context): List<File> =
    context.filesDir.listFiles { f -> f.isFile && f.name.endsWith(".glb", true) }
        ?.sortedBy { it.name } ?: emptyList()

/**
 * Retorna o caminho que o ArModelNode deve carregar: nome do asset,
 * ou URI do arquivo se o modelo foi importado para filesDir.
 */
fun resolveModelLocation(context: Context, fileName: String): String {
    try {
        context.assets.open(fileName).close()
        return fileName
    } catch (e: IOException) {
        val internal = File(context.filesDir, fileName)
        return if (internal.exists()) Uri.fromFile(internal).toString() else fileName
    }
}
