package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Filter scope for question verification status in historical analytics.
 */
enum class QuestionVerificationScope(val label: String, val description: String) {
    VERIFIED_ONLY("Verified Only", "Includes only human-approved questions. Unverified extractions are excluded."),
    ALL_EXTRACTIONS("All Extracted (Exploratory)", "Includes unverified extractions, clearly tagged with exploratory labels.")
}

/**
 * Filter scope for question-topic mapping status in historical analytics.
 */
enum class MappingVerificationScope(val label: String, val description: String) {
    APPROVED_ONLY("Approved Mappings Only", "Includes only human-verified question-to-topic mappings."),
    INCLUDE_AI_SUGGESTIONS("Include AI Suggestions (Exploratory)", "Separately includes AI suggestions, clearly flagged as unverified.")
}

/**
 * Marks attribution method for multi-topic questions.
 */
enum class MarksAttributionMethod(val label: String, val description: String) {
    FULL_OVERLAPPING(
        "Full Overlapping Attribution",
        "Each mapped topic receives the full question marks. Topic totals overlap and are NOT additive across the paper."
    ),
    FRACTIONAL_SPLIT(
        "Fractional Split Allocation",
        "A question's marks are divided equally among its N mapped topics (Marks / N)."
    )
}

/**
 * Scope parameters defining a historical analysis execution run.
 */
data class AnalysisScope(
    val projectId: String,
    val board: String = "CBSE",
    val classLevel: String = "10",
    val subject: String,
    val syllabusVersionId: String,
    val selectedDocumentIds: List<String> = emptyList(), // empty = all eligible in project
    val selectedYears: List<String> = emptyList(), // empty = all known years
    val selectedPaperSets: List<String> = emptyList(), // empty = all sets
    val questionVerificationScope: QuestionVerificationScope = QuestionVerificationScope.VERIFIED_ONLY,
    val mappingVerificationScope: MappingVerificationScope = MappingVerificationScope.APPROVED_ONLY,
    val attributionMethod: MarksAttributionMethod = MarksAttributionMethod.FULL_OVERLAPPING,
    val deduplicateRepeatedQuestions: Boolean = true
)

/**
 * Pre-analysis summary of the selected scope and data eligibility.
 */
data class ScopeSummary(
    val totalDocuments: Int,
    val successfullyProcessedDocuments: Int,
    val totalQuestionsExtracted: Int,
    val verifiedQuestionsCount: Int,
    val questionsWithVerifiedMappingsCount: Int,
    val questionsWithAiSuggestedMappingsCount: Int,
    val questionsWithUnknownMarksCount: Int,
    val questionsAwaitingReviewCount: Int,
    val ineligibleOrIncompleteExcludedCount: Int,
    val syllabusVersionId: String,
    val syllabusVersionName: String,
    val syllabusIsVerified: Boolean,
    val suspectedDuplicateDocumentsCount: Int
)

/**
 * Historical frequency and mark attribution metrics for an individual syllabus topic.
 */
data class HistoricalTopicMetrics(
    val topicId: String,
    val topicName: String,
    val chapterId: String,
    val chapterName: String,
    val isExcludedFromSyllabus: Boolean = false,
    val exclusionReason: String? = null,
    val questionAppearancesCount: Int, // Total number of times questions mapped to this topic appear
    val distinctPaperCount: Int, // Number of distinct papers containing this topic
    val distinctYearsCount: Int,
    val distinctYears: List<String>,
    val uniqueQuestionsCount: Int, // Distinct question wording after text deduplication
    val repeatedQuestionsCount: Int, // Questions with identical text appearing across papers
    val attributedMarks: Double, // Calculated via selected attribution method
    val printedMarksSum: Int, // Sum of printed marks for questions mapped to this topic
    val averageMarksPerPaper: Double, // Over papers containing the topic
    val minQuestionMarks: Int?,
    val maxQuestionMarks: Int?,
    val questionsWithUnknownMarks: Int,
    val frequencyByQuestionType: Map<String, Int>,
    val marksByQuestionType: Map<String, Double>,
    val frequencyByYear: Map<String, Int>,
    val frequencyByPaperSet: Map<String, Int>,
    val frequencyByMarksCategory: Map<String, Int>,
    val associatedQuestionIds: List<String>
)

/**
 * Aggregate historical metrics for an entire curriculum chapter.
 */
data class ChapterHistoricalMetrics(
    val chapterId: String,
    val chapterName: String,
    val totalQuestionAppearances: Int,
    val distinctPaperCount: Int,
    val attributedMarks: Double,
    val topicsCount: Int,
    val topicsWithQuestionsCount: Int,
    val untestedTopicsCount: Int,
    val topicsList: List<HistoricalTopicMetrics>
)

/**
 * Historical metrics by question format/type (MCQ, Assertion-Reason, Short Answer, etc.).
 */
data class QuestionTypeMetrics(
    val questionType: String,
    val questionCount: Int,
    val distinctPaperCount: Int,
    val totalAttributedMarks: Double,
    val percentageOfTotalQuestions: Double,
    val associatedChapters: List<String>,
    val frequencyByYear: Map<String, Int>
)

/**
 * Comparison metrics across examination years.
 */
data class YearComparisonMetrics(
    val year: String,
    val papersCount: Int,
    val questionsCount: Int,
    val totalAttributedMarks: Double,
    val chaptersCoveredCount: Int,
    val topicsCoveredCount: Int,
    val questionTypesCount: Map<String, Int>,
    val topChapters: List<String>
)

/**
 * Comprehensive data quality report for the analyzed scope.
 */
data class DataQualityReport(
    val paperCollectionCompletenessPercent: Float,
    val questionExtractionCompletionPercent: Float = 100f,
    val questionVerificationCompletionPercent: Float,
    val mappingVerificationCompletionPercent: Float,
    val marksVerificationCompletionPercent: Float,
    val missingYearMetadataCount: Int,
    val missingPaperSetMetadataCount: Int,
    val suspectedDuplicateDocs: List<SuspectedDuplicateDoc>,
    val unmappedQuestionsCount: Int,
    val uncertainQuestionTypesCount: Int,
    val qualityBadges: List<String>,
    val methodologyNotice: String
)

/**
 * Suspected duplicate document record.
 */
data class SuspectedDuplicateDoc(
    val doc1Id: String,
    val doc1Name: String,
    val doc2Id: String,
    val doc2Name: String,
    val reason: String
)

/**
 * Complete result of a historical analysis run.
 */
data class HistoricalAnalysisResult(
    val runId: String,
    val projectId: String,
    val scope: AnalysisScope,
    val scopeSummary: ScopeSummary,
    val executionTimestamp: Long,
    val status: String, // "COMPLETED", "STALE"
    val isStale: Boolean = false,
    val staleReason: String? = null,
    val totalEligiblePapers: Int,
    val totalVerifiedQuestions: Int,
    val totalVerifiedMappings: Int,
    val totalAttributedMarks: Double,
    val totalPrintedMarksAvailable: Int,
    val candidateAvailableMarksEstimate: Int,
    val chapterMetrics: List<ChapterHistoricalMetrics>,
    val topicMetrics: List<HistoricalTopicMetrics>,
    val questionTypeMetrics: List<QuestionTypeMetrics>,
    val yearComparisonMetrics: List<YearComparisonMetrics>,
    val dataQualityReport: DataQualityReport,
    val untestedTopicsCount: Int,
    val untestedTopicsList: List<HistoricalTopicMetrics>
)

/**
 * Room entity for persisting analysis run metadata and caching.
 */
@Entity(tableName = "historical_analysis_runs")
data class HistoricalAnalysisRunEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val syllabusVersionId: String,
    val scopeJson: String,
    val status: String = "COMPLETED", // "COMPLETED", "STALE"
    val attributionMethod: String = MarksAttributionMethod.FULL_OVERLAPPING.name,
    val includeAiSuggestions: Boolean = false,
    val totalEligiblePapers: Int = 0,
    val totalVerifiedQuestions: Int = 0,
    val totalVerifiedMappings: Int = 0,
    val totalAttributedMarks: Double = 0.0,
    val summaryJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
