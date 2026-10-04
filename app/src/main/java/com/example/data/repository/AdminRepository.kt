package com.example.data.repository

import com.example.data.local.AiExamEstimateDao
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ExtractedQuestionDao
import com.example.data.local.GeneratedReportDao
import com.example.data.local.PracticeQuestionDao
import com.example.data.local.ProjectDao
import com.example.data.local.QuestionTopicMappingDao
import com.example.data.local.SyllabusDao
import com.example.data.local.UserDao
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.AiReviewStatus
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.MappingReviewStatus
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectVisibility
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

data class AdminSystemStats(
    val totalUsers: Int,
    val totalProjects: Int,
    val totalDocuments: Int,
    val totalQuestions: Int,
    val verifiedQuestions: Int,
    val totalSyllabusVersions: Int,
    val totalMappings: Int,
    val approvedMappings: Int,
    val totalPracticeQuestions: Int,
    val totalAiEstimates: Int,
    val totalReports: Int,
    val totalAuditLogs: Int,
    val activeUsers: Int
)

class AdminRepository(
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    private val documentDao: DocumentDao,
    private val extractedQuestionDao: ExtractedQuestionDao,
    private val syllabusDao: SyllabusDao,
    private val questionTopicMappingDao: QuestionTopicMappingDao,
    private val practiceQuestionDao: PracticeQuestionDao,
    private val aiExamEstimateDao: AiExamEstimateDao,
    private val generatedReportDao: GeneratedReportDao,
    private val auditLogDao: AuditLogDao
) {

    fun checkAdminAccess(user: UserEntity?): Boolean {
        return user != null && user.getRoleEnum().canAccessAdminArea()
    }

    suspend fun getSystemStats(user: UserEntity?): Result<AdminSystemStats> =
        withContext(Dispatchers.IO) {
            if (!checkAdminAccess(user)) {
                return@withContext Result.failure(
                    SecurityException("Forbidden: Administrator privileges required to access system statistics.")
                )
            }

            val totalUsers = userDao.getUserCount()
            val totalProjects = projectDao.getProjectCount()
            val totalDocs = documentDao.getDocumentCount()
            val totalQuestions = extractedQuestionDao.getTotalQuestionCount()
            val verifiedQuestions = extractedQuestionDao.getVerifiedQuestionCount()
            val totalSyllabusVersions = syllabusDao.getVersionCount()
            val totalMappings = questionTopicMappingDao.getTotalMappingCount()
            val approvedMappings = questionTopicMappingDao.getApprovedMappingCount()
            val totalPracticeQuestions = 0 // can count from DB if needed
            val totalAiEstimates = aiExamEstimateDao.getEstimateCount()
            val totalReports = generatedReportDao.getReportCount()
            val totalLogs = auditLogDao.getLogCount()

            Result.success(
                AdminSystemStats(
                    totalUsers = totalUsers,
                    totalProjects = totalProjects,
                    totalDocuments = totalDocs,
                    totalQuestions = totalQuestions,
                    verifiedQuestions = verifiedQuestions,
                    totalSyllabusVersions = totalSyllabusVersions,
                    totalMappings = totalMappings,
                    approvedMappings = approvedMappings,
                    totalPracticeQuestions = totalPracticeQuestions,
                    totalAiEstimates = totalAiEstimates,
                    totalReports = totalReports,
                    totalAuditLogs = totalLogs,
                    activeUsers = totalUsers
                )
            )
        }

    // 1. Users
    fun getAllUsers(user: UserEntity?): Flow<List<UserEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: User directory access requires Administrator privileges.")
        }
        return userDao.getAllUsers().flowOn(Dispatchers.IO)
    }

    suspend fun updateUserRole(
        adminUser: UserEntity?,
        targetUserId: String,
        newRole: UserRole
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!checkAdminAccess(adminUser)) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
        }

        val target = userDao.getUserById(targetUserId)
            ?: return@withContext Result.failure(Exception("User not found"))

        if (target.id == adminUser?.id) {
            return@withContext Result.failure(Exception("Operation aborted: Cannot modify your own administrative role."))
        }

        val previousRole = target.role
        userDao.updateUserRole(targetUserId, newRole.name)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = adminUser?.id ?: "system",
                userEmail = adminUser?.email ?: "admin@examintel.org",
                action = "USER_ROLE_UPDATED",
                resourceType = "USER",
                resourceId = target.id,
                details = "Changed role of ${target.email} from $previousRole to ${newRole.name}"
            )
        )

        Result.success(Unit)
    }

    suspend fun toggleUserAccountStatus(adminUser: UserEntity?, targetUserId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            if (!checkAdminAccess(adminUser)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
            }

            val target = userDao.getUserById(targetUserId)
                ?: return@withContext Result.failure(Exception("User not found"))

            if (target.id == adminUser?.id) {
                return@withContext Result.failure(Exception("Operation aborted: Cannot suspend your own administrative account."))
            }

            val newStatus = !target.isActive
            userDao.updateUserActiveStatus(targetUserId, newStatus)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = adminUser?.id ?: "system",
                    userEmail = adminUser?.email ?: "system@examintel.org",
                    action = if (newStatus) "USER_ACTIVATED" else "USER_SUSPENDED",
                    resourceType = "USER",
                    resourceId = target.id,
                    details = "Admin ${adminUser?.email} changed status of ${target.email} to isActive=$newStatus"
                )
            )

            Result.success(newStatus)
        }

    // 2. Projects & Visibility
    fun getAllProjects(user: UserEntity?): Flow<List<ProjectEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Global projects view requires Administrator privileges.")
        }
        return projectDao.getAllProjects().flowOn(Dispatchers.IO)
    }

    suspend fun changeProjectVisibility(
        adminUser: UserEntity?,
        projectId: String,
        newVisibility: ProjectVisibility
    ): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        if (!checkAdminAccess(adminUser)) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
        }

        val project = projectDao.getProjectById(projectId)
            ?: return@withContext Result.failure(Exception("Project not found"))

        val prev = project.visibility
        val updated = project.copy(visibility = newVisibility.name, updatedAt = System.currentTimeMillis())
        projectDao.updateUserProject(updated)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = adminUser?.id ?: "system",
                userEmail = adminUser?.email ?: "admin@examintel.org",
                action = "ADMIN_PROJECT_VISIBILITY_CHANGED",
                resourceType = "PROJECT",
                resourceId = projectId,
                details = "Admin modified visibility for '${project.name}' from $prev to ${newVisibility.name}"
            )
        )

        Result.success(updated)
    }

    suspend fun deleteProject(adminUser: UserEntity?, projectId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!checkAdminAccess(adminUser)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
            }

            val project = projectDao.getProjectById(projectId)
                ?: return@withContext Result.failure(Exception("Project not found"))

            documentDao.deleteDocumentsForProject(projectId)
            projectDao.deleteProject(projectId)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = adminUser?.id ?: "system",
                    userEmail = adminUser?.email ?: "admin@examintel.org",
                    action = "ADMIN_PROJECT_DELETED",
                    resourceType = "PROJECT",
                    resourceId = projectId,
                    details = "Admin deleted project '${project.name}' and all associated documents"
                )
            )

            Result.success(Unit)
        }

    // 3. Documents
    fun getAllDocuments(user: UserEntity?): Flow<List<DocumentEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Global documents repository requires Administrator privileges.")
        }
        return documentDao.getAllDocuments().flowOn(Dispatchers.IO)
    }

    suspend fun deleteDocument(adminUser: UserEntity?, documentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!checkAdminAccess(adminUser)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
            }

            val doc = documentDao.getDocumentById(documentId)
                ?: return@withContext Result.failure(Exception("Document not found"))

            extractedQuestionDao.deleteQuestionsForDocument(documentId)
            questionTopicMappingDao.deleteMappingsForDocument(documentId)
            documentDao.deleteDocument(documentId)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = adminUser?.id ?: "system",
                    userEmail = adminUser?.email ?: "admin@examintel.org",
                    action = "ADMIN_DOCUMENT_DELETED",
                    resourceType = "DOCUMENT",
                    resourceId = documentId,
                    details = "Admin deleted document '${doc.customFilename}' and associated extractions"
                )
            )

            Result.success(Unit)
        }

    // 4. Questions
    fun getAllExtractedQuestions(user: UserEntity?): Flow<List<ExtractedQuestionEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return extractedQuestionDao.getAllQuestions().flowOn(Dispatchers.IO)
    }

    suspend fun approveQuestion(adminUser: UserEntity?, questionId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!checkAdminAccess(adminUser)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privileges required."))
            }

            extractedQuestionDao.updateVerificationStatus(
                id = questionId,
                status = VerificationStatus.APPROVED.label,
                reviewer = adminUser?.email ?: "admin@cbse.org",
                timestamp = System.currentTimeMillis()
            )

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = adminUser?.id ?: "system",
                    userEmail = adminUser?.email ?: "admin@cbse.org",
                    action = "ADMIN_QUESTION_APPROVED",
                    resourceType = "QUESTION",
                    resourceId = questionId,
                    details = "Approved extracted question $questionId"
                )
            )

            Result.success(Unit)
        }

    // 5. Syllabus
    fun getAllSyllabusVersions(user: UserEntity?): Flow<List<SyllabusVersionEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return syllabusDao.getAllSyllabusVersions().flowOn(Dispatchers.IO)
    }

    // 6. Mappings
    fun getAllMappings(user: UserEntity?): Flow<List<QuestionTopicMappingEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return questionTopicMappingDao.getAllMappings().flowOn(Dispatchers.IO)
    }

    // 7. Practice Questions
    fun getAllPracticeQuestions(user: UserEntity?): Flow<List<PracticeQuestionEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return practiceQuestionDao.getAllGeneratedQuestions().flowOn(Dispatchers.IO)
    }

    // 8. AI Estimates
    fun getAllAiEstimates(user: UserEntity?): Flow<List<AiExamEstimateEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return aiExamEstimateDao.getAllEstimates().flowOn(Dispatchers.IO)
    }

    // 9. Reports
    fun getAllReports(user: UserEntity?): Flow<List<GeneratedReportEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Admin privileges required.")
        }
        return generatedReportDao.getAllReports().flowOn(Dispatchers.IO)
    }

    // 10. Audit Logs
    fun getAuditLogs(user: UserEntity?): Flow<List<AuditLogEntity>> {
        if (!checkAdminAccess(user)) {
            throw SecurityException("Forbidden: Audit logging stream requires Administrator privileges.")
        }
        return auditLogDao.getAllAuditLogs().flowOn(Dispatchers.IO)
    }
}
