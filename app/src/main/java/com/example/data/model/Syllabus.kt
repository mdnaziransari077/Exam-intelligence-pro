package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyllabusSourceType(val label: String) {
    OFFICIAL_CBSE_DOCUMENT("Official CBSE Curriculum Document"),
    OFFICIAL_CURRICULUM_PAGE("Official CBSE Academic Portal"),
    ADMIN_UPLOAD("Administrator Uploaded Document"),
    MANUAL_IMPORT("Manual Curriculum Import")
}

enum class SyllabusVerificationStatus(val label: String) {
    VERIFIED("Verified Official"),
    UNVERIFIED("Unverified — Review Required"),
    UNDER_REVIEW("Under Review");

    companion object {
        fun fromLabel(label: String): SyllabusVerificationStatus =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: UNVERIFIED
    }
}

enum class SyllabusNodeType(val label: String) {
    UNIT("Unit"),
    CHAPTER("Chapter"),
    TOPIC("Topic"),
    SUBTOPIC("Subtopic")
}

enum class MappingReviewStatus(val label: String) {
    SUGGESTED("AI Suggested (Pending Review)"),
    APPROVED("Human Verified"),
    REJECTED("Rejected"),
    UNCERTAIN("Uncertain — Review Required");

    companion object {
        fun fromLabel(label: String): MappingReviewStatus =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: SUGGESTED
    }
}

enum class CoverageStatus(val label: String, val badgeColorHex: Long) {
    QUESTIONS_FOUND("Questions Found", 0xFF10B981), // Emerald Green
    NO_QUESTIONS_FOUND("No Questions Found in Analysed Collection", 0xFFF59E0B), // Amber
    MAPPING_REVIEW_REQUIRED("Mapping Review Required", 0xFF3B82F6), // Blue
    INSUFFICIENT_DATA("Insufficient Data", 0xFF6B7280), // Slate Grey
    EXCLUDED_FROM_SYLLABUS("Excluded from Selected Syllabus Version", 0xFF8B5CF6); // Purple
}

@Entity(tableName = "syllabus_versions")
data class SyllabusVersionEntity(
    @PrimaryKey
    val id: String,
    val board: String = "CBSE",
    val classLevel: String = "10",
    val subject: String,
    val academicSession: String, // e.g. "2024-2025", "2025-2026"
    val versionIdentifier: String, // e.g. "CBSE-10-MATH-2024-25-v1"
    val sourceType: String = SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.name,
    val sourceTitle: String,
    val sourceUrl: String? = null,
    val sourceDocumentRef: String? = null,
    val verificationStatus: String = SyllabusVerificationStatus.UNVERIFIED.label,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val reviewerNotes: String? = null,
    val importDate: Long = System.currentTimeMillis(),
    val changeHistory: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isVerified(): Boolean = verificationStatus == SyllabusVerificationStatus.VERIFIED.label
}

@Entity(tableName = "syllabus_nodes")
data class SyllabusNodeEntity(
    @PrimaryKey
    val id: String,
    val syllabusVersionId: String,
    val parentId: String? = null, // null for top-level Units or Chapters
    val nodeType: String = SyllabusNodeType.TOPIC.name,
    val code: String? = null, // e.g. "Unit I", "Ch 1", "1.2"
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val sourceReference: String? = null, // e.g. "CBSE 2024-25 Curriculum Sec 3, Page 12"
    val verificationStatus: String = SyllabusVerificationStatus.UNVERIFIED.label,
    val isExcluded: Boolean = false, // e.g. NCERT rationalized / deleted topics
    val exclusionReason: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "question_topic_mappings")
data class QuestionTopicMappingEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val sourceDocumentId: String,
    val projectId: String,
    val syllabusVersionId: String,
    val chapterId: String,
    val chapterName: String,
    val topicId: String,
    val topicName: String,
    val subtopicId: String? = null,
    val subtopicName: String? = null,
    val mappingMethod: String = "AI_SUGGESTION", // "AI_SUGGESTION" or "HUMAN_ASSIGNMENT"
    val reviewStatus: String = MappingReviewStatus.SUGGESTED.label,
    val reviewerEmail: String? = null,
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null,
    val explanation: String? = null,
    val confidenceScore: Float? = null, // 0.0 to 1.0 when genuine
    val isOutsideSyllabus: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isApproved(): Boolean = reviewStatus == MappingReviewStatus.APPROVED.label
    fun isPendingReview(): Boolean = reviewStatus == MappingReviewStatus.SUGGESTED.label
    fun isUncertain(): Boolean = reviewStatus == MappingReviewStatus.UNCERTAIN.label
    val mappingConfidence: Float get() = confidenceScore ?: 0f
}

@Entity(tableName = "mapping_review_logs")
data class MappingReviewLogEntity(
    @PrimaryKey
    val id: String,
    val mappingId: String,
    val questionId: String,
    val reviewerEmail: String,
    val action: String, // "ACCEPTED", "REJECTED", "MODIFIED", "MARKED_OUTSIDE_SYLLABUS", "MARKED_UNCERTAIN", "MULTI_TOPIC_ADDED"
    val previousStatus: String,
    val newStatus: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TopicCoverageStats(
    val topicId: String,
    val topicName: String,
    val chapterId: String,
    val chapterName: String,
    val subject: String,
    val academicSession: String,
    val totalMappedQuestions: Int,
    val distinctPaperCount: Int,
    val verifiedMappingCount: Int,
    val pendingMappingCount: Int,
    val verifiedMarksTotal: Int?, // null if questions have unverified/missing marks
    val hasUnverifiedQuestionMarks: Boolean,
    val coverageStatus: CoverageStatus,
    val isExcludedFromSyllabus: Boolean,
    val exclusionReason: String? = null
)
