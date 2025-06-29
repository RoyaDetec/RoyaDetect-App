package com.example.royadetect.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.royadetect.ui.screens.AnalysisResult
import com.example.royadetect.ui.screens.getSeverityDescription
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun generatePdfReport(context: Context, bitmap: Bitmap?, result: AnalysisResult): File {
    Log.d("RoyaDetect", "Iniciando generatePdfReport con bitmap: ${bitmap != null}")
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
    val textPaint = Paint().apply {
        textSize = 16f
        color = Color.BLACK
    }

    var yPosition = 50f

    // Title
    canvas.drawText("Reporte de Análisis de Roya", 50f, yPosition, titlePaint)
    yPosition += 40f

    // Image
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

    // Analysis Results
    canvas.drawText("Resultados del Análisis", 50f, yPosition, titlePaint)
    yPosition += 30f
    canvas.drawText("Nivel de Severidad: ${result.severityLevel}", 50f, yPosition, textPaint)
    yPosition += 20f
    canvas.drawText("Descripción: ${getSeverityDescription(result.severityLevel)}", 50f, yPosition, textPaint)
    yPosition += 20f
    canvas.drawText("Confianza: ${String.format(Locale.getDefault(), "%.1f", result.confidence  * 100)}%", 50f, yPosition, textPaint)
    yPosition += 30f

    // Recommendation
    canvas.drawText("Recomendación", 50f, yPosition, titlePaint)
    yPosition += 20f

    val recommendation = when (result.severityLevel) {
        0 -> "Mantenga un programa de monitoreo quincenal. No se requiere intervención química por el momento."
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
    }

    document.finishPage(page)

    // Determinar dónde guardar el archivo
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "RoyaReport_${timeStamp}.pdf"

    val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ - Usar directorio de archivos de la app (no requiere permisos)
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    } else {
        // Android 9 y anteriores - Usar caché
        File(context.cacheDir, fileName)
    }

    Log.d("RoyaDetect", "Intentando guardar PDF en: ${file.absolutePath}")

    try {
        // Crear directorio si no existe
        file.parentFile?.mkdirs()

        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        Log.d("RoyaDetect", "PDF generado exitosamente en: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error al guardar PDF: ${e.message}", e)
        throw e
    } finally {
        document.close()
    }

    return file
}