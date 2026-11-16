package com.ifpr.androidapptemplatear.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fotos ficam em filesDir/vehicle_photos: armazenamento interno do app,
 * que sobrevive ao fechamento do app e à reinicialização do aparelho.
 */
object PhotoStorage {

    private fun dir(context: Context): File =
        File(context.filesDir, "vehicle_photos").apply { mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(dir(context), "veh_${System.currentTimeMillis()}.jpg")

    fun uriForCamera(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copia uma imagem escolhida na galeria para o armazenamento interno. */
    suspend fun importFromUri(context: Context, source: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                val target = newPhotoFile(context)
                context.contentResolver.openInputStream(source)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                target
            } catch (e: Exception) {
                null
            }
        }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
