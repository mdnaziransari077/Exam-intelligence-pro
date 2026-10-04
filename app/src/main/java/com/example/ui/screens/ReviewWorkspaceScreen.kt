package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.QuestionReviewLogEntity
import com.example.data.model.QuestionType
import com.example.data.model.UserEntity
import com.example.data.model.VerificationStatus
import com.example.data.repository.ExportFormat
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewWorkspaceScreen(
    document: DocumentEntity?,
    questions: List<ExtractedQuestionEntity>,
    reviewLogs: List<QuestionReviewLogEntity>,
    currentUser: UserEntity?,
    isProcessing: Boolean,
    processingStageMessage: String?,
    onNavigateBack: () -> Unit,
    onApproveQuestion: (String) -> Unit,
    onRejectQuestion: (String, String) -> Unit,
    onEditQuestion: (String, String, String, String, QuestionType, Int?, String?, List<String>?, String?) -> Unit,
    onBulkApproveHighConfidence: (String) -> Unit,
    onAddManualQuestion: (String, String, String, String, String, QuestionType, Int?, String?, List<String>?, String?) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onReprocessDocument: (String) -> Unit,
    onExportQuestions: (String, ExportFormat, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    if (document == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Document not found or removed.", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onNavigateBack) { Text("Back") }
            }
        }
        return
    }

    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Questions, 1: Document Transcript & OCR
    var filterStatus by remember { mutableStateOf("All") } // "All", "Awaiting", "Approved", "Rejected", "Warnings"
    var searchQuery by remember { mutableStateOf("") }

    var questionToEdit by remember { mutableStateOf<ExtractedQuestionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    // Metrics
    val totalCount = questions.size
    val approvedCount = questions.count { it.verificationStatus == VerificationStatus.APPROVED.label }
    val rejectedCount = questions.count { it.verificationStatus == VerificationStatus.REJECTED.label }
    val awaitingCount = questions.count { it.verificationStatus == VerificationStatus.UNVERIFIED.label }
    val warningsCount = questions.count { !it.reviewWarnings.isNullOrBlank() || it.isMarksUnknown() }
    val highConfidenceUnverified = questions.count { it.isHighConfidence() && it.verificationStatus == VerificationStatus.UNVERIFIED.label }

    val progressFraction = if (totalCount > 0) (approvedCount + rejectedCount).toFloat() / totalCount else 0f

    val filteredQuestions = questions.filter { q ->
        val matchesSearch = searchQuery.isBlank() ||
                q.questionNumber.contains(searchQuery, ignoreCase = true) ||
                q.getEffectiveText().contains(searchQuery, ignoreCase = true) ||
                (q.topic?.contains(searchQuery, ignoreCase = true) == true) ||
                q.section.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterStatus) {
            "Awaiting" -> q.verificationStatus == VerificationStatus.UNVERIFIED.label
            "Approved" -> q.verificationStatus == VerificationStatus.APPROVED.label
            "Rejected" -> q.verificationStatus == VerificationStatus.REJECTED.label
            "Warnings" -> !q.reviewWarnings.isNullOrBlank() || q.isMarksUnknown()
            else -> true
        }
        matchesSearch && matchesFilter
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 860.dp

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("review_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = document.originalFilename,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = document.processingStatus)
                            }
                            Text(
                                text = "${document.documentCategory} • Extraction: ${document.extractionMethod}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toolbar actions
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (highConfidenceUnverified > 0) {
                                Button(
                                    onClick = { onBulkApproveHighConfidence(document.id) },
                                    modifier = Modifier.testTag("bulk_approve_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify High-Conf ($highConfidenceUnverified)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("add_manual_question_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Question", fontSize = 12.sp)
                            }

                            IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("export_questions_button")) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export Questions", tint = PrimaryBlue)
                            }

                            IconButton(onClick = { showLogsDialog = true }, modifier = Modifier.testTag("view_review_logs_button")) {
                                Icon(Icons.Default.History, contentDescription = "Review Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(
                                onClick = { onReprocessDocument(document.id) },
                                modifier = Modifier.testTag("reprocess_doc_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Re-run Extraction", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Review Progress Bar & Counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Verification Progress: ${(progressFraction * 100).toInt()}% reviewed ($approvedCount approved, $rejectedCount rejected of $totalCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (awaitingCount == 0 && totalCount > 0) "All Verified" else "$awaitingCount awaiting",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (awaitingCount == 0 && totalCount > 0) Color(0xFF10B981) else AcademicGold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Compact Tab Row (Mobile only)
            if (!isWideScreen) {
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Questions ($totalCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Document Transcript & OCR", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            // Body Area
            if (isWideScreen) {
                // Wide Screen: Split Side-by-Side
                Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Left Pane: Document Transcript & Original Text (42%)
                    Card(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        TranscriptViewerPane(
                            document = document,
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Pane: Extracted Question Cards (58%)
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        QuestionListControls(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            filterStatus = filterStatus,
                            onFilterChange = { filterStatus = it },
                            totalCount = totalCount,
                            awaitingCount = awaitingCount,
                            approvedCount = approvedCount,
                            rejectedCount = rejectedCount,
                            warningsCount = warningsCount
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuestionCardsList(
                            questions = filteredQuestions,
                            onApprove = onApproveQuestion,
                            onReject = onRejectQuestion,
                            onEdit = { questionToEdit = it },
                            onDelete = onDeleteQuestion
                        )
                    }
                }
            } else {
                // Compact Screen: Single active tab
                if (activeTab == 0) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        QuestionListControls(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            filterStatus = filterStatus,
                            onFilterChange = { filterStatus = it },
                            totalCount = totalCount,
                            awaitingCount = awaitingCount,
                            approvedCount = approvedCount,
                            rejectedCount = rejectedCount,
                            warningsCount = warningsCount
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuestionCardsList(
                            questions = filteredQuestions,
                            onApprove = onApproveQuestion,
                            onReject = onRejectQuestion,
                            onEdit = { questionToEdit = it },
                            onDelete = onDeleteQuestion
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        TranscriptViewerPane(
                            document = document,
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (questionToEdit != null) {
        EditQuestionDialog(
            question = questionToEdit!!,
            onDismiss = { questionToEdit = null },
            onConfirm = { number, section, text, type, marks, topic, options, choice ->
                onEditQuestion(
                    questionToEdit!!.id,
                    number,
                    section,
                    text,
                    type,
                    marks,
                    topic,
                    options,
                    choice
                )
                questionToEdit = null
            }
        )
    }

    if (showAddDialog) {
        AddQuestionDialog(
            documentId = document.id,
            projectId = document.projectId,
            suggestedNextNumber = (questions.size + 1).toString(),
            onDismiss = { showAddDialog = false },
            onConfirm = { number, section, text, type, marks, topic, options, choice ->
                onAddManualQuestion(
                    document.id,
                    document.projectId,
                    number,
                    section,
                    text,
                    type,
                    marks,
                    topic,
                    options,
                    choice
                )
                showAddDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportQuestionsDialog(
            documentId = document.id,
            onDismiss = { showExportDialog = false },
            onExport = { format, callback ->
                onExportQuestions(document.id, format, callback)
            }
        )
    }

    if (showLogsDialog) {
        ReviewLogsDialog(
            logs = reviewLogs,
            onDismiss = { showLogsDialog = false }
        )
    }

    // Processing Overlay
    if (isProcessing) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.padding(32.dp).width(340.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Phase 2 Pipeline Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = processingStageMessage ?: "Processing document...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Extracting text, OCR, and structuring questions for CBSE Class 10.",
                            fontSize = 11.sp,
                            color = IntelligenceCyan,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionListControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterChange: (String) -> Unit,
    totalCount: Int,
    awaitingCount: Int,
    approvedCount: Int,
    rejectedCount: Int,
    warningsCount: Int
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().testTag("questions_search_input"),
            placeholder = { Text("Search question text, topic, or number...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterStatus == "All",
                onClick = { onFilterChange("All") },
                label = { Text("All ($totalCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterStatus == "Awaiting",
                onClick = { onFilterChange("Awaiting") },
                label = { Text("Awaiting Review ($awaitingCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterStatus == "Approved",
                onClick = { onFilterChange("Approved") },
                label = { Text("Approved ($approvedCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterStatus == "Rejected",
                onClick = { onFilterChange("Rejected") },
                label = { Text("Rejected ($rejectedCount)", fontSize = 11.sp) }
            )
            if (warningsCount > 0) {
                FilterChip(
                    selected = filterStatus == "Warnings",
                    onClick = { onFilterChange("Warnings") },
                    label = { Text("⚠️ Needs Attention ($warningsCount)", fontSize = 11.sp, color = AcademicGold) }
                )
            }
        }
    }
}

@Composable
fun QuestionCardsList(
    questions: List<ExtractedQuestionEntity>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onEdit: (ExtractedQuestionEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    if (questions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("No matching questions found", fontWeight = FontWeight.Bold)
                Text("Try adjusting the search query or status filter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(questions, key = { it.id }) { question ->
                QuestionVerificationCard(
                    question = question,
                    onApprove = { onApprove(question.id) },
                    onReject = { onReject(question.id, "Rejected during verification") },
                    onEdit = { onEdit(question) },
                    onDelete = { onDelete(question.id) }
                )
            }
        }
    }
}

@Composable
fun QuestionVerificationCard(
    question: ExtractedQuestionEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isApproved = question.verificationStatus == VerificationStatus.APPROVED.label
    val isRejected = question.verificationStatus == VerificationStatus.REJECTED.label
    val isUnverified = question.verificationStatus == VerificationStatus.UNVERIFIED.label

    val borderColor = when {
        isApproved -> Color(0xFF10B981)
        isRejected -> Color(0xFFEF4444)
        !question.reviewWarnings.isNullOrBlank() || question.isMarksUnknown() -> AcademicGold
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val options = question.getOptionsList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
            .testTag("question_card_${question.questionNumber}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Header Row: Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Question Number Pill
                    Surface(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Q${question.questionNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Section Tag
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = question.section,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Marks Pill
                    if (question.marks != null && question.marks > 0) {
                        Surface(
                            color = IntelligenceCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${question.marks} ${if (question.marks == 1) "Mark" else "Marks"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntelligenceCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⚠️ Unspecified Marks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Status Badge & Confidence
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val confPercent = (question.confidence * 100).toInt()
                    Text(
                        text = "$confPercent% conf",
                        fontSize = 10.sp,
                        color = if (confPercent >= 85) Color(0xFF10B981) else AcademicGold,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        color = when {
                            isApproved -> Color(0xFF10B981).copy(alpha = 0.15f)
                            isRejected -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            else -> AcademicGold.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = question.verificationStatus,
                            color = when {
                                isApproved -> Color(0xFF10B981)
                                isRejected -> Color(0xFFEF4444)
                                else -> AcademicGold
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-header: Question Type & Topic
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question.questionType,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                if (!question.topic.isNullOrBlank()) {
                    Text(text = " • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Topic: ${question.topic}",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(text = " • Page ${question.sourcePage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Warning Banner if present
            if (!question.reviewWarnings.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = question.reviewWarnings, fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question Text
            SelectionContainer {
                Text(
                    text = question.getEffectiveText(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!question.subQuestionText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = question.subQuestionText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // MCQ Options
            if (options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { opt ->
                        Text(
                            text = opt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Alternative Internal Choice
            if (!question.internalChoice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = question.internalChoice,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryBlue,
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }

            if (question.userCorrectedText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Edited & corrected by reviewer (${question.reviewedBy ?: "Human"})",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Approve Button
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.testTag("approve_btn_${question.questionNumber}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApproved) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.85f)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isApproved) "Verified" else "Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Reject Button
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.testTag("reject_btn_${question.questionNumber}"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isRejected) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRejected) "Rejected" else "Reject", fontSize = 11.sp)
                    }

                    // Edit Button
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_btn_${question.questionNumber}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp)
                    }
                }

                // Delete Question
                IconButton(onClick = onDelete, modifier = Modifier.testTag("del_btn_${question.questionNumber}")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TranscriptViewerPane(
    document: DocumentEntity,
    modifier: Modifier = Modifier
) {
    val transcript = document.extractedText ?: "No transcript extracted yet. Run the extraction pipeline to see text here."
    var transcriptSearch by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Document Transcript & OCR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${document.originalFilename} (${document.formattedFileSize()})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = PrimaryBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Raw OCR Layer",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = transcriptSearch,
            onValueChange = { transcriptSearch = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search text within transcript...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = transcript,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuestionDialog(
    question: ExtractedQuestionEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, QuestionType, Int?, String?, List<String>?, String?) -> Unit
) {
    var number by remember { mutableStateOf(question.questionNumber) }
    var section by remember { mutableStateOf(question.section) }
    var text by remember { mutableStateOf(question.getEffectiveText()) }
    var marksText by remember { mutableStateOf(question.marks?.toString() ?: "") }
    var topic by remember { mutableStateOf(question.topic ?: "") }
    var choice by remember { mutableStateOf(question.internalChoice ?: "") }

    var selectedType by remember {
        mutableStateOf(QuestionType.fromLabel(question.questionType))
    }
    var typeExpanded by remember { mutableStateOf(false) }

    // Options for MCQs
    var rawOptionsText by remember {
        mutableStateOf(question.getOptionsList().joinToString("\n"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Verify Question Q${question.questionNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Q No.") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("Marks") },
                        placeholder = { Text("1-5") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Question Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        QuestionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("CBSE Topic / Chapter Tag") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Question Text / Stem") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == QuestionType.MCQ || selectedType == QuestionType.ASSERTION_REASON) {
                    OutlinedTextField(
                        value = rawOptionsText,
                        onValueChange = { rawOptionsText = it },
                        label = { Text("Options (one per line)") },
                        placeholder = { Text("(A) Option 1\n(B) Option 2\n(C) Option 3\n(D) Option 4") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = choice,
                    onValueChange = { choice = it },
                    label = { Text("Internal Alternative Choice (optional)") },
                    placeholder = { Text("[OR] State another theorem...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedMarks = marksText.toIntOrNull()
                    val optionsList = if (rawOptionsText.isNotBlank()) {
                        rawOptionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    } else null

                    onConfirm(
                        number,
                        section,
                        text,
                        selectedType,
                        parsedMarks,
                        topic.ifBlank { null },
                        optionsList,
                        choice.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Save & Approve", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionDialog(
    documentId: String,
    projectId: String,
    suggestedNextNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, QuestionType, Int?, String?, List<String>?, String?) -> Unit
) {
    var number by remember { mutableStateOf(suggestedNextNumber) }
    var section by remember { mutableStateOf("Section A") }
    var text by remember { mutableStateOf("") }
    var marksText by remember { mutableStateOf("1") }
    var topic by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf("") }

    var selectedType by remember { mutableStateOf(QuestionType.MCQ) }
    var typeExpanded by remember { mutableStateOf(false) }
    var rawOptionsText by remember { mutableStateOf("(A) \n(B) \n(C) \n(D) ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manually Add Question to Paper", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Q No.") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("Marks") },
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Question Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        QuestionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("CBSE Topic Tag") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Question Text") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == QuestionType.MCQ || selectedType == QuestionType.ASSERTION_REASON) {
                    OutlinedTextField(
                        value = rawOptionsText,
                        onValueChange = { rawOptionsText = it },
                        label = { Text("Options (one per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = choice,
                    onValueChange = { choice = it },
                    label = { Text("Alternative Choice (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        val parsedMarks = marksText.toIntOrNull() ?: 1
                        val optionsList = if (rawOptionsText.isNotBlank()) {
                            rawOptionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        } else null

                        onConfirm(
                            number,
                            section,
                            text,
                            selectedType,
                            parsedMarks,
                            topic.ifBlank { null },
                            optionsList,
                            choice.ifBlank { null }
                        )
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Add to Paper")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportQuestionsDialog(
    documentId: String,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var previewContent by remember { mutableStateOf("Generating export...") }
    var isReady by remember { mutableStateOf(false) }

    fun refreshContent(format: ExportFormat) {
        selectedFormat = format
        previewContent = "Generating ${format.label}..."
        isReady = false
        onExport(format) { res ->
            previewContent = res
            isReady = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshContent(ExportFormat.MARKDOWN)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Extracted Questions", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select target format:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { refreshContent(fmt) },
                            label = { Text(fmt.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = previewContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Exam Intelligence Export", previewContent)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Export copied to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = isReady
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy to Clipboard")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ReviewLogsDialog(
    logs: List<QuestionReviewLogEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd-MMM", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Human Verification Audit Trail", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No review actions recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = when (log.action) {
                                            "APPROVED", "BULK_APPROVED" -> Color(0xFF10B981)
                                            "REJECTED" -> Color(0xFFEF4444)
                                            "EDITED" -> PrimaryBlue
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "Reviewer: ${log.reviewerEmail}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
