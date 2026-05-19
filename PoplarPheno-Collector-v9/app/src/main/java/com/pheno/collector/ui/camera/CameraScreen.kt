package com.pheno.collector.ui.camera

import android.Manifest
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pheno.collector.data.model.CameraMode
import com.pheno.collector.data.model.Project
import com.pheno.collector.data.model.ReferenceType
import com.pheno.collector.ui.MainViewModel
import com.pheno.collector.util.FileNamingEngine
import com.pheno.collector.util.LocationProvider
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: MainViewModel, project: Project?, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // 权限
    val cameraPerm = rememberPermissionState(Manifest.permission.CAMERA)
    val locPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // 状态
    val cameraMode by viewModel.cameraMode.collectAsState()
    val magnification by viewModel.magnification.collectAsState()
    val groupMode by viewModel.isGroupMode.collectAsState()
    val groupIndex by viewModel.groupIndex.collectAsState()
    val currentPhotos by viewModel.currentPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showModePanel by remember { mutableStateOf(false) }
    var showPhotoForm by remember { mutableStateOf(false) }
    var showProjectInfo by remember { mutableStateOf(false) }
    var showRefOverlay by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // 拍照后待处理文件
    var pendingFile by remember { mutableStateOf<File?>(null) }

    // 表单
    var plantId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // 定位缓存
    var lastLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val controller = remember { CameraController(lifecycleOwner) }

    // 无项目提示
    if (project == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF121A11)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌿", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("请先创建或选择一个项目", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9))
                Spacer(Modifier.height(8.dp))
                Text("所有照片将保存到项目文件夹中", fontSize = 14.sp, color = Color(0xFF6A7B66))
                Spacer(Modifier.height(20.dp))
                Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("去创建项目", color = Color.White)
                }
            }
        }
        return
    }

    // 请求权限
    if (!cameraPerm.status.isGranted) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📷 需要相机权限", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9))
            Spacer(Modifier.height(12.dp))
            Text("请授予相机权限以采集植物表型照片", color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { cameraPerm.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("授予权限", color = Color.White)
            }
        }
        return
    }

    // 启动GPS
    LaunchedEffect(Unit) {
        if (locPerm.status.isGranted) {
            try {
                LocationProvider.getCurrentLocation()?.let { lastLocation = it }
            } catch (_: Exception) {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 相机预览
        AndroidView(factory = { controller.createPreviewView() }, modifier = Modifier.fillMaxSize())

        // 顶部栏 - 项目信息
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 48.dp).align(Alignment.TopStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xAA000000), modifier = Modifier.clickable { showProjectInfo = !showProjectInfo }) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🌿 ${project.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE8F5E9))
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50)) {
                            Text(project.id, fontSize = 10.sp, color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge("${cameraMode.icon} ${cameraMode.label}")
                Badge(ReferenceType.fromCode(project.referenceType).label)
                if (groupMode) Badge("📁 分组·${groupIndex + 1}", Color(0xAAFF9800))
                if (currentPhotos.isNotEmpty()) Badge("📷 ${currentPhotos.size}张")
            }

            // 项目详情展开
            if (showProjectInfo) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xE2181A11),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4D37))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(project.description, fontSize = 12.sp, color = Color(0xFFC8E6C9), maxLines = 3)
                        Spacer(Modifier.height(4.dp))
                        Text("📍 ${project.location} · 👤 ${project.researcher}", fontSize = 11.sp, color = Color(0xFF6A7B66))
                    }
                }
            }
        }

        // GPS状态指示
        if (locPerm.status.isGranted) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xAA000000),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 14.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (lastLocation != null) "🛰️ GPS" else "🛰️ 定位中...", fontSize = 11.sp, color = Color(0xFFA5D6A7))
                }
            }
        }

        // 右侧工具栏
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolBtn(cameraMode.icon, showModePanel) { showModePanel = !showModePanel }
            ToolBtn("📐") { showRefOverlay = !showRefOverlay }
            ToolBtn("📂") { onNavigateBack() }
        }

        // 模式选择面板
        if (showModePanel) {
            Surface(Modifier.align(Alignment.TopStart).padding(top = 160.dp, start = 20.dp, end = 70.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp), color = Color(0xF2181A11),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4D37))) {
                Column(Modifier.padding(18.dp)) {
                    Text("选择拍照模式", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFE8F5E9))
                    Spacer(Modifier.height(14.dp))
                    CameraMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (cameraMode == mode) Color(0x334CAF50) else Color.Transparent)
                            .clickable { viewModel.setCameraMode(mode); showModePanel = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(mode.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mode.label, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = if (cameraMode == mode) Color(0xFF4CAF50) else Color(0xFFC8E6C9))
                                Text(mode.desc, fontSize = 12.sp, color = Color(0xFF6A7B66))
                            }
                            if (cameraMode == mode) Text("✅")
                        }
                    }
                }
            }
        }

        // 参照物提示
        if (showRefOverlay && project.referenceType != ReferenceType.NONE.code) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 120.dp).align(Alignment.Center),
                contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0x60000000),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0x44FFFFFF))) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📐 请在拍摄时放置参照物", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(ReferenceType.fromCode(project.referenceType).label, fontSize = 20.sp, color = Color(0xFF4CAF50))
                        if (project.referenceSizeMm != null) {
                            Text("标准尺寸: ${project.referenceSizeMm} mm", fontSize = 14.sp, color = Color(0xFFC8E6C9))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("将参照物放在被摄物体旁边，尽量保证水平", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                    }
                }
            }
        }

        // 底部操作区
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 显微镜倍率
            if (cameraMode == CameraMode.MICROSCOPE) {
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x77000000)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔬 倍率:", color = Color(0xFFC8E6C9), fontSize = 12.sp)
                    listOf(40, 100, 200, 400, 1000).forEach { mag ->
                        Surface(shape = RoundedCornerShape(14.dp), color = if (magnification == mag) Color(0xBB4CAF50) else Color(0x14FFFFFF),
                            modifier = Modifier.clickable { viewModel.setMagnification(mag) }) {
                            Text("${mag}×", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                color = if (magnification == mag) Color.White else Color(0xFFA5D6A7), fontSize = 12.sp)
                        }
                    }
                }
            }

            // 快门
            Box(Modifier.size(84.dp).clip(CircleShape)
                .background(Color(0xFFE8F5E9)).border(5.dp, Color(0xFFE8F5E9), CircleShape)
                .clickable(enabled = !busy) {
                    busy = true
                    scope.launch {
                        // 获取GPS（在主线程请求）
                        if (locPerm.status.isGranted) {
                            try { LocationProvider.getCurrentLocation()?.let { lastLocation = it } } catch (_: Exception) {}
                        }
                        // IO操作：创建文件、查数据库
                        val (file, filename) = withContext(Dispatchers.IO) {
                            val dir = File(project.folderPath)
                            if (!dir.exists()) dir.mkdirs()
                            val photoCount = com.pheno.collector.data.local.AppDatabase.getInstance(context)
                                .photoDao().getPhotoCountByProject(project.id)
                            val fn = FileNamingEngine.generatePhotoFilename(
                                Date(), cameraMode, project.id, photoCount + 1
                            )
                            Pair(File(dir, fn), fn)
                        }
                        pendingFile = file

                        // 拍照（回调已在主线程）
                        controller.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build()
                        ) { success, exc ->
                            if (success) {
                                // 相册保存在IO线程
                                scope.launch(Dispatchers.IO) {
                                    saveToGallery(context, file, filename)
                                }
                                showPhotoForm = true
                            } else {
                                Toast.makeText(context, "拍照失败: ${exc?.message}", Toast.LENGTH_SHORT).show()
                            }
                            busy = false
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(color = Color(0xFF121A11), modifier = Modifier.size(40.dp))
                else Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xFF121A11)))
            }
        }
    }

    // 照片信息表单
    if (showPhotoForm && pendingFile != null) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoForm = false; pendingFile = null },
            containerColor = Color(0xFF1C281B),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 42.dp)) {
                Text(if (groupMode) "📁 分组·第${groupIndex + 1}张" else "📸 照片信息",
                    fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8F5E9))
                Spacer(Modifier.height(12.dp))

                // 预览小图
                pendingFile?.let { file ->
                    val bitmap = remember(file) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }
                    bitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it, contentDescription = "预览",
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Text("文件名: ${pendingFile?.name}", fontSize = 11.sp, color = Color(0xFF6A7B66))
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(value = plantId, onValueChange = { plantId = it },
                    label = { Text("植物编号/样本号*", color = Color(0xFF6A7B66)) },
                    placeholder = { Text("如: P001", color = Color(0xFF6A7B66)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8F5E9), focusedBorderColor = Color(0xFF4CAF50),
                        focusedContainerColor = Color(0xFF1C281B), unfocusedContainerColor = Color(0xFF1C281B)
                    ))
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("照片描述 (选填)", color = Color(0xFF6A7B66)) },
                    placeholder = { Text("如: 叶片正面/病害区域等", color = Color(0xFF6A7B66)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                    maxLines = 3, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8F5E9), focusedBorderColor = Color(0xFF4CAF50),
                        focusedContainerColor = Color(0xFF1C281B), unfocusedContainerColor = Color(0xFF1C281B)
                    ))
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPhotoForm = false; pendingFile = null },
                        modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9E9E9E))
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            val file = pendingFile ?: return@Button
                            scope.launch {
                                try {
                                    val desc = if (notes.isNotBlank()) notes else ""
                                    viewModel.savePhotoRecord(file.absolutePath, desc, lastLocation)
                                    Toast.makeText(context, "✅ 已保存", Toast.LENGTH_SHORT).show()
                                    showPhotoForm = false
                                    pendingFile = null
                                    plantId = ""
                                    notes = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) { Text("💾 保存记录", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bgColor: Color = Color(0x88000000)) {
    Surface(shape = RoundedCornerShape(22.dp), color = bgColor) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = Color(0xFFC8E6C9), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToolBtn(text: String, active: Boolean = false, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = if (active) Color(0xBB4CAF50) else Color(0x77000000),
        modifier = Modifier.size(44.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, fontSize = 18.sp) }
    }
}

private fun saveToGallery(context: android.content.Context, file: File, filename: String) {
    try {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PoplarPheno")
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            }
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { destUri ->
            file.inputStream().use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
}
