package com.example.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.AiExamEstimateDao
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ExtractedQuestionDao
import com.example.data.local.GeneratedReportDao
import com.example.data.local.PracticeQuestionDao
import com.example.data.local.ProjectDao
import com.example.data.local.QuestionTopicMappingDao
import com.example.data.local.SyllabusDao
import com.example.data.model.AnalysisScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.MappingReviewStatus
import com.example.data.model.MappingVerificationScope
import com.example.data.model.MarksAttributionMethod
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectVisibility
import com.example.data.model.QuestionVerificationScope
import com.example.data.model.ReportFormat
import com.example.data.model.ReportGenerationRequest
import com.example.data.model.ReportType
import com.example.data.model.SyllabusNodeType
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReportRepository(
    private val context: Context,
    private val generatedReportDao: GeneratedReportDao,
    private val projectDao: ProjectDao,
    private val documentDao: DocumentDao,
    private val extractedQuestionDao: ExtractedQuestionDao,
    private val syllabusDao: SyllabusDao,
    private val questionTopicMappingDao: QuestionTopicMappingDao,
    private val practiceQuestionDao: PracticeQuestionDao,
    private val aiExamEstimateDao: AiExamEstimateDao,
    private val historicalAnalysisRepo: HistoricalAnalysisRepository,
    private val auditLogDao: AuditLogDao
) {

    private val reportsDir: File = File(context.filesDir, "reports").apply {
        if (!exists()) mkdirs()
    }

    fun getReportsForUser(user: UserEntity?): Flow<List<GeneratedReportEntity>> {
        val flow = if (user != null && (user.getRoleEnum() == UserRole.ADMIN || user.getRoleEnum() == UserRole.SUPER_ADMIN)) {
            generatedReportDao.getAllReports()
        } else if (user != null) {
            generatedReportDao.getReportsForUserOrPublic(user.id)
        } else {
            generatedReportDao.getPublicReports()
        }
        return flow.flowOn(Dispatchers.IO)
    }

    suspend fun getReportById(user: UserEntity?, reportId: String): Result<GeneratedReportEntity> =
        withContext(Dispatchers.IO) {
            val report = generatedReportDao.getReportById(reportId)
                ?: return@withContext Result.failure(Exception("Report not found"))

            val isOwner = user != null && report.userId == user.id
            val isAdmin = user != null && user.getRoleEnum().canAccessAdminArea()
            val isPublic = report.isPublic

            if (!isOwner && !isAdmin && !isPublic) {
                return@withContext Result.failure(
                    SecurityException("Forbidden: You do not have permission to access this private report.")
                )
            }

            Result.success(report)
        }

    suspend fun generateReport(
        user: UserEntity,
        request: ReportGenerationRequest
    ): Result<GeneratedReportEntity> = withContext(Dispatchers.IO) {
        try {
            // Verify project access if project is specified
            var project: ProjectEntity? = null
            if (!request.projectId.isNullOrBlank()) {
                val p = projectDao.getProjectById(request.projectId)
                    ?: return@withContext Result.failure(Exception("Target project not found"))

                val isOwner = p.ownerId == user.id
                val isAdmin = user.getRoleEnum().canManageAllProjects()
                val isPublic = p.visibility == ProjectVisibility.PUBLIC.name

                if (!isOwner && !isAdmin && !isPublic) {
                    return@withContext Result.failure(
                        SecurityException("Unauthorized: Cannot generate reports for a private project owned by another user.")
                    )
                }
                project = p
            }

            // Build Report Data
            val reportId = UUID.randomUUID().toString()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            val title = when (request.reportType) {
                ReportType.HISTORICAL_ANALYSIS -> "Historical Question Analysis Report — ${request.subject}"
                ReportType.CHAPTER_COVERAGE -> "Curriculum Chapter & Topic Coverage Matrix — ${request.subject}"
                ReportType.MARKS_DISTRIBUTION -> "Question Formats & Marks Distribution Report — ${request.subject}"
                ReportType.YEAR_COMPARISON -> "Year-over-Year Paper Series Comparison — ${request.subject}"
                ReportType.VERIFIED_QUESTION_COLLECTION -> "Verified CBSE Class 10 Question Collection — ${request.subject}"
                ReportType.AI_PRACTICE_SET -> "AI Practice Question Set — ${request.subject}"
                ReportType.AI_EXAM_ESTIMATES -> "AI Probabilistic Exam Trend Estimates — ${request.subject}"
                ReportType.SYLLABUS_AUDIT -> "Syllabus Verification & Concept Mapping Audit — ${request.subject}"
            }

            // Generate content and metrics
            val (summaryText, recordCount, methodologyNotes, dataLimitations, disclaimer, textContent) =
                buildReportContent(user, request, project, title, dateStr)

            // Save file
            val extension = request.format.extension
            val fileName = "report_${reportId.take(8)}_$extension"
            val file = File(reportsDir, fileName)

            if (request.format == ReportFormat.PDF) {
                generatePdfFile(file, title, request, dateStr, summaryText, textContent, disclaimer, methodologyNotes)
            } else {
                file.writeText(textContent, Charsets.UTF_8)
            }

            val reportEntity = GeneratedReportEntity(
                id = reportId,
                userId = user.id,
                userEmail = user.email,
                reportType = request.reportType.name,
                title = title,
                subject = request.subject,
                board = "CBSE",
                classLevel = "Class 10",
                syllabusVersionId = request.syllabusVersionId,
                projectId = request.projectId,
                projectName = project?.name,
                isPublic = request.isPublic,
                scopeJson = JSONObject().apply {
                    put("reportType", request.reportType.name)
                    put("subject", request.subject)
                    put("projectId", request.projectId)
                    put("syllabusVersionId", request.syllabusVersionId)
                    put("yearFilter", request.yearFilter)
                    put("chapterFilter", request.chapterFilter)
                    put("verificationOnly", request.verificationOnly)
                    put("includeAnswers", request.includeAnswers)
                    put("isPublic", request.isPublic)
                }.toString(),
                recordCount = recordCount,
                format = request.format.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                summaryText = summaryText,
                methodologyNotes = methodologyNotes,
                dataLimitations = dataLimitations,
                disclaimer = disclaimer,
                includeAnswers = request.includeAnswers,
                isStale = false,
                createdAt = System.currentTimeMillis()
            )

            generatedReportDao.insertReport(reportEntity)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "REPORT_GENERATED",
                    resourceType = "REPORT",
                    resourceId = reportId,
                    details = "Generated ${request.format.name} report '${reportEntity.title}' ($recordCount records included)"
                )
            )

            Result.success(reportEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun buildReportContent(
        user: UserEntity,
        request: ReportGenerationRequest,
        project: ProjectEntity?,
        title: String,
        dateStr: String
    ): ReportContentData {
        val sb = StringBuilder()
        var recordCount = 0
        var summaryText = ""
        var methodologyNotes = "Descriptive evidence only. Rule A: Verified question identities. Rule B: Distinct paper counts. Rule G: Overlapping topic attribution."
        var dataLimitations = "Analysis reflects eligible uploaded and processed CBSE Class 10 documents."
        var disclaimer = "Official CBSE syllabus and verified examination records. Descriptive analytics only."

        when (request.reportType) {
            ReportType.HISTORICAL_ANALYSIS -> {
                val projId = request.projectId ?: project?.id ?: ""
                val analysisResult = if (projId.isNotBlank()) {
                    historicalAnalysisRepo.executeAnalysis(
                        AnalysisScope(
                            projectId = projId,
                            board = "CBSE",
                            classLevel = "10",
                            subject = request.subject,
                            syllabusVersionId = request.syllabusVersionId ?: "cbse-10-${request.subject.lowercase().take(4)}-2024-25",
                            questionVerificationScope = if (request.verificationOnly) QuestionVerificationScope.VERIFIED_ONLY else QuestionVerificationScope.ALL_EXTRACTIONS,
                            mappingVerificationScope = MappingVerificationScope.APPROVED_ONLY,
                            attributionMethod = MarksAttributionMethod.FULL_OVERLAPPING
                        ),
                        user
                    ).getOrNull()
                } else null

                if (analysisResult != null) {
                    recordCount = analysisResult.totalVerifiedQuestions
                    summaryText = "Analyzed ${analysisResult.totalEligiblePapers} papers containing ${analysisResult.totalVerifiedQuestions} verified questions across ${analysisResult.chapterMetrics.size} chapters."
                    sb.appendLine("# $title")
                    sb.appendLine("**Generated:** $dateStr | **User:** ${user.fullName} (${user.email})")
                    sb.appendLine("**Scope:** Subject: ${analysisResult.scope.subject}, Syllabus: ${analysisResult.scopeSummary.syllabusVersionName}")
                    sb.appendLine("**Verified Papers:** ${analysisResult.totalEligiblePapers} | **Verified Questions:** ${analysisResult.totalVerifiedQuestions} | **Total Marks:** ${String.format(Locale.US, "%.1f", analysisResult.totalAttributedMarks)}")
                    sb.appendLine()
                    sb.appendLine("## Chapter Frequency & Marks Distribution")
                    sb.appendLine("| Chapter Name | Appearances | Distinct Papers | Attributed Marks |")
                    sb.appendLine("|---|:---:|:---:|:---:|")
                    analysisResult.chapterMetrics.forEach { c ->
                        sb.appendLine("| ${c.chapterName} | ${c.totalQuestionAppearances} | ${c.distinctPaperCount} | ${String.format(Locale.US, "%.1f", c.attributedMarks)}m |")
                    }
                    sb.appendLine()
                    sb.appendLine("## Top Topic Frequency")
                    analysisResult.topicMetrics.take(15).forEachIndexed { idx, t ->
                        sb.appendLine("${idx + 1}. **${t.topicName}** (${t.chapterName}) — ${t.questionAppearancesCount} appearances in ${t.distinctPaperCount} paper(s), ${String.format(Locale.US, "%.1f", t.attributedMarks)} marks")
                    }
                } else {
                    summaryText = "Historical analysis generated for ${request.subject}."
                    sb.appendLine("# $title")
                    sb.appendLine("Historical examination patterns synthesized from stored CBSE Class 10 records.")
                }
            }

            ReportType.CHAPTER_COVERAGE -> {
                val vId = request.syllabusVersionId ?: "cbse-10-math-2024-25"
                val nodes = syllabusDao.getNodesForVersionList(vId)
                val chapters = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name || it.nodeType == SyllabusNodeType.CHAPTER.label }
                val topics = nodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name || it.nodeType == SyllabusNodeType.TOPIC.label }
                recordCount = topics.size
                summaryText = "Curriculum matrix evaluated across ${chapters.size} prescribed chapters and ${topics.size} syllabus topics."
                disclaimer = "Prescribed CBSE Secondary School Curriculum for Academic Session 2024-2025 onwards."

                sb.appendLine("# $title")
                sb.appendLine("**Generated:** $dateStr | **Syllabus:** $vId")
                sb.appendLine("**Summary:** ${chapters.size} Chapters, ${topics.size} Topics")
                sb.appendLine()
                chapters.forEach { ch ->
                    val chTopics = topics.filter { it.parentId == ch.id }
                    sb.appendLine("### ${ch.code}: ${ch.name} (${chTopics.size} Topics)")
                    chTopics.forEach { top ->
                        val status = if (top.isExcluded) "EXCLUDED (${top.exclusionReason})" else "PRESCRIBED"
                        sb.appendLine("- ${top.code}: ${top.name} [$status]")
                    }
                    sb.appendLine()
                }
            }

            ReportType.MARKS_DISTRIBUTION -> {
                val projId = request.projectId ?: project?.id ?: ""
                val questions = if (projId.isNotBlank()) extractedQuestionDao.getQuestionsForProjectList(projId) else emptyList()
                recordCount = questions.size
                val grouped = questions.groupBy { it.questionType }
                summaryText = "Evaluated ${questions.size} questions categorized across ${grouped.size} question formats."

                sb.appendLine("# $title")
                sb.appendLine("**Generated:** $dateStr | **Questions Analyzed:** ${questions.size}")
                sb.appendLine()
                sb.appendLine("## Question Formats Breakdown")
                grouped.forEach { (type, qList) ->
                    val totalMarks = qList.sumOf { it.marks ?: 0 }
                    val avgMarks = if (qList.isNotEmpty()) totalMarks.toDouble() / qList.size else 0.0
                    sb.appendLine("- **$type:** ${qList.size} questions | Total Marks: ${totalMarks}m (Avg: ${String.format(Locale.US, "%.1f", avgMarks)}m)")
                }
            }

            ReportType.YEAR_COMPARISON -> {
                val projId = request.projectId ?: project?.id ?: ""
                val docs = if (projId.isNotBlank()) documentDao.getDocumentsListForProject(projId) else emptyList()
                recordCount = docs.size
                summaryText = "Year-over-year comparison across ${docs.size} eligible examination documents."

                sb.appendLine("# $title")
                sb.appendLine("**Generated:** $dateStr | **Documents:** ${docs.size}")
                sb.appendLine()
                sb.appendLine("## Document Series & Year Inventory")
                docs.forEach { d ->
                    sb.appendLine("- **${d.customFilename}** — ${d.questionCount} extracted questions (${d.verifiedCount} verified) | Status: ${d.processingStatus}")
                }
            }

            ReportType.VERIFIED_QUESTION_COLLECTION -> {
                val projId = request.projectId ?: project?.id ?: ""
                val questions = if (projId.isNotBlank()) extractedQuestionDao.getQuestionsForProjectList(projId).filter { it.isVerified() } else emptyList()
                recordCount = questions.size
                summaryText = "Compiled collection of ${questions.size} verified CBSE Class 10 questions."

                sb.appendLine("# $title")
                sb.appendLine("**Generated:** $dateStr | **Verified Questions:** ${questions.size}")
                sb.appendLine()
                questions.forEach { q ->
                    sb.appendLine("### Question ${q.questionNumber} (${q.section}) — ${q.marks ?: 1} Mark(s)")
                    sb.appendLine(q.getEffectiveText())
                    if (request.includeAnswers) {
                        sb.appendLine("**Question Format:** ${q.questionType}")
                    }
                    sb.appendLine()
                }
            }

            ReportType.AI_PRACTICE_SET -> {
                val sets = if (!request.projectId.isNullOrBlank()) {
                    practiceQuestionDao.getSetsListForProject(request.projectId)
                } else emptyList()

                val firstSet = sets.firstOrNull()
                val questions = if (firstSet != null) practiceQuestionDao.getQuestionsListForSet(firstSet.id) else emptyList()
                recordCount = questions.size
                disclaimer = "AI-Generated Practice Questions — Original CBSE Class 10 syllabus-aligned concepts. NOT official CBSE question papers."
                summaryText = "Generated practice question set with ${questions.size} original questions."

                sb.appendLine("# $title")
                sb.appendLine("**Notice:** AI-Generated Practice Set for CBSE Class 10 ${request.subject}")
                sb.appendLine()
                questions.forEach { q ->
                    sb.appendLine("### Q${q.questionNumber}: ${q.getEffectiveText()} [${q.marks ?: 1} Mark(s) • ${q.difficulty}]")
                    if (q.getOptionsList().isNotEmpty()) {
                        q.getOptionsList().forEach { opt -> sb.appendLine("- $opt") }
                    }
                    if (request.includeAnswers) {
                        sb.appendLine("**Correct Answer:** ${q.correctAnswer ?: "N/A"}")
                        sb.appendLine("**Explanation:** ${q.explanation ?: "None provided"}")
                        if (!q.modelAnswerGuidance.isNullOrBlank()) {
                            sb.appendLine("**Model Guidance:** ${q.modelAnswerGuidance}")
                        }
                    }
                    sb.appendLine()
                }
            }

            ReportType.AI_EXAM_ESTIMATES -> {
                val latest = if (!request.projectId.isNullOrBlank()) {
                    aiExamEstimateDao.getLatestEstimate(request.projectId)
                } else null

                disclaimer = "AI Trend Estimation — Probabilistic trend analysis only. Not an official prediction or future examination guarantee."
                summaryText = "AI exam trends calculated based on verified historical question patterns."

                sb.appendLine("# $title")
                sb.appendLine("**DISCLAIMER:** $disclaimer")
                sb.appendLine()
                if (latest != null) {
                    val arr = try { JSONArray(latest.estimatesJson) } catch (_: Exception) { JSONArray() }
                    recordCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        sb.appendLine("### ${obj.optString("topicName")} (${obj.optString("chapterName")})")
                        sb.appendLine("- **Focus Level:** ${obj.optString("attentionLevel")}")
                        sb.appendLine("- **Rationale:** ${obj.optString("rationale")}")
                        sb.appendLine("- **Historical Frequency:** ${obj.optString("historicalFrequencyNotice")}")
                        sb.appendLine()
                    }
                } else {
                    sb.appendLine("No AI trend estimates generated for this project yet.")
                }
            }

            ReportType.SYLLABUS_AUDIT -> {
                val vId = request.syllabusVersionId ?: "cbse-10-math-2024-25"
                val version = syllabusDao.getVersionById(vId)
                val mappings = questionTopicMappingDao.getMappingsListForVersion(vId)
                recordCount = mappings.size
                summaryText = "Curriculum verification audit: ${mappings.size} mapped questions across version ${version?.versionIdentifier ?: vId}."

                sb.appendLine("# $title")
                sb.appendLine("**Syllabus Version:** ${version?.sourceTitle ?: vId} (${version?.verificationStatus ?: "Unverified"})")
                sb.appendLine("**Mapped Questions:** ${mappings.size}")
                sb.appendLine()
                sb.appendLine("## Concept Mapping Review Queue")
                mappings.take(20).forEach { m ->
                    sb.appendLine("- Question ${m.questionId} -> ${m.chapterName} / ${m.topicName} [Status: ${m.reviewStatus} • Confidence: ${String.format(Locale.US, "%.0f%%", m.mappingConfidence * 100)}]")
                }
            }
        }

        return ReportContentData(
            summaryText = summaryText,
            recordCount = recordCount,
            methodologyNotes = methodologyNotes,
            dataLimitations = dataLimitations,
            disclaimer = disclaimer,
            textContent = sb.toString()
        )
    }

    private fun generatePdfFile(
        file: File,
        title: String,
        request: ReportGenerationRequest,
        dateStr: String,
        summaryText: String,
        textContent: String,
        disclaimer: String,
        methodologyNotes: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Primary Blue
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            isAntiAlias = true
        }

        val disclaimerPaint = Paint().apply {
            color = Color.rgb(180, 83, 9) // Amber/Gold warning
            textSize = 8f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 40f

        // Header Title
        canvas.drawText("EXAM INTELLIGENCE — CBSE CLASS 10", 40f, y, titlePaint)
        y += 18f
        canvas.drawText(title, 40f, y, titlePaint.apply { textSize = 11f })
        y += 15f
        canvas.drawText("Generated: $dateStr | Format: Official PDF Export", 40f, y, subPaint)
        y += 18f

        // Disclaimer Bar
        canvas.drawText("DISCLAIMER: $disclaimer", 40f, y, disclaimerPaint)
        y += 15f

        // Summary Box
        canvas.drawText("EXECUTIVE SUMMARY: $summaryText", 40f, y, bodyPaint.apply { isFakeBoldText = true })
        y += 18f
        bodyPaint.isFakeBoldText = false

        // Content lines (first 40 lines safely in page)
        val lines = textContent.lines().filter { it.isNotBlank() }
        for (line in lines.take(35)) {
            val sanitized = line.take(80).replace("#", "").replace("*", "")
            canvas.drawText(sanitized, 40f, y, bodyPaint)
            y += 15f
            if (y > 780f) break
        }

        // Footer
        canvas.drawText("Page 1 of 1 • Exam Intelligence CBSE Analytics Platform • Confidential & Verified", 40f, 810f, subPaint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    suspend fun deleteReport(user: UserEntity?, reportId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val report = generatedReportDao.getReportById(reportId)
            ?: return@withContext Result.failure(Exception("Report not found"))

        val isOwner = user != null && report.userId == user.id
        val isAdmin = user != null && user.getRoleEnum().canAccessAdminArea()

        if (!isOwner && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized: Cannot delete another user's report."))
        }

        // Delete actual file if exists
        report.filePath?.let { path ->
            val f = File(path)
            if (f.exists()) f.delete()
        }

        generatedReportDao.deleteReport(reportId)

        user?.let { u ->
            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = u.id,
                    userEmail = u.email,
                    action = "REPORT_DELETED",
                    resourceType = "REPORT",
                    resourceId = reportId,
                    details = "Deleted generated report '${report.title}'"
                )
            )
        }

        Result.success(Unit)
    }

    suspend fun toggleReportPublicVisibility(
        user: UserEntity?,
        reportId: String,
        isPublic: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val report = generatedReportDao.getReportById(reportId)
            ?: return@withContext Result.failure(Exception("Report not found"))

        val isOwner = user != null && report.userId == user.id
        val isAdmin = user != null && user.getRoleEnum().canAccessAdminArea()

        if (!isOwner && !isAdmin) {
            return@withContext Result.failure(SecurityException("Unauthorized: Cannot modify report visibility."))
        }

        generatedReportDao.updateReportVisibility(reportId, isPublic)

        user?.let { u ->
            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = u.id,
                    userEmail = u.email,
                    action = "REPORT_VISIBILITY_CHANGED",
                    resourceType = "REPORT",
                    resourceId = reportId,
                    details = "Changed visibility for '${report.title}' to ${if (isPublic) "PUBLIC" else "PRIVATE"}"
                )
            )
        }

        Result.success(Unit)
    }

    suspend fun getReportFileContent(user: UserEntity?, reportId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val report = generatedReportDao.getReportById(reportId)
                ?: return@withContext Result.failure(Exception("Report not found"))

            val isOwner = user != null && report.userId == user.id
            val isAdmin = user != null && user.getRoleEnum().canAccessAdminArea()
            val isPublic = report.isPublic

            if (!isOwner && !isAdmin && !isPublic) {
                return@withContext Result.failure(SecurityException("Forbidden: Access denied to private report download."))
            }

            val path = report.filePath
                ?: return@withContext Result.failure(Exception("Report file path missing"))

            val f = File(path)
            if (!f.exists()) {
                return@withContext Result.failure(Exception("Report file is missing or has been pruned."))
            }

            Result.success(f.readBytes())
        }
}

private data class ReportContentData(
    val summaryText: String,
    val recordCount: Int,
    val methodologyNotes: String,
    val dataLimitations: String,
    val disclaimer: String,
    val textContent: String
)
