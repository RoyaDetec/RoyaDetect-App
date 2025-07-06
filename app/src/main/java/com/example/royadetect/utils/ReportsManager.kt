// utils/ReportsManager.kt
package com.example.royadetect.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.royadetect.data.entity.Report
import com.example.royadetect.data.repository.ReportRepository
import kotlinx.coroutines.flow.first
import java.io.File

class ReportsManager(private val context: Context, private val repository: ReportRepository) {

    /**
     * Sincroniza la base de datos con los archivos físicos
     * Elimina registros de la BD si el archivo no existe
     */
    suspend fun syncDatabaseWithFiles() {
        try {
            val allReports = repository.getAllReports().first()
            var deletedCount = 0

            allReports.forEach { report ->
                val file = File(report.pdfPath)
                if (!file.exists()) {
                    repository.deleteReport(report)
                    deletedCount++
                    Log.d("RoyaDetect", "Eliminado registro de BD para archivo inexistente: ${report.pdfPath}")
                }
            }

            Log.i("RoyaDetect", "Sincronización completada. Eliminados $deletedCount registros")
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error en sincronización de BD", e)
        }
    }

    /**
     * Limpia archivos huérfanos (archivos que no tienen registro en la BD)
     */
    suspend fun cleanOrphanFiles() {
        try {
            val allReports = repository.getAllReports().first()
            val reportPaths = allReports.map { it.pdfPath }.toSet()

            val allFiles = PdfManager.getAllReportFiles(context)
            var deletedCount = 0

            allFiles.forEach { file ->
                if (file.absolutePath !in reportPaths) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d("RoyaDetect", "Eliminado archivo huérfano: ${file.absolutePath}")
                    }
                }
            }

            Log.i("RoyaDetect", "Limpieza completada. Eliminados $deletedCount archivos huérfanos")
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error en limpieza de archivos huérfanos", e)
        }
    }

    /**
     * Abre un reporte específico
     */
    fun openReport(report: Report): Boolean {
        return try {
            val file = File(report.pdfPath)
            if (!file.exists()) {
                Log.e("RoyaDetect", "Archivo no encontrado: ${report.pdfPath}")
                return false
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al abrir reporte", e)
            false
        }
    }

    /**
     * Comparte un reporte específico
     */
    fun shareReport(report: Report): Boolean {
        return try {
            val file = File(report.pdfPath)
            if (!file.exists()) {
                Log.e("RoyaDetect", "Archivo no encontrado: ${report.pdfPath}")
                return false
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Análisis de Roya")
                putExtra(Intent.EXTRA_TEXT, "Reporte generado el ${report.date} - Nivel ${report.severityLevel}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
            true
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al compartir reporte", e)
            false
        }
    }

    /**
     * Elimina un reporte (archivo y registro de BD)
     */
    suspend fun deleteReport(report: Report): Boolean {
        return try {
            // Eliminar archivo físico
            val fileDeleted = PdfManager.deleteReportFile(report.pdfPath)

            // Eliminar registro de BD
            repository.deleteReport(report)

            Log.d("RoyaDetect", "Reporte eliminado - Archivo: $fileDeleted, BD: true")
            true
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al eliminar reporte", e)
            false
        }
    }

    /**
     * Obtiene estadísticas del directorio de reportes
     */
    fun getReportsStats(): ReportsStats {
        val allFiles = PdfManager.getAllReportFiles(context)
        val totalSize = PdfManager.getReportsDirectorySize(context)
        val reportsDir = PdfManager.getReportsDirectory(context)

        return ReportsStats(
            totalFiles = allFiles.size,
            totalSizeMB = totalSize,
            directoryPath = reportsDir.absolutePath,
            oldestFile = allFiles.minByOrNull { it.lastModified() },
            newestFile = allFiles.maxByOrNull { it.lastModified() }
        )
    }
}

data class ReportsStats(
    val totalFiles: Int,
    val totalSizeMB: Double,
    val directoryPath: String,
    val oldestFile: File?,
    val newestFile: File?
)