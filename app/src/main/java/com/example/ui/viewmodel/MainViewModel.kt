package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamDatabase
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.AiReviewStatus
import com.example.data.model.AnalysisScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.CBSE_CLASS_10_SUBJECTS
import com.example.data.model.CoverageStatus
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.HistoricalAnalysisResult
import com.example.data.model.HistoricalTopicMetrics
import com.example.data.model.MappingReviewLogEntity
import com.example.data.model.MappingReviewStatus
import com.example.data.model.MappingVerificationScope
import com.example.data.model.MarksAttributionMethod
import com.example.data.model.PracticeAttemptEntity
import com.example.data.model.PracticeDifficulty
import com.example.data.model.PracticeGenerationRequest
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.PracticeQuestionFormat
import com.example.data.model.PracticeQuestionSetEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectVisibility
import com.example.data.model.QuestionReviewLogEntity
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.QuestionType
import com.example.data.model.QuestionVerificationScope
import com.example.data.model.ReportGenerationRequest
import com.example.data.model.ScopeSummary
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusNodeType
import com.example.data.model.SyllabusSourceType
import com.example.data.model.SyllabusVerificationStatus
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.TopicCoverageStats
import com.example.data.model.TopicEstimateItem
import com.example.data.model.TopicPracticeSuggestion
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.AdminRepository
import com.example.data.repository.AdminSystemStats
import com.example.data.repository.AiPracticeRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatMessage
import com.example.data.repository.DocumentProcessingRepository
import com.example.data.repository.DocumentRepository
import com.example.data.repository.ExportFormat
import com.example.data.repository.GeminiModelOption
import com.example.data.repository.GeminiRepository
import com.example.data.repository.HistoricalAnalysisRepository
import com.example.data.repository.MessageSender
import com.example.data.repository.ProjectRepository
import com.example.data.repository.ReportRepository
import com.example.data.repository.SyllabusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ExamDatabase.getInstance(application)
    val authRepo = AuthRepository(application, db.userDao(), db.auditLogDao())
    val projectRepo = ProjectRepository(db.projectDao(), db.documentDao(), db.auditLogDao())
    val documentRepo = DocumentRepository(application, db.documentDao(), db.projectDao(), db.auditLogDao())
    val documentProcessingRepo = DocumentProcessingRepository(
        application,
        db.documentDao(),
        db.projectDao(),
        db.extractedQuestionDao(),
        db.questionReviewLogDao(),
        db.auditLogDao()
    )
    val adminRepo = AdminRepository(
        userDao = db.userDao(),
        projectDao = db.projectDao(),
        documentDao = db.documentDao(),
        extractedQuestionDao = db.extractedQuestionDao(),
        syllabusDao = db.syllabusDao(),
        questionTopicMappingDao = db.questionTopicMappingDao(),
        practiceQuestionDao = db.practiceQuestionDao(),
        aiExamEstimateDao = db.aiExamEstimateDao(),
        generatedReportDao = db.generatedReportDao(),
        auditLogDao = db.auditLogDao()
    )
    val geminiRepo = GeminiRepository()
    val syllabusRepo = SyllabusRepository(
        application,
        db.syllabusDao(),
        db.questionTopicMappingDao(),
        db.extractedQuestionDao(),
        db.documentDao(),
        db.projectDao(),
        db.auditLogDao()
    )
    val historicalAnalysisRepo = HistoricalAnalysisRepository(
        context = application,
        projectDao = db.projectDao(),
        documentDao = db.documentDao(),
        extractedQuestionDao = db.extractedQuestionDao(),
        questionTopicMappingDao = db.questionTopicMappingDao(),
        syllabusDao = db.syllabusDao(),
        historicalAnalysisDao = db.historicalAnalysisDao(),
        auditLogDao = db.auditLogDao()
    )
    val aiPracticeRepo = AiPracticeRepository(
        context = application,
        projectDao = db.projectDao(),
        documentDao = db.documentDao(),
        extractedQuestionDao = db.extractedQuestionDao(),
        questionTopicMappingDao = db.questionTopicMappingDao(),
        syllabusDao = db.syllabusDao(),
        practiceQuestionDao = db.practiceQuestionDao(),
        aiExamEstimateDao = db.aiExamEstimateDao(),
        auditLogDao = db.auditLogDao(),
        historicalAnalysisRepo = historicalAnalysisRepo
    )
    val reportRepo = ReportRepository(
        context = application,
        generatedReportDao = db.generatedReportDao(),
        projectDao = db.projectDao(),
        documentDao = db.documentDao(),
        extractedQuestionDao = db.extractedQuestionDao(),
        syllabusDao = db.syllabusDao(),
        questionTopicMappingDao = db.questionTopicMappingDao(),
        practiceQuestionDao = db.practiceQuestionDao(),
        aiExamEstimateDao = db.aiExamEstimateDao(),
        historicalAnalysisRepo = historicalAnalysisRepo,
        auditLogDao = db.auditLogDao()
    )

    val currentUser: StateFlow<UserEntity?> = authRepo.currentUser
    val isGuest: StateFlow<Boolean> = authRepo.isGuest

    // Navigation and screen state
    private val _currentRoute = MutableStateFlow("landing")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    // Phase 4 Historical Analysis State
    private val _analysisScope = MutableStateFlow<AnalysisScope?>(null)
    val analysisScope: StateFlow<AnalysisScope?> = _analysisScope.asStateFlow()

    private val _scopeSummary = MutableStateFlow<ScopeSummary?>(null)
    val scopeSummary: StateFlow<ScopeSummary?> = _scopeSummary.asStateFlow()

    private val _historicalAnalysisResult = MutableStateFlow<HistoricalAnalysisResult?>(null)
    val historicalAnalysisResult: StateFlow<HistoricalAnalysisResult?> = _historicalAnalysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _selectedTopicDetail = MutableStateFlow<HistoricalTopicMetrics?>(null)
    val selectedTopicDetail: StateFlow<HistoricalTopicMetrics?> = _selectedTopicDetail.asStateFlow()

    private val _topicAssociatedQuestions = MutableStateFlow<List<ExtractedQuestionEntity>>(emptyList())
    val topicAssociatedQuestions: StateFlow<List<ExtractedQuestionEntity>> = _topicAssociatedQuestions.asStateFlow()

    // Phase 5 AI Practice & Estimates State
    private val _activePracticeSet = MutableStateFlow<PracticeQuestionSetEntity?>(null)
    val activePracticeSet: StateFlow<PracticeQuestionSetEntity?> = _activePracticeSet.asStateFlow()

    private val _activePracticeQuestions = MutableStateFlow<List<PracticeQuestionEntity>>(emptyList())
    val activePracticeQuestions: StateFlow<List<PracticeQuestionEntity>> = _activePracticeQuestions.asStateFlow()

    private val _isGeneratingPractice = MutableStateFlow(false)
    val isGeneratingPractice: StateFlow<Boolean> = _isGeneratingPractice.asStateFlow()

    private val _practiceSuggestions = MutableStateFlow<List<TopicPracticeSuggestion>>(emptyList())
    val practiceSuggestions: StateFlow<List<TopicPracticeSuggestion>> = _practiceSuggestions.asStateFlow()

    private val _practiceAttempts = MutableStateFlow<List<PracticeAttemptEntity>>(emptyList())
    val practiceAttempts: StateFlow<List<PracticeAttemptEntity>> = _practiceAttempts.asStateFlow()

    private val _isEstimatingExam = MutableStateFlow(false)
    val isEstimatingExam: StateFlow<Boolean> = _isEstimatingExam.asStateFlow()

    val projectPracticeSets: StateFlow<List<PracticeQuestionSetEntity>> = _selectedProjectId
        .flatMapLatest { projId ->
            if (projId != null) aiPracticeRepo.getSetsForProject(projId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPracticeSets: StateFlow<List<PracticeQuestionSetEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) aiPracticeRepo.getSavedSetsForUser(user.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestAiExamEstimate: StateFlow<AiExamEstimateEntity?> = _selectedProjectId
        .flatMapLatest { projId ->
            if (projId != null) aiPracticeRepo.getLatestEstimateForProject(projId)
            else kotlinx.coroutines.flow.flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Notification / Error banners
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Projects list with reactive filter
    val projectSearchQuery = MutableStateFlow("")
    val selectedSubjectFilter = MutableStateFlow("All")

    val userProjects: StateFlow<List<ProjectEntity>> = currentUser
        .flatMapLatest { user -> projectRepo.getProjectsForUser(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        userProjects,
        projectSearchQuery,
        selectedSubjectFilter
    ) { projects, query, subject ->
        projects.filter { proj ->
            val matchesQuery = query.isBlank() ||
                    proj.name.contains(query, ignoreCase = true) ||
                    proj.description.contains(query, ignoreCase = true) ||
                    proj.subject.contains(query, ignoreCase = true)

            val matchesSubject = subject == "All" || proj.subject.equals(subject, ignoreCase = true)
            matchesQuery && matchesSubject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All documents for current user
    val userDocuments: StateFlow<List<DocumentEntity>> = currentUser
        .flatMapLatest { user -> documentRepo.getDocumentsForUser(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Documents for selected project
    val selectedProjectDocuments: StateFlow<List<DocumentEntity>> = _selectedProjectId
        .flatMapLatest { projId ->
            if (projId != null) documentRepo.getDocumentsForProject(projId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected project entity
    val selectedProject: StateFlow<ProjectEntity?> = combine(userProjects, _selectedProjectId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Document Processing & Verification State
    private val _selectedDocumentId = MutableStateFlow<String?>(null)
    val selectedDocumentId: StateFlow<String?> = _selectedDocumentId.asStateFlow()

    val selectedDocumentForReview: StateFlow<DocumentEntity?> = combine(userDocuments, _selectedDocumentId) { docs, id ->
        docs.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val documentQuestions: StateFlow<List<ExtractedQuestionEntity>> = _selectedDocumentId
        .flatMapLatest { docId ->
            if (docId != null) documentProcessingRepo.getQuestionsForDocument(docId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentReviewLogs: StateFlow<List<QuestionReviewLogEntity>> = _selectedDocumentId
        .flatMapLatest { docId ->
            if (docId != null) documentProcessingRepo.getReviewLogsForDocument(docId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessingDoc = MutableStateFlow(false)
    val isProcessingDoc: StateFlow<Boolean> = _isProcessingDoc.asStateFlow()

    private val _processingStage = MutableStateFlow<String?>(null)
    val processingStage: StateFlow<String?> = _processingStage.asStateFlow()

    // Admin State
    private val _adminStats = MutableStateFlow<AdminSystemStats?>(null)
    val adminStats: StateFlow<AdminSystemStats?> = _adminStats.asStateFlow()

    val adminUsersList: StateFlow<List<UserEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllUsers(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAuditLogs: StateFlow<List<AuditLogEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAuditLogs(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminProjects: StateFlow<List<ProjectEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllProjects(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminDocuments: StateFlow<List<DocumentEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllDocuments(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminExtractedQuestions: StateFlow<List<ExtractedQuestionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllExtractedQuestions(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminMappings: StateFlow<List<QuestionTopicMappingEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllMappings(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminPracticeQuestions: StateFlow<List<PracticeQuestionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllPracticeQuestions(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAiEstimates: StateFlow<List<AiExamEstimateEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllAiEstimates(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminReports: StateFlow<List<GeneratedReportEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.getRoleEnum().canAccessAdminArea()) {
                adminRepo.getAllReports(user)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 6: Reports State
    val userReports: StateFlow<List<GeneratedReportEntity>> = currentUser
        .flatMapLatest { user -> reportRepo.getReportsForUser(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    // Phase 3: Syllabus and Question-to-Topic Mapping State
    val allSyllabusVersions: StateFlow<List<SyllabusVersionEntity>> = syllabusRepo.getAllSyllabusVersions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSyllabusVersionId = MutableStateFlow<String?>("cbse-10-math-2024-25")
    val selectedSyllabusVersionId: StateFlow<String?> = _selectedSyllabusVersionId.asStateFlow()

    val selectedSyllabusVersion: StateFlow<SyllabusVersionEntity?> = combine(allSyllabusVersions, _selectedSyllabusVersionId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syllabusNodes: StateFlow<List<SyllabusNodeEntity>> = _selectedSyllabusVersionId
        .flatMapLatest { vId ->
            if (vId != null) syllabusRepo.getNodesForVersion(vId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectMappings: StateFlow<List<QuestionTopicMappingEntity>> = _selectedProjectId
        .flatMapLatest { projId ->
            if (projId != null) syllabusRepo.getMappingsForProject(projId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProjectQuestions: StateFlow<List<ExtractedQuestionEntity>> = _selectedProjectId
        .flatMapLatest { projId ->
            if (projId != null) db.extractedQuestionDao().getQuestionsForProject(projId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _topicCoverageList = MutableStateFlow<List<TopicCoverageStats>>(emptyList())
    val topicCoverageList: StateFlow<List<TopicCoverageStats>> = _topicCoverageList.asStateFlow()

    private val _isMappingProcessing = MutableStateFlow(false)
    val isMappingProcessing: StateFlow<Boolean> = _isMappingProcessing.asStateFlow()

    // Gemini Chatbot State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Welcome to Exam Intelligence AI! I'm your CBSE Class 10 specialist. Ask me about subject weightage, competency question breakdowns, paper analysis structure, or problem explanations.",
                modelTag = "Exam Intelligence AI"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    val selectedGeminiModel = MutableStateFlow(GeminiModelOption.PRO_HIGH_THINKING)
    val isHighThinkingEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            syllabusRepo.seedOfficialSyllabiIfEmpty()
            currentUser.collect { user ->
                if (user != null) {
                    if (user.getRoleEnum() == UserRole.STUDENT) {
                        projectRepo.seedSampleProjectsIfEmpty(user)
                    }
                    if (user.getRoleEnum().canAccessAdminArea()) {
                        refreshAdminStats()
                    }
                }
            }
        }
    }

    fun navigateTo(route: String) {
        _currentRoute.value = route
        clearMessages()
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        _currentRoute.value = "project_detail"
        val proj = userProjects.value.find { it.id == projectId }
        if (proj != null) {
            val matching = allSyllabusVersions.value.find {
                it.subject.equals(proj.subject, ignoreCase = true) && it.academicSession == proj.academicYear
            } ?: allSyllabusVersions.value.find {
                it.subject.contains(proj.subject.take(4), ignoreCase = true)
            }
            if (matching != null) {
                _selectedSyllabusVersionId.value = matching.id
            }
        }
        refreshCoverageStats()
        initOrUpdateAnalysisScope()
        refreshPracticeSuggestions()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _notificationMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissNotification() {
        _notificationMessage.value = null
    }

    // --- Authentication Actions ---

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepo.login(email, pass)
            _isLoading.value = false
            result.onSuccess {
                _notificationMessage.value = "Welcome back, ${it.fullName}!"
                onSuccess()
                _currentRoute.value = "dashboard"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Login failed"
            }
        }
    }

    fun register(
        email: String,
        pass: String,
        fullName: String,
        role: UserRole,
        adminCode: String?,
        school: String,
        examYear: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepo.register(email, pass, fullName, role, adminCode, school, examYear)
            _isLoading.value = false
            result.onSuccess {
                _notificationMessage.value = "Account created successfully as ${it.role}!"
                onSuccess()
                _currentRoute.value = "dashboard"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Registration failed"
            }
        }
    }

    fun continueAsGuest() {
        authRepo.continueAsGuest()
        _currentRoute.value = "dashboard"
        _notificationMessage.value = "Browsing in Guest Mode (Read-only capabilities)"
    }

    fun logout() {
        authRepo.logout()
        _currentRoute.value = "landing"
        _notificationMessage.value = "Signed out successfully"
    }

    fun resetPassword(email: String, newPass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepo.resetPassword(email, newPass)
            _isLoading.value = false
            result.onSuccess {
                _notificationMessage.value = "Password updated! You can now log in with your new password."
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Password reset failed"
            }
        }
    }

    // --- Project Operations ---

    fun createProject(
        name: String,
        subject: String,
        academicYear: String,
        description: String,
        visibility: ProjectVisibility
    ) {
        val user = currentUser.value
        if (user == null) {
            _errorMessage.value = "Please sign in to create a project."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = projectRepo.createProject(user, name, subject, academicYear, description, visibility)
            _isLoading.value = false
            res.onSuccess { created ->
                _notificationMessage.value = "Project '${created.name}' created!"
                selectProject(created.id)
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to create project"
            }
        }
    }

    fun updateProject(
        projectId: String,
        name: String,
        subject: String,
        academicYear: String,
        description: String,
        visibility: ProjectVisibility,
        status: ProjectStatus
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = projectRepo.updateProject(user, projectId, name, subject, academicYear, description, visibility, status)
            _isLoading.value = false
            res.onSuccess {
                _notificationMessage.value = "Project metadata updated."
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Update failed"
            }
        }
    }

    fun deleteProject(projectId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = projectRepo.deleteProject(user, projectId)
            _isLoading.value = false
            res.onSuccess {
                _notificationMessage.value = "Project deleted successfully."
                if (_selectedProjectId.value == projectId) {
                    _selectedProjectId.value = null
                    _currentRoute.value = "projects"
                }
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Delete failed"
            }
        }
    }

    // --- Document Operations ---

    fun uploadDocument(
        projectId: String,
        uri: Uri?,
        filename: String,
        category: DocumentCategory,
        mimeType: String,
        fileSizeBytes: Long
    ) {
        val user = currentUser.value
        if (user == null) {
            _errorMessage.value = "Authentication required to upload documents."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = documentRepo.saveUploadedFile(
                user = user,
                projectId = projectId,
                uri = uri,
                customFilename = filename,
                category = category,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes
            )
            _isLoading.value = false
            res.onSuccess { doc ->
                _notificationMessage.value = "Uploaded '${doc.originalFilename}'. Ready for Phase 2 OCR processing."
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Upload failed"
            }
        }
    }

    fun deleteDocument(docId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = documentRepo.deleteDocument(user, docId)
            _isLoading.value = false
            res.onSuccess {
                _notificationMessage.value = "Document deleted."
                if (_selectedDocumentId.value == docId) {
                    _selectedDocumentId.value = null
                    _currentRoute.value = "project_detail"
                }
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Could not delete document"
            }
        }
    }

    fun seedSampleDocsForCurrentProject() {
        val user = currentUser.value ?: return
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            documentRepo.seedSampleDocumentsIfEmpty(user, projId)
            _notificationMessage.value = "Sample CBSE question papers staged for this project."
        }
    }

    // --- Phase 2: Document Processing & Human Verification ---

    fun openReviewWorkspace(documentId: String) {
        _selectedDocumentId.value = documentId
        _currentRoute.value = "review_workspace"
    }

    fun startDocumentProcessing(documentId: String) {
        val user = currentUser.value
        if (user == null) {
            _errorMessage.value = "Authentication required to process documents."
            return
        }

        viewModelScope.launch {
            _isProcessingDoc.value = true
            _processingStage.value = "Initializing processing pipeline..."

            val result = documentProcessingRepo.processDocument(
                user = user,
                documentId = documentId,
                onStageUpdate = { _, message ->
                    _processingStage.value = message
                }
            )

            _isProcessingDoc.value = false
            _processingStage.value = null

            result.onSuccess { questions ->
                _notificationMessage.value = "Successfully extracted ${questions.size} structured questions!"
                openReviewWorkspace(documentId)
            }.onFailure { error ->
                _errorMessage.value = "Document processing failed: ${error.localizedMessage}"
            }
        }
    }

    fun approveQuestion(questionId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = documentProcessingRepo.approveQuestion(user, questionId)
            res.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Approval failed"
            }
        }
    }

    fun rejectQuestion(questionId: String, reason: String = "Rejected during human review") {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = documentProcessingRepo.rejectQuestion(user, questionId, reason)
            res.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Rejection failed"
            }
        }
    }

    fun editQuestion(
        questionId: String,
        number: String,
        section: String,
        text: String,
        type: QuestionType,
        marks: Int?,
        topic: String?,
        options: List<String>?,
        choice: String?
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = documentProcessingRepo.updateQuestion(
                user = user,
                questionId = questionId,
                editedNumber = number,
                editedSection = section,
                editedText = text,
                editedType = type,
                editedMarks = marks,
                editedTopic = topic,
                editedOptions = options,
                editedChoice = choice
            )
            res.onSuccess {
                _notificationMessage.value = "Question Q${it.questionNumber} verified and updated!"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to update question"
            }
        }
    }

    fun bulkApproveHighConfidence(documentId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = documentProcessingRepo.bulkApproveHighConfidence(user, documentId)
            _isLoading.value = false
            res.onSuccess { count ->
                _notificationMessage.value = "Batch approved $count high-confidence (>85%) questions!"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Bulk approval failed"
            }
        }
    }

    fun addManualQuestion(
        documentId: String,
        projectId: String,
        number: String,
        section: String,
        text: String,
        type: QuestionType,
        marks: Int?,
        topic: String?,
        options: List<String>?,
        choice: String?
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = documentProcessingRepo.addManualQuestion(
                user = user,
                documentId = documentId,
                projectId = projectId,
                questionNumber = number,
                section = section,
                text = text,
                type = type,
                marks = marks,
                topic = topic,
                options = options,
                choice = choice
            )
            res.onSuccess {
                _notificationMessage.value = "Added question Q${it.questionNumber} to document!"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to add question"
            }
        }
    }

    fun deleteQuestion(questionId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = documentProcessingRepo.deleteQuestion(user, questionId)
            res.onSuccess {
                _notificationMessage.value = "Question removed."
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to delete question"
            }
        }
    }

    fun exportDocumentQuestions(documentId: String, format: ExportFormat, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val content = documentProcessingRepo.exportQuestions(documentId, format)
            _isLoading.value = false
            onReady(content)
        }
    }

    // --- Admin Operations ---

    fun refreshAdminStats() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.getSystemStats(user)
            res.onSuccess { _adminStats.value = it }
        }
    }

    fun toggleUserStatus(userId: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.toggleUserAccountStatus(admin, userId)
            res.onSuccess { newStatus ->
                _notificationMessage.value = "User status updated to: ${if (newStatus) "Active" else "Suspended"}"
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Operation failed"
            }
        }
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.updateUserRole(admin, userId, newRole)
            res.onSuccess {
                _notificationMessage.value = "User role updated to ${newRole.displayName()}."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to update role"
            }
        }
    }

    fun changeProjectVisibility(projectId: String, newVisibility: ProjectVisibility) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.changeProjectVisibility(admin, projectId, newVisibility)
            res.onSuccess {
                _notificationMessage.value = "Project visibility changed to ${newVisibility.name}."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to change visibility"
            }
        }
    }

    fun adminDeleteProject(projectId: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.deleteProject(admin, projectId)
            res.onSuccess {
                _notificationMessage.value = "Project removed by administrator."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun adminDeleteDocument(documentId: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.deleteDocument(admin, documentId)
            res.onSuccess {
                _notificationMessage.value = "Document deleted by administrator."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun adminApproveQuestion(questionId: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = adminRepo.approveQuestion(admin, questionId)
            res.onSuccess {
                _notificationMessage.value = "Question approved by administrator."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Approval failed"
            }
        }
    }

    fun adminDeleteReport(reportId: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val res = reportRepo.deleteReport(admin, reportId)
            res.onSuccess {
                _notificationMessage.value = "Report deleted."
                refreshAdminStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to delete report"
            }
        }
    }

    // --- Phase 6: Reports Operations ---

    fun generateReport(request: ReportGenerationRequest) {
        val user = currentUser.value
        if (user == null) {
            _errorMessage.value = "Authentication required to generate official reports."
            return
        }

        viewModelScope.launch {
            _isGeneratingReport.value = true
            val res = reportRepo.generateReport(user, request)
            _isGeneratingReport.value = false
            res.onSuccess { rep ->
                _notificationMessage.value = "Report '${rep.title}' generated successfully!"
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Report generation failed"
            }
        }
    }

    fun deleteReport(reportId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = reportRepo.deleteReport(user, reportId)
            res.onSuccess {
                _notificationMessage.value = "Report deleted."
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to delete report"
            }
        }
    }

    fun toggleReportPublicVisibility(reportId: String, isPublic: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = reportRepo.toggleReportPublicVisibility(user, reportId, isPublic)
            res.onSuccess {
                _notificationMessage.value = if (isPublic) "Report is now publicly visible." else "Report made private."
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to update visibility"
            }
        }
    }

    // --- Gemini Chatbot ---

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank() || _isChatGenerating.value) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = prompt.trim()
        )
        _chatMessages.value = _chatMessages.value + userMessage
        _isChatGenerating.value = true

        val thinking = isHighThinkingEnabled.value
        val model = selectedGeminiModel.value

        viewModelScope.launch {
            val result = geminiRepo.sendChatMessage(
                history = _chatMessages.value,
                newPrompt = prompt,
                selectedModel = model,
                enableHighThinking = thinking
            )
            _isChatGenerating.value = false

            result.onSuccess { reply ->
                val assistantMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = reply,
                    modelTag = if (thinking) "Gemini 3.1 Pro (High Thinking)" else model.label,
                    isHighThinking = thinking
                )
                _chatMessages.value = _chatMessages.value + assistantMsg
            }.onFailure { error ->
                val errorMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "⚠️ Gemini API Error: ${error.localizedMessage}\n\nTip: You can configure the GEMINI_API_KEY in the AI Studio Secrets panel.",
                    modelTag = "System"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Chat history cleared. What CBSE Class 10 topic or question paper should we explore?",
                modelTag = "Exam Intelligence AI"
            )
        )
    }

    // --- Phase 3: Syllabus Intelligence & Mapping Operations ---

    fun selectSyllabusVersion(versionId: String) {
        _selectedSyllabusVersionId.value = versionId
        refreshCoverageStats()
        initOrUpdateAnalysisScope()
    }

    fun createOrImportSyllabus(
        subject: String,
        academicSession: String,
        versionIdentifier: String,
        sourceType: SyllabusSourceType,
        sourceTitle: String,
        sourceUrl: String?,
        sourceDocumentRef: String?,
        notes: String?,
        rawContentToParse: String? = null
    ) {
        val user = currentUser.value
        if (user == null || !user.getRoleEnum().canManageSyllabus()) {
            _errorMessage.value = "Unauthorized: Only Teachers and Administrators can create or import syllabi."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = syllabusRepo.createOrImportSyllabus(
                user, subject, academicSession, versionIdentifier, sourceType, sourceTitle, sourceUrl, sourceDocumentRef, notes, rawContentToParse
            )
            _isLoading.value = false
            res.onSuccess { created ->
                _notificationMessage.value = "Imported syllabus for ${created.subject} (${created.academicSession}). Status: Unverified"
                _selectedSyllabusVersionId.value = created.id
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to import syllabus"
            }
        }
    }

    fun verifySyllabusVersion(
        versionId: String,
        status: SyllabusVerificationStatus,
        notes: String?
    ) {
        val user = currentUser.value
        if (user == null || !user.getRoleEnum().canApproveSyllabus()) {
            _errorMessage.value = "Unauthorized: Only Teachers and Administrators can verify official syllabi."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = syllabusRepo.verifySyllabusVersion(user, versionId, status, notes)
            _isLoading.value = false
            res.onSuccess {
                _notificationMessage.value = "Syllabus verification status updated to '${status.label}'."
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Verification update failed"
            }
        }
    }

    fun addSyllabusNode(
        parentId: String?,
        nodeType: SyllabusNodeType,
        code: String?,
        name: String,
        description: String?,
        isExcluded: Boolean = false,
        exclusionReason: String? = null
    ) {
        val user = currentUser.value ?: return
        val vId = _selectedSyllabusVersionId.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.addSyllabusNode(user, vId, parentId, nodeType, code, name, description, isExcluded, exclusionReason)
            res.onSuccess {
                _notificationMessage.value = "Added ${nodeType.label}: '$name'."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Could not add node"
            }
        }
    }

    fun updateSyllabusNode(
        nodeId: String,
        name: String,
        code: String?,
        description: String?,
        isExcluded: Boolean,
        exclusionReason: String?
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.updateSyllabusNode(user, nodeId, name, code, description, isExcluded, exclusionReason)
            res.onSuccess {
                _notificationMessage.value = "Updated syllabus node."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Could not update node"
            }
        }
    }

    fun deleteSyllabusNode(nodeId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.deleteSyllabusNode(user, nodeId)
            res.onSuccess {
                _notificationMessage.value = "Node removed from syllabus."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Could not delete node"
            }
        }
    }

    fun mergeSyllabusNodes(sourceNodeId: String, targetNodeId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.mergeSyllabusNodes(user, sourceNodeId, targetNodeId)
            res.onSuccess {
                _notificationMessage.value = "Syllabus nodes merged successfully."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Merge failed"
            }
        }
    }

    fun autoMapProjectQuestions() {
        val user = currentUser.value
        val projId = _selectedProjectId.value
        val vId = _selectedSyllabusVersionId.value

        if (user == null || projId == null || vId == null) {
            _errorMessage.value = "Please select a project and an active syllabus version."
            return
        }

        viewModelScope.launch {
            _isMappingProcessing.value = true
            val docs = selectedProjectDocuments.value
            val allQuestions = mutableListOf<ExtractedQuestionEntity>()
            for (doc in docs) {
                allQuestions.addAll(db.extractedQuestionDao().getQuestionsListForDocument(doc.id))
            }

            if (allQuestions.isEmpty()) {
                _isMappingProcessing.value = false
                _errorMessage.value = "No questions found in this project. Please process question papers in Phase 2 first."
                return@launch
            }

            val res = syllabusRepo.batchMapQuestionsList(allQuestions, vId, user)
            _isMappingProcessing.value = false

            res.onSuccess { mappings ->
                _notificationMessage.value = "Generated ${mappings.size} topic mappings for ${allQuestions.size} questions."
                refreshCoverageStats()
            }.onFailure { error ->
                _errorMessage.value = "Auto mapping failed: ${error.localizedMessage}"
            }
        }
    }

    fun approveMapping(mappingId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.approveMapping(user, mappingId)
            res.onSuccess {
                _notificationMessage.value = "Mapping verified."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Approval failed"
            }
        }
    }

    fun rejectMapping(mappingId: String, notes: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.rejectMapping(user, mappingId, notes)
            res.onSuccess {
                _notificationMessage.value = "Mapping rejected."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Rejection failed"
            }
        }
    }

    fun modifyMapping(
        mappingId: String,
        newChapterId: String,
        newChapterName: String,
        newTopicId: String,
        newTopicName: String,
        notes: String?
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.modifyMapping(user, mappingId, newChapterId, newChapterName, newTopicId, newTopicName, notes)
            res.onSuccess {
                _notificationMessage.value = "Mapping updated to '$newTopicName'."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to reassign mapping"
            }
        }
    }

    fun markMappingOutsideSyllabus(mappingId: String, reason: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.markOutsideSyllabus(user, mappingId, reason)
            res.onSuccess {
                _notificationMessage.value = "Question marked as outside syllabus."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to mark outside syllabus"
            }
        }
    }

    fun markMappingUncertain(mappingId: String, reason: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.markUncertain(user, mappingId, reason)
            res.onSuccess {
                _notificationMessage.value = "Mapping marked uncertain for review."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to mark uncertain"
            }
        }
    }

    fun addMultiTopicMapping(
        questionId: String,
        sourceDocId: String,
        projectId: String,
        chapterId: String,
        chapterName: String,
        topicId: String,
        topicName: String,
        notes: String?
    ) {
        val user = currentUser.value ?: return
        val vId = _selectedSyllabusVersionId.value ?: return
        viewModelScope.launch {
            val res = syllabusRepo.addMultiTopicMapping(user, questionId, sourceDocId, projectId, vId, chapterId, chapterName, topicId, topicName, notes)
            res.onSuccess {
                _notificationMessage.value = "Additional topic '$topicName' mapped."
                refreshCoverageStats()
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Failed to add topic mapping"
            }
        }
    }

    fun deleteMapping(mappingId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            syllabusRepo.deleteMapping(user, mappingId)
            _notificationMessage.value = "Mapping removed."
            refreshCoverageStats()
            _selectedProjectId.value?.let { historicalAnalysisRepo.markRunsStale(it) }
        }
    }

    fun refreshCoverageStats() {
        val projId = _selectedProjectId.value ?: return
        val vId = _selectedSyllabusVersionId.value ?: return
        viewModelScope.launch {
            val stats = syllabusRepo.computeSyllabusCoverage(projId, vId)
            _topicCoverageList.value = stats
        }
    }

    // --- Phase 4: Historical Question Analysis & Paper Pattern Intelligence ---

    fun initOrUpdateAnalysisScope() {
        val proj = selectedProject.value ?: return
        val vId = _selectedSyllabusVersionId.value
            ?: allSyllabusVersions.value.find {
                it.subject.equals(proj.subject, ignoreCase = true)
            }?.id
            ?: allSyllabusVersions.value.firstOrNull()?.id
            ?: return

        val currentScope = _analysisScope.value
        val newScope = if (currentScope != null && currentScope.projectId == proj.id) {
            currentScope.copy(
                subject = proj.subject,
                syllabusVersionId = vId
            )
        } else {
            AnalysisScope(
                projectId = proj.id,
                board = proj.board,
                classLevel = "10",
                subject = proj.subject,
                syllabusVersionId = vId,
                selectedDocumentIds = emptyList(),
                questionVerificationScope = QuestionVerificationScope.VERIFIED_ONLY,
                mappingVerificationScope = MappingVerificationScope.APPROVED_ONLY,
                attributionMethod = MarksAttributionMethod.FULL_OVERLAPPING
            )
        }
        _analysisScope.value = newScope

        viewModelScope.launch {
            val summaryRes = historicalAnalysisRepo.computeScopeSummary(newScope, currentUser.value)
            summaryRes.onSuccess {
                _scopeSummary.value = it
            }
        }
    }

    fun updateScopeQuestionVerification(scope: QuestionVerificationScope) {
        val cur = _analysisScope.value ?: return
        val updated = cur.copy(questionVerificationScope = scope)
        _analysisScope.value = updated
        recomputeScopeSummary(updated)
    }

    fun updateScopeMappingVerification(scope: MappingVerificationScope) {
        val cur = _analysisScope.value ?: return
        val updated = cur.copy(mappingVerificationScope = scope)
        _analysisScope.value = updated
        recomputeScopeSummary(updated)
    }

    fun updateScopeAttributionMethod(method: MarksAttributionMethod) {
        val cur = _analysisScope.value ?: return
        val updated = cur.copy(attributionMethod = method)
        _analysisScope.value = updated
    }

    private fun recomputeScopeSummary(scope: AnalysisScope) {
        viewModelScope.launch {
            val res = historicalAnalysisRepo.computeScopeSummary(scope, currentUser.value)
            res.onSuccess {
                _scopeSummary.value = it
            }
        }
    }

    fun executeHistoricalAnalysis() {
        var scope = _analysisScope.value
        if (scope == null) {
            initOrUpdateAnalysisScope()
            scope = _analysisScope.value
        }
        val targetScope = scope ?: return
        val user = currentUser.value

        viewModelScope.launch {
            _isAnalyzing.value = true
            val result = historicalAnalysisRepo.executeAnalysis(targetScope, user)
            _isAnalyzing.value = false
            result.onSuccess { res ->
                _historicalAnalysisResult.value = res
                _notificationMessage.value = "Historical analysis completed: ${res.totalEligiblePapers} papers, ${res.totalVerifiedQuestions} questions."
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to execute historical analysis"
            }
        }
    }

    fun openTopicDetail(topic: HistoricalTopicMetrics) {
        _selectedTopicDetail.value = topic
        viewModelScope.launch {
            val qList = mutableListOf<ExtractedQuestionEntity>()
            for (qId in topic.associatedQuestionIds) {
                db.extractedQuestionDao().getQuestionById(qId)?.let { qList.add(it) }
            }
            _topicAssociatedQuestions.value = qList
        }
    }

    fun closeTopicDetail() {
        _selectedTopicDetail.value = null
        _topicAssociatedQuestions.value = emptyList()
    }

    fun exportHistoricalAnalysis(format: String, onComplete: (String) -> Unit) {
        val res = _historicalAnalysisResult.value
        if (res == null) {
            _errorMessage.value = "No historical analysis available to export. Run analysis first."
            return
        }
        val content = historicalAnalysisRepo.generateExportReport(res, format)
        onComplete(content)
    }

    // --- Phase 5: AI Practice Questions, Suggestions & Estimates ---

    fun generatePracticeQuestions(request: PracticeGenerationRequest) {
        val user = currentUser.value
        if (user == null) {
            _errorMessage.value = "Sign in to generate AI practice questions."
            return
        }

        viewModelScope.launch {
            _isGeneratingPractice.value = true
            val result = aiPracticeRepo.generatePracticeQuestions(
                user = user,
                request = request,
                modelOption = GeminiModelOption.FLASH_GENERAL
            )
            _isGeneratingPractice.value = false
            result.onSuccess { (set, questions) ->
                _activePracticeSet.value = set
                _activePracticeQuestions.value = questions
                _notificationMessage.value = "Generated ${questions.size} practice questions for '${request.chapterName}'."
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to generate practice questions"
            }
        }
    }

    fun refreshPracticeSuggestions() {
        val projId = _selectedProjectId.value ?: return
        val vId = _selectedSyllabusVersionId.value ?: return
        val user = currentUser.value

        viewModelScope.launch {
            val suggestions = aiPracticeRepo.computeEvidenceBasedSuggestions(projId, vId, user)
            _practiceSuggestions.value = suggestions
        }
    }

    fun recordPracticeAttempt(questionId: String, setId: String, selectedOption: String, isCorrect: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            aiPracticeRepo.recordAttempt(user.id, questionId, setId, selectedOption, isCorrect)
        }
    }

    fun toggleSavePracticeSet(setId: String, isSaved: Boolean) {
        viewModelScope.launch {
            aiPracticeRepo.toggleSaveSet(setId, isSaved)
            if (_activePracticeSet.value?.id == setId) {
                _activePracticeSet.value = _activePracticeSet.value?.copy(isSaved = isSaved)
            }
            _notificationMessage.value = if (isSaved) "Practice set saved to your library." else "Practice set removed from saved."
        }
    }

    fun selectPracticeSet(setId: String) {
        viewModelScope.launch {
            val set = db.practiceQuestionDao().getSetById(setId)
            if (set != null) {
                val questions = db.practiceQuestionDao().getQuestionsListForSet(setId)
                _activePracticeSet.value = set
                _activePracticeQuestions.value = questions
            }
        }
    }

    fun generateAiExamEstimates() {
        val projId = _selectedProjectId.value ?: return
        val vId = _selectedSyllabusVersionId.value ?: return
        val user = currentUser.value

        viewModelScope.launch {
            _isEstimatingExam.value = true
            val res = aiPracticeRepo.generateAiExamEstimates(projId, vId, user)
            _isEstimatingExam.value = false
            res.onSuccess {
                _notificationMessage.value = "AI Exam Estimates generated based on verified historical papers."
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Could not generate AI exam estimates"
            }
        }
    }

    fun reviewPracticeQuestion(
        questionId: String,
        newStatus: AiReviewStatus,
        correctedText: String?,
        notes: String?
    ) {
        val user = currentUser.value
        if (user == null || !user.getRoleEnum().canApproveQuestions()) {
            _errorMessage.value = "Unauthorized: Only Teachers and Administrators can verify practice questions."
            return
        }

        viewModelScope.launch {
            val res = aiPracticeRepo.reviewQuestion(
                reviewer = user,
                questionId = questionId,
                newStatus = newStatus,
                userCorrectedText = correctedText,
                notes = notes
            )
            res.onSuccess {
                _notificationMessage.value = "Practice question marked as '${newStatus.label}'."
                _activePracticeSet.value?.let { set ->
                    _activePracticeQuestions.value = db.practiceQuestionDao().getQuestionsListForSet(set.id)
                }
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to update review status"
            }
        }
    }

    fun exportPracticeSet(setId: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val text = aiPracticeRepo.exportPracticeSet(setId)
            onComplete(text)
        }
    }
}
