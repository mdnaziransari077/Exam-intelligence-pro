package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray

enum class QuestionType(val label: String) {
    MCQ("Multiple Choice (MCQ)"),
    ASSERTION_REASON("Assertion-Reason"),
    SHORT_ANSWER_1("Short Answer (2M)"),
    SHORT_ANSWER_2("Short Answer (3M)"),
    LONG_ANSWER("Long Answer (5M)"),
    CASE_BASED("Case / Source-Based (4M)"),
    UNKNOWN("Unknown Type");

    companion object {
        fun fromLabel(label: String): QuestionType {
            return entries.find {
                it.label.equals(label, ignoreCase = true) ||
                it.name.equals(label, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

enum class VerificationStatus(val label: String) {
    UNVERIFIED("Awaiting Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    NEEDS_EDIT("Needs Edit");

    companion object {
        fun fromLabel(label: String): VerificationStatus {
            return entries.find {
                it.label.equals(label, ignoreCase = true) ||
                it.name.equals(label, ignoreCase = true)
            } ?: UNVERIFIED
        }
    }
}

@Entity(tableName = "extracted_questions")
data class ExtractedQuestionEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val sourceDocumentId: String,
    val questionNumber: String, // e.g. "1", "14(a)", "21", "Section B - Q23"
    val section: String = "Section A", // e.g. "Section A", "Section B", "Section C", "Section D", "Section E"
    val parentQuestionId: String? = null,
    val questionText: String,
    val subQuestionText: String? = null,
    val questionType: String = QuestionType.UNKNOWN.label,
    val marks: Int? = null, // null when marks are unknown/unspecified, flagged for review
    val answerOptionsJson: String? = null, // JSON string array of options for MCQs
    val internalChoice: String? = null, // e.g. "OR: State Ohm's law and derive expression"
    val topic: String? = null, // CBSE Class 10 topic tag e.g. "Electricity", "Quadratic Equations"
    val confidence: Float = 0.92f, // Confidence score (0.0 to 1.0)
    val sourcePage: Int = 1,
    val extractionStatus: String = "SUCCESS", // "SUCCESS", "REVIEW_REQUIRED", "FAILED"
    val verificationStatus: String = VerificationStatus.UNVERIFIED.label,
    val reviewWarnings: String? = null, // e.g. "Marks not specified in paper — flagged for human review"
    val extractionMethod: String = "Auto OCR & Structure Parser",
    val originalExtractionText: String, // preserved permanently for auditing
    val userCorrectedText: String? = null, // user edited text if modified
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getEffectiveText(): String = userCorrectedText?.ifBlank { null } ?: questionText

    fun getOptionsList(): List<String> {
        if (answerOptionsJson.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(answerOptionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Throwable) {
            // Robust JVM fallback for unit test environments
            try {
                val clean = answerOptionsJson.trim().removeSurrounding("[", "]")
                if (clean.isBlank()) emptyList()
                else {
                    val regex = "\"([^\"]*)\"".toRegex()
                    regex.findAll(clean).map { it.groupValues[1] }.toList()
                }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    fun isMarksUnknown(): Boolean = marks == null || marks <= 0

    fun isVerified(): Boolean = verificationStatus == VerificationStatus.APPROVED.label

    fun isHighConfidence(): Boolean = confidence >= 0.85f && !isMarksUnknown() && reviewWarnings.isNullOrBlank()
}

@Entity(tableName = "question_review_logs")
data class QuestionReviewLogEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val sourceDocumentId: String,
    val reviewerEmail: String,
    val action: String, // "APPROVED", "REJECTED", "EDITED", "BULK_APPROVED", "MANUAL_ADDED"
    val previousStatus: String,
    val newStatus: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
