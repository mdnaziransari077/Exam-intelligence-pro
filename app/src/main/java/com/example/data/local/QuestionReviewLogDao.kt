package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QuestionReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionReviewLogDao {

    @Query("SELECT * FROM question_review_logs WHERE questionId = :questionId ORDER BY timestamp DESC")
    fun getLogsForQuestion(questionId: String): Flow<List<QuestionReviewLogEntity>>

    @Query("SELECT * FROM question_review_logs WHERE sourceDocumentId = :documentId ORDER BY timestamp DESC LIMIT 50")
    fun getLogsForDocument(documentId: String): Flow<List<QuestionReviewLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: QuestionReviewLogEntity)
}
