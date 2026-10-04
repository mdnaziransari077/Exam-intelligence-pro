package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.ExamBottomNavBar
import com.example.ui.components.ExamNavigationRail
import com.example.ui.components.ExamTopAppBar
import com.example.ui.components.NotificationBanner
import com.example.ui.screens.AIAssistantScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AiEstimatesScreen
import com.example.ui.screens.AiPracticeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.HistoricalAnalysisScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReviewWorkspaceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.SyllabusManagerScreen
import com.example.ui.theme.ExamIntelligenceTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExamIntelligenceTheme {
                val vm: MainViewModel = viewModel()
                MainAppContent(viewModel = vm)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val currentRoute by viewModel.currentRoute.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredProjects by viewModel.filteredProjects.collectAsState()
    val userDocuments by viewModel.userDocuments.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedProjectDocs by viewModel.selectedProjectDocuments.collectAsState()
    val searchQuery by viewModel.projectSearchQuery.collectAsState()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsState()

    val adminStats by viewModel.adminStats.collectAsState()
    val adminUsers by viewModel.adminUsersList.collectAsState()
    val adminLogs by viewModel.adminAuditLogs.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatGenerating by viewModel.isChatGenerating.collectAsState()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsState()
    val isHighThinking by viewModel.isHighThinkingEnabled.collectAsState()

    // Phase 2 Document Review State
    val selectedDocumentForReview by viewModel.selectedDocumentForReview.collectAsState()
    val documentQuestions by viewModel.documentQuestions.collectAsState()
    val documentReviewLogs by viewModel.documentReviewLogs.collectAsState()
    val isProcessingDoc by viewModel.isProcessingDoc.collectAsState()
    val processingStage by viewModel.processingStage.collectAsState()

    // Phase 3 Syllabus & Concept Mapping State
    val allSyllabusVersions by viewModel.allSyllabusVersions.collectAsState()
    val selectedSyllabusVersion by viewModel.selectedSyllabusVersion.collectAsState()
    val syllabusNodes by viewModel.syllabusNodes.collectAsState()
    val projectMappings by viewModel.projectMappings.collectAsState()
    val selectedProjectQuestions by viewModel.selectedProjectQuestions.collectAsState()
    val topicCoverageList by viewModel.topicCoverageList.collectAsState()
    val isMappingProcessing by viewModel.isMappingProcessing.collectAsState()

    // Phase 4 Historical Analysis State
    val analysisScope by viewModel.analysisScope.collectAsState()
    val scopeSummary by viewModel.scopeSummary.collectAsState()
    val historicalAnalysisResult by viewModel.historicalAnalysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val selectedTopicDetail by viewModel.selectedTopicDetail.collectAsState()
    val topicAssociatedQuestions by viewModel.topicAssociatedQuestions.collectAsState()

    // Phase 5 AI Practice & Estimates State
    val activePracticeSet by viewModel.activePracticeSet.collectAsState()
    val activePracticeQuestions by viewModel.activePracticeQuestions.collectAsState()
    val isGeneratingPractice by viewModel.isGeneratingPractice.collectAsState()
    val practiceSuggestions by viewModel.practiceSuggestions.collectAsState()
    val practiceAttempts by viewModel.practiceAttempts.collectAsState()
    val isEstimatingExam by viewModel.isEstimatingExam.collectAsState()
    val savedPracticeSets by viewModel.savedPracticeSets.collectAsState()
    val latestAiExamEstimate by viewModel.latestAiExamEstimate.collectAsState()

    // Phase 6 Reports & Admin State
    val userReports by viewModel.userReports.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val adminProjects by viewModel.adminProjects.collectAsState()
    val adminDocuments by viewModel.adminDocuments.collectAsState()
    val adminExtractedQuestions by viewModel.adminExtractedQuestions.collectAsState()
    val adminMappings by viewModel.adminMappings.collectAsState()
    val adminPracticeQuestions by viewModel.adminPracticeQuestions.collectAsState()
    val adminAiEstimates by viewModel.adminAiEstimates.collectAsState()
    val adminReports by viewModel.adminReports.collectAsState()

    // Handle Back Press
    BackHandler(enabled = currentRoute != "landing" && currentRoute != "dashboard") {
        when (currentRoute) {
            "review_workspace" -> viewModel.navigateTo(if (selectedProject != null) "project_detail" else "documents")
            "syllabus_manager" -> viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard")
            "historical_analysis" -> viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard")
            "ai_practice", "ai_estimates" -> viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard")
            "reports" -> viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard")
            "project_detail" -> viewModel.navigateTo("projects")
            "admin" -> viewModel.navigateTo("dashboard")
            "signin", "signup" -> viewModel.navigateTo("landing")
            else -> viewModel.navigateTo("dashboard")
        }
    }

    val isAuthFlow = currentRoute == "landing" || currentRoute == "signin" || currentRoute == "signup"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                if (!isAuthFlow) {
                    ExamTopAppBar(
                        currentUser = currentUser,
                        isGuest = isGuest,
                        onLoginClick = { viewModel.navigateTo("signin") },
                        onLogoutClick = { viewModel.logout() }
                    )
                }
            },
            bottomBar = {
                if (!isAuthFlow && !isWideScreen) {
                    ExamBottomNavBar(
                        currentRoute = currentRoute,
                        currentUser = currentUser,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isAuthFlow && isWideScreen) {
                    ExamNavigationRail(
                        currentRoute = currentRoute,
                        currentUser = currentUser,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }

                Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                    // Feedback Banners
                    if (notificationMessage != null) {
                        NotificationBanner(
                            message = notificationMessage!!,
                            onDismiss = { viewModel.dismissNotification() }
                        )
                    }

                    if (errorMessage != null) {
                        ErrorBanner(
                            message = errorMessage!!,
                            onDismiss = { viewModel.dismissError() }
                        )
                    }

                    // Main Screen Router
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (currentRoute) {
                            "landing" -> LandingScreen(
                                onNavigateToSignIn = { viewModel.navigateTo("signin") },
                                onNavigateToSignUp = { viewModel.navigateTo("signup") },
                                onContinueAsGuest = { viewModel.continueAsGuest() },
                                onQuickLogin = { email, pass ->
                                    viewModel.login(email, pass) {}
                                }
                            )

                            "signin" -> SignInScreen(
                                onSignIn = { email, pass -> viewModel.login(email, pass) {} },
                                onNavigateToSignUp = { viewModel.navigateTo("signup") },
                                onNavigateBack = { viewModel.navigateTo("landing") },
                                onResetPassword = { email, newPass -> viewModel.resetPassword(email, newPass) },
                                isLoading = isLoading
                            )

                            "signup" -> SignUpScreen(
                                onSignUp = { email, pass, name, role, code, school, year ->
                                    viewModel.register(email, pass, name, role, code, school, year) {}
                                },
                                onNavigateToSignIn = { viewModel.navigateTo("signin") },
                                onNavigateBack = { viewModel.navigateTo("landing") },
                                isLoading = isLoading
                            )

                            "dashboard" -> DashboardScreen(
                                currentUser = currentUser,
                                isGuest = isGuest,
                                projects = filteredProjects,
                                documents = userDocuments,
                                onNavigateToProjects = { viewModel.navigateTo("projects") },
                                onNavigateToCreateProject = { viewModel.navigateTo("projects") },
                                onNavigateToDocuments = { viewModel.navigateTo("documents") },
                                onNavigateToAiAssistant = { viewModel.navigateTo("ai_assistant") },
                                onNavigateToSyllabus = { viewModel.navigateTo("syllabus_manager") },
                                onNavigateToAiPractice = {
                                    viewModel.refreshPracticeSuggestions()
                                    viewModel.navigateTo("ai_practice")
                                },
                                onNavigateToAiEstimates = { viewModel.navigateTo("ai_estimates") },
                                onNavigateToReports = { viewModel.navigateTo("reports") },
                                onSelectProject = { viewModel.selectProject(it) }
                            )

                            "projects" -> ProjectsScreen(
                                currentUser = currentUser,
                                projects = filteredProjects,
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.projectSearchQuery.value = it },
                                selectedSubject = selectedSubject,
                                onSubjectChange = { viewModel.selectedSubjectFilter.value = it },
                                onSelectProject = { viewModel.selectProject(it) },
                                onCreateProject = { name, subj, year, desc, vis ->
                                    viewModel.createProject(name, subj, year, desc, vis)
                                }
                            )

                            "project_detail" -> ProjectDetailScreen(
                                project = selectedProject,
                                documents = selectedProjectDocs,
                                currentUser = currentUser,
                                onNavigateBack = { viewModel.navigateTo("projects") },
                                onUploadDocument = { uri, name, cat, mime, size ->
                                    selectedProject?.let { p ->
                                        viewModel.uploadDocument(p.id, uri, name, cat, mime, size)
                                    }
                                },
                                onDeleteDocument = { viewModel.deleteDocument(it) },
                                onUpdateProject = { name, subj, yr, desc, vis, status ->
                                    selectedProject?.let { p ->
                                        viewModel.updateProject(p.id, name, subj, yr, desc, vis, status)
                                    }
                                },
                                onDeleteProject = { viewModel.deleteProject(it) },
                                onSeedSampleDocs = { viewModel.seedSampleDocsForCurrentProject() },
                                onProcessDocument = { viewModel.startDocumentProcessing(it) },
                                onOpenReviewWorkspace = { viewModel.openReviewWorkspace(it) },
                                onOpenSyllabusManager = { viewModel.navigateTo("syllabus_manager") },
                                onOpenHistoricalAnalysis = { viewModel.navigateTo("historical_analysis") },
                                onOpenAiPractice = {
                                    viewModel.refreshPracticeSuggestions()
                                    viewModel.navigateTo("ai_practice")
                                },
                                onOpenAiEstimates = {
                                    viewModel.navigateTo("ai_estimates")
                                },
                                onOpenReports = {
                                    viewModel.navigateTo("reports")
                                }
                            )

                            "documents" -> DocumentsScreen(
                                documents = userDocuments,
                                projects = filteredProjects,
                                currentUser = currentUser,
                                onDeleteDocument = { viewModel.deleteDocument(it) },
                                onProcessDocument = { viewModel.startDocumentProcessing(it) },
                                onOpenReviewWorkspace = { viewModel.openReviewWorkspace(it) }
                            )

                            "review_workspace" -> ReviewWorkspaceScreen(
                                document = selectedDocumentForReview,
                                questions = documentQuestions,
                                reviewLogs = documentReviewLogs,
                                currentUser = currentUser,
                                isProcessing = isProcessingDoc,
                                processingStageMessage = processingStage,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "documents") },
                                onApproveQuestion = { viewModel.approveQuestion(it) },
                                onRejectQuestion = { id, reason -> viewModel.rejectQuestion(id, reason) },
                                onEditQuestion = { id, num, sec, text, type, marks, topic, opts, choice ->
                                    viewModel.editQuestion(id, num, sec, text, type, marks, topic, opts, choice)
                                },
                                onBulkApproveHighConfidence = { viewModel.bulkApproveHighConfidence(it) },
                                onAddManualQuestion = { docId, projId, num, sec, text, type, marks, topic, opts, choice ->
                                    viewModel.addManualQuestion(docId, projId, num, sec, text, type, marks, topic, opts, choice)
                                },
                                onDeleteQuestion = { viewModel.deleteQuestion(it) },
                                onReprocessDocument = { viewModel.startDocumentProcessing(it) },
                                onExportQuestions = { docId, format, callback ->
                                    viewModel.exportDocumentQuestions(docId, format, callback)
                                }
                            )

                            "syllabus_manager" -> SyllabusManagerScreen(
                                project = selectedProject,
                                allSyllabusVersions = allSyllabusVersions,
                                selectedVersion = selectedSyllabusVersion,
                                syllabusNodes = syllabusNodes,
                                mappings = projectMappings,
                                projectQuestions = selectedProjectQuestions,
                                coverageStats = topicCoverageList,
                                currentUser = currentUser,
                                isAutoMappingActive = isMappingProcessing,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard") },
                                onSelectVersion = { viewModel.selectSyllabusVersion(it) },
                                onImportSyllabus = { subj, sess, verId, type, title, url, docRef, notes, raw ->
                                    viewModel.createOrImportSyllabus(subj, sess, verId, type, title, url, docRef, notes, raw)
                                },
                                onVerifySyllabus = { vId, status, notes ->
                                    viewModel.verifySyllabusVersion(vId, status, notes)
                                },
                                onAddNode = { parentId, type, code, name, desc, isExcluded, reason ->
                                    viewModel.addSyllabusNode(parentId, type, code, name, desc, isExcluded, reason)
                                },
                                onUpdateNode = { id, name, code, desc, isExcluded, reason ->
                                    viewModel.updateSyllabusNode(id, name, code, desc, isExcluded, reason)
                                },
                                onDeleteNode = { viewModel.deleteSyllabusNode(it) },
                                onMergeNodes = { src, tgt -> viewModel.mergeSyllabusNodes(src, tgt) },
                                onAutoMapQuestions = { viewModel.autoMapProjectQuestions() },
                                onApproveMapping = { viewModel.approveMapping(it) },
                                onRejectMapping = { id, reason -> viewModel.rejectMapping(id, reason) },
                                onModifyMapping = { id, chId, chName, topId, topName, notes ->
                                    viewModel.modifyMapping(id, chId, chName, topId, topName, notes)
                                },
                                onMarkOutsideSyllabus = { id, reason -> viewModel.markMappingOutsideSyllabus(id, reason) },
                                onMarkUncertain = { id, reason -> viewModel.markMappingUncertain(id, reason) },
                                onAddMultiTopicMapping = { qId, docId, pId, chId, chName, topId, topName, notes ->
                                    viewModel.addMultiTopicMapping(qId, docId, pId, chId, chName, topId, topName, notes)
                                },
                                onDeleteMapping = { viewModel.deleteMapping(it) }
                            )

                            "historical_analysis" -> HistoricalAnalysisScreen(
                                project = selectedProject,
                                allSyllabusVersions = allSyllabusVersions,
                                selectedVersion = selectedSyllabusVersion,
                                analysisScope = analysisScope,
                                scopeSummary = scopeSummary,
                                analysisResult = historicalAnalysisResult,
                                isAnalyzing = isAnalyzing,
                                selectedTopicDetail = selectedTopicDetail,
                                topicAssociatedQuestions = topicAssociatedQuestions,
                                currentUser = currentUser,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard") },
                                onSelectSyllabusVersion = { viewModel.selectSyllabusVersion(it) },
                                onUpdateQuestionScope = { viewModel.updateScopeQuestionVerification(it) },
                                onUpdateMappingScope = { viewModel.updateScopeMappingVerification(it) },
                                onUpdateAttributionMethod = { viewModel.updateScopeAttributionMethod(it) },
                                onExecuteAnalysis = { viewModel.executeHistoricalAnalysis() },
                                onOpenTopicDetail = { viewModel.openTopicDetail(it) },
                                onCloseTopicDetail = { viewModel.closeTopicDetail() },
                                onExportReport = { format, callback -> viewModel.exportHistoricalAnalysis(format, callback) },
                                onPracticeTopic = { topicId, chapterName ->
                                    viewModel.refreshPracticeSuggestions()
                                    viewModel.navigateTo("ai_practice")
                                },
                                onNavigateToPractice = {
                                    viewModel.refreshPracticeSuggestions()
                                    viewModel.navigateTo("ai_practice")
                                }
                            )

                            "ai_practice" -> AiPracticeScreen(
                                project = selectedProject,
                                allSyllabusVersions = allSyllabusVersions,
                                selectedVersion = selectedSyllabusVersion,
                                syllabusNodes = syllabusNodes,
                                activeSet = activePracticeSet,
                                activeQuestions = activePracticeQuestions,
                                savedSets = savedPracticeSets,
                                suggestions = practiceSuggestions,
                                attempts = practiceAttempts,
                                isGenerating = isGeneratingPractice,
                                currentUser = currentUser,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard") },
                                onNavigateToEstimates = { viewModel.navigateTo("ai_estimates") },
                                onGenerateQuestions = { req -> viewModel.generatePracticeQuestions(req) },
                                onSelectSet = { setId -> viewModel.selectPracticeSet(setId) },
                                onToggleSaveSet = { setId, saved -> viewModel.toggleSavePracticeSet(setId, saved) },
                                onRecordAttempt = { qId, sId, opt, correct -> viewModel.recordPracticeAttempt(qId, sId, opt, correct) },
                                onReviewQuestion = { qId, status, corr, notes -> viewModel.reviewPracticeQuestion(qId, status, corr, notes) },
                                onExportSet = { setId, callback -> viewModel.exportPracticeSet(setId, callback) }
                            )

                            "ai_estimates" -> AiEstimatesScreen(
                                project = selectedProject,
                                latestEstimate = latestAiExamEstimate,
                                isGenerating = isEstimatingExam,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard") },
                                onGenerateEstimates = { viewModel.generateAiExamEstimates() },
                                onPracticeTopic = { topicId, chapterName ->
                                    viewModel.refreshPracticeSuggestions()
                                    viewModel.navigateTo("ai_practice")
                                }
                            )

                            "ai_assistant" -> AIAssistantScreen(
                                messages = chatMessages,
                                isGenerating = isChatGenerating,
                                selectedModel = selectedGeminiModel,
                                onModelChange = { viewModel.selectedGeminiModel.value = it },
                                isHighThinking = isHighThinking,
                                onHighThinkingToggle = { viewModel.isHighThinkingEnabled.value = it },
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onClearChat = { viewModel.clearChat() }
                            )

                            "reports" -> ReportsScreen(
                                currentUser = currentUser,
                                isGuest = isGuest,
                                projects = filteredProjects,
                                allSyllabusVersions = allSyllabusVersions,
                                reports = userReports,
                                isGenerating = isGeneratingReport,
                                onNavigateBack = { viewModel.navigateTo(if (selectedProject != null) "project_detail" else "dashboard") },
                                onGenerateReport = { viewModel.generateReport(it) },
                                onDeleteReport = { viewModel.deleteReport(it) },
                                onToggleReportPublic = { id, pub -> viewModel.toggleReportPublicVisibility(id, pub) }
                            )

                            "admin" -> AdminScreen(
                                currentUser = currentUser,
                                stats = adminStats,
                                users = adminUsers,
                                projects = adminProjects,
                                documents = adminDocuments,
                                questions = adminExtractedQuestions,
                                syllabusVersions = allSyllabusVersions,
                                mappings = adminMappings,
                                practiceQuestions = adminPracticeQuestions,
                                estimates = adminAiEstimates,
                                reports = adminReports,
                                auditLogs = adminLogs,
                                onToggleUserStatus = { viewModel.toggleUserStatus(it) },
                                onUpdateUserRole = { id, role -> viewModel.updateUserRole(id, role) },
                                onChangeProjectVisibility = { id, vis -> viewModel.changeProjectVisibility(id, vis) },
                                onDeleteProject = { viewModel.adminDeleteProject(it) },
                                onDeleteDocument = { viewModel.adminDeleteDocument(it) },
                                onApproveQuestion = { viewModel.adminApproveQuestion(it) },
                                onDeleteReport = { viewModel.adminDeleteReport(it) },
                                onRefreshStats = { viewModel.refreshAdminStats() },
                                onNavigateToSignIn = { viewModel.navigateTo("signin") }
                            )

                            "settings" -> SettingsScreen(
                                currentUser = currentUser,
                                isGuest = isGuest,
                                onNavigateToSignIn = { viewModel.navigateTo("signin") },
                                onLogout = { viewModel.logout() }
                            )
                        }

                        // Central Loading Overlay
                        if (isLoading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
