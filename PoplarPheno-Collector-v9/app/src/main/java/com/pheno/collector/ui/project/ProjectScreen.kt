package com.pheno.collector.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pheno.collector.data.model.CameraMode
import com.pheno.collector.data.model.Project
import com.pheno.collector.data.model.ReferenceType
import com.pheno.collector.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(viewModel: MainViewModel, onNavigateToCamera: () -> Unit) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌿 项目中心", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B2E1A),
                    titleContentColor = Color(0xFFE8F5E9)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF4CAF50),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "新建项目", tint = Color.White)
            }
        },
        containerColor = Color(0xFF121A11)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // 当前活跃项目卡片
            if (activeProject != null) {
                ActiveProjectCard(
                    project = activeProject!!,
                    photoCount = viewModel.currentPhotos.collectAsState().value.size,
                    onContinue = onNavigateToCamera
                )
                Spacer(Modifier.height(16.dp))
            }

            // 项目列表
            Text("📂 项目列表", color = Color(0xFFA5D6A7), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(projects) { project ->
                    ProjectListItem(
                        project = project,
                        isActive = project.id == activeProject?.id,
                        onClick = { viewModel.switchProject(project.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, loc, researcher ->
                viewModel.createProject(name, desc, loc, researcher)
                showCreateDialog = false
                onNavigateToCamera()
            }
        )
    }
}

@Composable
fun ActiveProjectCard(project: Project, photoCount: Int, onContinue: () -> Unit) {
    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(project.createdAt))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A27)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▶", color = Color(0xFF4CAF50), fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(project.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50)) {
                    Text(project.id, fontSize = 11.sp, color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(project.description, fontSize = 13.sp, color = Color(0xFFA5D6A7), maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("📷 ${photoCount}张照片", fontSize = 13.sp, color = Color(0xFFC8E6C9))
                    Text("📍 ${project.location}", fontSize = 13.sp, color = Color(0xFFC8E6C9))
                }
                Text("${dateStr}创建", fontSize = 12.sp, color = Color(0xFF6A7B66))
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("📷 继续采集", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ProjectListItem(project: Project, isActive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF2A3A27) else Color(0xFF1C281B)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isActive) 2.dp else 1.dp,
            if (isActive) Color(0xFF4CAF50) else Color(0xFF2A3A27)
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isActive) "▶" else "⏸", color = if (isActive) Color(0xFF4CAF50) else Color(0xFF6A7B66))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color.White else Color(0xFFC8E6C9))
                Text("${project.id} · ${project.location}", fontSize = 12.sp, color = Color(0xFF6A7B66))
            }
            if (isActive) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0x334CAF50)) {
                    Text("当前", fontSize = 11.sp, color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var researcher by remember { mutableStateOf("") }
    var cameraMode by remember { mutableStateOf(CameraMode.CLOSEUP) }
    var referenceType by remember { mutableStateOf(ReferenceType.NONE) }
    var showModeDropdown by remember { mutableStateOf(false) }
    var showRefDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C281B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4D37))
        ) {
            LazyColumn(Modifier.padding(20.dp)) {
                item {
                    Text("🌱 新建采集项目", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8F5E9))
                    Spacer(Modifier.height(4.dp))
                    Text("创建一个标准化项目，照片将自动保存到项目文件夹",
                        fontSize = 13.sp, color = Color(0xFF6A7B66))
                    Spacer(Modifier.height(16.dp))
                }

                item { TextFieldSection("项目名称*", name, "如：杨树病害调查2025") { name = it } }
                item { Spacer(Modifier.height(12.dp)) }
                item { TextFieldSection("项目说明", description, "采集目标、备注等") { description = it } }
                item { Spacer(Modifier.height(12.dp)) }
                item { TextFieldSection("采集地点", location, "如：山东临沂") { location = it } }
                item { Spacer(Modifier.height(12.dp)) }
                item { TextFieldSection("研究人员", researcher, "你的名字") { researcher = it } }
                item { Spacer(Modifier.height(16.dp)) }

                // 拍摄模式选择
                item {
                    Text("拍摄模式", color = Color(0xFFC8E6C9), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    CameraMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (cameraMode == mode) Color(0x334CAF50) else Color.Transparent)
                                .clickable { cameraMode = mode }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mode.label, fontSize = 14.sp, color = Color(0xFFE8F5E9), fontWeight = FontWeight.Medium)
                                Text(mode.desc, fontSize = 11.sp, color = Color(0xFF6A7B66))
                            }
                            if (cameraMode == mode) Text("✅", fontSize = 18.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }

                // 参照物选择
                item {
                    Text("参照物", color = Color(0xFFC8E6C9), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    ReferenceType.entries.forEach { ref ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (referenceType == ref) Color(0x334CAF50) else Color.Transparent)
                                .clickable { referenceType = ref }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ref.label, fontSize = 14.sp, color = Color(0xFFE8F5E9), fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            if (referenceType == ref) Text("✅", fontSize = 18.sp)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5D6A7))
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onCreate(name, description, location, researcher)
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF2C4A2A)
                            )
                        ) {
                            Text("✅ 创建项目", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextFieldSection(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = Color(0xFFC8E6C9), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF6A7B66)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE8F5E9),
                unfocusedTextColor = Color(0xFFE8F5E9),
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFF2A3A27),
                focusedContainerColor = Color(0xFF1C281B),
                unfocusedContainerColor = Color(0xFF1C281B)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
    }
}
