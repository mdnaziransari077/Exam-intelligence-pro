package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ACADEMIC_YEARS
import com.example.data.model.CBSE_CLASS_10_SUBJECTS
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.ProcessingStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectVisibility
import com.example.data.model.UserEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    project: ProjectEntity?,
    documents: List<DocumentEntity>,
    currentUser: UserEntity?,
    onNavigateBack: () -> Unit,
    onUploadDocument: (Uri?, String, DocumentCategory, String, Long) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onUpdateProject: (String, String, String, String, ProjectVisibility, ProjectStatus) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSeedSampleDocs: () -> Unit,
    onProcessDocument: (String) -> Unit = {},
    onOpenReviewWorkspace: (String) -> Unit = {},
    onOpenSyllabusManager: () -> Unit = {},
    onOpenHistoricalAnalysis: () -> Unit = {},
    onOpenAiPractice: () -> Unit = {},
    onOpenAiEstimates: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (project == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Project not found", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateBack) { Text("Back to Projects") }
            }
        }
        return
    }

    var showUploadDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteProjectDialog by remember { mutableStateOf(false) }
    var docToDeleteId by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top App Bar row with Back button and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("project_detail_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.testTag("project_detail_edit_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = PrimaryBlue)
                }

                IconButton(
                    onClick = { showDeleteProjectDialog = true },
                    modifier = Modifier.testTag("project_detail_delete_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Project Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created by ${project.ownerName} • ${dateFormat.format(Date(project.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(status = project.status)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${project.board} • ${project.className}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = project.subject,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = project.academicYear,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (project.isPrivate()) Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (project.isPrivate()) "Private" else "Public",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (project.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Phase 3 Syllabus Intelligence & Concept Coverage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syllabus Intelligence & Topic Coverage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                    }
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Phase 3 Active",
                            color = Color(0xFF065F46),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Map questions to the official CBSE Class 10 ${project.subject} syllabus hierarchy, verify concept alignments, inspect mark weightage, and detect untested curriculum gaps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenSyllabusManager,
                    modifier = Modifier.fillMaxWidth().testTag("open_syllabus_manager_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Syllabus & Coverage Matrix", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phase 4 Historical Question Analytics & Paper Patterns Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = IntelligenceCyan.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, IntelligenceCyan.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = IntelligenceCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Historical Analysis & Paper Patterns", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0E7490))
                    }
                    Surface(
                        color = IntelligenceCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Phase 4 Active",
                            color = Color(0xFF0E7490),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Evaluate topic appearance frequencies, mark distributions across chapters, question format breakdowns (MCQ, Assertion-Reason, Short & Long Answer), and year-by-year patterns across eligible uploaded papers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenHistoricalAnalysis,
                    modifier = Modifier.fillMaxWidth().testTag("open_historical_analysis_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2))
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze Historical Paper Patterns", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phase 5 AI Practice & Estimates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AcademicGold.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AcademicGold.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AcademicGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Practice & Exam Estimates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                    }
                    Surface(
                        color = AcademicGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Phase 5 Active",
                            color = Color(0xFF92400E),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generate original CBSE syllabus-aligned practice questions (MCQ, Assertion-Reason, Short & Long Answer), test topic understanding, and explore evidence-based practice suggestions and separate AI Exam Estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenAiPractice,
                        modifier = Modifier.weight(1f).testTag("open_ai_practice_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Practice Questions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenAiEstimates,
                        modifier = Modifier.weight(1f).testTag("open_ai_estimates_button")
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IntelligenceCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Exam Estimates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenReports,
                    modifier = Modifier.fillMaxWidth().testTag("open_project_reports_button")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Executive Reports & PDF Exports", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Document Management Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Staged Documents (${documents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PDF, Word (DOCX), or scanned paper photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showUploadDialog = true },
                modifier = Modifier.testTag("project_stage_doc_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Document", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (documents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = IntelligenceCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No documents staged yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Upload CBSE board question papers, sample papers, or marking schemes for this project.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showUploadDialog = true },
                            modifier = Modifier.testTag("empty_doc_upload_button")
                        ) {
                            Text("Upload Document")
                        }
                        OutlinedButton(
                            onClick = onSeedSampleDocs,
                            modifier = Modifier.testTag("empty_seed_sample_docs_button")
                        ) {
                            Text("Stage Sample Papers")
                        }
                    }
                }
            }
        } else {
            documents.forEach { doc ->
                DocumentDetailCard(
                    doc = doc,
                    onProcess = { onProcessDocument(doc.id) },
                    onReview = { onOpenReviewWorkspace(doc.id) },
                    onDelete = { docToDeleteId = doc.id }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialogs
    if (showUploadDialog) {
        DocumentUploadDialog(
            projectName = project.name,
            onDismiss = { showUploadDialog = false },
            onConfirm = { uri, filename, category, mime, size ->
                onUploadDocument(uri, filename, category, mime, size)
                showUploadDialog = false
            }
        )
    }

    if (showEditDialog) {
        EditProjectDialog(
            project = project,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, subject, year, desc, vis, status ->
                onUpdateProject(name, subject, year, desc, vis, status)
                showEditDialog = false
            }
        )
    }

    if (showDeleteProjectDialog) {
        ConfirmDeleteDialog(
            title = "Delete Project?",
            message = "Are you sure you want to delete '${project.name}' and all ${documents.size} staged documents? This action cannot be undone.",
            onConfirm = {
                showDeleteProjectDialog = false
                onDeleteProject(project.id)
            },
            onDismiss = { showDeleteProjectDialog = false }
        )
    }

    if (docToDeleteId != null) {
        ConfirmDeleteDialog(
            title = "Remove Document?",
            message = "Are you sure you want to remove this document from the project?",
            onConfirm = {
                val id = docToDeleteId
                docToDeleteId = null
                if (id != null) onDeleteDocument(id)
            },
            onDismiss = { docToDeleteId = null }
        )
    }
}

@Composable
fun DocumentDetailCard(
    doc: DocumentEntity,
    onProcess: () -> Unit,
    onReview: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when {
        doc.mimeType.contains("pdf", ignoreCase = true) || doc.originalFilename.endsWith(".pdf", ignoreCase = true) ->
            Icons.Default.PictureAsPdf
        doc.mimeType.contains("image", ignoreCase = true) || doc.originalFilename.endsWith(".png", ignoreCase = true) || doc.originalFilename.endsWith(".jpg", ignoreCase = true) ->
            Icons.Default.Image
        else -> Icons.Default.Description
    }

    val iconColor = when {
        doc.mimeType.contains("pdf", ignoreCase = true) -> Color(0xFFEF4444)
        doc.mimeType.contains("image", ignoreCase = true) -> PrimaryBlue
        else -> IntelligenceCyan
    }

    val isProcessing = doc.processingStatus == ProcessingStatus.QUEUED.label ||
            doc.processingStatus == ProcessingStatus.EXTRACTING_TEXT.label ||
            doc.processingStatus == ProcessingStatus.DETECTING_QUESTIONS.label

    val isReadyForReview = doc.processingStatus == ProcessingStatus.AWAITING_REVIEW.label ||
            doc.processingStatus == ProcessingStatus.PARTIALLY_REVIEWED.label ||
            doc.processingStatus == ProcessingStatus.VERIFIED.label

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_card_${doc.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.originalFilename,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${doc.documentCategory} • ${doc.formattedFileSize()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = doc.processingStatus)
                    if (doc.questionCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${doc.verifiedCount}/${doc.questionCount} verified",
                            fontSize = 10.sp,
                            color = if (doc.verifiedCount == doc.questionCount) Color(0xFF10B981) else AcademicGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button depending on pipeline stage
            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OCR...", fontSize = 11.sp, color = PrimaryBlue)
                }
            } else if (isReadyForReview) {
                Button(
                    onClick = onReview,
                    modifier = Modifier.testTag("review_doc_btn_${doc.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify (${doc.questionCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onProcess,
                    modifier = Modifier.testTag("process_doc_btn_${doc.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Process", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_doc_button_${doc.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Document", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: (Uri?, String, DocumentCategory, String, Long) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var filename by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.PREVIOUS_YEAR_PAPER) }
    var mimeType by remember { mutableStateOf("application/pdf") }
    var fileSize by remember { mutableStateOf(2_500_000L) }

    var categoryExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        filename = it.getString(nameIndex) ?: "document.pdf"
                    }
                    if (sizeIndex != -1) {
                        fileSize = it.getLong(sizeIndex)
                    }
                }
            }
            val detectedMime = context.contentResolver.getType(uri) ?: "application/pdf"
            mimeType = detectedMime
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stage Document for $projectName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a CBSE question paper, official marking scheme, or notes file (PDF, DOCX, or Image up to 25MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // File Selector Box
                Surface(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("file_picker_trigger"),
                    color = PrimaryBlue.copy(alpha = 0.05f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filename.isBlank()) "Choose File from Storage" else filename,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = if (selectedUri != null) "Size: ${(fileSize / 1024)} KB" else "Supported: .pdf, .docx, .png, .jpg",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom filename override if needed
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Document Label / Filename") },
                    placeholder = { Text("e.g. CBSE_Math_2024_Set1.pdf") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upload_filename_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("upload_category_selector"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DocumentCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label, fontSize = 13.sp) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Phase 1 note: File will be securely saved with metadata marked as 'Uploaded — awaiting processing'.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (filename.isBlank()) "CBSE_Paper_${System.currentTimeMillis()}.pdf" else filename
                    onConfirm(selectedUri, finalName, selectedCategory, mimeType, fileSize)
                },
                enabled = filename.isNotBlank() || selectedUri != null,
                modifier = Modifier.testTag("upload_dialog_submit_button")
            ) {
                Text("Stage Document")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, ProjectVisibility, ProjectStatus) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var selectedSubject by remember { mutableStateOf(project.subject) }
    var selectedYear by remember { mutableStateOf(project.academicYear) }
    var description by remember { mutableStateOf(project.description) }
    var isPrivate by remember { mutableStateOf(project.isPrivate()) }
    var showPublicConfirmDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(ProjectStatus.fromLabel(project.status)) }

    var subjectExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Project Metadata", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_project_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subject
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        CBSE_CLASS_10_SUBJECTS.forEach { subj ->
                            DropdownMenuItem(
                                text = { Text(subj) },
                                onClick = {
                                    selectedSubject = subj
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        ProjectStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    selectedStatus = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Private Visibility", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isPrivate) "Only you and admins can access" else "Publicly visible to students & visitors",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                // Toggling from private to public requires explicit confirmation
                                showPublicConfirmDialog = true
                            } else {
                                isPrivate = true
                            }
                        }
                    )
                }

                if (showPublicConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showPublicConfirmDialog = false },
                        title = { Text("Confirm Public Visibility") },
                        text = {
                            Text(
                                "Making '${project.name}' PUBLIC allows all students and visitors to view the prescribed curriculum coverage, question analysis, and public reports.\n\nYour original documents and internal review annotations will remain protected. Proceed?",
                                fontSize = 12.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isPrivate = false
                                    showPublicConfirmDialog = false
                                }
                            ) {
                                Text("Make Public")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showPublicConfirmDialog = false }) {
                                Text("Keep Private")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val visibility = if (isPrivate) ProjectVisibility.PRIVATE else ProjectVisibility.PUBLIC
                    onConfirm(name, selectedSubject, selectedYear, description, visibility, selectedStatus)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("edit_project_confirm_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
