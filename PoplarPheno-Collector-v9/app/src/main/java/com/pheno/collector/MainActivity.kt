package com.pheno.collector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pheno.collector.ui.MainViewModel
import com.pheno.collector.ui.camera.CameraScreen
import com.pheno.collector.ui.data.DataScreen
import com.pheno.collector.ui.project.ProjectScreen
import com.pheno.collector.util.LocationProvider

private val ForestDark = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF81C784),
    surface = Color(0xFF1B2E1A),
    background = Color(0xFF121A11),
    surfaceVariant = Color(0xFF2A3A27),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFFE8F5E9),
    onBackground = Color(0xFFE8F5E9),
)

enum class Screen { PROJECT, CAMERA, DATA }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocationProvider.init(this)
        setContent {
            MaterialTheme(colorScheme = ForestDark) {
                App(viewModel)
            }
        }
    }
}

@Composable
fun App(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.PROJECT) }
    val activeProject by viewModel.activeProject.collectAsState()

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = Color(0xFF1B2E1A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3A27))) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NavItem("📂", "项目", currentScreen == Screen.PROJECT) { currentScreen = Screen.PROJECT }
                    NavItem("📷", "拍摄", currentScreen == Screen.CAMERA) { currentScreen = Screen.CAMERA }
                    NavItem("📊", "数据", currentScreen == Screen.DATA) { currentScreen = Screen.DATA }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.PROJECT -> ProjectScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = { currentScreen = Screen.CAMERA }
                )
                Screen.CAMERA -> CameraScreen(
                    viewModel = viewModel,
                    project = activeProject,
                    onNavigateBack = { currentScreen = Screen.PROJECT }
                )
                Screen.DATA -> DataScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.CAMERA }
                )
            }
        }
    }
}

@Composable
private fun NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 22.sp)
        Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Color(0xFF4CAF50) else Color(0xFF6A7B66))
    }
}
