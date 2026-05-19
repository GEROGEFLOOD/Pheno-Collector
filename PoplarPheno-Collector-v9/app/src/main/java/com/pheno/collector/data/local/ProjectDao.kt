package com.pheno.collector.data.local

import androidx.room.*
import com.pheno.collector.data.model.PhotoRecord
import com.pheno.collector.data.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * 项目数据访问对象
 */
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProject(): Project?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): Project?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Query("UPDATE projects SET isActive = 0")
    suspend fun deactivateAllProjects()

    @Query("UPDATE projects SET isActive = 1 WHERE id = :projectId")
    suspend fun activateProject(projectId: String)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)
}
