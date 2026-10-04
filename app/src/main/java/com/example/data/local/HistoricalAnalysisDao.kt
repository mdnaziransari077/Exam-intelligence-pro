package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HistoricalAnalysisRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricalAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: HistoricalAnalysisRunEntity)

    @Update
    suspend fun updateRun(run: HistoricalAnalysisRunEntity)

    @Query("SELECT * FROM historical_analysis_runs WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestRunForProject(projectId: String): Flow<HistoricalAnalysisRunEntity?>

    @Query("SELECT * FROM historical_analysis_runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: String): HistoricalAnalysisRunEntity?

    @Query("UPDATE historical_analysis_runs SET status = 'STALE', updatedAt = :timestamp WHERE projectId = :projectId")
    suspend fun markRunsStaleForProject(projectId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM historical_analysis_runs WHERE projectId = :projectId")
    suspend fun deleteRunsForProject(projectId: String)

    @Query("SELECT COUNT(*) FROM historical_analysis_runs WHERE projectId = :projectId")
    suspend fun getRunCountForProject(projectId: String): Int
}
