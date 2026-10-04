package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AiExamEstimateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiExamEstimateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: AiExamEstimateEntity)

    @Query("SELECT * FROM ai_exam_estimates WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestEstimateForProject(projectId: String): Flow<AiExamEstimateEntity?>

    @Query("SELECT * FROM ai_exam_estimates WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestEstimate(projectId: String): AiExamEstimateEntity?

    @Query("SELECT * FROM ai_exam_estimates ORDER BY createdAt DESC")
    fun getAllEstimates(): Flow<List<AiExamEstimateEntity>>

    @Query("SELECT COUNT(*) FROM ai_exam_estimates")
    suspend fun getEstimateCount(): Int

    @Query("SELECT * FROM ai_exam_estimates WHERE id = :id LIMIT 1")
    suspend fun getEstimateById(id: String): AiExamEstimateEntity?

    @Query("DELETE FROM ai_exam_estimates WHERE projectId = :projectId")
    suspend fun deleteEstimatesForProject(projectId: String)
}
