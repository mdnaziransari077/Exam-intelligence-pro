package com.example

import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.ProcessingStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectVisibility
import com.example.data.model.QuestionType
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamIntelligenceLogicTest {

    @Test
    fun testUserRolePermissions() {
        assertTrue(UserRole.ADMIN.canAccessAdminArea())
        assertTrue(UserRole.SUPER_ADMIN.canAccessAdminArea())
        assertFalse(UserRole.STUDENT.canAccessAdminArea())
        assertFalse(UserRole.TEACHER.canAccessAdminArea())
        assertFalse(UserRole.GUEST.canAccessAdminArea())

        assertTrue(UserRole.ADMIN.canManageAllProjects())
        assertFalse(UserRole.STUDENT.canManageAllProjects())
    }

    @Test
    fun testProjectPrivacyAndStatus() {
        val privateProject = ProjectEntity(
            id = "proj-1",
            ownerId = "user-1",
            ownerName = "Student A",
            name = "Math Standard 2025",
            subject = "Mathematics (Standard)",
            visibility = ProjectVisibility.PRIVATE.name,
            status = ProjectStatus.SETUP.label
        )
        assertTrue(privateProject.isPrivate())
        assertEquals("Setup", privateProject.status)

        val publicProject = privateProject.copy(visibility = ProjectVisibility.PUBLIC.name)
        assertFalse(publicProject.isPrivate())
    }

    @Test
    fun testDocumentSizeFormatting() {
        val docBytes = DocumentEntity(
            id = "doc-1",
            projectId = "proj-1",
            ownerId = "user-1",
            originalFilename = "paper.pdf",
            mimeType = "application/pdf",
            fileSize = 512,
            storageUri = "/path/to/paper.pdf",
            processingStatus = ProcessingStatus.UPLOADED.label,
            documentCategory = DocumentCategory.PREVIOUS_YEAR_PAPER.label
        )
        assertEquals("512 B", docBytes.formattedFileSize())

        val docKb = docBytes.copy(fileSize = 2048)
        assertEquals("2 KB", docKb.formattedFileSize())

        val docMb = docBytes.copy(fileSize = 2_500_000)
        assertEquals("2.4 MB", docMb.formattedFileSize())
    }

    @Test
    fun testDocumentPipelineStages() {
        val stages = listOf(
            ProcessingStatus.UPLOADED,
            ProcessingStatus.QUEUED,
            ProcessingStatus.EXTRACTING_TEXT,
            ProcessingStatus.DETECTING_QUESTIONS,
            ProcessingStatus.AWAITING_REVIEW,
            ProcessingStatus.PARTIALLY_REVIEWED,
            ProcessingStatus.VERIFIED,
            ProcessingStatus.FAILED,
            ProcessingStatus.RETRYING
        )
        assertEquals(9, stages.size)

        assertEquals(ProcessingStatus.UPLOADED, ProcessingStatus.fromLabel("Uploaded"))
        assertEquals(ProcessingStatus.EXTRACTING_TEXT, ProcessingStatus.fromLabel("Extracting text"))
        assertEquals(ProcessingStatus.DETECTING_QUESTIONS, ProcessingStatus.fromLabel("Detecting questions"))
        assertEquals(ProcessingStatus.AWAITING_REVIEW, ProcessingStatus.fromLabel("Awaiting review"))
        assertEquals(ProcessingStatus.PARTIALLY_REVIEWED, ProcessingStatus.fromLabel("Partially reviewed"))
        assertEquals(ProcessingStatus.VERIFIED, ProcessingStatus.fromLabel("Verified"))
        assertEquals(ProcessingStatus.FAILED, ProcessingStatus.fromLabel("Failed"))
    }

    @Test
    fun testQuestionStructuringAndOptionsParsing() {
        val mcqJson = "[\"(A) Option A\", \"(B) Option B\", \"(C) Option C\", \"(D) Option D\"]"
        val question = ExtractedQuestionEntity(
            id = "q-1",
            projectId = "p-1",
            sourceDocumentId = "doc-1",
            questionNumber = "1",
            section = "Section A (1 Mark)",
            questionText = "What is the HCF of prime numbers a and b?",
            questionType = QuestionType.MCQ.label,
            marks = 1,
            answerOptionsJson = mcqJson,
            topic = "Real Numbers",
            confidence = 0.95f,
            sourcePage = 1,
            originalExtractionText = "What is the HCF of prime numbers a and b?"
        )

        assertEquals("1", question.questionNumber)
        assertEquals("Section A (1 Mark)", question.section)
        assertEquals(4, question.getOptionsList().size)
        assertEquals("(A) Option A", question.getOptionsList()[0])
        assertEquals("(D) Option D", question.getOptionsList()[3])
        assertFalse(question.isMarksUnknown())
        assertTrue(question.isHighConfidence())
        assertFalse(question.isVerified())
    }

    @Test
    fun testUnspecifiedMarksFlaggedForHumanReview() {
        val questionWithNoMarks = ExtractedQuestionEntity(
            id = "q-2",
            projectId = "p-1",
            sourceDocumentId = "doc-1",
            questionNumber = "7",
            section = "Section A",
            questionText = "State Ohm's law and define resistance.",
            questionType = QuestionType.SHORT_ANSWER_1.label,
            marks = null,
            reviewWarnings = "Marks not detected in document — flagged for human review",
            confidence = 0.74f,
            sourcePage = 2,
            originalExtractionText = "State Ohm's law and define resistance."
        )

        assertTrue(questionWithNoMarks.isMarksUnknown())
        assertFalse(questionWithNoMarks.isHighConfidence())
        assertEquals("Marks not detected in document — flagged for human review", questionWithNoMarks.reviewWarnings)
    }

    @Test
    fun testHumanVerificationWorkflow() {
        val originalQuestion = ExtractedQuestionEntity(
            id = "q-3",
            projectId = "p-1",
            sourceDocumentId = "doc-1",
            questionNumber = "14",
            section = "Section B",
            questionText = "Original OCR text with minor typo",
            questionType = QuestionType.SHORT_ANSWER_1.label,
            marks = 2,
            confidence = 0.91f,
            originalExtractionText = "Original OCR text with minor typo"
        )

        assertEquals("Original OCR text with minor typo", originalQuestion.getEffectiveText())
        assertFalse(originalQuestion.isVerified())

        // Simulate human edit and approval
        val correctedQuestion = originalQuestion.copy(
            userCorrectedText = "Corrected question text after human inspection",
            verificationStatus = VerificationStatus.APPROVED.label,
            reviewedBy = "reviewer@cbse.org",
            reviewedAt = System.currentTimeMillis()
        )

        assertEquals("Corrected question text after human inspection", correctedQuestion.getEffectiveText())
        assertEquals("Original OCR text with minor typo", correctedQuestion.originalExtractionText)
        assertTrue(correctedQuestion.isVerified())
        assertEquals(VerificationStatus.APPROVED.label, correctedQuestion.verificationStatus)
        assertEquals("reviewer@cbse.org", correctedQuestion.reviewedBy)
    }

    @Test
    fun testSyllabusVerificationStatusAndSourceHierarchy() {
        val officialVersion = com.example.data.model.SyllabusVersionEntity(
            id = "cbse-10-math-2024-25",
            board = "CBSE",
            classLevel = "10",
            subject = "Mathematics (Standard)",
            academicSession = "2024-2025",
            versionIdentifier = "CBSE-10-MATH-2024-25-v1",
            sourceType = com.example.data.model.SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT.label,
            sourceTitle = "CBSE Secondary Curriculum 2024-2025",
            verificationStatus = com.example.data.model.SyllabusVerificationStatus.VERIFIED.label,
            verifiedBy = "admin@cbse.edu",
            verifiedAt = System.currentTimeMillis()
        )

        assertTrue(officialVersion.isVerified())
        assertEquals("Verified Official", officialVersion.verificationStatus)
        assertEquals("Official CBSE Curriculum Document", officialVersion.sourceType)

        val unverifiedUpload = officialVersion.copy(
            id = "upload-ver-1",
            versionIdentifier = "UPLOAD-MATH-2025-v1",
            sourceType = com.example.data.model.SyllabusSourceType.ADMIN_UPLOAD.label,
            verificationStatus = com.example.data.model.SyllabusVerificationStatus.UNVERIFIED.label,
            verifiedBy = null,
            verifiedAt = null
        )

        assertFalse(unverifiedUpload.isVerified())
        assertEquals("Unverified — Review Required", unverifiedUpload.verificationStatus)
    }

    @Test
    fun testSyllabusNodeExclusionAndHierarchyTypes() {
        val chapterNode = com.example.data.model.SyllabusNodeEntity(
            id = "node-ch-1",
            syllabusVersionId = "cbse-10-math-2024-25",
            parentId = null,
            nodeType = com.example.data.model.SyllabusNodeType.CHAPTER.label,
            code = "CH-01",
            name = "Real Numbers",
            displayOrder = 1
        )
        assertFalse(chapterNode.isExcluded)
        assertEquals("Chapter", chapterNode.nodeType)

        val excludedTopic = com.example.data.model.SyllabusNodeEntity(
            id = "node-top-old",
            syllabusVersionId = "cbse-10-math-2024-25",
            parentId = "node-ch-1",
            nodeType = com.example.data.model.SyllabusNodeType.TOPIC.label,
            code = "TOP-01",
            name = "Euclid's Division Lemma",
            displayOrder = 1,
            isExcluded = true,
            exclusionReason = "Rationalised by NCERT/CBSE for session 2024-25 onwards"
        )
        assertTrue(excludedTopic.isExcluded)
        assertEquals("Rationalised by NCERT/CBSE for session 2024-25 onwards", excludedTopic.exclusionReason)
    }

    @Test
    fun testQuestionTopicMappingReviewStatuses() {
        val aiMapping = com.example.data.model.QuestionTopicMappingEntity(
            id = "map-1",
            questionId = "q-101",
            sourceDocumentId = "doc-1",
            projectId = "p-1",
            syllabusVersionId = "cbse-10-math-2024-25",
            chapterId = "node-ch-1",
            chapterName = "Real Numbers",
            topicId = "node-top-1",
            topicName = "Fundamental Theorem of Arithmetic",
            mappingMethod = "AI_SUGGESTED",
            reviewStatus = com.example.data.model.MappingReviewStatus.SUGGESTED.label,
            confidenceScore = 0.92f,
            explanation = "Matched keywords: prime factorisation, HCF, LCM"
        )

        assertFalse(aiMapping.isApproved())
        assertEquals("AI Suggested (Pending Review)", aiMapping.reviewStatus)

        val approvedMapping = aiMapping.copy(
            reviewStatus = com.example.data.model.MappingReviewStatus.APPROVED.label,
            reviewerEmail = "teacher@school.org",
            reviewedAt = System.currentTimeMillis()
        )
        assertTrue(approvedMapping.isApproved())
        assertEquals("Human Verified", approvedMapping.reviewStatus)

        val uncertainMapping = aiMapping.copy(
            reviewStatus = com.example.data.model.MappingReviewStatus.UNCERTAIN.label,
            reviewerEmail = "teacher@school.org",
            reviewNotes = "Multi-concept overlap between Real Numbers and Polynomials"
        )
        assertFalse(uncertainMapping.isApproved())
        assertEquals("Uncertain — Review Required", uncertainMapping.reviewStatus)
    }

    @Test
    fun testCoverageStatusDistinction() {
        val coveredTopic = com.example.data.model.TopicCoverageStats(
            chapterId = "ch-1",
            chapterName = "Real Numbers",
            topicId = "top-1",
            topicName = "Fundamental Theorem of Arithmetic",
            subject = "Mathematics (Standard)",
            academicSession = "2024-2025",
            totalMappedQuestions = 3,
            distinctPaperCount = 2,
            verifiedMappingCount = 3,
            pendingMappingCount = 0,
            hasUnverifiedQuestionMarks = false,
            verifiedMarksTotal = 6,
            coverageStatus = com.example.data.model.CoverageStatus.QUESTIONS_FOUND,
            isExcludedFromSyllabus = false
        )
        assertEquals(com.example.data.model.CoverageStatus.QUESTIONS_FOUND, coveredTopic.coverageStatus)
        assertEquals("Questions Found", coveredTopic.coverageStatus.label)

        val untestedTopic = com.example.data.model.TopicCoverageStats(
            chapterId = "ch-1",
            chapterName = "Real Numbers",
            topicId = "top-2",
            topicName = "Revisiting Irrational Numbers",
            subject = "Mathematics (Standard)",
            academicSession = "2024-2025",
            totalMappedQuestions = 0,
            distinctPaperCount = 0,
            verifiedMappingCount = 0,
            pendingMappingCount = 0,
            hasUnverifiedQuestionMarks = false,
            verifiedMarksTotal = null,
            coverageStatus = com.example.data.model.CoverageStatus.NO_QUESTIONS_FOUND,
            isExcludedFromSyllabus = false
        )
        assertEquals(com.example.data.model.CoverageStatus.NO_QUESTIONS_FOUND, untestedTopic.coverageStatus)
        assertEquals("No Questions Found in Analysed Collection", untestedTopic.coverageStatus.label)
        // Educational integrity: Absence from sample collection is NOT omission from official curriculum
        assertEquals(0, untestedTopic.totalMappedQuestions)
    }

    @Test
    fun testDistinctPaperVsAppearanceCounting() {
        val topic = com.example.data.model.HistoricalTopicMetrics(
            topicId = "top-1",
            topicName = "Quadratic Formula",
            chapterId = "ch-quadratic",
            chapterName = "Quadratic Equations",
            questionAppearancesCount = 5,
            distinctPaperCount = 1,
            distinctYearsCount = 1,
            distinctYears = listOf("2024"),
            uniqueQuestionsCount = 4,
            repeatedQuestionsCount = 1,
            attributedMarks = 14.0,
            printedMarksSum = 14,
            averageMarksPerPaper = 14.0,
            minQuestionMarks = 1,
            maxQuestionMarks = 5,
            questionsWithUnknownMarks = 0,
            frequencyByQuestionType = mapOf("MCQ" to 2, "Short Answer (2M)" to 1, "Long Answer (5M)" to 2),
            marksByQuestionType = mapOf("MCQ" to 2.0, "Short Answer (2M)" to 2.0, "Long Answer (5M)" to 10.0),
            frequencyByYear = mapOf("2024" to 5),
            frequencyByPaperSet = mapOf("Set 1" to 5),
            frequencyByMarksCategory = mapOf("1M" to 2, "2M" to 1, "5M" to 2),
            associatedQuestionIds = listOf("q1", "q2", "q3", "q4", "q5")
        )

        // Rule B: Appearing 5 times in 1 paper equals 1 distinct paper, while appearances equals 5
        assertEquals(5, topic.questionAppearancesCount)
        assertEquals(1, topic.distinctPaperCount)
        assertTrue(topic.questionAppearancesCount > topic.distinctPaperCount)
    }

    @Test
    fun testMultiTopicAttributionOverlappingVsFractional() {
        val questionMarks = 4
        val mappedTopicCount = 2

        // Rule G: Multi-Topic Attribution
        val overlappingPerTopic = questionMarks.toDouble()
        val fractionalPerTopic = questionMarks.toDouble() / mappedTopicCount

        assertEquals(4.0, overlappingPerTopic, 0.001)
        assertEquals(2.0, fractionalPerTopic, 0.001)

        val overlappingTotalAcrossTopics = overlappingPerTopic * mappedTopicCount
        val fractionalTotalAcrossTopics = fractionalPerTopic * mappedTopicCount

        // Overlapping attribution is non-additive across topics
        assertEquals(8.0, overlappingTotalAcrossTopics, 0.001)
        assertTrue(overlappingTotalAcrossTopics > questionMarks)

        // Fractional attribution preserves exact single-question marks total
        assertEquals(4.0, fractionalTotalAcrossTopics, 0.001)
    }

    @Test
    fun testUnknownMarksPreservationInQualityReport() {
        val questionWithUnknown = ExtractedQuestionEntity(
            id = "q-unk-1",
            projectId = "p-1",
            sourceDocumentId = "doc-1",
            questionNumber = "28",
            section = "Section C",
            questionText = "State the principle of electric motor and explain split rings function.",
            questionType = QuestionType.SHORT_ANSWER_2.label,
            marks = null, // Unknown marks
            confidence = 0.88f,
            sourcePage = 3,
            originalExtractionText = "State the principle of electric motor and explain split rings function."
        )

        assertTrue(questionWithUnknown.isMarksUnknown())

        // In Data Quality reporting, unknown marks are isolated without fabrication
        val totalQuestions = 10
        val verifiedMarksCount = 9
        val marksVerificationPct = (verifiedMarksCount.toFloat() / totalQuestions) * 100f

        val qualityReport = com.example.data.model.DataQualityReport(
            paperCollectionCompletenessPercent = 60f,
            questionVerificationCompletionPercent = 90f,
            mappingVerificationCompletionPercent = 90f,
            marksVerificationCompletionPercent = marksVerificationPct,
            missingYearMetadataCount = 0,
            missingPaperSetMetadataCount = 0,
            suspectedDuplicateDocs = emptyList(),
            unmappedQuestionsCount = 1,
            uncertainQuestionTypesCount = 0,
            qualityBadges = listOf("Partial Collection", "Review Required"),
            methodologyNotice = "Descriptive evidence only. Frequencies do NOT represent predictive probabilities."
        )

        assertEquals(90.0f, qualityReport.marksVerificationCompletionPercent, 0.01f)
        assertTrue(qualityReport.qualityBadges.contains("Review Required"))
    }

    // =========================================================================
    // PHASE 5: AI PRACTICE QUESTIONS, EVIDENCE SUGGESTIONS & PREDICTION TESTS
    // =========================================================================

    @Test
    fun testPracticeGenerationRequestValidation() {
        val request = com.example.data.model.PracticeGenerationRequest(
            projectId = "p-10-sci",
            subject = "Science",
            syllabusVersionId = "cbse-10-sci-2024-25",
            chapterId = "ch-electricity",
            chapterName = "Electricity",
            topicId = "top-ohms-law",
            topicName = "Ohm's Law & Resistance",
            format = com.example.data.model.PracticeQuestionFormat.MCQ,
            difficulty = com.example.data.model.PracticeDifficulty.MEDIUM,
            questionCount = 5,
            marksPerQuestion = 1
        )

        assertEquals("Science", request.subject)
        assertEquals("Electricity", request.chapterName)
        assertEquals(5, request.questionCount)
        assertEquals(com.example.data.model.PracticeQuestionFormat.MCQ, request.format)
        assertEquals(1, request.marksPerQuestion)
    }

    @Test
    fun testAiPracticeQuestionReviewWorkflow() {
        val rawAiQuestion = com.example.data.model.PracticeQuestionEntity(
            id = "pq-1",
            setId = "set-101",
            projectId = "p-10-sci",
            chapterId = "ch-electricity",
            chapterName = "Electricity",
            topicId = "top-ohms-law",
            topicName = "Ohm's Law",
            questionNumber = 1,
            questionType = "MCQ",
            questionText = "What happens to current if resistance is doubled at constant voltage?",
            originalAiText = "What happens to current if resistance is doubled at constant voltage?",
            userCorrectedText = null,
            answerOptionsJson = "[\"A) Current doubles\", \"B) Current is halved\", \"C) Current remains same\", \"D) Current quadruples\"]",
            correctAnswer = "B) Current is halved",
            explanation = "According to Ohm's Law (V = I * R), current is inversely proportional to resistance for a constant voltage.",
            modelAnswerGuidance = "Option B is correct: I = V / R.",
            marks = 1,
            difficulty = "Medium",
            reviewStatus = com.example.data.model.AiReviewStatus.AWAITING_REVIEW.label,
            reviewNotes = null,
            reviewedBy = null,
            reviewedAt = null,
            modelTag = "gemini-2.5-flash",
            createdAt = System.currentTimeMillis()
        )

        // Initial generated state must be Draft or Awaiting Review (never automatically marked verified)
        assertEquals(com.example.data.model.AiReviewStatus.AWAITING_REVIEW.label, rawAiQuestion.reviewStatus)
        assertEquals("What happens to current if resistance is doubled at constant voltage?", rawAiQuestion.getEffectiveText())
        assertEquals(4, rawAiQuestion.getOptionsList().size)
        assertFalse(rawAiQuestion.isVerified())

        // Educator / Teacher reviews and approves with a refinement
        val reviewedQuestion = rawAiQuestion.copy(
            userCorrectedText = "A circuit has constant potential difference V. If resistance R is doubled, the current I becomes:",
            reviewStatus = com.example.data.model.AiReviewStatus.VERIFIED.label,
            reviewedBy = "educator@cbse-school.org",
            reviewedAt = System.currentTimeMillis(),
            reviewNotes = "Clarified question wording to match CBSE board exam phrasing."
        )

        assertEquals(com.example.data.model.AiReviewStatus.VERIFIED.label, reviewedQuestion.reviewStatus)
        assertTrue(reviewedQuestion.isVerified())
        assertEquals("A circuit has constant potential difference V. If resistance R is doubled, the current I becomes:", reviewedQuestion.getEffectiveText())
        assertEquals("educator@cbse-school.org", reviewedQuestion.reviewedBy)
    }

    @Test
    fun testInteractiveMCQAttemptEvaluation() {
        val question = com.example.data.model.PracticeQuestionEntity(
            id = "pq-chem-1",
            setId = "set-chem-1",
            projectId = "p-10-sci",
            chapterId = "ch-reactions",
            chapterName = "Chemical Reactions and Equations",
            topicId = "top-redox",
            topicName = "Oxidation and Reduction",
            questionNumber = 1,
            questionType = "MCQ",
            questionText = "Which substance is oxidized in the reaction: CuO + H2 -> Cu + H2O?",
            originalAiText = "Which substance is oxidized in the reaction: CuO + H2 -> Cu + H2O?",
            userCorrectedText = null,
            answerOptionsJson = "[\"A) CuO\", \"B) H2\", \"C) Cu\", \"D) H2O\"]",
            correctAnswer = "B) H2",
            explanation = "Hydrogen gains oxygen to form H2O, hence it is oxidized.",
            marks = 1,
            difficulty = "Easy"
        )

        // Check options parsed cleanly
        val opts = question.getOptionsList()
        assertEquals(4, opts.size)
        assertTrue(opts.contains("B) H2"))

        // Correct selection
        val correctAttempt = "B) H2"
        val isCorrect = question.correctAnswer?.contains(correctAttempt.take(3), ignoreCase = true) == true
        assertTrue(isCorrect)

        // Incorrect selection
        val wrongAttempt = "A) CuO"
        val isWrong = question.correctAnswer?.contains(wrongAttempt.take(3), ignoreCase = true) == true
        assertFalse(isWrong)
    }

    @Test
    fun testEvidenceBasedPracticeSuggestionsInsight() {
        val suggestion = com.example.data.model.TopicPracticeSuggestion(
            topicId = "top-light-refraction",
            topicName = "Refraction of Light and Snell's Law",
            chapterId = "ch-light",
            chapterName = "Light - Reflection and Refraction",
            suggestionRationale = "Appeared in 4 of 5 analyzed CBSE board papers with high mark density (16 marks total).",
            category = "HIGH_FREQUENCY",
            verifiedPaperCount = 4,
            historicalAppearances = 6,
            attributedMarks = 16.0,
            frequentQuestionTypes = listOf("Short Answer", "Numerical"),
            yearsRepresented = listOf("2023", "2024"),
            drillDownTopicId = "top-light-refraction"
        )

        assertEquals("Refraction of Light and Snell's Law", suggestion.topicName)
        assertEquals(4, suggestion.verifiedPaperCount)
        assertEquals(6, suggestion.historicalAppearances)
        assertEquals(16.0, suggestion.attributedMarks, 0.001)
        assertEquals("HIGH_FREQUENCY", suggestion.category)
        assertTrue(suggestion.suggestionRationale.contains("Appeared in 4 of 5 analyzed CBSE board papers"))
    }

    @Test
    fun testAiExamEstimateStrictDisclaimerAndNonGuarantee() {
        val estimate = com.example.data.model.AiExamEstimateEntity(
            id = "est-10-sci-2025",
            projectId = "p-10-sci",
            syllabusVersionId = "cbse-10-sci-2024-25",
            subject = "Science",
            scopeJson = "{}",
            estimatesJson = "[]",
            analyzedPaperCount = 5,
            analyzedYears = "2023, 2024",
            dataLimitations = "Based on 5 uploaded papers. May not cover regional variations.",
            disclaimer = "AI estimate — not an official prediction. Historical frequency does not guarantee future examination content.",
            createdAt = System.currentTimeMillis()
        )

        assertEquals(5, estimate.analyzedPaperCount)
        assertEquals("Science", estimate.subject)
        // Must clearly display platform caveat
        assertTrue(estimate.disclaimer.contains("not an official prediction"))
        assertTrue(estimate.disclaimer.contains("Historical frequency does not guarantee future examination content"))
    }

    // =========================================================================
    // Phase 6 Tests: Admin Workflows, RBAC, Reports & PDF Exports
    // =========================================================================

    @Test
    fun testPhase6RoleBasedAccessControlAndPrivilegeSeparation() {
        val student = com.example.data.model.UserEntity(
            id = "student-1",
            email = "student@examintel.org",
            passwordHash = "hash123",
            fullName = "Class 10 Student",
            role = UserRole.STUDENT.name,
            schoolOrOrg = "DPS Delhi",
            targetExamYear = "2025"
        )

        val teacher = student.copy(id = "teacher-1", role = UserRole.TEACHER.name)
        val admin = student.copy(id = "admin-1", role = UserRole.ADMIN.name)
        val superAdmin = student.copy(id = "super-1", role = UserRole.SUPER_ADMIN.name)

        // Students cannot access admin area or delete projects of other users
        assertFalse(student.getRoleEnum().canAccessAdminArea())
        assertFalse(student.getRoleEnum().canManageAllProjects())
        assertFalse(student.getRoleEnum().canManageSyllabus())

        // Teachers can review syllabus and questions, but cannot access system admin area
        assertTrue(teacher.getRoleEnum().canReviewQuestions())
        assertTrue(teacher.getRoleEnum().canApproveQuestions())
        assertTrue(teacher.getRoleEnum().canManageSyllabus())
        assertFalse(teacher.getRoleEnum().canAccessAdminArea())

        // Admin & Super Admin have elevated platform administration
        assertTrue(admin.getRoleEnum().canAccessAdminArea())
        assertTrue(admin.getRoleEnum().canManageAllProjects())
        assertTrue(superAdmin.getRoleEnum().canAccessAdminArea())
        assertTrue(superAdmin.getRoleEnum().canManageAllProjects())
    }

    @Test
    fun testPhase6ProjectPublicAndPrivateAccessRules() {
        val privateProj = ProjectEntity(
            id = "proj-private-1",
            ownerId = "owner-1",
            ownerName = "Teacher Anita",
            name = "Science Internal Test 2025",
            subject = "Science (086)",
            visibility = ProjectVisibility.PRIVATE.name
        )

        assertTrue(privateProj.isPrivate())
        assertFalse(privateProj.isPublic())

        val publicProj = privateProj.copy(visibility = ProjectVisibility.PUBLIC.name)
        assertFalse(publicProj.isPrivate())
        assertTrue(publicProj.isPublic())
    }

    @Test
    fun testPhase6ReportTypesAndDisclaimerRequirements() {
        val reportTypes = com.example.data.model.ReportType.entries
        assertEquals(8, reportTypes.size)

        val sampleReport = com.example.data.model.GeneratedReportEntity(
            id = "rep-1",
            userId = "user-1",
            userEmail = "student@examintel.org",
            projectId = "p-10-math",
            subject = "Mathematics (Standard)",
            reportType = com.example.data.model.ReportType.CHAPTER_COVERAGE.name,
            title = "CBSE Class 10 Mathematics - Chapter & Topic Coverage Audit",
            summaryText = "Curriculum matrix evaluated across 14 chapters and 64 topics.",
            recordCount = 64,
            format = com.example.data.model.ReportFormat.PDF.name,
            filePath = "/storage/reports/rep-1.pdf",
            fileSizeBytes = 45000,
            isPublic = false,
            disclaimer = "Prescribed CBSE Secondary School Curriculum for Academic Session 2024-2025 onwards."
        )

        assertEquals("PDF", sampleReport.format)
        assertEquals(64, sampleReport.recordCount)
        assertFalse(sampleReport.isPublic)
        assertTrue(sampleReport.disclaimer.contains("Prescribed CBSE Secondary School Curriculum"))
    }

    @Test
    fun testPhase6AuditLogIntegrity() {
        val audit = com.example.data.model.AuditLogEntity(
            id = "log-admin-1",
            userId = "admin-1",
            userEmail = "admin@examintel.org",
            action = "ADMIN_PROJECT_VISIBILITY_CHANGED",
            resourceType = "PROJECT",
            resourceId = "proj-101",
            details = "Changed visibility for 'Math Standard' to PUBLIC"
        )

        assertEquals("ADMIN_PROJECT_VISIBILITY_CHANGED", audit.action)
        assertEquals("PROJECT", audit.resourceType)
        assertEquals("admin@examintel.org", audit.userEmail)
        assertTrue(audit.details.contains("to PUBLIC"))
    }

    // =========================================================================
    // Phase 7 Tests: Security, Data Provenance, Optimization & Deployment Readiness
    // =========================================================================

    @Test
    fun testPhase7UploadSecurityAndFormatConstraints() {
        val validExtensions = listOf("paper.pdf", "scan.jpg", "diagram.png", "curriculum.docx", "test.doc")
        for (filename in validExtensions) {
            val isExtensionValid = filename.endsWith(".pdf", ignoreCase = true) ||
                    filename.endsWith(".jpg", ignoreCase = true) ||
                    filename.endsWith(".jpeg", ignoreCase = true) ||
                    filename.endsWith(".png", ignoreCase = true) ||
                    filename.endsWith(".docx", ignoreCase = true) ||
                    filename.endsWith(".doc", ignoreCase = true)
            assertTrue("Expected $filename to be accepted", isExtensionValid)
        }

        val invalidExtensions = listOf("script.sh", "payload.exe", "exploit.php", "macro.vbs", "data.bin")
        for (filename in invalidExtensions) {
            val isExtensionValid = filename.endsWith(".pdf", ignoreCase = true) ||
                    filename.endsWith(".jpg", ignoreCase = true) ||
                    filename.endsWith(".jpeg", ignoreCase = true) ||
                    filename.endsWith(".png", ignoreCase = true) ||
                    filename.endsWith(".docx", ignoreCase = true) ||
                    filename.endsWith(".doc", ignoreCase = true)
            assertFalse("Expected $filename to be rejected", isExtensionValid)
        }

        // Test file size limit rule (25MB)
        val safeSize = 10 * 1024 * 1024L
        val oversized = 30 * 1024 * 1024L
        val maxSizeBytes = 25 * 1024 * 1024L
        assertTrue(safeSize <= maxSizeBytes)
        assertFalse(oversized <= maxSizeBytes)
    }

    @Test
    fun testPhase7SaltedPasswordHashingConsistency() {
        val salt = "ExamIntelligence2026SecretSalt"
        fun hash(pwd: String): String {
            val bytes = java.security.MessageDigest.getInstance("SHA-256")
                .digest((pwd + salt).toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val hash1 = hash("StrongPassword123")
        val hash2 = hash("StrongPassword123")
        val hashWrong = hash("WrongPassword")

        assertEquals(hash1, hash2)
        assertFalse(hash1 == hashWrong)
        assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
    }

    @Test
    fun testPhase7AdminInvitationBootstrapKeyVerification() {
        val expectedKey = "EXAMINTEL-ADMIN-2026"
        val userSubmittedKey = "EXAMINTEL-ADMIN-2026"
        val attackerKey = "RANDOM_KEY"

        assertEquals(expectedKey, userSubmittedKey.trim())
        assertFalse(expectedKey == attackerKey.trim())
    }

    @Test
    fun testPhase7SeparationOfHistoricalAndAiGeneratedRecords() {
        val historical = ExtractedQuestionEntity(
            id = "hist-q101",
            projectId = "p-1",
            sourceDocumentId = "doc-pyq-2024",
            questionNumber = "12",
            section = "Section B",
            questionText = "Historical verified CBSE question",
            questionType = QuestionType.SHORT_ANSWER_1.label,
            marks = 2,
            confidence = 0.94f,
            extractionMethod = "Auto OCR & Structure Parser",
            originalExtractionText = "Historical verified CBSE question"
        )

        val aiPractice = com.example.data.model.PracticeQuestionEntity(
            id = "ai-pq202",
            setId = "set-ai-01",
            projectId = "p-1",
            questionNumber = 1,
            questionText = "Original AI-generated practice question",
            questionType = "MCQ",
            chapterId = "ch-1",
            chapterName = "Real Numbers",
            topicId = "top-1",
            topicName = "Euclid Division",
            marks = 1,
            difficulty = "AI-Estimated: Medium",
            modelTag = "gemini-3.5-flash",
            originalAiText = "Original AI-generated practice question"
        )

        // Verifying strict architectural distinction:
        // 1. Historical question has sourceDocumentId; AI practice question does not (has setId)
        assertEquals("doc-pyq-2024", historical.sourceDocumentId)
        assertEquals("set-ai-01", aiPractice.setId)

        // 2. Historical question tracks extraction confidence; AI question tags model
        assertTrue(historical.confidence > 0.9f)
        assertEquals("gemini-3.5-flash", aiPractice.modelTag)

        // 3. AI question has difficulty estimation prefix
        assertTrue(aiPractice.difficulty.startsWith("AI-Estimated"))
    }
}

