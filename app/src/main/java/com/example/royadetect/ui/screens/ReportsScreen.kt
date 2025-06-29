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

    // Observar reportes
    val reports by repository.getAllReports().collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf<Report?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

        Spacer(modifier = Modifier.height(16.dp))

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

                // Lista de reportes
                if (reports.isEmpty()) {
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
                        items(reports) { report ->
                            ReportRow(
                                report = report,
                                onViewPdf = { pdfPath ->
                                    scope.launch {
                                        try {
                                            val pdfFile = File(pdfPath)
                                            if (pdfFile.exists()) {
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
                                                } catch (e: Exception) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Abrir PDF con..."))
                                                }
                                            } else {
                                                errorMessage = "El archivo PDF no existe"
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
                                // Eliminar archivo PDF
                                val pdfFile = File(report.pdfPath)
                                if (pdfFile.exists()) {
                                    pdfFile.delete()
                                }
                                // Eliminar de la base de datos
                                repository.deleteReport(report)
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                errorMessage = "Error al eliminar reporte: ${e.message}"
                                Log.e("RoyaDetect", "Error eliminando reporte", e)
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
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        ) {
            Text(error)
        }
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