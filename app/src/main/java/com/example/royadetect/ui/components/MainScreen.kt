// ui/components/MainScreen.kt
package com.example.royadetect.ui.components

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
import com.example.royadetect.ui.screens.CameraScreen
import com.example.royadetect.ui.screens.HomeScreen
import com.example.royadetect.ui.screens.MenuScreen
import com.example.royadetect.ui.screens.ReportsScreen

enum class Screen {
    HOME, CAMERA, REPORTS, MENU
}

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showMenu by remember { mutableStateOf(false) }

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
                }
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