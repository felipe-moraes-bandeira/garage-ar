package com.ifpr.androidapptemplatear.ui

import android.content.Context
import java.io.File

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
