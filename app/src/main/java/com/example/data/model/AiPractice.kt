package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray

enum class PracticeDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    MIXED("Mixed Difficulty");

    companion object {
        fun fromLabel(label: String): PracticeDifficulty {
            return entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class PracticeQuestionFormat(val label: String, val typicalMarks: Int) {
    MCQ("Multiple Choice (MCQ)", 1),
    VERY_SHORT_ANSWER("Very Short Answer (VSA)", 1),
    SHORT_ANSWER_1("Short Answer (SA-I - 2M)", 2),
    SHORT_ANSWER_2("Short Answer (SA-II - 3M)", 3),
    LONG_ANSWER("Long Answer (LA - 5M)", 5),
    ASSERTION_REASON("Assertion-Reason", 1),
    CASE_BASED("Case / Source-Based (4M)", 4),
    NUMERICAL("Numerical Problem", 3),
    DIAGRAM_BASED("Diagram-Based Question", 3),
    MIXED("Mixed Formats", 0);

    companion object {
        fun fromLabel(label: String): PracticeQuestionFormat {
            return entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) } ?: MCQ
        }
    }
}

enum class AiReviewStatus(val label: String) {
    DRAFT("Draft / Unreviewed"),
    AWAITING_REVIEW("Awaiting Review"),
    VERIFIED("Human Verified"),
    EDITED("Human Edited"),
    REJECTED("Rejected");

    companion object {
        fun fromLabel(label: String): AiReviewStatus {
            return entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) } ?: DRAFT
        }
    }
}

@Entity(tableName = "practice_question_sets")
data class PracticeQuestionSetEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val userId: String,
    val syllabusVersionId: String,
    val subject: String,
    val chapterId: String,
    val chapterName: String,
    val topicId: String?,
    val topicName: String?,
    val questionFormat: String,
    val difficulty: String,
    val questionCount: Int,
    val modelTag: String,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "practice_questions")
data class PracticeQuestionEntity(
    @PrimaryKey
    val id: String,
    val setId: String,
    val projectId: String,
    val questionNumber: Int,
    val questionText: String,
    val questionType: String,
    val chapterId: String,
    val chapterName: String,
    val topicId: String?,
    val topicName: String?,
    val marks: Int?,
    val difficulty: String, // e.g. "AI-Estimated: Medium"
    val answerOptionsJson: String? = null, // JSON array of options for MCQ
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val modelAnswerGuidance: String? = null,
    val syllabusReference: String? = null,
    val modelTag: String = "gemini-3.5-flash",
    val originalAiText: String,
    val userCorrectedText: String? = null,
    val reviewStatus: String = AiReviewStatus.DRAFT.label,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEffectiveText(): String = userCorrectedText?.ifBlank { null } ?: questionText

    fun isVerified(): Boolean = reviewStatus == AiReviewStatus.VERIFIED.label || reviewStatus == AiReviewStatus.EDITED.label

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
}

@Entity(tableName = "practice_attempts")
data class PracticeAttemptEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val setId: String,
    val userId: String,
    val selectedOption: String?,
    val isCorrect: Boolean?,
    val attemptedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_review_logs")
data class AiReviewLogEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val reviewerEmail: String,
    val action: String, // "APPROVED", "EDITED", "REJECTED", "RESET"
    val previousStatus: String,
    val newStatus: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_exam_estimates")
data class AiExamEstimateEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val syllabusVersionId: String,
    val subject: String,
    val scopeJson: String,
    val estimatesJson: String,
    val analyzedPaperCount: Int,
    val analyzedYears: String,
    val dataLimitations: String,
    val disclaimer: String = "AI estimate — not an official prediction. Historical frequency does not guarantee future examination content.",
    val createdAt: Long = System.currentTimeMillis()
)

data class PracticeGenerationRequest(
    val projectId: String,
    val subject: String,
    val syllabusVersionId: String,
    val chapterId: String,
    val chapterName: String,
    val topicId: String? = null,
    val topicName: String? = null,
    val format: PracticeQuestionFormat = PracticeQuestionFormat.MCQ,
    val difficulty: PracticeDifficulty = PracticeDifficulty.MEDIUM,
    val questionCount: Int = 3,
    val marksPerQuestion: Int? = null,
    val syllabusContext: String = "",
    val historicalQuestionsContext: List<String> = emptyList()
)

data class TopicPracticeSuggestion(
    val topicId: String,
    val topicName: String,
    val chapterId: String,
    val chapterName: String,
    val suggestionRationale: String,
    val category: String, // "HIGH_FREQUENCY", "UNTESTED_GAP", "FORMAT_FOCUSED"
    val verifiedPaperCount: Int,
    val historicalAppearances: Int,
    val attributedMarks: Double,
    val frequentQuestionTypes: List<String>,
    val yearsRepresented: List<String>,
    val drillDownTopicId: String
)

data class TopicEstimateItem(
    val topicId: String,
    val topicName: String,
    val chapterName: String,
    val attentionLevel: String, // "High Attention", "Moderate Focus", "Core Curriculum Foundation"
    val rationale: String,
    val historicalFrequencyNotice: String,
    val syllabusStatus: String,
    val caveat: String
)
