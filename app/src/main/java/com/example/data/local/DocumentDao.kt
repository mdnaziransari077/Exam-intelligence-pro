package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE projectId = :projectId ORDER BY uploadTimestamp DESC")
    fun getDocumentsForProject(projectId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE projectId = :projectId ORDER BY uploadTimestamp DESC")
    suspend fun getDocumentsListForProject(projectId: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE ownerId = :ownerId ORDER BY uploadTimestamp DESC")
    fun getDocumentsForOwner(ownerId: String): Flow<List<DocumentEntity>>

    @Query("SELECT d.* FROM documents d INNER JOIN projects p ON d.projectId = p.id WHERE p.visibility = 'PUBLIC' ORDER BY d.uploadTimestamp DESC")
    fun getDocumentsForPublicProjects(): Flow<List<DocumentEntity>>

    @Query("SELECT d.* FROM documents d INNER JOIN projects p ON d.projectId = p.id WHERE d.ownerId = :ownerId OR p.visibility = 'PUBLIC' ORDER BY d.uploadTimestamp DESC")
    fun getDocumentsForUserOrPublic(ownerId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY uploadTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents WHERE projectId = :projectId")
    suspend fun getDocumentCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET processingStatus = :status, errorDetails = :errorDetails, processingStartedAt = :startedAt, processingCompletedAt = :completedAt WHERE id = :id")
    suspend fun updateProcessingStatus(id: String, status: String, errorDetails: String?, startedAt: Long?, completedAt: Long?)

    @Query("UPDATE documents SET processingStatus = :status, questionCount = :questionCount, verifiedCount = :verifiedCount, extractedText = :extractedText, extractionMethod = :method, processingCompletedAt = :completedAt WHERE id = :id")
    suspend fun updateExtractionResults(id: String, status: String, questionCount: Int, verifiedCount: Int, extractedText: String?, method: String, completedAt: Long)

    @Query("UPDATE documents SET verifiedCount = :verifiedCount, processingStatus = :status WHERE id = :id")
    suspend fun updateVerificationCounts(id: String, verifiedCount: Int, status: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM documents WHERE projectId = :projectId")
    suspend fun deleteDocumentsForProject(projectId: String)
}
