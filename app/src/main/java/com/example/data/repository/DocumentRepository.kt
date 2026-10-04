package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ProjectDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.ProcessingStatus
import com.example.data.model.ProjectStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val projectDao: ProjectDao,
    private val auditLogDao: AuditLogDao
) {

    fun getDocumentsForProject(projectId: String): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsForProject(projectId).flowOn(Dispatchers.IO)
    }

    fun getDocumentsForUser(user: UserEntity?): Flow<List<DocumentEntity>> {
        val flow = if (user != null && (user.getRoleEnum() == UserRole.ADMIN || user.getRoleEnum() == UserRole.SUPER_ADMIN)) {
            documentDao.getAllDocuments()
        } else if (user != null) {
            documentDao.getDocumentsForUserOrPublic(user.id)
        } else {
            documentDao.getDocumentsForPublicProjects()
        }
        return flow.flowOn(Dispatchers.IO)
    }

    suspend fun saveUploadedFile(
        user: UserEntity,
        projectId: String,
        uri: Uri?,
        customFilename: String,
        category: DocumentCategory,
        mimeType: String,
        fileSizeBytes: Long
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId)
            ?: return@withContext Result.failure(Exception("Project not found"))

        val isOwner = project.ownerId == user.id
        val isAdmin = user.getRoleEnum().canManageAllProjects()
        if (!isOwner && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized: Cannot upload documents to another user's project."))
        }

        // Validate allowed file types
        val validTypes = listOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
        )

        val isExtensionValid = customFilename.endsWith(".pdf", ignoreCase = true) ||
                customFilename.endsWith(".jpg", ignoreCase = true) ||
                customFilename.endsWith(".jpeg", ignoreCase = true) ||
                customFilename.endsWith(".png", ignoreCase = true) ||
                customFilename.endsWith(".docx", ignoreCase = true) ||
                customFilename.endsWith(".doc", ignoreCase = true)

        if (!validTypes.any { mimeType.contains(it, ignoreCase = true) } && !isExtensionValid) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid file format. Supported formats: PDF, JPG, PNG, DOCX, DOC")
            )
        }

        // 25MB max size check
        val maxSizeBytes = 25 * 1024 * 1024L
        if (fileSizeBytes > maxSizeBytes) {
            return@withContext Result.failure(
                IllegalArgumentException("File size exceeds 25 MB maximum limit.")
            )
        }

        val docId = UUID.randomUUID().toString()
        val docsDir = File(context.filesDir, "project_docs/$projectId")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val sanitizedName = customFilename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destinationFile = File(docsDir, "${docId}_$sanitizedName")

        var actualSize = fileSizeBytes
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                actualSize = destinationFile.length()
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Failed to read and save file: ${e.localizedMessage}"))
            }
        } else {
            // Virtual/sample placeholder storage creation
            destinationFile.writeText("Exam Intelligence Document Storage Placeholder for: $customFilename\nProject: ${project.name}\nCBSE Class 10")
            actualSize = destinationFile.length()
        }

        val documentEntity = DocumentEntity(
            id = docId,
            projectId = projectId,
            ownerId = user.id,
            originalFilename = customFilename,
            mimeType = mimeType,
            fileSize = actualSize,
            storageUri = destinationFile.absolutePath,
            uploadTimestamp = System.currentTimeMillis(),
            processingStatus = ProcessingStatus.UPLOADED.label,
            documentCategory = category.label,
            errorDetails = null
        )

        documentDao.insertDocument(documentEntity)

        // Update project stats
        val totalDocs = documentDao.getDocumentCountForProject(projectId)
        val newStatus = if (project.status == ProjectStatus.SETUP.label) {
            ProjectStatus.DOCUMENTS_ADDED.label
        } else {
            project.status
        }
        projectDao.updateProjectStats(projectId, newStatus, totalDocs, System.currentTimeMillis())

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userEmail = user.email,
                action = "DOCUMENT_UPLOADED",
                resourceType = "DOCUMENT",
                resourceId = documentEntity.id,
                details = "Uploaded '${documentEntity.originalFilename}' (${documentEntity.formattedFileSize()}) to project '${project.name}'"
            )
        )

        Result.success(documentEntity)
    }

    suspend fun deleteDocument(user: UserEntity, documentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val doc = documentDao.getDocumentById(documentId)
                ?: return@withContext Result.failure(Exception("Document not found"))

            val project = projectDao.getProjectById(doc.projectId)
            val isOwner = doc.ownerId == user.id || (project != null && project.ownerId == user.id)
            val isAdmin = user.getRoleEnum().canManageAllProjects()
            if (!isOwner && !isAdmin) {
                return@withContext Result.failure(SecurityException("Unauthorized: Cannot delete this document."))
            }

            // Remove file from disk
            try {
                val file = File(doc.storageUri)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}

            documentDao.deleteDocument(documentId)

            if (project != null) {
                val totalDocs = documentDao.getDocumentCountForProject(project.id)
                val newStatus = if (totalDocs == 0) ProjectStatus.SETUP.label else project.status
                projectDao.updateProjectStats(project.id, newStatus, totalDocs, System.currentTimeMillis())
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "DOCUMENT_DELETED",
                    resourceType = "DOCUMENT",
                    resourceId = documentId,
                    details = "Deleted document '${doc.originalFilename}'"
                )
            )

            Result.success(Unit)
        }

    suspend fun seedSampleDocumentsIfEmpty(user: UserEntity, projectId: String) =
        withContext(Dispatchers.IO) {
            if (documentDao.getDocumentCountForProject(projectId) == 0) {
                val sampleDocs = listOf(
                    Triple(
                        "CBSE_Class10_Math_Standard_BoardPaper_2024.pdf",
                        "application/pdf",
                        DocumentCategory.PREVIOUS_YEAR_PAPER
                    ),
                    Triple(
                        "CBSE_Class10_Math_Official_SamplePaper_2025.pdf",
                        "application/pdf",
                        DocumentCategory.SAMPLE_PAPER
                    ),
                    Triple(
                        "Class10_Mathematics_MarkingScheme_2024.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        DocumentCategory.MARKING_SCHEME
                    )
                )

                for ((filename, mime, cat) in sampleDocs) {
                    saveUploadedFile(
                        user = user,
                        projectId = projectId,
                        uri = null,
                        customFilename = filename,
                        category = cat,
                        mimeType = mime,
                        fileSizeBytes = when {
                            filename.endsWith(".pdf") -> 2_450_000L
                            else -> 890_000L
                        }
                    )
                }
            }
        }
}
