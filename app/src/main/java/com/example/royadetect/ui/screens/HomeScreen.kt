package com.example.royadetect.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.royadetect.ui.theme.RoyaGreen
import com.example.royadetect.ui.theme.RoyaLightGreen
import com.example.royadetect.utils.RequestCameraPermission
import com.example.royadetect.utils.RequestStoragePermission
import com.example.royadetect.utils.hasCameraPermission
import com.example.royadetect.utils.hasStoragePermission
import com.example.royadetect.utils.generatePdfReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

import com.example.royadetect.data.database.AppDatabase
import com.example.royadetect.data.entity.Report
import com.example.royadetect.data.repository.ReportRepository


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionRequest by remember { mutableStateOf(false) }
    var showStoragePermissionRequest by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf<String?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

// Configurar base de datos
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ReportRepository(database.reportDao()) }
    // Verificar estado del permiso al inicio
    LaunchedEffect(Unit) {
        Log.d("RoyaDetect", "Verificando permiso de almacenamiento al inicio: ${hasStoragePermission(context)}")
    }

    // Crear URI para la foto de cámara
    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = File(context.cacheDir, "images")
        storageDir.mkdirs()

        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // Función común para procesar imagen
    fun processImage(uri: Uri) {
        scope.launch {
            isProcessing = true
            errorMessage = null
            analysisResult = null
            debugInfo = null
            try {
                Log.d("RoyaDetect", "Iniciando procesamiento de imagen desde URI: $uri")
                val inputStream = context.contentResolver.openInputStream(uri)
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap == null) {
                    throw Exception("No se pudo decodificar la imagen")
                }
                Log.d("RoyaDetect", "Bitmap original cargado: ${originalBitmap?.width}x${originalBitmap?.height}")
                val result = processImageAndAnalyzeDebug(context, uri)
                analysisResult = result.first
                debugInfo = result.second
                Log.d("RoyaDetect", "Procesamiento exitoso: ${result.first}")
            } catch (e: Exception) {
                errorMessage = "Error al procesar la imagen: ${e.message ?: "Desconocido"}"
                Log.e("RoyaDetect", "Error en procesamiento", e)
                e.printStackTrace()
            } finally {
                isProcessing = false
            }
        }
    }

    // Launcher para tomar foto con cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            Log.d("RoyaDetect", "Foto tomada exitosamente")
            processImage(photoUri!!)
        } else if (!success) {
            errorMessage = "No se pudo tomar la foto"
            Log.e("RoyaDetect", "Error al tomar foto")
        }
    }

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            Log.d("RoyaDetect", "Imagen seleccionada de galería: $imageUri")
            processImage(imageUri)
        }
    }

    // Launcher para abrir configuración de permisos
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val hasPermission = hasStoragePermission(context)
        Log.d("RoyaDetect", "Regresó de configuración, permiso: $hasPermission")
        if (hasPermission) {
            errorMessage = null
        } else {
            errorMessage = "El permiso de almacenamiento sigue denegado"
        }
    }

    // Diálogo para permisos denegados permanentemente
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permiso Requerido") },
            text = { Text("El permiso de almacenamiento es necesario para guardar el reporte. Por favor, habilítalo en la configuración de la aplicación.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        settingsLauncher.launch(intent)
                    }
                ) {
                    Text("Ir a Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botón Tomar Foto
        Button(
            onClick = {
                if (hasCameraPermission(context)) {
                    photoUri = createImageUri()
                    cameraLauncher.launch(photoUri!!)
                } else {
                    showPermissionRequest = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyaGreen),
            shape = RoundedCornerShape(8.dp),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Procesando...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Tomar Foto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // Manejo de permisos de cámara
        if (showPermissionRequest) {
            RequestCameraPermission { granted ->
                showPermissionRequest = false
                if (granted) {
                    photoUri = createImageUri()
                    cameraLauncher.launch(photoUri!!)
                } else {
                    errorMessage = "Se necesita permiso de cámara para tomar fotos"
                }
            }
        }

        // Botón Ver Galería
        Button(
            onClick = {
                galleryLauncher.launch("image/*")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyaLightGreen),
            shape = RoundedCornerShape(8.dp),
            enabled = !isProcessing
        ) {
            Text(
                text = "Ver Galería",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // Botón Descargar Reporte
        analysisResult?.let { result ->
            Button(
                onClick = {
                    if (originalBitmap == null) {
                        errorMessage = "Error: No se encontró la imagen original"
                        Log.e("RoyaDetect", "Bitmap original es nulo")
                        return@Button
                    }

                    scope.launch {
                        try {
                            Log.d("RoyaDetect", "Iniciando generación de PDF...")
                            val pdfFile = generatePdfReport(context, originalBitmap, result)
                            Log.d("RoyaDetect", "PDF generado en: ${pdfFile.absolutePath}")

                            // Guardar en base de datos
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())

                            val report = Report(
                                date = currentDate,
                                severityLevel = result.severityLevel,
                                confidence = result.confidence,
                                pdfPath = pdfFile.absolutePath
                            )

                            repository.insertReport(report)
                            Log.d("RoyaDetect", "Reporte guardado en base de datos")

                            // Abrir PDF como antes
                            val pdfUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                pdfFile
                            )
                            Log.d("RoyaDetect", "URI del PDF: $pdfUri")

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(pdfUri, "application/pdf")
                                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }

                            try {
                                context.startActivity(intent)
                                Log.d("RoyaDetect", "Intent para abrir PDF iniciado")
                            } catch (e: Exception) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Abrir PDF con..."))
                                } catch (e2: Exception) {
                                    errorMessage = "PDF generado y guardado en: ${pdfFile.absolutePath}\nNo se encontró app para abrirlo"
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error al generar el reporte: ${e.message ?: "Desconocido"}"
                            Log.e("RoyaDetect", "Error generando PDF", e)
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isProcessing && originalBitmap != null
            ) {
                Text(
                    text = "Descargar Reporte",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
        // Manejo de permisos de almacenamiento
        if (showStoragePermissionRequest) {
            RequestStoragePermission { granted ->
                showStoragePermissionRequest = false
                Log.d("RoyaDetect", "Resultado de solicitud de permiso: $granted")
                if (granted) {
                    Log.d("RoyaDetect", "Permiso de almacenamiento otorgado")
                } else {
                    Log.e("RoyaDetect", "Permiso de almacenamiento denegado")
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            context as android.app.Activity,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )) {
                        showPermissionSettingsDialog = true
                        Log.d("RoyaDetect", "Permiso denegado permanentemente")
                    } else {
                        errorMessage = "Se necesita permiso de almacenamiento para guardar el reporte"
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Debug info
        debugInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Debug: $info",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        // Mostrar resultado del análisis
        analysisResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (result.severityLevel) {
                        0 -> Color(0xFF4CAF50)
                        1 -> Color(0xFF8BC34A)
                        2 -> Color(0xFFFFEB3B)
                        3 -> Color(0xFFFF9800)
                        4 -> Color(0xFFF44336)
                        else -> RoyaGreen
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (result.severityLevel == 0) "Sin Roya Detectada" else "Roya Detectada",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (result.severityLevel > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nivel de Severidad: ${result.severityLevel}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getSeverityDescription(result.severityLevel),
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confianza: ${String.format(Locale.getDefault(), "%.1f", result.confidence * 100)}%",
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Mostrar error si existe
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Mensaje informativo
        if (analysisResult == null && errorMessage == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = RoyaGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Por favor, toma una foto clara de la hoja que presente más síntomas de roya. Asegúrate de que la imagen esté bien enfocada y con buena luz.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

data class AnalysisResult(
    val severityLevel: Int,
    val confidence: Float,
    val predictions: FloatArray
)

// Función para redimensionar manteniendo aspect ratio y agregar padding
private fun resizeImageWithPadding(originalBitmap: Bitmap, targetSize: Int = 224): Bitmap {
    val originalWidth = originalBitmap.width
    val originalHeight = originalBitmap.height

    Log.d("RoyaDetect", "Imagen original: ${originalWidth}x${originalHeight}")

    // Calcular el factor de escala para mantener aspect ratio
    val scaleFactor = min(
        targetSize.toFloat() / originalWidth,
        targetSize.toFloat() / originalHeight
    )

    val scaledWidth = (originalWidth * scaleFactor).toInt()
    val scaledHeight = (originalHeight * scaleFactor).toInt()

    Log.d("RoyaDetect", "Imagen escalada: ${scaledWidth}x${scaledHeight}")

    // Redimensionar manteniendo aspect ratio
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

    // Crear bitmap final con padding blanco
    val finalBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)

    // Llenar con fondo blanco
    canvas.drawColor(android.graphics.Color.WHITE)

    // Calcular posición para centrar la imagen
    val offsetX = (targetSize - scaledWidth) / 2f
    val offsetY = (targetSize - scaledHeight) / 2f

    // Dibujar la imagen centrada
    canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)

    Log.d("RoyaDetect", "Imagen final con padding: ${finalBitmap.width}x${finalBitmap.height}")

    return finalBitmap
}

// Función corregida para convertir a ByteBuffer (uint8)
private fun bitmapToByteBufferUint8(bitmap: Bitmap): ByteBuffer {
    Log.d("RoyaDetect", "Convirtiendo bitmap ${bitmap.width}x${bitmap.height} a ByteBuffer uint8")

    // Buffer size para uint8: 224 * 224 * 3 * 1 byte = 150,528 bytes
    val expectedSize = 224 * 224 * 3
    val byteBuffer = ByteBuffer.allocateDirect(expectedSize)
    byteBuffer.order(ByteOrder.nativeOrder())

    Log.d("RoyaDetect", "Buffer allocated: $expectedSize bytes")

    val intValues = IntArray(224 * 224)
    bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

    Log.d("RoyaDetect", "Pixels extracted: ${intValues.size}")

    // Convertir pixels a uint8 (0-255)
    for (pixel in intValues) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        // Poner valores como bytes (uint8: 0-255)
        byteBuffer.put(r.toByte())
        byteBuffer.put(g.toByte())
        byteBuffer.put(b.toByte())
    }

    Log.d("RoyaDetect", "Buffer position after filling: ${byteBuffer.position()}")
    byteBuffer.rewind()

    return byteBuffer
}

// FUNCIÓN PRINCIPAL CON DEBUG COMPLETO
suspend fun processImageAndAnalyzeDebug(context: Context, imageUri: Uri): Pair<AnalysisResult, String> {
    return withContext(Dispatchers.IO) {
        val debugMessages = mutableListOf<String>()

        try {
          //  debugMessages.add("1. Iniciando carga de imagen...")
            Log.d("RoyaDetect", "Paso 1: Cargando imagen desde URI: $imageUri")

            // Información del modelo primero
            debugModelInfo(context)

            // Verificar si el archivo modelo existe
            try {
                val modelFileExists = context.assets.list("")?.contains("modelo.tflite") ?: false
             //   debugMessages.add("2. Modelo existe: $modelFileExists")
                Log.d("RoyaDetect", "Modelo modelo.tflite existe: $modelFileExists")

                if (!modelFileExists) {
                    throw Exception("El archivo modelo.tflite no se encuentra en assets")
                }
            } catch (e: Exception) {
               // debugMessages.add("2. Error verificando modelo: ${e.message}")
                throw Exception("Error accediendo a assets: ${e.message}")
            }

            // Cargar imagen
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
              //  debugMessages.add("3. Error: No se pudo abrir la imagen")
                throw Exception("No se pudo abrir la imagen")
            }

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
               // debugMessages.add("3. Error: No se pudo decodificar la imagen")
                throw Exception("No se pudo decodificar la imagen")
            }

           // debugMessages.add("3. Imagen cargada: ${originalBitmap.width}x${originalBitmap.height}")
            Log.d("RoyaDetect", "Imagen original: ${originalBitmap.width}x${originalBitmap.height}")

            // Redimensionar con padding (mantener aspect ratio)
           // debugMessages.add("4. Redimensionando con padding a 224x224...")
            val processedBitmap = resizeImageWithPadding(originalBitmap, 224)
            Log.d("RoyaDetect", "Imagen procesada: ${processedBitmap.width}x${processedBitmap.height}")

            // Convertir a ByteBuffer uint8
          //  debugMessages.add("5. Convirtiendo a ByteBuffer uint8...")
            val inputBuffer = bitmapToByteBufferUint8(processedBitmap)
          //  debugMessages.add("6. Buffer creado: ${inputBuffer.capacity()} bytes")
            Log.d("RoyaDetect", "Buffer size: ${inputBuffer.capacity()} bytes")

            // Ejecutar modelo
           // debugMessages.add("7. Ejecutando modelo TensorFlow...")
            val predictions = runModelDebug(context, inputBuffer)
          //  debugMessages.add("8. Predicciones obtenidas: ${predictions.size} clases")
            Log.d("RoyaDetect", "Predicciones: ${predictions.contentToString()}")

            // Procesar resultado
            val maxIndex = predictions.indices.maxByOrNull { predictions[it] } ?: 0
            val confidence = predictions[maxIndex]
          ///  debugMessages.add("9. Resultado: Clase $maxIndex, Confianza ${String.format("%.3f", confidence)}")

            val result = AnalysisResult(
                severityLevel = maxIndex,
                confidence = confidence,
                predictions = predictions
            )

            Log.d("RoyaDetect", "Procesamiento completado exitosamente")
            return@withContext Pair(result, debugMessages.joinToString("; "))

        } catch (e: Exception) {
           // debugMessages.add("ERROR: ${e.message}")
            Log.e("RoyaDetect", "Error en procesamiento", e)
            throw Exception("${debugMessages.joinToString("; ")} - ${e.message}", e)
        }
    }
}

// FUNCIÓN PARA MANEJAR TIPOS DE SALIDA
private fun runModelDebug(context: Context, inputBuffer: ByteBuffer): FloatArray {
    var interpreter: Interpreter? = null

    try {
        Log.d("RoyaDetect", "Cargando modelo TensorFlow Lite...")

        val modelFileDescriptor = context.assets.openFd("modelo.tflite")
        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelFileDescriptor.startOffset
        val declaredLength = modelFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        Log.d("RoyaDetect", "Modelo cargado, creando intérprete...")

        // Configurar opciones del intérprete
        val options = Interpreter.Options()
        options.setNumThreads(4)
        interpreter = Interpreter(modelBuffer, options)

        // Debug del modelo
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        Log.d("RoyaDetect", "Input tensor shape: ${inputTensor.shape().contentToString()}")
        Log.d("RoyaDetect", "Input tensor type: ${inputTensor.dataType()}")
        Log.d("RoyaDetect", "Output tensor shape: ${outputTensor.shape().contentToString()}")
        Log.d("RoyaDetect", "Output tensor type: ${outputTensor.dataType()}")
        Log.d("RoyaDetect", "Input buffer capacity: ${inputBuffer.capacity()}")

        // Preparar output según el tipo detectado
        val outputShape = outputTensor.shape()
        val outputSize = if (outputShape.size >= 2) outputShape[1] else 5

        // Verificar el tipo real del tensor de salida
        val outputType = outputTensor.dataType()
        Log.d("RoyaDetect", "Tipo de salida detectado: $outputType")

        Log.d("RoyaDetect", "Ejecutando inferencia...")

        val result = when (outputType) {
            DataType.UINT8 -> {
                Log.d("RoyaDetect", "Procesando salida UINT8...")
                val outputBuffer = ByteBuffer.allocateDirect(outputSize)
                outputBuffer.order(ByteOrder.nativeOrder())

                interpreter.run(inputBuffer, outputBuffer)

                outputBuffer.rewind()
                FloatArray(outputSize) { i ->
                    val uint8Value = outputBuffer.get().toInt() and 0xFF
                    val normalizedValue = uint8Value / 255.0f
                    Log.d("RoyaDetect", "Output[$i]: $uint8Value -> $normalizedValue")
                    normalizedValue
                }
            }
            DataType.FLOAT32 -> {
                Log.d("RoyaDetect", "Procesando salida FLOAT32...")
                val outputArray = Array(1) { FloatArray(outputSize) }
                interpreter.run(inputBuffer, outputArray)
                outputArray[0]
            }
            else -> {
                throw Exception("Tipo de salida no soportado: $outputType")
            }
        }

        Log.d("RoyaDetect", "Inferencia completada: ${result.contentToString()}")
        return result

    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error en runModel", e)
        throw e
    } finally {
        interpreter?.close()
    }
}

// FUNCIÓN PARA DIAGNÓSTICO DEL MODELO
private fun debugModelInfo(context: Context) {
    var interpreter: Interpreter? = null

    try {
        Log.d("RoyaDetect", "=== DIAGNÓSTICO DEL MODELO ===")

        val modelFileDescriptor = context.assets.openFd("modelo.tflite")
        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelFileDescriptor.startOffset
        val declaredLength = modelFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        interpreter = Interpreter(modelBuffer)

        // Información completa del modelo
        Log.d("RoyaDetect", "Número de tensores de entrada: ${interpreter.inputTensorCount}")
        Log.d("RoyaDetect", "Número de tensores de salida: ${interpreter.outputTensorCount}")

        for (i in 0 until interpreter.inputTensorCount) {
            val tensor = interpreter.getInputTensor(i)
            Log.d("RoyaDetect", "Input $i - Shape: ${tensor.shape().contentToString()}, Type: ${tensor.dataType()}")
        }

        for (i in 0 until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(i)
            Log.d("RoyaDetect", "Output $i - Shape: ${tensor.shape().contentToString()}, Type: ${tensor.dataType()}")
        }

        Log.d("RoyaDetect", "=== FIN DIAGNÓSTICO ===")

    } catch (e: Exception) {
        Log.e("RoyaDetect", "Error obteniendo info del modelo", e)
    } finally {
        interpreter?.close()
    }
}

fun getSeverityDescription(level: Int): String {
    return when (level) {
        0 -> "Hoja saludable"
        1 -> "Síntomas leves de roya"
        2 -> "Roya moderada"
        3 -> "Roya severa"
        4 -> "Roya muy severa"
        else -> "Nivel desconocido"
    }
}
