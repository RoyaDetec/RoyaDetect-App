// ui/utils/PdfGenerator.kt
package com.example.royadetect.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.royadetect.ui.screens.AnalysisResult
import com.example.royadetect.ui.screens.getSeverityDescription
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfManager {
    private const val REPORTS_FOLDER = "RoyaReportsApp"
    private const val REPORTS_SUBFOLDER = "reports"

    /**
     * Crea y retorna el directorio específico para los reportes
     */
    fun getReportsDirectory(context: Context): File {
        // Usar el almacenamiento interno de la aplicación
        val appStorageDir = context.filesDir
        val reportsDir = File(appStorageDir, "$REPORTS_FOLDER/$REPORTS_SUBFOLDER")

        // Crear el directorio si no existe
        if (!reportsDir.exists()) {
            val created = reportsDir.mkdirs()
            Log.d("RoyaDetect", "Directorio de reportes creado: $created - ${reportsDir.absolutePath}")
        }

        return reportsDir
    }

    /**
     * Genera un nombre único para el archivo PDF con el prefijo royadetect_reporte_pdf
     */
    fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "royadetect_reporte_pdf_${timeStamp}.pdf"
    }

    /**
     * Obtiene todos los archivos PDF en el directorio de reportes
     */
    fun getAllReportFiles(context: Context): List<File> {
        val reportsDir = getReportsDirectory(context)
        return reportsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".pdf")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Elimina un archivo de reporte específico
     */
    fun deleteReportFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("RoyaDetect", "Archivo eliminado: $deleted - $filePath")
                deleted
            } else {
                Log.w("RoyaDetect", "Archivo no encontrado para eliminar: $filePath")
                false
            }
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al eliminar archivo: $filePath", e)
            false
        }
    }

    /**
     * Limpia archivos antiguos (mantiene solo los últimos N archivos)
     */
    fun cleanOldReports(context: Context, maxFiles: Int = 50) {
        try {
            val allFiles = getAllReportFiles(context)
            if (allFiles.size > maxFiles) {
                val filesToDelete = allFiles.drop(maxFiles)
                filesToDelete.forEach { file ->
                    val deleted = file.delete()
                    Log.d("RoyaDetect", "Archivo antiguo eliminado: $deleted - ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al limpiar archivos antiguos", e)
        }
    }

    /**
     * Obtiene el tamaño total del directorio de reportes en MB
     */
    fun getReportsDirectorySize(context: Context): Double {
        val reportsDir = getReportsDirectory(context)
        var size = 0L

        reportsDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }

        return size / (1024.0 * 1024.0) // Convertir a MB
    }
}

fun generatePdfReport(context: Context, bitmap: Bitmap?, result: AnalysisResult): File {
    Log.d("RoyaDetect", "Iniciando generatePdfReport con bitmap: ${bitmap != null}")

    // Obtener el directorio específico para reportes
    val reportsDir = PdfManager.getReportsDirectory(context)
    val fileName = PdfManager.generateFileName()
    val outputFile = File(reportsDir, fileName)

    Log.d("RoyaDetect", "Generando PDF en: ${outputFile.absolutePath}")

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
    val page = document.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint()
    val titlePaint = Paint().apply {
        textSize = 24f
        isFakeBoldText = true
        color = Color.BLACK
    }
    val subtitlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = Color.BLACK
    }
    val textPaint = Paint().apply {
        textSize = 16f
        color = Color.BLACK
    }
    val smallTextPaint = Paint().apply {
        textSize = 14f
        color = Color.GRAY
    }

    var yPosition = 50f

    // Header con información de la app
    canvas.drawText("RoyaDetect - Reporte de Análisis", 50f, yPosition, titlePaint)
    yPosition += 30f

    val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("Fecha: $currentDate", 50f, yPosition, smallTextPaint)
    yPosition += 30f

    // Imagen
    if (bitmap != null) {
        Log.d("RoyaDetect", "Escalando bitmap para PDF: ${bitmap.width}x${bitmap.height}")
        try {
            // Calcular dimensiones manteniendo aspect ratio
            val maxWidth = 495f
            val maxHeight = 300f
            val originalWidth = bitmap.width.toFloat()
            val originalHeight = bitmap.height.toFloat()

            // Calcular factor de escala manteniendo proporción
            val scaleWidth = maxWidth / originalWidth
            val scaleHeight = maxHeight / originalHeight
            val scaleFactor = kotlin.math.min(scaleWidth, scaleHeight)

            val newWidth = (originalWidth * scaleFactor).toInt()
            val newHeight = (originalHeight * scaleFactor).toInt()

            Log.d("RoyaDetect", "Nuevas dimensiones: ${newWidth}x${newHeight}")

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Centrar la imagen en el espacio disponible
            val offsetX = 50f + (maxWidth - newWidth) / 2f
            val offsetY = yPosition + (maxHeight - newHeight) / 2f

            canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)
            yPosition += maxHeight + 20f // Espacio fijo para layout consistente

        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error al escalar bitmap: ${e.message}", e)
            canvas.drawText("Error al cargar la imagen", 50f, yPosition, textPaint)
            yPosition += 30f
        }
    } else {
        Log.w("RoyaDetect", "Bitmap es nulo")
        canvas.drawText("Imagen no disponible", 50f, yPosition, textPaint)
        yPosition += 30f
    }

    // Resultados del análisis
    canvas.drawText("Resultados del Análisis", 50f, yPosition, subtitlePaint)
    yPosition += 30f

    // Información del diagnóstico
    val severityText = when (result.severityLevel) {
        0 -> "Sin Roya Detectada"
        else -> "Roya Detectada - Nivel ${result.severityLevel}"
    }
    canvas.drawText("Diagnóstico: $severityText", 50f, yPosition, textPaint)
    yPosition += 20f

    canvas.drawText("Descripción: ${getSeverityDescription(result.severityLevel)}", 50f, yPosition, textPaint)
    yPosition += 20f

    canvas.drawText("Confianza: ${String.format(Locale.getDefault(), "%.1f", result.confidence * 100)}%", 50f, yPosition, textPaint)
    yPosition += 30f

    // Recomendaciones
    canvas.drawText("Recomendaciones", 50f, yPosition, subtitlePaint)
    yPosition += 25f

    val recommendation = when (result.severityLevel) {
        0 -> "Mantenga un programa de monitoreo quincenal. No se requiere intervención química por el momento. Continue con las buenas prácticas agrícolas."
        1 -> "Inicie aplicaciones preventivas de fungicidas sistémicos (ej. triazoles) de baja toxicidad. Fortalezca la nutrición del cultivo, especialmente con potasio y magnesio."
        2 -> "Implemente un programa de manejo integrado: combine aplicaciones de fungicidas sistémicos y de contacto (ej. mancozeb), elimine hojas muy afectadas y ajuste la sombra del cafetal. Intensifique el monitoreo semanal."
        3 -> "Aplique tratamientos secuenciales con fungicidas sistémicos de acción prolongada y mezcle con productos de contacto. Considere la resiembra con variedades resistentes. Podas sanitarias recomendadas."
        4 -> "Ejecute un plan de emergencia: tratamientos fungicidas intensivos (rotación de principios activos para evitar resistencia), eliminación de plantas severamente afectadas, control de sombra, renovación del lote si hay alta defoliación. Consultar con un fitopatólogo o extensionista agrícola."
        else -> "Consulte a un especialista para un diagnóstico preciso y un plan de manejo adaptado a las condiciones de su finca."
    }

    // Dividir texto largo en múltiples líneas
    val words = recommendation.split(" ")
    var currentLine = ""
    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val textWidth = textPaint.measureText(testLine)

        if (textWidth > 495) { // Ancho máximo
            canvas.drawText(currentLine, 50f, yPosition, textPaint)
            yPosition += 20f
            currentLine = word
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) {
        canvas.drawText(currentLine, 50f, yPosition, textPaint)
        yPosition += 30f
    }

    // Footer
    yPosition = 820f // Cerca del final de la página
    canvas.drawText("Generado por RoyaDetect App", 50f, yPosition, smallTextPaint)
    canvas.drawText("Archivo: ${fileName}", 300f, yPosition, smallTextPaint)

    document.finishPage(page)

    try {
        FileOutputStream(outputFile).use { outputStream ->
            document.writeTo(outputStream)
        }
        Log.d("RoyaDetect", "PDF generado exitosamente en: ${outputFile.absolutePath}")

        // Limpiar archivos antiguos si es necesario
        PdfManager.cleanOldReports(context)

        // Log del estado del directorio
        val directorySize = PdfManager.getReportsDirectorySize(context)
        val fileCount = PdfManager.getAllReportFiles(context).size
        Log.d("RoyaDetect", "Directorio de reportes: $fileCount archivos, ${String.format("%.2f", directorySize)} MB")

    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error al guardar PDF: ${e.message}", e)
        throw e
    } finally {
        document.close()
    }

    return outputFile
}