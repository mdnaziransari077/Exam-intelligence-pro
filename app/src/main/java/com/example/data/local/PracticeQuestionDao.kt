package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AiReviewLogEntity
import com.example.data.model.PracticeAttemptEntity
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.PracticeQuestionSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: PracticeQuestionSetEntity)

    @Update
    suspend fun updateSet(set: PracticeQuestionSetEntity)

    @Query("SELECT * FROM practice_question_sets WHERE id = :setId LIMIT 1")
    suspend fun getSetById(setId: String): PracticeQuestionSetEntity?

    @Query("SELECT * FROM practice_question_sets WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getSetsForProject(projectId: String): Flow<List<PracticeQuestionSetEntity>>

    @Query("SELECT * FROM practice_question_sets WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getSetsListForProject(projectId: String): List<PracticeQuestionSetEntity>

    @Query("SELECT * FROM practice_question_sets WHERE userId = :userId AND isSaved = 1 ORDER BY createdAt DESC")
    fun getSavedSetsForUser(userId: String): Flow<List<PracticeQuestionSetEntity>>

    @Query("UPDATE practice_question_sets SET isSaved = :isSaved WHERE id = :setId")
    suspend fun updateSetSavedStatus(setId: String, isSaved: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<PracticeQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: PracticeQuestionEntity)

    @Update
    suspend fun updateQuestion(question: PracticeQuestionEntity)

    @Query("SELECT * FROM practice_questions WHERE setId = :setId ORDER BY questionNumber ASC")
    fun getQuestionsForSet(setId: String): Flow<List<PracticeQuestionEntity>>

    @Query("SELECT * FROM practice_questions WHERE setId = :setId ORDER BY questionNumber ASC")
    suspend fun getQuestionsListForSet(setId: String): List<PracticeQuestionEntity>

    @Query("SELECT * FROM practice_questions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getAllQuestionsForProject(projectId: String): Flow<List<PracticeQuestionEntity>>

    @Query("SELECT * FROM practice_questions ORDER BY createdAt DESC")
    fun getAllGeneratedQuestions(): Flow<List<PracticeQuestionEntity>>

    @Query("SELECT * FROM practice_questions WHERE id = :questionId LIMIT 1")
    suspend fun getQuestionById(questionId: String): PracticeQuestionEntity?

    @Query("UPDATE practice_questions SET reviewStatus = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, reviewNotes = :notes WHERE id = :questionId")
    suspend fun updateReviewStatus(questionId: String, status: String, reviewedBy: String?, reviewedAt: Long?, notes: String?)

    @Query("UPDATE practice_questions SET userCorrectedText = :correctedText, marks = :marks, reviewStatus = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, reviewNotes = :notes WHERE id = :questionId")
    suspend fun updateQuestionContent(questionId: String, correctedText: String, marks: Int?, status: String, reviewedBy: String?, reviewedAt: Long?, notes: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: PracticeAttemptEntity)

    @Query("SELECT * FROM practice_attempts WHERE setId = :setId AND userId = :userId")
    fun getAttemptsForSet(setId: String, userId: String): Flow<List<PracticeAttemptEntity>>

    @Query("SELECT * FROM practice_attempts WHERE setId = :setId AND userId = :userId")
    suspend fun getAttemptsListForSet(setId: String, userId: String): List<PracticeAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: AiReviewLogEntity)

    @Query("SELECT * FROM ai_review_logs WHERE questionId = :questionId ORDER BY timestamp DESC")
    fun getReviewLogsForQuestion(questionId: String): Flow<List<AiReviewLogEntity>>

    @Query("DELETE FROM practice_question_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query("DELETE FROM practice_questions WHERE setId = :setId")
    suspend fun deleteQuestionsForSet(setId: String)
}
