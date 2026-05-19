package com.pheno.collector.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.pheno.collector.data.model.CameraMode
import com.pheno.collector.data.model.PhotoRecord
import com.pheno.collector.data.model.Project
import com.pheno.collector.data.model.ReferenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据导出引擎 - 生成标准JSON和CSV文件
 * 支持SAF文件夹选择器和内部存储
 */
object ExportEngine {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 导出项目到 App 的外部文件目录 (可被文件管理器访问)
     * 包含：JSON元数据、CSV清单、README、以及所有照片副本
     */
    suspend fun exportProject(
        context: Context,
        project: Project,
        photos: List<PhotoRecord>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = FileNamingEngine.getExportDir(context, project.name, project.id)
        val timestamp = dateFormat.format(Date())

        writeExportFiles(exportDir, project, photos, timestamp)
        copyPhotoFiles(exportDir, photos)
        exportDir
    }

    /**
     * 导出到用户选择的SAF文件夹（含照片）
     */
    fun exportToSafFolder(
        context: Context,
        treeUri: Uri,
        project: Project,
        photos: List<PhotoRecord>
    ): String {
        val timestamp = dateFormat.format(Date())
        val docFile: DocumentFile = DocumentFile.fromTreeUri(context, treeUri)
            ?: return "无法访问选定文件夹"

        // 创建项目子文件夹
        var projectFolder = docFile.findFile("${project.id}_export")
        if (projectFolder == null) {
            projectFolder = docFile.createDirectory("${project.id}_export")
        }
        if (projectFolder == null) return "无法创建导出文件夹"

        // 生成并写入元数据
        val manifestJson = buildManifestJson(project, photos, timestamp)
        val photosCsv = buildPhotosCsv(project, photos, timestamp)
        val readmeTxt = buildReadmeTxt(project, photos, timestamp)

        writeDocumentFile(context, projectFolder, "${project.id}_manifest.json", manifestJson)
        writeDocumentFile(context, projectFolder, "${project.id}_photos.csv", photosCsv)
        writeDocumentFile(context, projectFolder, "${project.id}_README.txt", readmeTxt)

        // 复制照片到SAF
        var copied = 0
        photos.forEach { photo ->
            val srcFile = File(photo.filePath)
            if (srcFile.exists()) {
                if (writeDocumentFileBytes(context, projectFolder, photo.filename, srcFile)) copied++
            }
        }

        return "导出完成: ${photos.size}张照片, $copied 已复制"
    }

    private fun writeExportFiles(dir: File, project: Project, photos: List<PhotoRecord>, timestamp: String) {
        FileWriter(File(dir, "${project.id}_manifest.json")).use { it.write(buildManifestJson(project, photos, timestamp)) }
        FileWriter(File(dir, "${project.id}_photos.csv")).use { it.write(buildPhotosCsv(project, photos, timestamp)) }
        FileWriter(File(dir, "${project.id}_README.txt")).use { it.write(buildReadmeTxt(project, photos, timestamp)) }
    }

    private fun writeDocumentFile(ctx: Context, folder: DocumentFile, name: String, content: String) {
        folder.findFile(name)?.delete()
        val newFile: DocumentFile? = folder.createFile("text/plain", name)
        if (newFile != null) {
            ctx.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }

    /** 将照片文件复制到导出目录的 photos/ 子文件夹 */
    private fun copyPhotoFiles(exportDir: File, photos: List<PhotoRecord>) {
        val photosDir = File(exportDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        photos.forEach { photo ->
            val src = File(photo.filePath)
            if (src.exists()) {
                val dest = File(photosDir, photo.filename)
                src.copyTo(dest, overwrite = true)
            }
        }
    }

    /** 写入二进制照片到SAF DocumentFile */
    private fun writeDocumentFileBytes(ctx: Context, folder: DocumentFile, name: String, src: File): Boolean {
        return try {
            folder.findFile(name)?.delete()
            val newFile = folder.createFile("image/jpeg", name)
            if (newFile != null) {
                ctx.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    src.inputStream().use { it.copyTo(out) }
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun buildManifestJson(project: Project, photos: List<PhotoRecord>, timestamp: String): String {
        val json = JSONObject()
        json.put("format_version", "1.0.0")
        json.put("export_timestamp", timestamp)
        json.put("app_name", "PoplarPheno-Collector")
        json.put("app_version", "5.0.0-pro")

        val projObj = JSONObject().apply {
            put("project_id", project.id)
            put("project_name", project.name)
            put("description", project.description)
            put("location", project.location)
            put("researcher", project.researcher)
            put("created_at", isoFormat.format(Date(project.createdAt)))
            put("camera_mode", project.cameraMode)
            put("camera_mode_label", CameraMode.fromCode(project.cameraMode).label)
            put("reference_type", project.referenceType)
            put("reference_type_label", ReferenceType.fromCode(project.referenceType).label)
            if (project.referenceSizeMm != null) put("reference_size_mm", project.referenceSizeMm.toDouble())
            put("photo_count", photos.size)
        }
        json.put("project", projObj)

        val photoArr = JSONArray()
        photos.forEach { photo ->
            val p = JSONObject().apply {
                put("index", photo.photoIndex)
                put("filename", photo.filename)
                put("file_path", photo.filePath)
                put("description", photo.description)
                put("timestamp", isoFormat.format(Date(photo.timestamp)))
                put("camera_mode", photo.cameraMode)
                photo.magnification?.let { put("magnification", it) }
                put("reference_type", photo.referenceType)
                photo.referenceSizeMm?.let { put("reference_size_mm", it.toDouble()) }
                if (photo.latitude != null && photo.longitude != null) {
                    put("gps", JSONObject().apply {
                        put("latitude", photo.latitude)
                        put("longitude", photo.longitude)
                        photo.altitude?.let { put("altitude", it) }
                        photo.accuracy?.let { put("accuracy", it.toDouble()) }
                    })
                }
            }
            photoArr.put(p)
        }
        json.put("photos", photoArr)
        json.put("server_meta", JSONObject().apply {
            put("batch_analysis_ready", true)
            put("encoding", "UTF-8")
        })
        return json.toString(2)
    }

    private fun buildPhotosCsv(project: Project, photos: List<PhotoRecord>, timestamp: String): String {
        val sb = StringBuilder()
        sb.append("index,filename,file_path,description,timestamp,mode,magnification,reference_type,reference_size_mm,latitude,longitude,altitude,accuracy\n")
        photos.forEach { p ->
            sb.append("${p.photoIndex},")
            sb.append("\"${p.filename}\",")
            sb.append("\"${p.filePath}\",")
            sb.append("\"${p.description.replace("\"", "\"\"")}\",")
            sb.append("${isoFormat.format(Date(p.timestamp))},")
            sb.append("${p.cameraMode},")
            sb.append("${p.magnification ?: ""},")
            sb.append("${p.referenceType},")
            sb.append("${p.referenceSizeMm ?: ""},")
            sb.append("${p.latitude ?: ""},")
            sb.append("${p.longitude ?: ""},")
            sb.append("${p.altitude ?: ""},")
            sb.append("${p.accuracy ?: ""}\n")
        }
        return sb.toString()
    }

    private fun buildReadmeTxt(project: Project, photos: List<PhotoRecord>, timestamp: String): String = buildString {
        append("============================================\n")
        append("  PoplarPheno-Collector 项目导出\n")
        append("============================================\n\n")
        append("项目编号: ${project.id}\n")
        append("项目名称: ${project.name}\n")
        append("拍摄模式: ${CameraMode.fromCode(project.cameraMode).label}\n")
        append("参照物: ${ReferenceType.fromCode(project.referenceType).label}\n")
        if (project.referenceSizeMm != null) append("参照物尺寸: ${project.referenceSizeMm} mm\n")
        append("地点: ${project.location}\n")
        append("研究人员: ${project.researcher}\n")
        append("照片数量: ${photos.size}\n")
        append("导出时间: $timestamp\n\n")
        append("文件结构:\n")
        append("  ${project.id}_manifest.json  - 完整元数据 (JSON)\n")
        append("  ${project.id}_photos.csv     - 照片清单 (CSV)\n")
        append("  ${project.id}_README.txt     - 本文件\n\n")
        append("命名规则: YYYYMMDD_MODE_PROJECTID_INDEX.jpg\n")
        append("============================================\n")
    }
}
