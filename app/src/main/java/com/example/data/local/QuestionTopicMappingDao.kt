package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MappingReviewLogEntity
import com.example.data.model.QuestionTopicMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionTopicMappingDao {

    @Query("SELECT * FROM question_topic_mappings WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getMappingsForProject(projectId: String): Flow<List<QuestionTopicMappingEntity>>

    @Query("SELECT * FROM question_topic_mappings WHERE projectId = :projectId")
    suspend fun getMappingsListForProject(projectId: String): List<QuestionTopicMappingEntity>

    @Query("SELECT * FROM question_topic_mappings WHERE sourceDocumentId = :documentId")
    fun getMappingsForDocument(documentId: String): Flow<List<QuestionTopicMappingEntity>>

    @Query("SELECT * FROM question_topic_mappings WHERE sourceDocumentId = :documentId")
    suspend fun getMappingsListForDocument(documentId: String): List<QuestionTopicMappingEntity>

    @Query("SELECT * FROM question_topic_mappings WHERE questionId = :questionId")
    fun getMappingsForQuestion(questionId: String): Flow<List<QuestionTopicMappingEntity>>

    @Query("SELECT * FROM question_topic_mappings WHERE questionId = :questionId")
    suspend fun getMappingsListForQuestion(questionId: String): List<QuestionTopicMappingEntity>

    @Query("SELECT * FROM question_topic_mappings WHERE id = :id LIMIT 1")
    suspend fun getMappingById(id: String): QuestionTopicMappingEntity?

    @Query("SELECT COUNT(*) FROM question_topic_mappings WHERE projectId = :projectId")
    suspend fun getMappingCountForProject(projectId: String): Int

    @Query("SELECT * FROM question_topic_mappings ORDER BY createdAt DESC")
    fun getAllMappings(): Flow<List<QuestionTopicMappingEntity>>

    @Query("SELECT COUNT(*) FROM question_topic_mappings")
    suspend fun getTotalMappingCount(): Int

    @Query("SELECT COUNT(*) FROM question_topic_mappings WHERE reviewStatus = 'APPROVED'")
    suspend fun getApprovedMappingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: QuestionTopicMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<QuestionTopicMappingEntity>)

    @Update
    suspend fun updateMapping(mapping: QuestionTopicMappingEntity)

    @Query("DELETE FROM question_topic_mappings WHERE id = :id")
    suspend fun deleteMapping(id: String)

    @Query("DELETE FROM question_topic_mappings WHERE questionId = :questionId")
    suspend fun deleteMappingsForQuestion(questionId: String)

    @Query("DELETE FROM question_topic_mappings WHERE projectId = :projectId")
    suspend fun deleteMappingsForProject(projectId: String)

    @Query("DELETE FROM question_topic_mappings WHERE sourceDocumentId = :documentId")
    suspend fun deleteMappingsForDocument(documentId: String)

    @Query("SELECT * FROM question_topic_mappings WHERE syllabusVersionId = :versionId")
    suspend fun getMappingsListForVersion(versionId: String): List<QuestionTopicMappingEntity>

    // --- Review Logs ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: MappingReviewLogEntity)

    @Query("SELECT * FROM mapping_review_logs WHERE questionId = :questionId ORDER BY timestamp DESC")
    fun getReviewLogsForQuestion(questionId: String): Flow<List<MappingReviewLogEntity>>

    @Query("SELECT * FROM mapping_review_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentReviewLogs(): Flow<List<MappingReviewLogEntity>>
}
