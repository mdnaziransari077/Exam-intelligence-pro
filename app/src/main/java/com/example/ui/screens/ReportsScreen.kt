package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CBSE_CLASS_10_SUBJECTS
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ReportFormat
import com.example.data.model.ReportGenerationRequest
import com.example.data.model.ReportType
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    currentUser: UserEntity?,
    isGuest: Boolean,
    projects: List<ProjectEntity>,
    allSyllabusVersions: List<SyllabusVersionEntity>,
    reports: List<GeneratedReportEntity>,
    isGenerating: Boolean,
    onNavigateBack: () -> Unit,
    onGenerateReport: (ReportGenerationRequest) -> Unit,
    onDeleteReport: (String) -> Unit,
    onToggleReportPublic: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Reports (${reports.size})", "Public Reports (${reports.count { it.isPublic }})")

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedReportForView by remember { mutableStateOf<GeneratedReportEntity?>(null) }
    var reportToDeleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reports_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Analytics Reports & PDF Exports",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "CBSE Class 10 Historical Evidence & Curricular Audits",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("create_report_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Filtered Reports List
        val displayedReports = remember(reports, selectedTabIndex) {
            if (selectedTabIndex == 1) reports.filter { it.isPublic }
            else reports
        }

        if (isGenerating) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Synthesizing records and generating PDF export...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (displayedReports.isEmpty() && !isGenerating) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedTabIndex == 1) "No Public Reports Available" else "No Reports Generated Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate printable PDFs, Markdown documentation, or CSV exports from your verified historical CBSE Class 10 records.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Create First Report")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedReports, key = { it.id }) { r ->
                    ReportCardItem(
                        report = r,
                        dateFormat = dateFormat,
                        canManage = currentUser != null && (r.userId == currentUser.id || currentUser.getRoleEnum().canAccessAdminArea()),
                        onViewReport = { selectedReportForView = r },
                        onTogglePublic = { onToggleReportPublic(r.id, it) },
                        onDelete = { reportToDeleteId = r.id },
                        onCopyText = {
                            clipboardManager.setText(AnnotatedString(r.summaryText))
                            Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Create Report Dialog
    if (showCreateDialog) {
        CreateReportDialog(
            projects = projects,
            allSyllabusVersions = allSyllabusVersions,
            isGenerating = isGenerating,
            onDismiss = { showCreateDialog = false },
            onConfirm = { req ->
                onGenerateReport(req)
                showCreateDialog = false
            }
        )
    }

    // View Report Dialog
    if (selectedReportForView != null) {
        val rep = selectedReportForView!!
        AlertDialog(
            onDismissRequest = { selectedReportForView = null },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (rep.format == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Assessment,
                            contentDescription = null,
                            tint = if (rep.format == "PDF") Color(0xFFDC2626) else PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(rep.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Generated on ${dateFormat.format(Date(rep.createdAt))} • Format: ${rep.format}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AcademicGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DISCLAIMER: ${rep.disclaimer}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Text("Executive Summary:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(rep.summaryText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                    if (rep.methodologyNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Methodology & Counting Rules:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(rep.methodologyNotes, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (rep.filePath != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Storage Location: ${rep.filePath}", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("File Size: ${rep.fileSizeBytes / 1024} KB", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("${rep.title}\n\n${rep.summaryText}\n\nDisclaimer: ${rep.disclaimer}"))
                        Toast.makeText(context, "Report details copied", Toast.LENGTH_SHORT).show()
                        selectedReportForView = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Details")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedReportForView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (reportToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { reportToDeleteId = null },
            title = { Text("Delete Report Record?") },
            text = { Text("This will permanently remove the generated report and associated export file from disk.") },
            confirmButton = {
                Button(
                    onClick = {
                        reportToDeleteId?.let { onDeleteReport(it) }
                        reportToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportToDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ReportCardItem(
    report: GeneratedReportEntity,
    dateFormat: SimpleDateFormat,
    canManage: Boolean,
    onViewReport: () -> Unit,
    onTogglePublic: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCopyText: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("report_card_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (report.format == "PDF") Color(0xFFFEE2E2) else PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = report.format,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.format == "PDF") Color(0xFFDC2626) else PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.subject,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (report.isPublic) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (report.isPublic) "Public Access" else "Private to You",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.isPublic) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = report.summaryText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${dateFormat.format(Date(report.createdAt))} • ${report.recordCount} Records • ${report.fileSizeBytes / 1024} KB",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onViewReport) {
                        Icon(Icons.Default.Visibility, contentDescription = "View Details", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopyText) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    if (canManage) {
                        IconButton(onClick = { onTogglePublic(!report.isPublic) }) {
                            Icon(
                                if (report.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = "Toggle Visibility",
                                tint = if (report.isPublic) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateReportDialog(
    projects: List<ProjectEntity>,
    allSyllabusVersions: List<SyllabusVersionEntity>,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ReportGenerationRequest) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.HISTORICAL_ANALYSIS) }
    var selectedFormat by remember { mutableStateOf(ReportFormat.PDF) }
    var selectedSubject by remember { mutableStateOf("Science") }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(projects.firstOrNull()) }
    var includeAnswers by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Generate Analytics or PDF Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Report Type:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                ReportType.entries.forEach { type ->
                    val isSel = selectedType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type },
                        color = if (isSel) PrimaryBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue) else null
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSel) Icons.Default.CheckCircle else Icons.Default.Assessment,
                                contentDescription = null,
                                tint = if (isSel) PrimaryBlue else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(type.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(type.description, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Text("Export Format:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.label.take(8), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Subject:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CBSE_CLASS_10_SUBJECTS.take(3).forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Answers & Public Flags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Include Answers & Explanations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Where authorized and requested", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = includeAnswers, onCheckedChange = { includeAnswers = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Make Report Public", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Allow public students to view", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ReportGenerationRequest(
                            reportType = selectedType,
                            format = selectedFormat,
                            subject = selectedSubject,
                            projectId = selectedProject?.id,
                            includeAnswers = includeAnswers,
                            isPublic = isPublic
                        )
                    )
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (isGenerating) "Generating..." else "Generate Export")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
