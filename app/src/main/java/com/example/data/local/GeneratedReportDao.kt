package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GeneratedReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedReportDao {
    @Query("SELECT * FROM generated_reports WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReportsForUser(userId: String): Flow<List<GeneratedReportEntity>>

    @Query("SELECT * FROM generated_reports WHERE isPublic = 1 ORDER BY createdAt DESC")
    fun getPublicReports(): Flow<List<GeneratedReportEntity>>

    @Query("SELECT * FROM generated_reports WHERE userId = :userId OR isPublic = 1 ORDER BY createdAt DESC")
    fun getReportsForUserOrPublic(userId: String): Flow<List<GeneratedReportEntity>>

    @Query("SELECT * FROM generated_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<GeneratedReportEntity>>

    @Query("SELECT * FROM generated_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): GeneratedReportEntity?

    @Query("SELECT COUNT(*) FROM generated_reports")
    suspend fun getReportCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: GeneratedReportEntity)

    @Update
    suspend fun updateReport(report: GeneratedReportEntity)

    @Query("UPDATE generated_reports SET isPublic = :isPublic WHERE id = :id")
    suspend fun updateReportVisibility(id: String, isPublic: Boolean)

    @Query("UPDATE generated_reports SET isStale = 1 WHERE projectId = :projectId")
    suspend fun markReportsStaleForProject(projectId: String)

    @Query("DELETE FROM generated_reports WHERE id = :id")
    suspend fun deleteReport(id: String)
}
