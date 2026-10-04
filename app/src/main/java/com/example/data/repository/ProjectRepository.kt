package com.example.data.repository

import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ProjectDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectVisibility
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val documentDao: DocumentDao,
    private val auditLogDao: AuditLogDao
) {

    fun getProjectsForUser(user: UserEntity?): Flow<List<ProjectEntity>> {
        val flow = if (user != null && (user.getRoleEnum() == UserRole.ADMIN || user.getRoleEnum() == UserRole.SUPER_ADMIN)) {
            projectDao.getAllProjects()
        } else if (user != null) {
            projectDao.getProjectsForUserOrPublic(user.id)
        } else {
            // Guest sees explicitly public projects only
            projectDao.getPublicProjects()
        }
        return flow.flowOn(Dispatchers.IO)
    }

    suspend fun changeProjectVisibility(
        user: UserEntity,
        projectId: String,
        newVisibility: ProjectVisibility
    ): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(projectId)
            ?: return@withContext Result.failure(Exception("Project not found"))

        val isOwner = existing.ownerId == user.id
        val isAdmin = user.getRoleEnum().canManageAllProjects()
        if (!isOwner && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized: Only the project owner or an administrator can modify project visibility."))
        }

        val previousVisibility = existing.visibility
        val updated = existing.copy(
            visibility = newVisibility.name,
            updatedAt = System.currentTimeMillis()
        )

        projectDao.updateUserProject(updated)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "PROJECT_VISIBILITY_CHANGED",
                resourceType = "PROJECT",
                resourceId = updated.id,
                details = "Changed visibility for '${updated.name}' from $previousVisibility to ${newVisibility.name}"
            )
        )

        Result.success(updated)
    }

    suspend fun getProjectById(projectId: String, user: UserEntity?): Result<ProjectEntity> =
        withContext(Dispatchers.IO) {
            val project = projectDao.getProjectById(projectId)
                ?: return@withContext Result.failure(Exception("Project not found"))

            // Enforce private project authorization
            val isOwner = user != null && project.ownerId == user.id
            val isAdmin = user != null && user.getRoleEnum().canManageAllProjects()
            val isPublic = project.visibility == ProjectVisibility.PUBLIC.name

            if (!isOwner && !isAdmin && !isPublic) {
                return@withContext Result.failure(
                    SecurityException("Access Denied: This project is private to its creator.")
                )
            }

            Result.success(project)
        }

    suspend fun createProject(
        user: UserEntity,
        name: String,
        subject: String,
        academicYear: String,
        description: String,
        visibility: ProjectVisibility
    ): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext Result.failure(Exception("Project title is required"))
        }

        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            ownerId = user.id,
            ownerName = user.fullName,
            name = name.trim(),
            board = "CBSE",
            className = "Class 10",
            subject = subject,
            academicYear = academicYear,
            description = description.trim(),
            visibility = visibility.name,
            status = ProjectStatus.SETUP.label,
            documentCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        projectDao.insertProject(project)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "PROJECT_CREATED",
                resourceType = "PROJECT",
                resourceId = project.id,
                details = "Created project '${project.name}' for ${project.subject}"
            )
        )

        Result.success(project)
    }

    suspend fun updateProject(
        user: UserEntity,
        projectId: String,
        name: String,
        subject: String,
        academicYear: String,
        description: String,
        visibility: ProjectVisibility,
        status: ProjectStatus
    ): Result<ProjectEntity> = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(projectId)
            ?: return@withContext Result.failure(Exception("Project not found"))

        val isOwner = existing.ownerId == user.id
        val isAdmin = user.getRoleEnum().canManageAllProjects()
        if (!isOwner && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized: You do not own this project."))
        }

        val updated = existing.copy(
            name = name.trim().ifBlank { existing.name },
            subject = subject,
            academicYear = academicYear,
            description = description.trim(),
            visibility = visibility.name,
            status = status.label,
            updatedAt = System.currentTimeMillis()
        )

        projectDao.updateUserProject(updated)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "PROJECT_UPDATED",
                resourceType = "PROJECT",
                resourceId = updated.id,
                details = "Updated project '${updated.name}'"
            )
        )

        Result.success(updated)
    }

    suspend fun deleteProject(user: UserEntity, projectId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val existing = projectDao.getProjectById(projectId)
                ?: return@withContext Result.failure(Exception("Project not found"))

            val isOwner = existing.ownerId == user.id
            val isAdmin = user.getRoleEnum().canManageAllProjects()
            if (!isOwner && !isAdmin) {
                return@withContext Result.failure(SecurityException("Unauthorized: Cannot delete another user's project."))
            }

            documentDao.deleteDocumentsForProject(projectId)
            projectDao.deleteProject(projectId)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "PROJECT_DELETED",
                    resourceType = "PROJECT",
                    resourceId = projectId,
                    details = "Deleted project '${existing.name}' and all associated documents"
                )
            )

            Result.success(Unit)
        }

    suspend fun seedSampleProjectsIfEmpty(user: UserEntity) = withContext(Dispatchers.IO) {
        if (projectDao.getProjectCountForOwner(user.id) == 0) {
            val p1 = ProjectEntity(
                id = "proj-math-2025",
                ownerId = user.id,
                ownerName = user.fullName,
                name = "CBSE Class 10 Math Standard 2025 Mastery",
                board = "CBSE",
                className = "Class 10",
                subject = "Mathematics (Standard)",
                academicYear = "2024-2025",
                description = "Compilation of previous 5 years CBSE board question papers, official sample papers, and marking schemes for topic-wise mark weightage analysis.",
                visibility = ProjectVisibility.PRIVATE.name,
                status = ProjectStatus.DOCUMENTS_ADDED.label,
                documentCount = 3,
                createdAt = System.currentTimeMillis() - 86400000L * 3,
                updatedAt = System.currentTimeMillis() - 86400000L * 1
            )

            val p2 = ProjectEntity(
                id = "proj-science-2025",
                ownerId = user.id,
                ownerName = user.fullName,
                name = "Class 10 Science Competency Questions",
                board = "CBSE",
                className = "Class 10",
                subject = "Science (086)",
                academicYear = "2024-2025",
                description = "Focus on chemical reactions, electricity numericals, and biology life processes assertion-reason questions.",
                visibility = ProjectVisibility.PRIVATE.name,
                status = ProjectStatus.SETUP.label,
                documentCount = 1,
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                updatedAt = System.currentTimeMillis() - 86400000L * 2
            )

            projectDao.insertProject(p1)
            projectDao.insertProject(p2)
        }
    }
}
