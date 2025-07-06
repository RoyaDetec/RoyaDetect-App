// ui/screens/ReportsScreen.kt
package com.example.royadetect.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.royadetect.data.database.AppDatabase
import com.example.royadetect.data.entity.Report
import com.example.royadetect.data.repository.ReportRepository
import com.example.royadetect.ui.theme.RoyaGreen
import com.example.royadetect.ui.theme.RoyaLightGreen
import com.example.royadetect.utils.PdfManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Inicializar base de datos y repositorio
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ReportRepository(database.reportDao()) }

    // Observar reportes y filtrarlos
    val allReports by repository.getAllReports().collectAsState(initial = emptyList())

    // Filtrar solo reportes que estén en la carpeta correcta y tengan el prefijo correcto
    val filteredReports = remember(allReports) {
        allReports.filter { report ->
            isValidAppReport(report, context)
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Report?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDirectoryInfo by remember { mutableStateOf(false) }

    // Información del directorio (solo archivos válidos de la app)
    var directoryInfo by remember { mutableStateOf<String?>(null) }

    // Obtener información del directorio al cargar
    LaunchedEffect(Unit) {
        try {
            val validFiles = getValidAppReportFiles(context)
            val totalFiles = validFiles.size
            val dirSize = calculateDirectorySize(validFiles)
            val reportsDir = PdfManager.getReportsDirectory(context)
            directoryInfo = "Directorio: ${reportsDir.absolutePath}\nArchivos válidos: $totalFiles\nTamaño: ${String.format("%.2f", dirSize)} MB"
        } catch (e: Exception) {
            Log.e("RoyaDetect", "Error obteniendo info del directorio", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "Reportes Realizados",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Información del directorio (opcional)
        if (showDirectoryInfo) {
            directoryInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = info,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Botón para mostrar/ocultar info del directorio
        TextButton(
            onClick = { showDirectoryInfo = !showDirectoryInfo },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (showDirectoryInfo) "Ocultar Info" else "Mostrar Info Directorio",
                fontSize = 12.sp,
                color = RoyaGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabla de reportes
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Encabezados de la tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            RoyaGreen,
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fecha",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = "Severidad",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ver",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Eliminar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

                // Lista de reportes filtrados
                if (filteredReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay reportes disponibles",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn {
                        items(filteredReports) { report ->
                            ReportRow(
                                report = report,
                                onViewPdf = { pdfPath ->
                                    scope.launch {
                                        try {
                                            val pdfFile = File(pdfPath)
                                            Log.d("RoyaDetect", "Intentando abrir PDF: ${pdfFile.absolutePath}")
                                            Log.d("RoyaDetect", "Archivo existe: ${pdfFile.exists()}")

                                            // Validar que el archivo sea válido antes de abrirlo
                                            if (pdfFile.exists() && isValidAppReportFile(pdfFile, context)) {
                                                val pdfUri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    pdfFile
                                                )

                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(pdfUri, "application/pdf")
                                                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }

                                                try {
                                                    context.startActivity(intent)
                                                    Log.d("RoyaDetect", "PDF abierto exitosamente")
                                                } catch (e: Exception) {
                                                    Log.w("RoyaDetect", "No se pudo abrir PDF directamente, intentando compartir", e)
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                        putExtra(Intent.EXTRA_TEXT, "Reporte de análisis de roya")
                                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Abrir PDF con..."))
                                                }
                                            } else {
                                                errorMessage = "El archivo PDF no es válido o no pertenece a la aplicación"
                                                Log.e("RoyaDetect", "Archivo PDF no válido o no encontrado: ${pdfFile.absolutePath}")
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Error al abrir PDF: ${e.message}"
                                            Log.e("RoyaDetect", "Error abriendo PDF", e)
                                        }
                                    }
                                },
                                onDelete = { showDeleteDialog = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para generar nuevo reporte
                Button(
                    onClick = {
                        // Este botón podría navegar de vuelta al HomeScreen
                        // o implementar otra funcionalidad según necesites
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyaLightGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Generar Reporte nuevo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    showDeleteDialog?.let { report ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar Reporte") },
            text = { Text("¿Estás seguro de que deseas eliminar este reporte del ${report.date}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Validar que el archivo sea válido antes de eliminarlo
                                if (isValidAppReport(report, context)) {
                                    // Usar PdfManager para eliminar el archivo
                                    val fileDeleted = PdfManager.deleteReportFile(report.pdfPath)
                                    Log.d("RoyaDetect", "Archivo eliminado: $fileDeleted")

                                    // Eliminar de la base de datos
                                    repository.deleteReport(report)
                                    Log.d("RoyaDetect", "Reporte eliminado de la base de datos")

                                    showDeleteDialog = null

                                    // Actualizar información del directorio
                                    val validFiles = getValidAppReportFiles(context)
                                    val totalFiles = validFiles.size
                                    val dirSize = calculateDirectorySize(validFiles)
                                    val reportsDir = PdfManager.getReportsDirectory(context)
                                    directoryInfo = "Directorio: ${reportsDir.absolutePath}\nArchivos válidos: $totalFiles\nTamaño: ${String.format("%.2f", dirSize)} MB"
                                } else {
                                    errorMessage = "No se puede eliminar: el archivo no es válido"
                                    showDeleteDialog = null
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error al eliminar reporte: ${e.message}"
                                Log.e("RoyaDetect", "Error eliminando reporte", e)
                                showDeleteDialog = null
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mostrar mensaje de error
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { errorMessage = null }
                ) {
                    Text("OK", color = Color.White)
                }
            }
        }
    }
}

// Función para validar si un reporte es válido de la aplicación
private fun isValidAppReport(report: Report, context: android.content.Context): Boolean {
    val pdfFile = File(report.pdfPath)
    return isValidAppReportFile(pdfFile, context)
}

// Función para validar si un archivo PDF es válido de la aplicación
private fun isValidAppReportFile(file: File, context: android.content.Context): Boolean {
    return try {
        val reportsDir = PdfManager.getReportsDirectory(context)
        val fileName = file.name

        // Verificar que el archivo esté en la carpeta correcta
        val isInCorrectDirectory = file.parentFile?.absolutePath == reportsDir.absolutePath

        // Verificar que tenga el prefijo correcto
        val hasCorrectPrefix = fileName.startsWith("royadetect_reporte_pdf_")

        // Verificar que tenga extensión PDF
        val hasPdfExtension = fileName.endsWith(".pdf", ignoreCase = true)

        // Verificar que el archivo exista
        val fileExists = file.exists()

        Log.d("RoyaDetect", "Validando archivo: $fileName")
        Log.d("RoyaDetect", "- En directorio correcto: $isInCorrectDirectory")
        Log.d("RoyaDetect", "- Tiene prefijo correcto: $hasCorrectPrefix")
        Log.d("RoyaDetect", "- Tiene extensión PDF: $hasPdfExtension")
        Log.d("RoyaDetect", "- Archivo existe: $fileExists")

        isInCorrectDirectory && hasCorrectPrefix && hasPdfExtension && fileExists
    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error validando archivo: ${file.absolutePath}", e)
        false
    }
}

// Función para obtener solo archivos válidos de la aplicación
private fun getValidAppReportFiles(context: android.content.Context): List<File> {
    return try {
        val reportsDir = PdfManager.getReportsDirectory(context)
        val allFiles = reportsDir.listFiles() ?: emptyArray()

        allFiles.filter { file ->
            file.isFile &&
                    file.name.startsWith("royadetect_reporte_pdf_") &&
                    file.name.endsWith(".pdf", ignoreCase = true)
        }
    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error obteniendo archivos válidos", e)
        emptyList()
    }
}

// Función para calcular el tamaño de archivos específicos
private fun calculateDirectorySize(files: List<File>): Double {
    return try {
        val totalBytes = files.sumOf { it.length() }
        totalBytes / (1024.0 * 1024.0) // Convertir a MB
    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error calculando tamaño del directorio", e)
        0.0
    }
}

@Composable
fun ReportRow(
    report: Report,
    onViewPdf: (String) -> Unit,
    onDelete: (Report) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (report.severityLevel) {
                0 -> Color(0xFF4CAF50)
                1 -> Color(0xFF8BC34A)
                2 -> Color(0xFFFFEB3B)
                3 -> Color(0xFFFF9800)
                4 -> Color(0xFFF44336)
                else -> Color(0xFFE0E0E0)
            }
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fecha
            Text(
                text = report.date,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(2f)
            )

            // Severidad
            Text(
                text = "Nivel ${report.severityLevel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Center
            )

            // Botón Ver PDF
            IconButton(
                onClick = { onViewPdf(report.pdfPath) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Ver PDF",
                    tint = Color.White
                )
            }

            // Botón Eliminar
            IconButton(
                onClick = { onDelete(report) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White
                )
            }
        }
    }
}