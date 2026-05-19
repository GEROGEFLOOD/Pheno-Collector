package com.pheno.collector.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 照片记录实体 - 每张照片的完整元数据
 * 与项目关联，支持标准化导出
 */
@Entity(
    tableName = "photo_records",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class PhotoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: String,          // 关联项目ID
    val filename: String,           // 标准化文件名: 日期_模式_项目号_编号.jpg
    val filePath: String,           // 完整文件路径
    val description: String = "",   // 照片描述（可选）
    val photoIndex: Int,            // 项目内编号 (001, 002...)
    val timestamp: Long = System.currentTimeMillis(),

    // GPS信息
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,

    // 拍摄参数
    val cameraMode: String,         // 拍摄模式代码
    val magnification: Int? = null, // 显微镜倍率
    val referenceType: String,      // 参照物类型代码
    val referenceSizeMm: Float? = null, // 参照物尺寸

    // 设备信息
    val deviceModel: String = "",
    val androidVersion: String = ""
)

/**
 * 项目完整导出数据（用于生成JSON）
 */
data class ProjectExport(
    val project: Project,
    val photos: List<PhotoRecord>,
    val exportTime: Long = System.currentTimeMillis()
)
