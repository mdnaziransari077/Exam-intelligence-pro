package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {

    @Query("SELECT * FROM syllabus_versions ORDER BY academicSession DESC, subject ASC")
    fun getAllSyllabusVersions(): Flow<List<SyllabusVersionEntity>>

    @Query("SELECT * FROM syllabus_versions WHERE subject = :subject AND academicSession = :session LIMIT 1")
    suspend fun getVersionForSubjectAndSession(subject: String, session: String): SyllabusVersionEntity?

    @Query("SELECT * FROM syllabus_versions WHERE subject = :subject ORDER BY academicSession DESC")
    fun getVersionsForSubject(subject: String): Flow<List<SyllabusVersionEntity>>

    @Query("SELECT * FROM syllabus_versions WHERE id = :id LIMIT 1")
    suspend fun getVersionById(id: String): SyllabusVersionEntity?

    @Query("SELECT COUNT(*) FROM syllabus_versions")
    suspend fun getVersionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: SyllabusVersionEntity)

    @Update
    suspend fun updateVersion(version: SyllabusVersionEntity)

    @Query("UPDATE syllabus_versions SET verificationStatus = :status, verifiedBy = :verifiedBy, verifiedAt = :verifiedAt, reviewerNotes = :notes, updatedAt = :verifiedAt WHERE id = :id")
    suspend fun updateVersionVerification(id: String, status: String, verifiedBy: String, verifiedAt: Long, notes: String?)

    // --- Syllabus Nodes ---

    @Query("SELECT * FROM syllabus_nodes WHERE syllabusVersionId = :versionId ORDER BY displayOrder ASC, id ASC")
    fun getNodesForVersion(versionId: String): Flow<List<SyllabusNodeEntity>>

    @Query("SELECT * FROM syllabus_nodes WHERE syllabusVersionId = :versionId ORDER BY displayOrder ASC, id ASC")
    suspend fun getNodesListForVersion(versionId: String): List<SyllabusNodeEntity>

    @Query("SELECT * FROM syllabus_nodes WHERE syllabusVersionId = :versionId ORDER BY displayOrder ASC, id ASC")
    suspend fun getNodesForVersionList(versionId: String): List<SyllabusNodeEntity>

    @Query("SELECT * FROM syllabus_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeById(id: String): SyllabusNodeEntity?

    @Query("SELECT * FROM syllabus_nodes WHERE parentId = :parentId ORDER BY displayOrder ASC")
    suspend fun getChildNodes(parentId: String): List<SyllabusNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: SyllabusNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<SyllabusNodeEntity>)

    @Update
    suspend fun updateNode(node: SyllabusNodeEntity)

    @Query("UPDATE syllabus_nodes SET parentId = :newParentId, displayOrder = :newOrder, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateNodeParent(id: String, newParentId: String?, newOrder: Int, timestamp: Long)

    @Query("UPDATE syllabus_nodes SET isExcluded = :isExcluded, exclusionReason = :reason, updatedAt = :timestamp WHERE id = :id")
    suspend fun setNodeExclusion(id: String, isExcluded: Boolean, reason: String?, timestamp: Long)

    @Query("DELETE FROM syllabus_nodes WHERE id = :id")
    suspend fun deleteNode(id: String)

    @Query("DELETE FROM syllabus_nodes WHERE parentId = :parentId")
    suspend fun deleteChildNodes(parentId: String)

    @Query("DELETE FROM syllabus_nodes WHERE syllabusVersionId = :versionId")
    suspend fun deleteNodesForVersion(versionId: String)
}
