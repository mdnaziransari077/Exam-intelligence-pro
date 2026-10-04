package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AiExamEstimateDao
import com.example.data.local.AuditLogDao
import com.example.data.local.DocumentDao
import com.example.data.local.ExtractedQuestionDao
import com.example.data.local.PracticeQuestionDao
import com.example.data.local.ProjectDao
import com.example.data.local.QuestionTopicMappingDao
import com.example.data.local.SyllabusDao
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.AiReviewLogEntity
import com.example.data.model.AiReviewStatus
import com.example.data.model.AnalysisScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.HistoricalAnalysisResult
import com.example.data.model.MappingVerificationScope
import com.example.data.model.MarksAttributionMethod
import com.example.data.model.PracticeAttemptEntity
import com.example.data.model.PracticeDifficulty
import com.example.data.model.PracticeGenerationRequest
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.PracticeQuestionFormat
import com.example.data.model.PracticeQuestionSetEntity
import com.example.data.model.QuestionVerificationScope
import com.example.data.model.SyllabusNodeType
import com.example.data.model.TopicEstimateItem
import com.example.data.model.TopicPracticeSuggestion
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class AiPracticeRepository(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val documentDao: DocumentDao,
    private val extractedQuestionDao: ExtractedQuestionDao,
    private val questionTopicMappingDao: QuestionTopicMappingDao,
    private val syllabusDao: SyllabusDao,
    private val practiceQuestionDao: PracticeQuestionDao,
    private val aiExamEstimateDao: AiExamEstimateDao,
    private val auditLogDao: AuditLogDao,
    private val historicalAnalysisRepo: HistoricalAnalysisRepository
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getSetsForProject(projectId: String): Flow<List<PracticeQuestionSetEntity>> {
        return practiceQuestionDao.getSetsForProject(projectId).flowOn(Dispatchers.IO)
    }

    fun getQuestionsForSet(setId: String): Flow<List<PracticeQuestionEntity>> {
        return practiceQuestionDao.getQuestionsForSet(setId).flowOn(Dispatchers.IO)
    }

    fun getAllGeneratedQuestionsForProject(projectId: String): Flow<List<PracticeQuestionEntity>> {
        return practiceQuestionDao.getAllQuestionsForProject(projectId).flowOn(Dispatchers.IO)
    }

    fun getAllGeneratedQuestions(): Flow<List<PracticeQuestionEntity>> {
        return practiceQuestionDao.getAllGeneratedQuestions().flowOn(Dispatchers.IO)
    }

    fun getSavedSetsForUser(userId: String): Flow<List<PracticeQuestionSetEntity>> {
        return practiceQuestionDao.getSavedSetsForUser(userId).flowOn(Dispatchers.IO)
    }

    fun getLatestEstimateForProject(projectId: String): Flow<AiExamEstimateEntity?> {
        return aiExamEstimateDao.getLatestEstimateForProject(projectId).flowOn(Dispatchers.IO)
    }

    fun getAttemptsForSet(setId: String, userId: String): Flow<List<PracticeAttemptEntity>> {
        return practiceQuestionDao.getAttemptsForSet(setId, userId).flowOn(Dispatchers.IO)
    }

    /**
     * Module A: Generates original, syllabus-aligned practice questions via Gemini API.
     */
    suspend fun generatePracticeQuestions(
        user: UserEntity,
        request: PracticeGenerationRequest,
        modelOption: GeminiModelOption = GeminiModelOption.FLASH_GENERAL
    ): Result<Pair<PracticeQuestionSetEntity, List<PracticeQuestionEntity>>> = withContext(Dispatchers.IO) {
        try {
            // Validation
            if (request.questionCount < 1 || request.questionCount > 10) {
                return@withContext Result.failure(IllegalArgumentException("Question count must be between 1 and 10 questions per set."))
            }

            val project = projectDao.getProjectById(request.projectId)
                ?: return@withContext Result.failure(IllegalArgumentException("Project not found"))

            // Project access check
            val isOwner = project.ownerId == user.id
            val isAdmin = user.getRoleEnum().canManageAllProjects()
            val isPublic = project.visibility == "PUBLIC"
            if (!isOwner && !isAdmin && !isPublic) {
                return@withContext Result.failure(SecurityException("Unauthorized: Cannot generate practice questions in private project."))
            }

            // Syllabus verification check
            val syllabusVer = syllabusDao.getVersionById(request.syllabusVersionId)
            val syllabusTitle = syllabusVer?.sourceTitle ?: "CBSE Class 10 Curriculum"
            val isSyllabusVerified = syllabusVer?.isVerified() == true

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Gemini API key is not configured in Secrets. AI Practice generation requires a valid Gemini API key. No mock questions will be fabricated."
                    )
                )
            }

            val setId = UUID.randomUUID().toString()
            val prompt = buildGenerationPrompt(request, syllabusTitle, isSyllabusVerified)

            val rawJson = callGeminiApi(prompt, modelOption.modelId, apiKey)
                ?: return@withContext Result.failure(
                    IllegalStateException("AI Service timed out or returned an empty response. Please verify network connectivity and retry.")
                )

            val generatedQuestions = parseGeneratedQuestionsJson(
                rawJson = rawJson,
                setId = setId,
                projectId = request.projectId,
                chapterId = request.chapterId,
                chapterName = request.chapterName,
                topicId = request.topicId,
                topicName = request.topicName,
                requestedFormat = request.format,
                requestedDifficulty = request.difficulty,
                modelTag = modelOption.modelId
            )

            if (generatedQuestions.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("AI generation produced malformed structured output. Please retry.")
                )
            }

            val setEntity = PracticeQuestionSetEntity(
                id = setId,
                projectId = request.projectId,
                userId = user.id,
                syllabusVersionId = request.syllabusVersionId,
                subject = request.subject,
                chapterId = request.chapterId,
                chapterName = request.chapterName,
                topicId = request.topicId,
                topicName = request.topicName,
                questionFormat = request.format.label,
                difficulty = request.difficulty.label,
                questionCount = generatedQuestions.size,
                modelTag = modelOption.modelId,
                isSaved = false,
                createdAt = System.currentTimeMillis()
            )

            // Persist to Room
            practiceQuestionDao.insertSet(setEntity)
            practiceQuestionDao.insertQuestions(generatedQuestions)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "AI_PRACTICE_GENERATED",
                    resourceType = "PRACTICE_SET",
                    resourceId = setId,
                    details = "Generated ${generatedQuestions.size} questions for '${request.chapterName} - ${request.topicName ?: "All Topics"}' [${modelOption.label}]"
                )
            )

            Result.success(Pair(setEntity, generatedQuestions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildGenerationPrompt(
        req: PracticeGenerationRequest,
        syllabusTitle: String,
        isSyllabusVerified: Boolean
    ): String {
        return """
            You are the Exam Intelligence AI Assessment Specialist for CBSE Class 10.
            Your task is to generate original, high-quality, syllabus-aligned practice questions.
            
            SPECIFICATIONS:
            - Board: CBSE
            - Class: 10
            - Subject: ${req.subject}
            - Syllabus: $syllabusTitle (Verified Official: $isSyllabusVerified)
            - Chapter: ${req.chapterName}
            - Topic: ${req.topicName ?: "Entire Chapter"}
            - Question Format: ${req.format.label}
            - Difficulty Level: ${req.difficulty.label}
            - Number of Questions to Generate: ${req.questionCount}
            
            IMPORTANT CONSTRAINTS:
            1. All questions must test genuine concepts from the prescribed CBSE Class 10 curriculum.
            2. Do NOT copy verbatim from copyrighted CBSE papers. Generate fresh, original problems testing identical competencies.
            3. For MCQs: provide exactly 4 distinct options with letter labels: "(A) ...", "(B) ...", "(C) ...", "(D) ...". Exactly one correct option.
            4. For Subjective / Short / Long Answer / Case-Based: provide clear step-by-step model answer guidance. Label it as suggested answer guidance, not an official marking scheme.
            5. Provide a lucid, Class 10-level conceptual explanation for each question.
            
            RETURN A STRICT JSON ARRAY of objects with this exact structure:
            [
              {
                "questionNumber": 1,
                "questionText": "Full question statement here...",
                "questionType": "${req.format.label}",
                "marks": ${if (req.marksPerQuestion != null) req.marksPerQuestion else if (req.format.typicalMarks > 0) req.format.typicalMarks else 1},
                "difficulty": "AI-Estimated: ${req.difficulty.label}",
                "options": ["(A) Option 1", "(B) Option 2", "(C) Option 3", "(D) Option 4"], // null if not MCQ
                "correctAnswer": "(A) Option 1", // or brief correct answer statement
                "explanation": "Detailed step-by-step conceptual explanation...",
                "modelAnswerGuidance": "Step 1: ..., Step 2: ..., Key points for full marks...",
                "syllabusReference": "CBSE Class 10 ${req.subject} • ${req.chapterName}"
              }
            ]
            Return ONLY the valid JSON array. No markdown code blocks, no backticks, no preamble.
        """.trimIndent()
    }

    private fun callGeminiApi(prompt: String, modelId: String, apiKey: String): String? {
        val root = JSONObject()
        val contentsArray = JSONArray()
        val turnObj = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray().apply {
                put(JSONObject().put("text", prompt))
            }
            put("parts", parts)
        }
        contentsArray.put(turnObj)
        root.put("contents", contentsArray)

        val genConfig = JSONObject().apply {
            put("temperature", 0.2)
            put("responseMimeType", "application/json")
        }
        root.put("generationConfig", genConfig)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(root.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val responseJson = JSONObject(responseBody)
        val textReply = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")

        return textReply?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
    }

    private fun parseGeneratedQuestionsJson(
        rawJson: String,
        setId: String,
        projectId: String,
        chapterId: String,
        chapterName: String,
        topicId: String?,
        topicName: String?,
        requestedFormat: PracticeQuestionFormat,
        requestedDifficulty: PracticeDifficulty,
        modelTag: String
    ): List<PracticeQuestionEntity> {
        val list = mutableListOf<PracticeQuestionEntity>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qNum = obj.optInt("questionNumber", i + 1)
                val qText = obj.optString("questionText", "").trim()
                if (qText.isBlank()) continue

                val qType = obj.optString("questionType", requestedFormat.label)
                val marks = if (obj.has("marks") && !obj.isNull("marks")) obj.getInt("marks") else if (requestedFormat.typicalMarks > 0) requestedFormat.typicalMarks else 1
                val diff = obj.optString("difficulty", "AI-Estimated: ${requestedDifficulty.label}")

                val optArr = obj.optJSONArray("options")
                val optionsJson = if (optArr != null && optArr.length() > 0) {
                    optArr.toString()
                } else null

                val correct = obj.optString("correctAnswer", null)
                val explanation = obj.optString("explanation", null)
                val guidance = obj.optString("modelAnswerGuidance", null)
                val ref = obj.optString("syllabusReference", "CBSE Class 10 • $chapterName")

                val qEntity = PracticeQuestionEntity(
                    id = UUID.randomUUID().toString(),
                    setId = setId,
                    projectId = projectId,
                    questionNumber = qNum,
                    questionText = qText,
                    questionType = qType,
                    chapterId = chapterId,
                    chapterName = chapterName,
                    topicId = topicId,
                    topicName = topicName,
                    marks = marks,
                    difficulty = diff,
                    answerOptionsJson = optionsJson,
                    correctAnswer = correct,
                    explanation = explanation,
                    modelAnswerGuidance = guidance,
                    syllabusReference = ref,
                    modelTag = modelTag,
                    originalAiText = qText,
                    userCorrectedText = null,
                    reviewStatus = AiReviewStatus.DRAFT.label,
                    reviewedBy = null,
                    reviewedAt = null,
                    reviewNotes = null,
                    createdAt = System.currentTimeMillis()
                )
                list.add(qEntity)
            }
        } catch (_: Exception) {
            // Error handled by returning empty list
        }
        return list
    }

    /**
     * Module C: Generates evidence-based topic practice suggestions using Phase 4 analytics.
     */
    suspend fun computeEvidenceBasedSuggestions(
        projectId: String,
        syllabusVersionId: String,
        user: UserEntity?
    ): List<TopicPracticeSuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<TopicPracticeSuggestion>()
        try {
            val project = projectDao.getProjectById(projectId) ?: return@withContext emptyList()
            val scope = AnalysisScope(
                projectId = projectId,
                subject = project.subject,
                syllabusVersionId = syllabusVersionId,
                questionVerificationScope = QuestionVerificationScope.VERIFIED_ONLY,
                mappingVerificationScope = MappingVerificationScope.APPROVED_ONLY,
                attributionMethod = MarksAttributionMethod.FULL_OVERLAPPING
            )

            val analysisResult = historicalAnalysisRepo.executeAnalysis(scope, user).getOrNull()
            if (analysisResult == null || analysisResult.totalEligiblePapers == 0) {
                return@withContext emptyList() // Insufficient verified data
            }

            // 1. High Historical Weightage suggestions (Top 3 tested topics)
            val topTestedTopics = analysisResult.topicMetrics
                .filter { it.attributedMarks > 0 }
                .sortedByDescending { it.attributedMarks }
                .take(3)

            for (t in topTestedTopics) {
                suggestions.add(
                    TopicPracticeSuggestion(
                        topicId = t.topicId,
                        topicName = t.topicName,
                        chapterId = t.chapterId,
                        chapterName = t.chapterName,
                        suggestionRationale = "High Historical Mark Weightage: Observed in ${t.distinctPaperCount} paper(s) with ~${String.format(Locale.US, "%.1f", t.attributedMarks)} marks attributed.",
                        category = "HIGH_FREQUENCY",
                        verifiedPaperCount = t.distinctPaperCount,
                        historicalAppearances = t.questionAppearancesCount,
                        attributedMarks = t.attributedMarks,
                        frequentQuestionTypes = t.frequencyByQuestionType.keys.toList(),
                        yearsRepresented = t.distinctYears,
                        drillDownTopicId = t.topicId
                    )
                )
            }

            // 2. Untested Curriculum Gap suggestions (Core syllabus topics with 0 question appearances)
            val untestedTopics = analysisResult.untestedTopicsList.take(3)
            for (u in untestedTopics) {
                suggestions.add(
                    TopicPracticeSuggestion(
                        topicId = u.topicId,
                        topicName = u.topicName,
                        chapterId = u.chapterId,
                        chapterName = u.chapterName,
                        suggestionRationale = "Untested Curriculum Gap: Prescribed in official CBSE curriculum but not tested in this sample collection. Critical to practice for complete syllabus mastery.",
                        category = "UNTESTED_GAP",
                        verifiedPaperCount = 0,
                        historicalAppearances = 0,
                        attributedMarks = 0.0,
                        frequentQuestionTypes = emptyList(),
                        yearsRepresented = emptyList(),
                        drillDownTopicId = u.topicId
                    )
                )
            }

            // 3. Competency / Format-focused suggestions (Assertion-Reason / Case-based)
            val formatTopics = analysisResult.topicMetrics
                .filter { t -> t.frequencyByQuestionType.keys.any { it.contains("Assertion", true) || it.contains("Case", true) } }
                .sortedByDescending { it.questionAppearancesCount }
                .take(2)

            for (f in formatTopics) {
                if (suggestions.none { it.topicId == f.topicId }) {
                    suggestions.add(
                        TopicPracticeSuggestion(
                            topicId = f.topicId,
                            topicName = f.topicName,
                            chapterId = f.chapterId,
                            chapterName = f.chapterName,
                            suggestionRationale = "Competency Format Concentration: Frequently observed with Assertion-Reason or Case-Based questions.",
                            category = "FORMAT_FOCUSED",
                            verifiedPaperCount = f.distinctPaperCount,
                            historicalAppearances = f.questionAppearancesCount,
                            attributedMarks = f.attributedMarks,
                            frequentQuestionTypes = f.frequencyByQuestionType.keys.toList(),
                            yearsRepresented = f.distinctYears,
                            drillDownTopicId = f.topicId
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        suggestions
    }

    /**
     * Module D: Separate AI Exam Estimates / Predictions Module.
     * Tentative qualitative estimate strictly isolated from historical facts and predictions.
     */
    suspend fun generateAiExamEstimates(
        projectId: String,
        syllabusVersionId: String,
        user: UserEntity?
    ): Result<AiExamEstimateEntity> = withContext(Dispatchers.IO) {
        try {
            val project = projectDao.getProjectById(projectId)
                ?: return@withContext Result.failure(IllegalArgumentException("Project not found"))

            val scope = AnalysisScope(
                projectId = projectId,
                subject = project.subject,
                syllabusVersionId = syllabusVersionId,
                questionVerificationScope = QuestionVerificationScope.VERIFIED_ONLY,
                mappingVerificationScope = MappingVerificationScope.APPROVED_ONLY,
                attributionMethod = MarksAttributionMethod.FULL_OVERLAPPING
            )

            val analysisResult = historicalAnalysisRepo.executeAnalysis(scope, user).getOrNull()
            if (analysisResult == null || analysisResult.totalEligiblePapers == 0) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Insufficient verified historical data. To generate AI Exam Estimates, at least one verified question paper and verified syllabus version are required."
                    )
                )
            }

            val estimates = mutableListOf<TopicEstimateItem>()

            // 1. High Attention: Top historical weightage topics
            analysisResult.topicMetrics.filter { it.attributedMarks > 0 }
                .sortedByDescending { it.attributedMarks }
                .take(4)
                .forEach { t ->
                    estimates.add(
                        TopicEstimateItem(
                            topicId = t.topicId,
                            topicName = t.topicName,
                            chapterName = t.chapterName,
                            attentionLevel = "High Recommended Attention",
                            rationale = "Consistently tested across multiple papers with significant mark weightage (~${String.format(Locale.US, "%.1f", t.attributedMarks)}m).",
                            historicalFrequencyNotice = "Observed in ${t.distinctPaperCount} paper(s) over ${t.distinctYears.joinToString(", ")}.",
                            syllabusStatus = "Official Prescribed Topic",
                            caveat = "Historical prominence does not guarantee questions in upcoming examinations."
                        )
                    )
                }

            // 2. Untested Curriculum Attention
            analysisResult.untestedTopicsList.take(3).forEach { u ->
                estimates.add(
                    TopicEstimateItem(
                        topicId = u.topicId,
                        topicName = u.topicName,
                        chapterName = u.chapterName,
                        attentionLevel = "Curriculum Gap Alert",
                        rationale = "Active syllabus topic with 0 appearances in the current collection. High potential area for unexpected board questions.",
                        historicalFrequencyNotice = "0 appearances in current ${analysisResult.totalEligiblePapers} sample papers.",
                        syllabusStatus = "Official Prescribed Topic",
                        caveat = "Students should prepare all syllabus topics to ensure zero blind spots."
                    )
                )
            }

            val jsonArr = JSONArray()
            estimates.forEach { e ->
                jsonArr.put(
                    JSONObject().apply {
                        put("topicId", e.topicId)
                        put("topicName", e.topicName)
                        put("chapterName", e.chapterName)
                        put("attentionLevel", e.attentionLevel)
                        put("rationale", e.rationale)
                        put("historicalFrequencyNotice", e.historicalFrequencyNotice)
                        put("syllabusStatus", e.syllabusStatus)
                        put("caveat", e.caveat)
                    }
                )
            }

            val analyzedYearsStr = analysisResult.yearComparisonMetrics.map { it.year }.joinToString(", ")
            val limitationsStr = "Sample size: ${analysisResult.totalEligiblePapers} paper(s). Missing or unverified years are not inferred. All estimates are strictly tentative and educational."

            val estimateEntity = AiExamEstimateEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                syllabusVersionId = syllabusVersionId,
                subject = project.subject,
                scopeJson = JSONObject().apply {
                    put("papersCount", analysisResult.totalEligiblePapers)
                    put("verifiedQuestions", analysisResult.totalVerifiedQuestions)
                }.toString(),
                estimatesJson = jsonArr.toString(),
                analyzedPaperCount = analysisResult.totalEligiblePapers,
                analyzedYears = analyzedYearsStr,
                dataLimitations = limitationsStr,
                disclaimer = "AI estimate — not an official prediction. Historical frequency does not guarantee future examination content.",
                createdAt = System.currentTimeMillis()
            )

            aiExamEstimateDao.insertEstimate(estimateEntity)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user?.id ?: "student",
                    userEmail = user?.email ?: "student@cbse.org",
                    action = "AI_ESTIMATES_GENERATED",
                    resourceType = "PROJECT",
                    resourceId = projectId,
                    details = "Generated tentative AI exam estimates for ${project.subject} based on ${analysisResult.totalEligiblePapers} papers."
                )
            )

            Result.success(estimateEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Module B: Record MCQ attempt by student for immediate interactive feedback.
     */
    suspend fun recordAttempt(
        userId: String,
        questionId: String,
        setId: String,
        selectedOption: String?,
        isCorrect: Boolean?
    ) = withContext(Dispatchers.IO) {
        val attempt = PracticeAttemptEntity(
            id = UUID.randomUUID().toString(),
            questionId = questionId,
            setId = setId,
            userId = userId,
            selectedOption = selectedOption,
            isCorrect = isCorrect,
            attemptedAt = System.currentTimeMillis()
        )
        practiceQuestionDao.insertAttempt(attempt)
    }

    /**
     * Module B: Toggle saving practice set to project / user account.
     */
    suspend fun toggleSaveSet(setId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        practiceQuestionDao.updateSetSavedStatus(setId, isSaved)
    }

    /**
     * Module E: Admin review and editing of AI-generated questions.
     */
    suspend fun reviewQuestion(
        reviewer: UserEntity,
        questionId: String,
        newStatus: AiReviewStatus,
        userCorrectedText: String? = null,
        marks: Int? = null,
        notes: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val question = practiceQuestionDao.getQuestionById(questionId)
            ?: return@withContext Result.failure(IllegalArgumentException("Question not found"))

        // Authorization check: Teacher or Admin
        if (!reviewer.getRoleEnum().canApproveQuestions()) {
            return@withContext Result.failure(SecurityException("Unauthorized: Only Teachers and Administrators can verify practice questions."))
        }

        val previousStatus = question.reviewStatus
        val timestamp = System.currentTimeMillis()

        if (userCorrectedText != null) {
            practiceQuestionDao.updateQuestionContent(
                questionId = questionId,
                correctedText = userCorrectedText.trim(),
                marks = marks ?: question.marks,
                status = newStatus.label,
                reviewedBy = reviewer.email,
                reviewedAt = timestamp,
                notes = notes
            )
        } else {
            practiceQuestionDao.updateReviewStatus(
                questionId = questionId,
                status = newStatus.label,
                reviewedBy = reviewer.email,
                reviewedAt = timestamp,
                notes = notes
            )
        }

        practiceQuestionDao.insertReviewLog(
            AiReviewLogEntity(
                id = UUID.randomUUID().toString(),
                questionId = questionId,
                reviewerEmail = reviewer.email,
                action = newStatus.name,
                previousStatus = previousStatus,
                newStatus = newStatus.label,
                notes = notes,
                timestamp = timestamp
            )
        )

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = reviewer.id,
                userEmail = reviewer.email,
                action = "AI_QUESTION_REVIEWED",
                resourceType = "PRACTICE_QUESTION",
                resourceId = questionId,
                details = "Reviewer ${reviewer.email} marked question as '${newStatus.label}'. Notes: ${notes ?: "None"}"
            )
        )

        Result.success(Unit)
    }

    /**
     * Export practice set to Markdown or Text.
     */
    suspend fun exportPracticeSet(setId: String): String = withContext(Dispatchers.IO) {
        val set = practiceQuestionDao.getSetById(setId) ?: return@withContext "Practice set not found."
        val questions = practiceQuestionDao.getQuestionsListForSet(setId)
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(set.createdAt))

        buildString {
            appendLine("# CBSE Class 10 Practice Set: ${set.subject}")
            appendLine("- **Chapter:** ${set.chapterName}")
            if (!set.topicName.isNullOrBlank()) appendLine("- **Topic:** ${set.topicName}")
            appendLine("- **Format:** ${set.questionFormat} • **Difficulty:** ${set.difficulty}")
            appendLine("- **Date:** $dateStr • **Model Tag:** ${set.modelTag}")
            appendLine()
            appendLine("> **Notice:** These are AI-generated practice questions aligned with the CBSE curriculum for practice purposes. They are not official previous-year questions.")
            appendLine()
            questions.forEachIndexed { idx, q ->
                appendLine("### Q${idx + 1} (${q.marks ?: 1} Marks) — ${q.difficulty}")
                appendLine(q.getEffectiveText())
                if (!q.getOptionsList().isNullOrEmpty()) {
                    appendLine()
                    q.getOptionsList().forEach { opt ->
                        appendLine("- $opt")
                    }
                }
                if (!q.correctAnswer.isNullOrBlank()) {
                    appendLine()
                    appendLine("**Correct Answer:** ${q.correctAnswer}")
                }
                if (!q.explanation.isNullOrBlank()) {
                    appendLine()
                    appendLine("**Explanation:** ${q.explanation}")
                }
                if (!q.modelAnswerGuidance.isNullOrBlank()) {
                    appendLine()
                    appendLine("**Suggested Answer Guidance:** ${q.modelAnswerGuidance}")
                }
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }
}
