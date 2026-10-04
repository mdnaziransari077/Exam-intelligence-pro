package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun getProjectsForOwner(ownerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE visibility = 'PUBLIC' ORDER BY updatedAt DESC")
    fun getPublicProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE ownerId = :ownerId OR visibility = 'PUBLIC' ORDER BY updatedAt DESC")
    fun getProjectsForUserOrPublic(ownerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :ownerId")
    suspend fun getProjectCountForOwner(ownerId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateUserProject(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status, documentCount = :docCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectStats(id: String, status: String, docCount: Int, updatedAt: Long)

    @Query("UPDATE projects SET visibility = :visibility, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectVisibility(id: String, visibility: String, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}
