package com.pheno.collector.data.local

import androidx.room.*
import com.pheno.collector.data.model.PhotoRecord
import kotlinx.coroutines.flow.Flow

/**
 * 照片记录数据访问对象
 */
@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_records WHERE projectId = :projectId ORDER BY photoIndex ASC")
    fun getPhotosByProject(projectId: String): Flow<List<PhotoRecord>>

    @Query("SELECT * FROM photo_records WHERE projectId = :projectId ORDER BY photoIndex ASC")
    suspend fun getPhotosByProjectSync(projectId: String): List<PhotoRecord>

    @Query("SELECT * FROM photo_records WHERE id = :id")
    suspend fun getPhotoById(id: Long): PhotoRecord?

    @Query("SELECT COUNT(*) FROM photo_records WHERE projectId = :projectId")
    suspend fun getPhotoCountByProject(projectId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoRecord): Long

    @Update
    suspend fun updatePhoto(photo: PhotoRecord)

    @Query("UPDATE photo_records SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String)

    @Delete
    suspend fun deletePhoto(photo: PhotoRecord)

    @Query("DELETE FROM photo_records WHERE projectId = :projectId")
    suspend fun deletePhotosByProject(projectId: String)
}
