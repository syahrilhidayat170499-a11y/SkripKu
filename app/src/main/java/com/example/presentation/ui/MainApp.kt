package com.example.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.ui.screens.*
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.ScripKuTheme
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val label: String) {
    object Tulis : Screen("tulis", "Tulis")
    object Outline : Screen("outline", "Outline")
    object Karakter : Screen("karakter", "Karakter")
    object Setting : Screen("setting", "Setting")
}

@Composable
fun MainApp() {
    var appState by remember { mutableStateOf("splash") } // "splash", "onboarding", "main"
    val viewModel: MainViewModel = viewModel()
    val currentProject by viewModel.currentProject.collectAsState()
    val activeScene by viewModel.activeScene.collectAsState()

    var activeTab by remember { mutableStateOf<Screen>(Screen.Tulis) }
    var inEditorMode by remember { mutableStateOf(false) }

    // Splash Timer Delay
    LaunchedEffect(Unit) {
        delay(1500)
        appState = "onboarding"
    }

    when (appState) {
        "splash" -> {
            ScripKuTheme(theme = "dark") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "[ ScripKu ]",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI Screenwriting Native Companion",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(72.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        "onboarding" -> {
            ScripKuTheme(theme = "dark") {
                OnboardingScreen(onFinished = {
                    appState = "main"
                })
            }
        }
        "main" -> {
            // Main structure scaffolding with responsive bottom navbar
            ScripKuTheme(theme = if (inEditorMode && activeTab == Screen.Tulis) viewModel.editorTheme.collectAsState().value else "dark") {
                Scaffold(
                    bottomBar = {
                        // Keep navbar hidden inside active typewriter editor mode to save screen real estate
                        if (!inEditorMode || activeTab != Screen.Tulis) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                val navItems = listOf(Screen.Tulis, Screen.Outline, Screen.Karakter, Screen.Setting)
                                navItems.forEach { screen ->
                                    val isSelected = activeTab == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            activeTab = screen
                                        },
                                        label = { Text(screen.label) },
                                        icon = {
                                            val icon = when (screen) {
                                                Screen.Tulis -> Icons.Default.Edit
                                                Screen.Outline -> Icons.Default.List
                                                Screen.Karakter -> Icons.Default.Person
                                                Screen.Setting -> Icons.Default.Settings
                                            }
                                            Icon(imageVector = icon, contentDescription = screen.label)
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            Screen.Tulis -> {
                                if (inEditorMode && currentProject != null && activeScene != null) {
                                    EditorScreen(
                                        viewModel = viewModel,
                                        onBack = { inEditorMode = false }
                                    )
                                } else {
                                    ProjectListScreen(
                                        viewModel = viewModel,
                                        onProjectSelected = {
                                            inEditorMode = true
                                        }
                                    )
                                }
                            }
                            Screen.Outline -> {
                                OutlineScreen(
                                    viewModel = viewModel,
                                    onNavigateToEditor = {
                                        activeTab = Screen.Tulis
                                        inEditorMode = true
                                    }
                                )
                            }
                            Screen.Karakter -> {
                                CharacterScreen(viewModel = viewModel)
                            }
                            Screen.Setting -> {
                                SettingScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
