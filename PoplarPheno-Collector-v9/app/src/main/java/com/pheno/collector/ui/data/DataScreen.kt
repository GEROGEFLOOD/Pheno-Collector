package com.pheno.collector.ui.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pheno.collector.data.model.PhotoRecord
import com.pheno.collector.data.model.Project
import com.pheno.collector.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProject by viewModel.activeProject.collectAsState()
    val photos by viewModel.currentPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var exportMessage by remember { mutableStateOf<String?>(null) }

    // SAF 文件夹选择器
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 持久化权限
            context.contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.exportToFolder(it) { msg ->
                exportMessage = msg
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 数据管理", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B2E1A), titleContentColor = Color(0xFFE8F5E9)
                ),
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← 返回", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        containerColor = Color(0xFF121A11)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            activeProject?.let { project ->
                StatCard(project, photos)
                Spacer(Modifier.height(12.dp))

                // 导出按钮
                Button(
                    onClick = { viewModel.exportCurrentProject { _ -> exportMessage = "已导出到应用文件夹" } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("📤 快速导出 (JSON+CSV)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(8.dp))

                // SAF 文件夹选择导出
                OutlinedButton(
                    onClick = { safLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784))
                ) {
                    Text("📂 选择导出文件夹 (SAF)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(4.dp))
                Text("提示：照片存储在App私有目录，同时保存到系统相册(DCIM/PoplarPheno)",
                    fontSize = 11.sp, color = Color(0xFF6A7B66))
            }

            exportMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF2A3A27), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, fontSize = 12.sp, color = Color(0xFFC8E6C9), modifier = Modifier.weight(1f))
                        TextButton(onClick = { exportMessage = null }) {
                            Text("关", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("📷 照片: ${photos.size}张", color = Color(0xFFA5D6A7), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(photos, key = { it.id }) { photo ->
                    PhotoItem(photo) { viewModel.deletePhoto(photo) }
                }
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun StatCard(project: Project, photos: List<PhotoRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A27)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌿 ${project.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50)) {
                    Text(project.id, fontSize = 11.sp, color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("📷", "照片", "${photos.size}")
                StatItem("📐", "参照物", com.pheno.collector.data.model.ReferenceType.fromCode(project.referenceType).label)
                StatItem("🎯", "模式", com.pheno.collector.data.model.CameraMode.fromCode(project.cameraMode).label)
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9))
        Text(label, fontSize = 11.sp, color = Color(0xFF6A7B66))
    }
}

@Composable
fun PhotoItem(photo: PhotoRecord, onDelete: () -> Unit) {
    val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(photo.timestamp))
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C281B))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${photo.photoIndex}. ${photo.filename}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC8E6C9))
                if (photo.description.isNotBlank()) {
                    Text(photo.description, fontSize = 12.sp, color = Color(0xFFA5D6A7), maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(timeStr, fontSize = 11.sp, color = Color(0xFF6A7B66))
                    if (photo.latitude != null && photo.longitude != null) {
                        Text("🛰️ GPS", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = Color(0xFFE57373))
            }
        }
    }
}
