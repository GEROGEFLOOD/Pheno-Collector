package com.pheno.collector.ui

import android.app.Application
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pheno.collector.data.local.AppDatabase
import com.pheno.collector.data.model.*
import com.pheno.collector.util.ExportEngine
import com.pheno.collector.util.FileNamingEngine
import com.pheno.collector.util.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 主ViewModel - 管理所有应用状态
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val projectDao = db.projectDao()
    private val photoDao = db.photoDao()

    // --- 项目列表 ---
    val allProjects: StateFlow<List<Project>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 当前活跃项目 ---
    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject

    // --- 当前项目照片 ---
    private val _currentPhotos = MutableStateFlow<List<PhotoRecord>>(emptyList())
    val currentPhotos: StateFlow<List<PhotoRecord>> = _currentPhotos

    // --- UI状态 ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    // --- 拍照参数 ---
    private val _cameraMode = MutableStateFlow(CameraMode.CLOSEUP)
    val cameraMode: StateFlow<CameraMode> = _cameraMode

    private val _referenceType = MutableStateFlow(ReferenceType.NONE)
    val referenceType: StateFlow<ReferenceType> = _referenceType

    private val _magnification = MutableStateFlow(40)
    val magnification: StateFlow<Int> = _magnification

    // --- 分组采集 ---
    private val _isGroupMode = MutableStateFlow(false)
    val isGroupMode: StateFlow<Boolean> = _isGroupMode

    private val _groupPlantId = MutableStateFlow("")
    val groupPlantId: StateFlow<String> = _groupPlantId

    private val _groupNotes = MutableStateFlow("")
    val groupNotes: StateFlow<String> = _groupNotes

    private val _groupIndex = MutableIntStateFlow(0)
    val groupIndex: StateFlow<Int> = _groupIndex.asStateFlow()

    init {
        LocationProvider.init(application)
        loadActiveProject()
    }

    /**
     * 加载当前活跃项目
     */
    fun loadActiveProject() {
        viewModelScope.launch {
            val project = projectDao.getActiveProject()
            _activeProject.value = project
            project?.let { loadPhotos(it.id) }
        }
    }

    /**
     * 加载项目照片
     */
    private fun loadPhotos(projectId: String) {
        viewModelScope.launch {
            photoDao.getPhotosByProject(projectId).collect { photos ->
                _currentPhotos.value = photos
            }
        }
    }

    /**
     * 创建新项目
     */
    fun createProject(
        name: String,
        description: String,
        location: String,
        researcher: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val count = projectDao.getProjectCount()
                val projectId = FileNamingEngine.generateProjectId(count + 1)
                val folderPath = FileNamingEngine.getProjectDir(
                    getApplication(), name, projectId
                ).absolutePath

                // 停用旧项目
                projectDao.deactivateAllProjects()

                val project = Project(
                    id = projectId,
                    name = name,
                    description = description,
                    location = location,
                    researcher = researcher,
                    folderPath = folderPath,
                    cameraMode = _cameraMode.value.code,
                    referenceType = _referenceType.value.code
                )

                projectDao.insertProject(project)
                _activeProject.value = project
                loadPhotos(project.id)

                _toastMessage.emit("✅ 项目 $projectId 创建成功")
            } catch (e: Exception) {
                _toastMessage.emit("❌ 创建失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 切换活跃项目
     */
    fun switchProject(projectId: String) {
        viewModelScope.launch {
            projectDao.deactivateAllProjects()
            projectDao.activateProject(projectId)
            val project = projectDao.getProjectById(projectId)
            _activeProject.value = project
            project?.let {
                loadPhotos(it.id)
                // 同步模式设置
                _cameraMode.value = CameraMode.fromCode(it.cameraMode)
                _referenceType.value = ReferenceType.fromCode(it.referenceType)
            }
        }
    }

    /**
     * 删除项目
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectDao.deleteProjectById(projectId)
            if (_activeProject.value?.id == projectId) {
                _activeProject.value = null
                _currentPhotos.value = emptyList()
            }
            _toastMessage.emit("🗑️ 项目已删除")
        }
    }

    /**
     * 更新项目模式
     */
    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
        viewModelScope.launch {
            _activeProject.value?.let { project ->
                val updated = project.copy(cameraMode = mode.code)
                projectDao.updateProject(updated)
                _activeProject.value = updated
            }
        }
    }

    fun setReferenceType(type: ReferenceType) {
        _referenceType.value = type
        val size = when (type) {
            ReferenceType.COIN_1YUAN -> 25.0f
            ReferenceType.COIN_5JIAO -> 20.5f
            else -> null
        }
        viewModelScope.launch {
            _activeProject.value?.let { project ->
                val updated = project.copy(referenceType = type.code, referenceSizeMm = size)
                projectDao.updateProject(updated)
                _activeProject.value = updated
            }
        }
    }

    fun setMagnification(mag: Int) {
        _magnification.value = mag
    }

    /**
     * 分组模式控制
     */
    fun startGroupMode(plantId: String, notes: String) {
        _isGroupMode.value = true
        _groupPlantId.value = plantId
        _groupNotes.value = notes
        _groupIndex.intValue = 0
    }

    fun advanceGroup() {
        _groupIndex.intValue++
    }

    fun endGroupMode() {
        _isGroupMode.value = false
        _groupPlantId.value = ""
        _groupNotes.value = ""
        _groupIndex.intValue = 0
    }

    /**
     * 保存照片记录
     */
    suspend fun savePhotoRecord(
        filePath: String,
        description: String,
        location: Location?
    ): PhotoRecord {
        val project = _activeProject.value ?: throw IllegalStateException("无活跃项目")
        val mode = _cameraMode.value
        val index = photoDao.getPhotoCountByProject(project.id) + 1

        val filename = FileNamingEngine.generatePhotoFilename(
            date = Date(), cameraMode = mode, projectId = project.id, index = index
        )

        val record = PhotoRecord(
            projectId = project.id,
            filename = filename,
            filePath = filePath,
            description = description,
            photoIndex = index,
            cameraMode = mode.code,
            magnification = if (mode == CameraMode.MICROSCOPE) _magnification.value else null,
            referenceType = _referenceType.value.code,
            referenceSizeMm = project.referenceSizeMm,
            latitude = location?.latitude?.takeIf { it != 0.0 },
            longitude = location?.longitude?.takeIf { it != 0.0 },
            altitude = location?.altitude,
            accuracy = location?.accuracy,
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE
        )

        photoDao.insertPhoto(record)
        return record
    }

    /**
     * 更新照片描述
     */
    fun updatePhotoDescription(photoId: Long, description: String) {
        viewModelScope.launch {
            photoDao.updateDescription(photoId, description)
        }
    }

    /**
     * 删除照片记录
     */
    fun deletePhoto(photo: PhotoRecord) {
        viewModelScope.launch {
            photoDao.deletePhoto(photo)
            // 同时删除文件
            try {
                java.io.File(photo.filePath).delete()
            } catch (_: Exception) {}
        }
    }

    /**
     * 导出当前项目数据
     */
    fun exportCurrentProject(onResult: (File) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = _activeProject.value ?: throw IllegalStateException("无活跃项目")
                val photos = photoDao.getPhotosByProjectSync(project.id)
                val exportDir = ExportEngine.exportProject(getApplication(), project, photos)
                onResult(exportDir)
                _toastMessage.emit("✅ 导出成功: ${exportDir.absolutePath}")
            } catch (e: Exception) {
                _toastMessage.emit("❌ 导出失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 导出到用户选择的SAF文件夹 */
    fun exportToFolder(treeUri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val project = _activeProject.value ?: throw IllegalStateException("无活跃项目")
                val photos = photoDao.getPhotosByProjectSync(project.id)
                val msg = ExportEngine.exportToSafFolder(
                    getApplication(), treeUri, project, photos
                )
                onResult(msg)
                _toastMessage.emit(msg)
            } catch (e: Exception) {
                _toastMessage.emit("❌ 导出失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * MutableIntStateFlow 辅助类
 */
class MutableIntStateFlow(initial: Int) {
    private val _flow = MutableStateFlow(initial)
    val flow: StateFlow<Int> = _flow
    var intValue: Int
        get() = _flow.value
        set(value) { _flow.value = value }
    fun asStateFlow(): StateFlow<Int> = _flow
}
