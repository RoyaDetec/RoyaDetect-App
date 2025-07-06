// ui/components/MainScreen.kt
package com.example.royadetect.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.royadetect.ui.screens.CameraScreen
import com.example.royadetect.ui.screens.HomeScreen
import com.example.royadetect.ui.screens.MenuScreen
import com.example.royadetect.ui.screens.ReportsScreen
import com.example.royadetect.ui.screens.AnalysisResult
import java.io.File

enum class Screen {
    HOME, CAMERA, REPORTS, MENU
}

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showMenu by remember { mutableStateOf(false) }

    // Estados compartidos para la imagen y análisis
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }

    val context = LocalContext.current

    // Launcher para solicitar permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, abrir cámara
            launchCamera(context) { uri ->
                selectedImageUri = uri
                // Cambiar a la pantalla HOME para mostrar la imagen
                currentScreen = Screen.HOME
                showMenu = false
            }
        } else {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para capturar imagen
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // La imagen se guardó exitosamente
            // selectedImageUri ya está configurado
            currentScreen = Screen.HOME
            showMenu = false
        }
    }

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Cambiar a la pantalla HOME para mostrar la imagen
            currentScreen = Screen.HOME
            showMenu = false
        }
    }

    // Función para abrir la cámara
    val openCamera = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                // Permiso ya concedido, abrir cámara
                launchCamera(context) { uri ->
                    selectedImageUri = uri
                    cameraLauncher.launch(uri)
                }
            }
            else -> {
                // Solicitar permiso
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Función para abrir la galería
    val openGallery = {
        galleryLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopBar(
                onMenuClick = {
                    showMenu = !showMenu
                    currentScreen = if (showMenu) Screen.MENU else Screen.HOME
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                    showMenu = false
                },
                onCameraClick = openCamera,
                onGalleryClick = openGallery
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen()
                Screen.CAMERA -> CameraScreen()
                Screen.REPORTS -> ReportsScreen()
                Screen.MENU -> MenuScreen()
            }
        }
    }
}

// Función auxiliar para crear el archivo temporal para la cámara
private fun launchCamera(context: Context, onUriCreated: (Uri) -> Unit) {
    try {
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        onUriCreated(uri)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al crear archivo temporal", Toast.LENGTH_SHORT).show()
    }
}