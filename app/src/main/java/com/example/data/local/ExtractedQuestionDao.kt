package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExtractedQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtractedQuestionDao {

    @Query("SELECT * FROM extracted_questions WHERE sourceDocumentId = :documentId ORDER BY sourcePage ASC, rowid ASC")
    fun getQuestionsForDocument(documentId: String): Flow<List<ExtractedQuestionEntity>>

    @Query("SELECT * FROM extracted_questions WHERE sourceDocumentId = :documentId ORDER BY sourcePage ASC, rowid ASC")
    suspend fun getQuestionsListForDocument(documentId: String): List<ExtractedQuestionEntity>

    @Query("SELECT * FROM extracted_questions WHERE projectId = :projectId ORDER BY sourcePage ASC, rowid ASC")
    fun getQuestionsForProject(projectId: String): Flow<List<ExtractedQuestionEntity>>

    @Query("SELECT * FROM extracted_questions WHERE projectId = :projectId ORDER BY sourcePage ASC, rowid ASC")
    suspend fun getQuestionsForProjectList(projectId: String): List<ExtractedQuestionEntity>

    @Query("SELECT * FROM extracted_questions ORDER BY updatedAt DESC")
    fun getAllQuestions(): Flow<List<ExtractedQuestionEntity>>

    @Query("SELECT * FROM extracted_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): ExtractedQuestionEntity?

    @Query("SELECT COUNT(*) FROM extracted_questions WHERE sourceDocumentId = :documentId")
    suspend fun getQuestionCountForDocument(documentId: String): Int

    @Query("SELECT COUNT(*) FROM extracted_questions WHERE sourceDocumentId = :documentId AND verificationStatus = :status")
    suspend fun getCountByStatusForDocument(documentId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM extracted_questions WHERE verificationStatus = 'APPROVED'")
    suspend fun getVerifiedQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM extracted_questions")
    suspend fun getTotalQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: ExtractedQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<ExtractedQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: ExtractedQuestionEntity)

    @Query("UPDATE extracted_questions SET verificationStatus = :status, reviewedBy = :reviewer, reviewedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateVerificationStatus(id: String, status: String, reviewer: String, timestamp: Long)

    @Query("UPDATE extracted_questions SET verificationStatus = :status, reviewedBy = :reviewer, reviewedAt = :timestamp, updatedAt = :timestamp WHERE id IN (:ids)")
    suspend fun bulkUpdateVerificationStatus(ids: List<String>, status: String, reviewer: String, timestamp: Long)

    @Query("DELETE FROM extracted_questions WHERE id = :id")
    suspend fun deleteQuestion(id: String)

    @Query("DELETE FROM extracted_questions WHERE sourceDocumentId = :documentId")
    suspend fun deleteQuestionsForDocument(documentId: String)
}
