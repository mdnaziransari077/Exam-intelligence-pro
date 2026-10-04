package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReportType(val label: String, val description: String) {
    HISTORICAL_ANALYSIS(
        "Historical Question Analysis",
        "Deep-dive analysis of verified topic appearance frequency and marks distribution."
    ),
    CHAPTER_COVERAGE(
        "Chapter & Topic Coverage Matrix",
        "Curriculum alignment report highlighting tested concepts and untested syllabus gaps."
    ),
    MARKS_DISTRIBUTION(
        "Question Formats & Marks Distribution",
        "Breakdown by question types (MCQ, VSA, SA-I, SA-II, LA) and mark weightings."
    ),
    YEAR_COMPARISON(
        "Year-over-Year & Paper Set Comparison",
        "Cross-year pattern comparison across examination sessions and paper series."
    ),
    VERIFIED_QUESTION_COLLECTION(
        "Verified Historical Question Collection",
        "Curated collection of verified CBSE Class 10 board questions."
    ),
    AI_PRACTICE_SET(
        "AI Practice Question Set",
        "Original syllabus-aligned practice questions with optional answers and explanations."
    ),
    AI_EXAM_ESTIMATES(
        "AI Probabilistic Exam Trend Estimates",
        "Separate AI trend estimations strictly distinguished from historical facts."
    ),
    SYLLABUS_AUDIT(
        "Syllabus Verification & Concept Mapping Audit",
        "Administrative audit of curriculum versions, review logs, and mapping confidence."
    );

    companion object {
        fun fromLabel(label: String): ReportType =
            entries.find { it.name.equals(label, ignoreCase = true) || it.label.equals(label, ignoreCase = true) }
                ?: HISTORICAL_ANALYSIS
    }
}

enum class ReportFormat(val label: String, val extension: String, val mimeType: String) {
    PDF("PDF Document (.pdf)", "pdf", "application/pdf"),
    MARKDOWN("Markdown (.md)", "md", "text/markdown"),
    CSV("CSV Spreadsheet (.csv)", "csv", "text/csv"),
    JSON("Structured Data (.json)", "json", "application/json");

    companion object {
        fun fromLabel(label: String): ReportFormat =
            entries.find { it.name.equals(label, ignoreCase = true) || it.label.equals(label, ignoreCase = true) }
                ?: PDF
    }
}

@Entity(tableName = "generated_reports")
data class GeneratedReportEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userEmail: String,
    val reportType: String,
    val title: String,
    val subject: String,
    val board: String = "CBSE",
    val classLevel: String = "Class 10",
    val syllabusVersionId: String? = null,
    val projectId: String? = null,
    val projectName: String? = null,
    val isPublic: Boolean = false,
    val scopeJson: String = "{}",
    val recordCount: Int = 0,
    val format: String = ReportFormat.PDF.name,
    val filePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val summaryText: String = "",
    val methodologyNotes: String = "",
    val dataLimitations: String = "",
    val disclaimer: String = "Official CBSE syllabus and verified examination records. Descriptive analytics only.",
    val includeAnswers: Boolean = false,
    val isStale: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getReportTypeEnum(): ReportType = ReportType.fromLabel(reportType)
    fun getReportFormatEnum(): ReportFormat = ReportFormat.fromLabel(format)
}

data class ReportGenerationRequest(
    val reportType: ReportType,
    val format: ReportFormat = ReportFormat.PDF,
    val subject: String,
    val projectId: String? = null,
    val syllabusVersionId: String? = null,
    val yearFilter: String? = null,
    val chapterFilter: String? = null,
    val verificationOnly: Boolean = true,
    val includeAnswers: Boolean = false,
    val isPublic: Boolean = false
)
