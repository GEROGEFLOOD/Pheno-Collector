package com.pheno.collector.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 项目实体 - 每个采集项目对应一个文件夹
 * 标准化JSON格式，方便后续服务器批量分析
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString().take(8).uppercase(), // PRJ001 格式
    val name: String,           // 项目名称
    val description: String,    // 项目说明
    val location: String,       // 采集地点描述
    val researcher: String,     // 研究人员
    val createdAt: Long = System.currentTimeMillis(),
    val cameraMode: String = "CLOSEUP",     // 拍摄模式
    val referenceType: String = "NONE",     // 参照物类型
    val referenceSizeMm: Float? = null,     // 参照物尺寸(mm)
    val isActive: Boolean = true,           // 是否当前活跃项目
    val folderPath: String                  // 项目文件夹路径
)

/**
 * 拍摄模式枚举
 */
enum class CameraMode(val code: String, val icon: String, val label: String, val desc: String) {
    CLOSEUP("CLOSEUP", "🔍", "近照模式", "组培/盆栽/叶片近距拍摄"),
    DISTANCE("DISTANCE", "🌳", "远照模式", "成体苗/野外树木远距拍摄"),
    MICROSCOPE("MICROSCOPE", "🔬", "显微镜模式", "支持手动输入放大倍率");

    companion object {
        fun fromCode(code: String): CameraMode = entries.find { it.code == code } ?: CLOSEUP
    }
}

/**
 * 参照物类型枚举
 */
enum class ReferenceType(val code: String, val label: String, val defaultSizeMm: Float?) {
    NONE("NONE", "无", null),
    RULER("RULER", "标尺", null),
    COIN_1YUAN("COIN_1YUAN", "1元硬币", 25.0f),
    COIN_5JIAO("COIN_5JIAO", "5角硬币", 20.5f),
    A4_PAPER("A4_PAPER", "A4纸", null),
    COLOR_CARD("COLOR_CARD", "比色卡", null),
    CUSTOM("CUSTOM", "自定义", null);

    companion object {
        fun fromCode(code: String): ReferenceType = entries.find { it.code == code } ?: NONE
    }
}
