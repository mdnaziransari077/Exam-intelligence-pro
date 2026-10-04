package com.example.data.repository

import android.content.Context
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ExtractedQuestionDao
import com.example.data.local.HistoricalAnalysisDao
import com.example.data.local.ProjectDao
import com.example.data.local.QuestionTopicMappingDao
import com.example.data.local.SyllabusDao
import com.example.data.model.AnalysisScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChapterHistoricalMetrics
import com.example.data.model.DataQualityReport
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.HistoricalAnalysisResult
import com.example.data.model.HistoricalAnalysisRunEntity
import com.example.data.model.HistoricalTopicMetrics
import com.example.data.model.MappingReviewStatus
import com.example.data.model.MappingVerificationScope
import com.example.data.model.MarksAttributionMethod
import com.example.data.model.ProcessingStatus
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.QuestionTypeMetrics
import com.example.data.model.QuestionVerificationScope
import com.example.data.model.ScopeSummary
import com.example.data.model.SuspectedDuplicateDoc
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusNodeType
import com.example.data.model.UserEntity
import com.example.data.model.VerificationStatus
import com.example.data.model.YearComparisonMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HistoricalAnalysisRepository(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val documentDao: DocumentDao,
    private val extractedQuestionDao: ExtractedQuestionDao,
    private val questionTopicMappingDao: QuestionTopicMappingDao,
    private val syllabusDao: SyllabusDao,
    private val historicalAnalysisDao: HistoricalAnalysisDao,
    private val auditLogDao: AuditLogDao
) {

    fun getLatestRunForProject(projectId: String): Flow<HistoricalAnalysisRunEntity?> {
        return historicalAnalysisDao.getLatestRunForProject(projectId).flowOn(Dispatchers.IO)
    }

    suspend fun markRunsStale(projectId: String) = withContext(Dispatchers.IO) {
        historicalAnalysisDao.markRunsStaleForProject(projectId)
    }

    /**
     * Compute pre-analysis scope summary before running deep analytics.
     */
    suspend fun computeScopeSummary(
        scope: AnalysisScope,
        user: UserEntity?
    ): Result<ScopeSummary> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(scope.projectId)
                ?: return@withContext Result.failure(IllegalArgumentException("Project not found"))

            val allDocs = documentDao.getDocumentsListForProject(scope.projectId)
            val eligibleDocs = if (scope.selectedDocumentIds.isNotEmpty()) {
                allDocs.filter { it.id in scope.selectedDocumentIds }
            } else {
                allDocs
            }

            val processedDocs = eligibleDocs.filter {
                it.processingStatus == ProcessingStatus.VERIFIED.label ||
                it.processingStatus == ProcessingStatus.AWAITING_REVIEW.label ||
                it.processingStatus == ProcessingStatus.PARTIALLY_REVIEWED.label
            }

            val docIds = processedDocs.map { it.id }
            val allQuestions = mutableListOf<ExtractedQuestionEntity>()
            for (dId in docIds) {
                allQuestions.addAll(extractedQuestionDao.getQuestionsListForDocument(dId))
            }

            val verifiedQuestions = allQuestions.filter { it.verificationStatus == VerificationStatus.APPROVED.label }
            val awaitingReview = allQuestions.filter { it.verificationStatus != VerificationStatus.APPROVED.label }
            val unknownMarks = allQuestions.filter { it.isMarksUnknown() }

            // Mappings for the selected syllabus version
            val allMappings = mutableListOf<QuestionTopicMappingEntity>()
            for (q in allQuestions) {
                val maps = questionTopicMappingDao.getMappingsListForQuestion(q.id)
                allMappings.addAll(maps.filter { it.syllabusVersionId == scope.syllabusVersionId })
            }

            val verifiedMappings = allMappings.filter { it.reviewStatus == MappingReviewStatus.APPROVED.label }
            val aiMappings = allMappings.filter { it.reviewStatus == MappingReviewStatus.SUGGESTED.label }

            val syllabusVer = syllabusDao.getVersionById(scope.syllabusVersionId)

            val suspectedDuplicates = detectSuspectedDuplicates(eligibleDocs)

            val summary = ScopeSummary(
                totalDocuments = eligibleDocs.size,
                successfullyProcessedDocuments = processedDocs.size,
                totalQuestionsExtracted = allQuestions.size,
                verifiedQuestionsCount = verifiedQuestions.size,
                questionsWithVerifiedMappingsCount = verifiedMappings.map { it.questionId }.distinct().size,
                questionsWithAiSuggestedMappingsCount = aiMappings.map { it.questionId }.distinct().size,
                questionsWithUnknownMarksCount = unknownMarks.size,
                questionsAwaitingReviewCount = awaitingReview.size,
                ineligibleOrIncompleteExcludedCount = (allDocs.size - eligibleDocs.size) + (eligibleDocs.size - processedDocs.size),
                syllabusVersionId = scope.syllabusVersionId,
                syllabusVersionName = syllabusVer?.sourceTitle ?: "CBSE Class 10 Syllabus",
                syllabusIsVerified = syllabusVer?.isVerified() == true,
                suspectedDuplicateDocumentsCount = suspectedDuplicates.size
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes the comprehensive historical analysis engine adhering strictly to Phase 4 rules.
     */
    suspend fun executeAnalysis(
        scope: AnalysisScope,
        user: UserEntity?
    ): Result<HistoricalAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(scope.projectId)
                ?: return@withContext Result.failure(IllegalArgumentException("Project not found"))

            // Authorization check
            val isOwner = user != null && project.ownerId == user.id
            val isAdmin = user != null && user.getRoleEnum().canManageAllProjects()
            val isPublic = project.visibility == "PUBLIC"
            if (!isOwner && !isAdmin && !isPublic) {
                return@withContext Result.failure(SecurityException("Unauthorized: Cannot analyze private project."))
            }

            // Load Syllabus Nodes
            val syllabusVersion = syllabusDao.getVersionById(scope.syllabusVersionId)
                ?: return@withContext Result.failure(IllegalArgumentException("Syllabus version not found"))

            val allNodes = syllabusDao.getNodesListForVersion(scope.syllabusVersionId)
            val chapterNodes = allNodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.label }
            val topicNodes = allNodes.filter { it.nodeType == SyllabusNodeType.TOPIC.label }

            // Load Documents
            val allProjectDocs = documentDao.getDocumentsListForProject(scope.projectId)
            val eligibleDocs = if (scope.selectedDocumentIds.isNotEmpty()) {
                allProjectDocs.filter { it.id in scope.selectedDocumentIds }
            } else {
                allProjectDocs
            }.filter {
                // Must be processed without failure
                it.processingStatus != ProcessingStatus.FAILED.label &&
                it.processingStatus != ProcessingStatus.UPLOADED.label &&
                it.processingStatus != ProcessingStatus.QUEUED.label
            }

            val docMap = eligibleDocs.associateBy { it.id }

            // Extract all questions in scope
            val rawQuestions = mutableListOf<ExtractedQuestionEntity>()
            for (doc in eligibleDocs) {
                rawQuestions.addAll(extractedQuestionDao.getQuestionsListForDocument(doc.id))
            }

            // Filter questions based on QuestionVerificationScope
            val questionsInScope = when (scope.questionVerificationScope) {
                QuestionVerificationScope.VERIFIED_ONLY -> rawQuestions.filter {
                    it.verificationStatus == VerificationStatus.APPROVED.label
                }
                QuestionVerificationScope.ALL_EXTRACTIONS -> rawQuestions
            }

            val questionMap = questionsInScope.associateBy { it.id }

            // Load mappings for questions in scope
            val rawMappings = mutableListOf<QuestionTopicMappingEntity>()
            for (q in questionsInScope) {
                val qMappings = questionTopicMappingDao.getMappingsListForQuestion(q.id)
                rawMappings.addAll(qMappings.filter { it.syllabusVersionId == scope.syllabusVersionId })
            }

            // Filter mappings based on MappingVerificationScope
            val eligibleMappings = when (scope.mappingVerificationScope) {
                MappingVerificationScope.APPROVED_ONLY -> rawMappings.filter {
                    it.reviewStatus == MappingReviewStatus.APPROVED.label && !it.isOutsideSyllabus
                }
                MappingVerificationScope.INCLUDE_AI_SUGGESTIONS -> rawMappings.filter {
                    (it.reviewStatus == MappingReviewStatus.APPROVED.label ||
                     it.reviewStatus == MappingReviewStatus.SUGGESTED.label) && !it.isOutsideSyllabus
                }
            }

            // Group mappings by Question ID to handle Multi-topic attribution (Rule G)
            val mappingsByQuestion = eligibleMappings.groupBy { it.questionId }

            // Group mappings by Topic ID
            val mappingsByTopic = eligibleMappings.groupBy { it.topicId }

            // Track repeated questions (Rule C)
            val questionTextToDocs = mutableMapOf<String, MutableSet<String>>()
            for (q in questionsInScope) {
                val normText = normalizeQuestionText(q.getEffectiveText())
                if (normText.length > 20) {
                    questionTextToDocs.getOrPut(normText) { mutableSetOf() }.add(q.sourceDocumentId)
                }
            }

            // Track candidate available marks vs printed marks (Rule F)
            var totalPrintedMarks = 0
            var candidateAvailableMarksEstimate = 0
            for (q in questionsInScope) {
                val qMarks = q.marks ?: 0
                totalPrintedMarks += qMarks
                // Internal choice accounting
                if (q.internalChoice != null && q.internalChoice.contains("OR", ignoreCase = true)) {
                    // One question with an internal alternative does not duplicate marks for candidate
                    candidateAvailableMarksEstimate += qMarks
                } else {
                    candidateAvailableMarksEstimate += qMarks
                }
            }

            // Build Topic Metrics
            val topicMetricsList = mutableListOf<HistoricalTopicMetrics>()

            for (topicNode in topicNodes) {
                val topicMappings = mappingsByTopic[topicNode.id] ?: emptyList()
                val parentChapter = chapterNodes.find { it.id == topicNode.parentId }

                val associatedQuestions = topicMappings.mapNotNull { questionMap[it.questionId] }.distinctBy { it.id }
                val associatedDocIds = associatedQuestions.map { it.sourceDocumentId }.distinct()

                // Calculate distinct years and paper sets
                val distinctYears = associatedDocIds.mapNotNull { docId ->
                    docMap[docId]?.let { detectDocumentYear(it, project.academicYear) }
                }.distinct().sorted()

                val frequencyByYear = mutableMapOf<String, Int>()
                val frequencyByPaperSet = mutableMapOf<String, Int>()
                val frequencyByQuestionType = mutableMapOf<String, Int>()
                val marksByQuestionType = mutableMapOf<String, Double>()
                val frequencyByMarksCat = mutableMapOf<String, Int>()

                var attributedMarksSum = 0.0
                var printedMarksSum = 0
                val validMarksList = mutableListOf<Int>()
                var unknownMarksCount = 0
                var repeatedQuestionCount = 0

                for (q in associatedQuestions) {
                    val qMarks = q.marks
                    if (qMarks == null || qMarks <= 0) {
                        unknownMarksCount++
                        frequencyByMarksCat["Unknown Marks"] = (frequencyByMarksCat["Unknown Marks"] ?: 0) + 1
                    } else {
                        validMarksList.add(qMarks)
                        printedMarksSum += qMarks
                        frequencyByMarksCat["${qMarks}M"] = (frequencyByMarksCat["${qMarks}M"] ?: 0) + 1
                    }

                    // Multi-Topic Attribution calculation (Rule G)
                    val qMappingCount = mappingsByQuestion[q.id]?.size ?: 1
                    val marksToAdd = when (scope.attributionMethod) {
                        MarksAttributionMethod.FULL_OVERLAPPING -> (qMarks ?: 0).toDouble()
                        MarksAttributionMethod.FRACTIONAL_SPLIT -> {
                            if (qMappingCount > 0) (qMarks ?: 0).toDouble() / qMappingCount
                            else (qMarks ?: 0).toDouble()
                        }
                    }
                    attributedMarksSum += marksToAdd

                    // Question type frequency
                    val qType = q.questionType
                    frequencyByQuestionType[qType] = (frequencyByQuestionType[qType] ?: 0) + 1
                    marksByQuestionType[qType] = (marksByQuestionType[qType] ?: 0.0) + marksToAdd

                    // Year & Paper Set frequency
                    val doc = docMap[q.sourceDocumentId]
                    if (doc != null) {
                        val year = detectDocumentYear(doc, project.academicYear)
                        frequencyByYear[year] = (frequencyByYear[year] ?: 0) + 1

                        val set = detectPaperSet(doc)
                        frequencyByPaperSet[set] = (frequencyByPaperSet[set] ?: 0) + 1
                    }

                    // Repeated question check (Rule C)
                    val normText = normalizeQuestionText(q.getEffectiveText())
                    if ((questionTextToDocs[normText]?.size ?: 0) > 1) {
                        repeatedQuestionCount++
                    }
                }

                val avgMarksPerPaper = if (associatedDocIds.isNotEmpty()) {
                    attributedMarksSum / associatedDocIds.size
                } else 0.0

                val uniqueQuestionsCount = associatedQuestions.size - (if (repeatedQuestionCount > 1) repeatedQuestionCount / 2 else 0)

                topicMetricsList.add(
                    HistoricalTopicMetrics(
                        topicId = topicNode.id,
                        topicName = topicNode.name,
                        chapterId = parentChapter?.id ?: "unknown_ch",
                        chapterName = parentChapter?.name ?: "General / Unassigned",
                        isExcludedFromSyllabus = topicNode.isExcluded,
                        exclusionReason = topicNode.exclusionReason,
                        questionAppearancesCount = associatedQuestions.size,
                        distinctPaperCount = associatedDocIds.size,
                        distinctYearsCount = distinctYears.size,
                        distinctYears = distinctYears,
                        uniqueQuestionsCount = maxOf(0, uniqueQuestionsCount),
                        repeatedQuestionsCount = repeatedQuestionCount,
                        attributedMarks = attributedMarksSum,
                        printedMarksSum = printedMarksSum,
                        averageMarksPerPaper = avgMarksPerPaper,
                        minQuestionMarks = validMarksList.minOrNull(),
                        maxQuestionMarks = validMarksList.maxOrNull(),
                        questionsWithUnknownMarks = unknownMarksCount,
                        frequencyByQuestionType = frequencyByQuestionType,
                        marksByQuestionType = marksByQuestionType,
                        frequencyByYear = frequencyByYear,
                        frequencyByPaperSet = frequencyByPaperSet,
                        frequencyByMarksCategory = frequencyByMarksCat,
                        associatedQuestionIds = associatedQuestions.map { it.id }
                    )
                )
            }

            // Build Chapter Metrics
            val chapterMetricsList = mutableListOf<ChapterHistoricalMetrics>()
            for (ch in chapterNodes) {
                val chTopics = topicMetricsList.filter { it.chapterId == ch.id }
                val totalAppearances = chTopics.sumOf { it.questionAppearancesCount }
                val chAttributedMarks = chTopics.sumOf { it.attributedMarks }
                val distinctPapers = chTopics.flatMap { t ->
                    t.associatedQuestionIds.mapNotNull { questionMap[it]?.sourceDocumentId }
                }.distinct().size

                val testedTopics = chTopics.count { it.questionAppearancesCount > 0 }
                val untestedTopics = chTopics.count { it.questionAppearancesCount == 0 && !it.isExcludedFromSyllabus }

                chapterMetricsList.add(
                    ChapterHistoricalMetrics(
                        chapterId = ch.id,
                        chapterName = ch.name,
                        totalQuestionAppearances = totalAppearances,
                        distinctPaperCount = distinctPapers,
                        attributedMarks = chAttributedMarks,
                        topicsCount = chTopics.size,
                        topicsWithQuestionsCount = testedTopics,
                        untestedTopicsCount = untestedTopics,
                        topicsList = chTopics
                    )
                )
            }

            // Build Question Type Metrics
            val questionTypeMetricsList = mutableListOf<QuestionTypeMetrics>()
            val allQuestionTypes = questionsInScope.map { it.questionType }.distinct()
            val totalInScopeCount = questionsInScope.size

            for (qType in allQuestionTypes) {
                val typeQuestions = questionsInScope.filter { it.questionType == qType }
                val distinctPapers = typeQuestions.map { it.sourceDocumentId }.distinct().size

                var typeAttributedMarks = 0.0
                val chapters = mutableSetOf<String>()
                val yearFreq = mutableMapOf<String, Int>()

                for (q in typeQuestions) {
                    val qMappings = mappingsByQuestion[q.id] ?: emptyList()
                    val qMarks = q.marks ?: 0

                    val marksToAdd = when (scope.attributionMethod) {
                        MarksAttributionMethod.FULL_OVERLAPPING -> qMarks.toDouble()
                        MarksAttributionMethod.FRACTIONAL_SPLIT -> {
                            if (qMappings.isNotEmpty()) qMarks.toDouble()
                            else qMarks.toDouble()
                        }
                    }
                    typeAttributedMarks += marksToAdd

                    for (m in qMappings) {
                        chapters.add(m.chapterName)
                    }

                    docMap[q.sourceDocumentId]?.let { d ->
                        val yr = detectDocumentYear(d, project.academicYear)
                        yearFreq[yr] = (yearFreq[yr] ?: 0) + 1
                    }
                }

                val pct = if (totalInScopeCount > 0) (typeQuestions.size.toDouble() / totalInScopeCount) * 100.0 else 0.0

                questionTypeMetricsList.add(
                    QuestionTypeMetrics(
                        questionType = qType,
                        questionCount = typeQuestions.size,
                        distinctPaperCount = distinctPapers,
                        totalAttributedMarks = typeAttributedMarks,
                        percentageOfTotalQuestions = pct,
                        associatedChapters = chapters.toList().sorted(),
                        frequencyByYear = yearFreq
                    )
                )
            }

            // Build Year-wise Comparison Metrics
            val yearComparisonMetricsList = mutableListOf<YearComparisonMetrics>()
            val allYears = eligibleDocs.map { detectDocumentYear(it, project.academicYear) }.distinct().sorted()

            for (year in allYears) {
                val yearDocs = eligibleDocs.filter { detectDocumentYear(it, project.academicYear) == year }
                val yearDocIds = yearDocs.map { it.id }.toSet()
                val yearQuestions = questionsInScope.filter { it.sourceDocumentId in yearDocIds }

                val yearMappings = yearQuestions.flatMap { mappingsByQuestion[it.id] ?: emptyList() }
                val chaptersCovered = yearMappings.map { it.chapterId }.distinct().size
                val topicsCovered = yearMappings.map { it.topicId }.distinct().size

                val totalMarks = yearQuestions.sumOf { it.marks ?: 0 }.toDouble()

                val qTypeCounts = yearQuestions.groupBy { it.questionType }.mapValues { it.value.size }
                val topChaps = yearMappings.groupBy { it.chapterName }
                    .entries.sortedByDescending { it.value.size }
                    .take(3).map { it.key }

                yearComparisonMetricsList.add(
                    YearComparisonMetrics(
                        year = year,
                        papersCount = yearDocs.size,
                        questionsCount = yearQuestions.size,
                        totalAttributedMarks = totalMarks,
                        chaptersCoveredCount = chaptersCovered,
                        topicsCoveredCount = topicsCovered,
                        questionTypesCount = qTypeCounts,
                        topChapters = topChaps
                    )
                )
            }

            // Data Quality Report (Part 10)
            val suspectedDuplicates = detectSuspectedDuplicates(eligibleDocs)
            val unmappedCount = questionsInScope.count { (mappingsByQuestion[it.id]?.size ?: 0) == 0 }
            val uncertainTypesCount = questionsInScope.count { it.questionType.contains("Unknown", ignoreCase = true) }
            val questionsWithUnknownMarks = questionsInScope.count { it.isMarksUnknown() }

            val totalRawQ = rawQuestions.size
            val verifiedQCount = rawQuestions.count { it.verificationStatus == VerificationStatus.APPROVED.label }
            val qVerificationPct = if (totalRawQ > 0) (verifiedQCount.toFloat() / totalRawQ) * 100f else 0f

            val totalMappingsCount = rawMappings.size
            val verifiedMappingsCount = rawMappings.count { it.reviewStatus == MappingReviewStatus.APPROVED.label }
            val mappingVerificationPct = if (totalMappingsCount > 0) (verifiedMappingsCount.toFloat() / totalMappingsCount) * 100f else 0f

            val verifiedMarksCount = questionsInScope.count { !it.isMarksUnknown() }
            val marksVerificationPct = if (questionsInScope.isNotEmpty()) (verifiedMarksCount.toFloat() / questionsInScope.size) * 100f else 0f

            val missingYearCount = eligibleDocs.count { detectDocumentYear(it, project.academicYear) == "Unknown Year" }
            val missingSetCount = eligibleDocs.count { detectPaperSet(it) == "Standard / Unspecified" }

            val qualityBadges = mutableListOf<String>()
            if (scope.questionVerificationScope == QuestionVerificationScope.VERIFIED_ONLY &&
                scope.mappingVerificationScope == MappingVerificationScope.APPROVED_ONLY &&
                qVerificationPct >= 99f && mappingVerificationPct >= 99f
            ) {
                qualityBadges.add("Verified Historical Data")
            } else {
                qualityBadges.add("Review Required")
            }

            if (eligibleDocs.size < 5) {
                qualityBadges.add("Partial Collection")
            }

            if (scope.mappingVerificationScope == MappingVerificationScope.INCLUDE_AI_SUGGESTIONS) {
                qualityBadges.add("Exploratory AI Suggestions")
            }

            val dataQualityReport = DataQualityReport(
                paperCollectionCompletenessPercent = minOf(100f, (eligibleDocs.size / 5f) * 100f),
                questionExtractionCompletionPercent = 100f,
                questionVerificationCompletionPercent = qVerificationPct,
                mappingVerificationCompletionPercent = mappingVerificationPct,
                marksVerificationCompletionPercent = marksVerificationPct,
                missingYearMetadataCount = missingYearCount,
                missingPaperSetMetadataCount = missingSetCount,
                suspectedDuplicateDocs = suspectedDuplicates,
                unmappedQuestionsCount = unmappedCount,
                uncertainQuestionTypesCount = uncertainTypesCount,
                qualityBadges = qualityBadges,
                methodologyNotice = "Descriptive historical analytics based solely on verified uploaded examination records. Frequencies do NOT represent predictive probabilities of future CBSE examinations."
            )

            val untestedTopicsList = topicMetricsList.filter { it.questionAppearancesCount == 0 && !it.isExcludedFromSyllabus }

            val scopeSummary = ScopeSummary(
                totalDocuments = eligibleDocs.size,
                successfullyProcessedDocuments = eligibleDocs.size,
                totalQuestionsExtracted = rawQuestions.size,
                verifiedQuestionsCount = verifiedQCount,
                questionsWithVerifiedMappingsCount = verifiedMappingsCount,
                questionsWithAiSuggestedMappingsCount = rawMappings.count { it.reviewStatus == MappingReviewStatus.SUGGESTED.label },
                questionsWithUnknownMarksCount = questionsWithUnknownMarks,
                questionsAwaitingReviewCount = rawQuestions.count { it.verificationStatus != VerificationStatus.APPROVED.label },
                ineligibleOrIncompleteExcludedCount = allProjectDocs.size - eligibleDocs.size,
                syllabusVersionId = scope.syllabusVersionId,
                syllabusVersionName = syllabusVersion.sourceTitle,
                syllabusIsVerified = syllabusVersion.isVerified(),
                suspectedDuplicateDocumentsCount = suspectedDuplicates.size
            )

            val runId = UUID.randomUUID().toString()

            val result = HistoricalAnalysisResult(
                runId = runId,
                projectId = scope.projectId,
                scope = scope,
                scopeSummary = scopeSummary,
                executionTimestamp = System.currentTimeMillis(),
                status = "COMPLETED",
                isStale = false,
                staleReason = null,
                totalEligiblePapers = eligibleDocs.size,
                totalVerifiedQuestions = questionsInScope.size,
                totalVerifiedMappings = eligibleMappings.size,
                totalAttributedMarks = topicMetricsList.sumOf { it.attributedMarks },
                totalPrintedMarksAvailable = totalPrintedMarks,
                candidateAvailableMarksEstimate = candidateAvailableMarksEstimate,
                chapterMetrics = chapterMetricsList,
                topicMetrics = topicMetricsList,
                questionTypeMetrics = questionTypeMetricsList,
                yearComparisonMetrics = yearComparisonMetricsList,
                dataQualityReport = dataQualityReport,
                untestedTopicsCount = untestedTopicsList.size,
                untestedTopicsList = untestedTopicsList
            )

            // Persist run metadata in Room (Part 9)
            historicalAnalysisDao.insertRun(
                HistoricalAnalysisRunEntity(
                    id = runId,
                    projectId = scope.projectId,
                    syllabusVersionId = scope.syllabusVersionId,
                    scopeJson = serializeScope(scope),
                    status = "COMPLETED",
                    attributionMethod = scope.attributionMethod.name,
                    includeAiSuggestions = scope.mappingVerificationScope == MappingVerificationScope.INCLUDE_AI_SUGGESTIONS,
                    totalEligiblePapers = eligibleDocs.size,
                    totalVerifiedQuestions = questionsInScope.size,
                    totalVerifiedMappings = eligibleMappings.size,
                    totalAttributedMarks = result.totalAttributedMarks,
                    summaryJson = serializeRunSummary(result),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user?.id ?: "guest",
                    userEmail = user?.email ?: "guest@cbse.org",
                    action = "HISTORICAL_ANALYSIS_EXECUTED",
                    resourceType = "PROJECT",
                    resourceId = scope.projectId,
                    details = "Executed Phase 4 Historical Analysis on ${eligibleDocs.size} papers, ${questionsInScope.size} questions."
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detects suspected duplicate documents in the collection (Rule D).
     */
    fun detectSuspectedDuplicates(docs: List<DocumentEntity>): List<SuspectedDuplicateDoc> {
        val duplicates = mutableListOf<SuspectedDuplicateDoc>()
        for (i in docs.indices) {
            for (j in i + 1 until docs.size) {
                val d1 = docs[i]
                val d2 = docs[j]

                val isSameName = d1.originalFilename.equals(d2.originalFilename, ignoreCase = true)
                val isSameSize = d1.fileSize == d2.fileSize && d1.fileSize > 0
                val isSameQCount = d1.questionCount > 0 && d1.questionCount == d2.questionCount

                if (isSameName) {
                    duplicates.add(
                        SuspectedDuplicateDoc(
                            doc1Id = d1.id,
                            doc1Name = d1.originalFilename,
                            doc2Id = d2.id,
                            doc2Name = d2.originalFilename,
                            reason = "Identical filename detected in project collection"
                        )
                    )
                } else if (isSameSize && isSameQCount) {
                    duplicates.add(
                        SuspectedDuplicateDoc(
                            doc1Id = d1.id,
                            doc1Name = d1.originalFilename,
                            doc2Id = d2.id,
                            doc2Name = d2.originalFilename,
                            reason = "Identical file size (${d1.fileSize} B) and question count (${d1.questionCount})"
                        )
                    )
                }
            }
        }
        return duplicates
    }

    /**
     * Extracts or detects examination year from document metadata and filename.
     */
    fun detectDocumentYear(doc: DocumentEntity, fallbackProjectYear: String): String {
        val textToSearch = "${doc.originalFilename} ${doc.extractedText?.take(500) ?: ""}"

        // Look for 2018-2026
        val yearRegex = "(201[89]|202[0-9])".toRegex()
        val match = yearRegex.find(textToSearch)
        if (match != null) {
            return match.value
        }

        // Fallback to project academic year
        val projYearMatch = yearRegex.find(fallbackProjectYear)
        return projYearMatch?.value ?: "2024"
    }

    /**
     * Detects paper set/series from filename and header text.
     */
    fun detectPaperSet(doc: DocumentEntity): String {
        val text = "${doc.originalFilename} ${doc.extractedText?.take(500) ?: ""}".lowercase()
        return when {
            text.contains("set 1") || text.contains("set-1") || text.contains("set_1") -> "Set 1"
            text.contains("set 2") || text.contains("set-2") || text.contains("set_2") -> "Set 2"
            text.contains("set 3") || text.contains("set-3") || text.contains("set_3") -> "Set 3"
            text.contains("sample") || text.contains("sqp") -> "Official Sample Paper (SQP)"
            text.contains("compartment") -> "Compartment / Supplementary"
            text.contains("marking") -> "Marking Scheme"
            else -> "Standard / Annual"
        }
    }

    private fun normalizeQuestionText(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace("^q(uestion)?\\s*[0-9]+[a-z]?\\.?\\s*".toRegex(), "")
            .replace("[^a-z0-9]".toRegex(), "")
    }

    private fun serializeScope(scope: AnalysisScope): String {
        return JSONObject().apply {
            put("projectId", scope.projectId)
            put("subject", scope.subject)
            put("syllabusVersionId", scope.syllabusVersionId)
            put("questionVerificationScope", scope.questionVerificationScope.name)
            put("mappingVerificationScope", scope.mappingVerificationScope.name)
            put("attributionMethod", scope.attributionMethod.name)
            put("selectedDocsCount", scope.selectedDocumentIds.size)
        }.toString()
    }

    private fun serializeRunSummary(result: HistoricalAnalysisResult): String {
        return JSONObject().apply {
            put("totalEligiblePapers", result.totalEligiblePapers)
            put("totalVerifiedQuestions", result.totalVerifiedQuestions)
            put("totalAttributedMarks", result.totalAttributedMarks)
            put("untestedTopicsCount", result.untestedTopicsCount)
            put("qualityBadges", JSONArray(result.dataQualityReport.qualityBadges))
        }.toString()
    }

    /**
     * Generates an exportable comprehensive report string.
     */
    fun generateExportReport(
        result: HistoricalAnalysisResult,
        format: String // "markdown", "csv", "json"
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(result.executionTimestamp))
        return when (format.lowercase()) {
            "markdown", "md" -> buildMarkdownReport(result, dateStr)
            "csv" -> buildCsvReport(result)
            "json" -> buildJsonReport(result, dateStr)
            else -> buildMarkdownReport(result, dateStr)
        }
    }

    private fun buildMarkdownReport(r: HistoricalAnalysisResult, dateStr: String): String = buildString {
        appendLine("# CBSE Class 10 Historical Examination Analysis Report")
        appendLine("- **Subject:** ${r.scope.subject}")
        appendLine("- **Syllabus Version:** ${r.scopeSummary.syllabusVersionName}")
        appendLine("- **Attribution Method:** ${r.scope.attributionMethod.label}")
        appendLine("- **Verification Scope:** ${r.scope.questionVerificationScope.label} | ${r.scope.mappingVerificationScope.label}")
        appendLine("- **Generated:** $dateStr")
        appendLine()
        appendLine("> **Methodological Notice & Phase Boundaries:**")
        appendLine("> This report presents descriptive historical data from ${r.totalEligiblePapers} analyzed examination papers. Historical appearance frequencies do NOT guarantee appearance in future examinations. Students must prepare the complete prescribed CBSE curriculum.")
        appendLine()
        appendLine("## 1. Scope & Corpus Completeness")
        appendLine("- Analyzed Papers: ${r.totalEligiblePapers}")
        appendLine("- Total Verified Questions: ${r.totalVerifiedQuestions}")
        appendLine("- Total Verified Topic Mappings: ${r.totalVerifiedMappings}")
        appendLine("- Questions with Unknown Marks: ${r.scopeSummary.questionsWithUnknownMarksCount}")
        appendLine("- Suspected Duplicate Documents: ${r.scopeSummary.suspectedDuplicateDocumentsCount}")
        appendLine()
        appendLine("## 2. Chapter-Wise Historical Appearance & Mark Weightage")
        appendLine("| Chapter | Topics Count | Tested Topics | Untested Topics | Total Appearances | Distinct Papers | Attributed Marks |")
        appendLine("|---|:---:|:---:|:---:|:---:|:---:|:---:|")
        r.chapterMetrics.forEach { c ->
            appendLine("| ${c.chapterName} | ${c.topicsCount} | ${c.topicsWithQuestionsCount} | ${c.untestedTopicsCount} | ${c.totalQuestionAppearances} | ${c.distinctPaperCount} | ${String.format(Locale.US, "%.1f", c.attributedMarks)}m |")
        }
        appendLine()
        appendLine("## 3. Topic-Wise Frequency & Question Traceability")
        appendLine("| Chapter | Topic | Appearances | Distinct Papers | Distinct Years | Attributed Marks | Min-Max Marks | Status |")
        appendLine("|---|---|:---:|:---:|:---:|:---:|:---:|---|")
        r.topicMetrics.forEach { t ->
            val minMax = if (t.minQuestionMarks != null && t.maxQuestionMarks != null) "${t.minQuestionMarks}-${t.maxQuestionMarks}m" else "N/A"
            val status = if (t.isExcludedFromSyllabus) "Excluded" else if (t.questionAppearancesCount > 0) "Tested" else "Untested in Sample"
            appendLine("| ${t.chapterName} | ${t.topicName} | ${t.questionAppearancesCount} | ${t.distinctPaperCount} | ${t.distinctYearsCount} | ${String.format(Locale.US, "%.1f", t.attributedMarks)}m | $minMax | $status |")
        }
        appendLine()
        appendLine("## 4. Question Format Breakdown")
        appendLine("| Question Format | Question Count | Distinct Papers | Total Marks | % of Questions |")
        appendLine("|---|:---:|:---:|:---:|:---:|")
        r.questionTypeMetrics.forEach { q ->
            appendLine("| ${q.questionType} | ${q.questionCount} | ${q.distinctPaperCount} | ${String.format(Locale.US, "%.1f", q.totalAttributedMarks)}m | ${String.format(Locale.US, "%.1f%%", q.percentageOfTotalQuestions)} |")
        }
        appendLine()
        appendLine("## 5. Year-Wise Examination Pattern")
        appendLine("| Year | Papers | Questions | Marks | Chapters Tested | Topics Tested |")
        appendLine("|---|:---:|:---:|:---:|:---:|:---:|")
        r.yearComparisonMetrics.forEach { y ->
            appendLine("| ${y.year} | ${y.papersCount} | ${y.questionsCount} | ${String.format(Locale.US, "%.1f", y.totalAttributedMarks)}m | ${y.chaptersCoveredCount} | ${y.topicsCoveredCount} |")
        }
    }

    private fun buildCsvReport(r: HistoricalAnalysisResult): String = buildString {
        appendLine("Chapter,Topic,Question Appearances,Distinct Papers,Distinct Years,Attributed Marks,Printed Marks Sum,Min Marks,Max Marks,Unknown Marks Count,Is Excluded")
        r.topicMetrics.forEach { t ->
            appendLine("\"${t.chapterName.replace("\"", "\"\"")}\",\"${t.topicName.replace("\"", "\"\"")}\",${t.questionAppearancesCount},${t.distinctPaperCount},${t.distinctYearsCount},${t.attributedMarks},${t.printedMarksSum},${t.minQuestionMarks ?: ""},${t.maxQuestionMarks ?: ""},${t.questionsWithUnknownMarks},${t.isExcludedFromSyllabus}")
        }
    }

    private fun buildJsonReport(r: HistoricalAnalysisResult, dateStr: String): String {
        val root = JSONObject().apply {
            put("board", "CBSE")
            put("classLevel", "10")
            put("subject", r.scope.subject)
            put("generatedDate", dateStr)
            put("attributionMethod", r.scope.attributionMethod.name)
            put("totalEligiblePapers", r.totalEligiblePapers)
            put("totalVerifiedQuestions", r.totalVerifiedQuestions)
            put("totalAttributedMarks", r.totalAttributedMarks)
            put("untestedTopicsCount", r.untestedTopicsCount)

            val chapArr = JSONArray()
            r.chapterMetrics.forEach { c ->
                chapArr.put(JSONObject().apply {
                    put("chapterId", c.chapterId)
                    put("chapterName", c.chapterName)
                    put("totalAppearances", c.totalQuestionAppearances)
                    put("distinctPaperCount", c.distinctPaperCount)
                    put("attributedMarks", c.attributedMarks)
                    put("untestedTopicsCount", c.untestedTopicsCount)
                })
            }
            put("chapters", chapArr)

            val topicArr = JSONArray()
            r.topicMetrics.forEach { t ->
                topicArr.put(JSONObject().apply {
                    put("topicId", t.topicId)
                    put("topicName", t.topicName)
                    put("chapterName", t.chapterName)
                    put("questionAppearancesCount", t.questionAppearancesCount)
                    put("distinctPaperCount", t.distinctPaperCount)
                    put("distinctYearsCount", t.distinctYearsCount)
                    put("attributedMarks", t.attributedMarks)
                    put("isExcluded", t.isExcludedFromSyllabus)
                })
            }
            put("topics", topicArr)
        }
        return root.toString(2)
    }
}
