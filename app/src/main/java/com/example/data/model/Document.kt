package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProcessingStatus(val label: String) {
    UPLOADED("Uploaded"),
    QUEUED("Queued"),
    EXTRACTING_TEXT("Extracting text"),
    DETECTING_QUESTIONS("Detecting questions"),
    AWAITING_REVIEW("Awaiting review"),
    PARTIALLY_REVIEWED("Partially reviewed"),
    VERIFIED("Verified"),
    FAILED("Failed"),
    RETRYING("Retrying");

    companion object {
        fun fromLabel(label: String): ProcessingStatus {
            return entries.find { it.label.equals(label, ignoreCase = true) } ?: UPLOADED
        }
    }
}

enum class DocumentCategory(val label: String) {
    PREVIOUS_YEAR_PAPER("CBSE Board Question Paper"),
    SAMPLE_PAPER("Official Sample Paper (SQP)"),
    MARKING_SCHEME("Marking Scheme & Answers"),
    SYLLABUS_GUIDE("Syllabus & Curriculum Guide"),
    NOTES_PRACTICE("Practice Material / Notes")
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val ownerId: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val storageUri: String,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val processingStatus: String = ProcessingStatus.UPLOADED.label,
    val documentCategory: String = DocumentCategory.PREVIOUS_YEAR_PAPER.label,
    val extractedText: String? = null,
    val questionCount: Int = 0,
    val verifiedCount: Int = 0,
    val processingStartedAt: Long? = null,
    val processingCompletedAt: Long? = null,
    val extractionMethod: String = "Automated Document Pipeline",
    val errorDetails: String? = null
) {
    val customFilename: String get() = originalFilename
    val category: String get() = documentCategory

    fun formattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", fileSize.toDouble() / (1024 * 1024))
        }
    }
}
