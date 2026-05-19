package com.pheno.collector.util

import android.content.Context
import android.net.Uri
import com.pheno.collector.data.model.CameraMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件命名与路径管理工具
 * Android 10+ 兼容：主存储使用 apps 私有目录 (data/data/.../files/)
 * 照片同时保存到相册 MediaStore 供用户查看
 */
object FileNamingEngine {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 获取App内部存储根目录 (无需权限，Android任意版本可用)
     * 结构: <filesDir>/PhenoCollector/
     */
    fun getAppRootDir(context: Context): File {
        val dir = File(context.filesDir, "PhenoCollector")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取或创建项目文件夹
     * 结构: <filesDir>/PhenoCollector/<ProjectName>_<ProjectId>/
     */
    fun getProjectDir(context: Context, projectName: String, projectId: String): File {
        val safeName = projectName.replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-]"), "_")
        val dir = File(getAppRootDir(context), "${safeName}_${projectId}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 生成标准化照片文件名
     * 格式: <日期>_<模式代码>_<项目号>_<编号>.jpg
     * 示例: 20260518_CLOSEUP_PRJ001_001.jpg
     */
    fun generatePhotoFilename(
        date: Date = Date(),
        cameraMode: CameraMode,
        projectId: String,
        index: Int
    ): String {
        val dateStr = dateFormat.format(date)
        val paddedIndex = String.format("%03d", index)
        return "${dateStr}_${cameraMode.code}_${projectId}_${paddedIndex}.jpg"
    }

    /**
     * 生成时间戳字符串
     */
    fun getTimestamp(): String = timeFormat.format(Date())

    /**
     * 生成项目编号 PRJ + 3位序号
     */
    fun generateProjectId(sequence: Int): String {
        return String.format("PRJ%03d", sequence)
    }

    /**
     * 将内部文件复制到外部可访问目录（用于 SAE 导出）
     */
    fun getExportDir(context: Context, projectName: String, projectId: String): File {
        val dir = File(context.getExternalFilesDir(null), "exports/${projectName}_${projectId}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
