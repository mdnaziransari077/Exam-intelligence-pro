package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoverageStatus
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.MappingReviewStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusNodeType
import com.example.data.model.SyllabusSourceType
import com.example.data.model.SyllabusVerificationStatus
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.TopicCoverageStats
import com.example.data.model.UserEntity
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusManagerScreen(
    project: ProjectEntity?,
    allSyllabusVersions: List<SyllabusVersionEntity>,
    selectedVersion: SyllabusVersionEntity?,
    syllabusNodes: List<SyllabusNodeEntity>,
    mappings: List<QuestionTopicMappingEntity>,
    projectQuestions: List<ExtractedQuestionEntity>,
    coverageStats: List<TopicCoverageStats>,
    currentUser: UserEntity?,
    isAutoMappingActive: Boolean,
    onNavigateBack: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onImportSyllabus: (String, String, String, SyllabusSourceType, String, String?, String?, String?, String?) -> Unit,
    onVerifySyllabus: (String, SyllabusVerificationStatus, String?) -> Unit,
    onAddNode: (String?, SyllabusNodeType, String?, String, String?, Boolean, String?) -> Unit,
    onUpdateNode: (String, String, String?, String?, Boolean, String?) -> Unit,
    onDeleteNode: (String) -> Unit,
    onMergeNodes: (String, String) -> Unit,
    onAutoMapQuestions: () -> Unit,
    onApproveMapping: (String) -> Unit,
    onRejectMapping: (String, String) -> Unit,
    onModifyMapping: (String, String, String, String, String, String?) -> Unit,
    onMarkOutsideSyllabus: (String, String) -> Unit,
    onMarkUncertain: (String, String) -> Unit,
    onAddMultiTopicMapping: (String, String, String, String, String, String, String, String?) -> Unit,
    onDeleteMapping: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Hierarchy, 1: Question Mappings, 2: Coverage Matrix, 3: Untested Topics

    // Dialog States
    var showImportDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var nodeToEdit by remember { mutableStateOf<SyllabusNodeEntity?>(null) }
    var nodeToMerge by remember { mutableStateOf<SyllabusNodeEntity?>(null) }
    var mappingToReassign by remember { mutableStateOf<QuestionTopicMappingEntity?>(null) }
    var questionForMultiTopic by remember { mutableStateOf<ExtractedQuestionEntity?>(null) }

    val isTeacherOrAdmin = currentUser?.getRoleEnum()?.canManageSyllabus() == true
    val canApprove = currentUser?.getRoleEnum()?.canApproveSyllabus() == true

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // App Bar & Version Picker
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("syllabus_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Syllabus Intelligence",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CBSE Class 10",
                                        color = PrimaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = project?.name ?: "Curriculum Alignment & Concept Mapping",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action toolbar
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onAutoMapQuestions,
                            modifier = Modifier.testTag("auto_map_questions_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            enabled = !isAutoMappingActive
                        ) {
                            if (isAutoMappingActive) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mapping...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Map Questions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isTeacherOrAdmin) {
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.testTag("import_syllabus_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Syllabus Version Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var versionDropdownOpen by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = versionDropdownOpen,
                        onExpandedChange = { versionDropdownOpen = !versionDropdownOpen },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedVersion?.let { "${it.subject} (${it.academicSession}) • ${it.verificationStatus}" } ?: "Select Syllabus Version",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Syllabus Version", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionDropdownOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        )

                        ExposedDropdownMenu(
                            expanded = versionDropdownOpen,
                            onDismissRequest = { versionDropdownOpen = false }
                        ) {
                            allSyllabusVersions.forEach { ver ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "${ver.subject} (${ver.academicSession})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (ver.isVerified()) {
                                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text(text = "Source: ${ver.sourceTitle}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        onSelectVersion(ver.id)
                                        versionDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    if (canApprove && selectedVersion != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showVerifyDialog = true },
                            modifier = Modifier.testTag("verify_syllabus_version_button")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (selectedVersion.isVerified()) Color(0xFF10B981) else AcademicGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedVersion.isVerified()) "Re-verify" else "Verify", fontSize = 11.sp)
                        }
                    }
                }

                // Provenance & Source Metadata Bar
                if (selectedVersion != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SyllabusProvenanceBar(
                        version = selectedVersion,
                        onOpenUrl = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Hierarchy (${syllabusNodes.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Question Mappings (${mappings.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Coverage Matrix", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    val untestedCount = coverageStats.count { it.coverageStatus == CoverageStatus.NO_QUESTIONS_FOUND }
                    Text("Untested Topics ($untestedCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (untestedCount > 0) AcademicGold else MaterialTheme.colorScheme.onSurface)
                },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = AcademicGold) }
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when (selectedTab) {
                0 -> SyllabusHierarchyTab(
                    version = selectedVersion,
                    nodes = syllabusNodes,
                    isTeacherOrAdmin = isTeacherOrAdmin,
                    onAddNodeClick = { showAddNodeDialog = true },
                    onEditNode = { nodeToEdit = it },
                    onDeleteNode = onDeleteNode,
                    onMergeClick = { nodeToMerge = it }
                )
                1 -> QuestionMappingsTab(
                    questions = projectQuestions,
                    mappings = mappings,
                    syllabusNodes = syllabusNodes,
                    onApprove = onApproveMapping,
                    onReject = onRejectMapping,
                    onReassign = { mappingToReassign = it },
                    onAddMultiTopic = { q -> questionForMultiTopic = q },
                    onMarkOutside = onMarkOutsideSyllabus,
                    onMarkUncertain = onMarkUncertain,
                    onDelete = onDeleteMapping
                )
                2 -> CoverageMatrixTab(
                    stats = coverageStats,
                    version = selectedVersion,
                    project = project
                )
                3 -> UntestedTopicsTab(
                    stats = coverageStats,
                    version = selectedVersion,
                    project = project
                )
            }
        }
    }

    // --- Interactive Dialogs ---

    if (showImportDialog) {
        ImportSyllabusDialog(
            defaultSubject = project?.subject ?: "Mathematics (Standard)",
            defaultSession = project?.academicYear ?: "2024-2025",
            onDismiss = { showImportDialog = false },
            onConfirm = { subj, sess, verId, type, title, url, docRef, notes, rawContent ->
                onImportSyllabus(subj, sess, verId, type, title, url, docRef, notes, rawContent)
                showImportDialog = false
            }
        )
    }

    if (showVerifyDialog && selectedVersion != null) {
        VerifySyllabusDialog(
            version = selectedVersion,
            onDismiss = { showVerifyDialog = false },
            onConfirm = { status, notes ->
                onVerifySyllabus(selectedVersion.id, status, notes)
                showVerifyDialog = false
            }
        )
    }

    if (showAddNodeDialog && selectedVersion != null) {
        AddSyllabusNodeDialog(
            existingNodes = syllabusNodes,
            onDismiss = { showAddNodeDialog = false },
            onConfirm = { parentId, type, code, name, desc, isExcluded, reason ->
                onAddNode(parentId, type, code, name, desc, isExcluded, reason)
                showAddNodeDialog = false
            }
        )
    }

    if (nodeToEdit != null) {
        EditSyllabusNodeDialog(
            node = nodeToEdit!!,
            onDismiss = { nodeToEdit = null },
            onConfirm = { name, code, desc, isExcluded, reason ->
                onUpdateNode(nodeToEdit!!.id, name, code, desc, isExcluded, reason)
                nodeToEdit = null
            }
        )
    }

    if (nodeToMerge != null) {
        MergeSyllabusNodeDialog(
            sourceNode = nodeToMerge!!,
            availableTargets = syllabusNodes.filter { it.id != nodeToMerge!!.id && it.nodeType == nodeToMerge!!.nodeType },
            onDismiss = { nodeToMerge = null },
            onConfirm = { targetId ->
                onMergeNodes(nodeToMerge!!.id, targetId)
                nodeToMerge = null
            }
        )
    }

    if (mappingToReassign != null) {
        ReassignMappingDialog(
            mapping = mappingToReassign!!,
            syllabusNodes = syllabusNodes,
            onDismiss = { mappingToReassign = null },
            onConfirm = { chId, chName, topId, topName, notes ->
                onModifyMapping(mappingToReassign!!.id, chId, chName, topId, topName, notes)
                mappingToReassign = null
            }
        )
    }

    if (questionForMultiTopic != null && selectedVersion != null) {
        AddMultiTopicMappingDialog(
            question = questionForMultiTopic!!,
            syllabusNodes = syllabusNodes,
            projectId = project?.id ?: questionForMultiTopic!!.projectId,
            sourceDocId = questionForMultiTopic!!.sourceDocumentId,
            onDismiss = { questionForMultiTopic = null },
            onConfirm = { chId, chName, topId, topName, notes ->
                onAddMultiTopicMapping(
                    questionForMultiTopic!!.id,
                    questionForMultiTopic!!.sourceDocumentId,
                    project?.id ?: questionForMultiTopic!!.projectId,
                    chId,
                    chName,
                    topId,
                    topName,
                    notes
                )
                questionForMultiTopic = null
            }
        )
    }
}

@Composable
fun SyllabusProvenanceBar(
    version: SyllabusVersionEntity,
    onOpenUrl: (String) -> Unit
) {
    val isVerified = version.isVerified()
    val bgColor = if (isVerified) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
    val textColor = if (isVerified) Color(0xFF065F46) else Color(0xFF92400E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                if (isVerified) Icons.Default.Verified else Icons.Default.Info,
                contentDescription = null,
                tint = if (isVerified) Color(0xFF10B981) else AcademicGold,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isVerified) "Verified Official: ${version.sourceTitle}" else "Unverified: ${version.sourceTitle} (Review Required)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!version.sourceUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenUrl(version.sourceUrl) }
            ) {
                Text(
                    text = "Source Link",
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// =========================================================================
// TAB 0: SYLLABUS HIERARCHY
// =========================================================================

@Composable
fun SyllabusHierarchyTab(
    version: SyllabusVersionEntity?,
    nodes: List<SyllabusNodeEntity>,
    isTeacherOrAdmin: Boolean,
    onAddNodeClick: () -> Unit,
    onEditNode: (SyllabusNodeEntity) -> Unit,
    onDeleteNode: (String) -> Unit,
    onMergeClick: (SyllabusNodeEntity) -> Unit
) {
    if (version == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please select or import a syllabus version.")
        }
        return
    }

    val chapters = nodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name || it.nodeType == SyllabusNodeType.UNIT.name }
        .sortedBy { it.displayOrder }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Structured CBSE Class 10 Hierarchy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Board ➔ Class 10 ➔ ${version.subject} (${version.academicSession})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isTeacherOrAdmin) {
                Button(
                    onClick = onAddNodeClick,
                    modifier = Modifier.testTag("add_syllabus_node_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (nodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No syllabus nodes in this version yet", fontWeight = FontWeight.Bold)
                    Text("Use 'Add Node' or import a curriculum document.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    val childTopics = nodes.filter { it.parentId == chapter.id }.sortedBy { it.displayOrder }

                    ChapterHierarchyCard(
                        chapter = chapter,
                        topics = childTopics,
                        isTeacherOrAdmin = isTeacherOrAdmin,
                        onEditNode = onEditNode,
                        onDeleteNode = onDeleteNode,
                        onMergeClick = onMergeClick
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterHierarchyCard(
    chapter: SyllabusNodeEntity,
    topics: List<SyllabusNodeEntity>,
    isTeacherOrAdmin: Boolean,
    onEditNode: (SyllabusNodeEntity) -> Unit,
    onDeleteNode: (String) -> Unit,
    onMergeClick: (SyllabusNodeEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("chapter_node_${chapter.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Chapter Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = chapter.code ?: "Chapter",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = chapter.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isTeacherOrAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { onEditNode(chapter) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Chapter", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onMergeClick(chapter) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Merge, contentDescription = "Merge Chapter", modifier = Modifier.size(15.dp), tint = PrimaryBlue)
                        }
                        IconButton(onClick = { onDeleteNode(chapter.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Chapter", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (!chapter.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chapter.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Topics List
            if (topics.isEmpty()) {
                Text(
                    text = "No subtopics registered under this chapter yet.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topics.forEach { topic ->
                        TopicItemRow(
                            topic = topic,
                            isTeacherOrAdmin = isTeacherOrAdmin,
                            onEdit = { onEditNode(topic) },
                            onDelete = { onDeleteNode(topic.id) },
                            onMerge = { onMergeClick(topic) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopicItemRow(
    topic: SyllabusNodeEntity,
    isTeacherOrAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMerge: () -> Unit
) {
    val isExcluded = topic.isExcluded

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isExcluded) Color(0xFFF3E8FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!topic.code.isNullOrBlank()) {
                    Text(text = topic.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = topic.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isExcluded) Color(0xFF6B21A8) else MaterialTheme.colorScheme.onSurface
                )
                if (isExcluded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF9333EA).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Rationalized / Excluded",
                            color = Color(0xFF9333EA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (isExcluded && !topic.exclusionReason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${topic.exclusionReason}",
                    fontSize = 10.sp,
                    color = Color(0xFF7E22CE)
                )
            } else if (!topic.description.isNullOrBlank()) {
                Text(
                    text = topic.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isTeacherOrAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Topic", modifier = Modifier.size(13.dp))
                }
                IconButton(onClick = onMerge, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Merge, contentDescription = "Merge Topic", modifier = Modifier.size(13.dp), tint = PrimaryBlue)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Topic", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: QUESTION-TO-TOPIC MAPPINGS REVIEW
// =========================================================================

@Composable
fun QuestionMappingsTab(
    questions: List<ExtractedQuestionEntity>,
    mappings: List<QuestionTopicMappingEntity>,
    syllabusNodes: List<SyllabusNodeEntity>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onReassign: (QuestionTopicMappingEntity) -> Unit,
    onAddMultiTopic: (ExtractedQuestionEntity) -> Unit,
    onMarkOutside: (String, String) -> Unit,
    onMarkUncertain: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Approved", "Uncertain", "Outside"
    var searchQuery by remember { mutableStateOf("") }

    val questionMap = remember(questions) { questions.associateBy { it.id } }

    val filteredMappings = mappings.filter { m ->
        val q = questionMap[m.questionId]
        val matchesSearch = searchQuery.isBlank() ||
                m.topicName.contains(searchQuery, ignoreCase = true) ||
                m.chapterName.contains(searchQuery, ignoreCase = true) ||
                (q?.questionNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                (q?.getEffectiveText()?.contains(searchQuery, ignoreCase = true) == true)

        val matchesFilter = when (filterStatus) {
            "Pending" -> m.reviewStatus == MappingReviewStatus.SUGGESTED.label
            "Approved" -> m.reviewStatus == MappingReviewStatus.APPROVED.label && !m.isOutsideSyllabus
            "Uncertain" -> m.reviewStatus == MappingReviewStatus.UNCERTAIN.label
            "Outside" -> m.isOutsideSyllabus
            else -> true
        }

        matchesSearch && matchesFilter
    }

    val pendingCount = mappings.count { it.reviewStatus == MappingReviewStatus.SUGGESTED.label }
    val approvedCount = mappings.count { it.reviewStatus == MappingReviewStatus.APPROVED.label && !it.isOutsideSyllabus }
    val uncertainCount = mappings.count { it.reviewStatus == MappingReviewStatus.UNCERTAIN.label }
    val outsideCount = mappings.count { it.isOutsideSyllabus }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("mappings_search_input"),
            placeholder = { Text("Search question, chapter, or topic mapping...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
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
                onClick = { filterStatus = "All" },
                label = { Text("All (${mappings.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = filterStatus == "Pending",
                onClick = { filterStatus = "Pending" },
                label = { Text("AI Suggestions ($pendingCount)", fontSize = 11.sp, color = AcademicGold) }
            )
            FilterChip(
                selected = filterStatus == "Approved",
                onClick = { filterStatus = "Approved" },
                label = { Text("Human Verified ($approvedCount)", fontSize = 11.sp, color = Color(0xFF10B981)) }
            )
            FilterChip(
                selected = filterStatus == "Uncertain",
                onClick = { filterStatus = "Uncertain" },
                label = { Text("Uncertain ($uncertainCount)", fontSize = 11.sp, color = Color(0xFFF97316)) }
            )
            if (outsideCount > 0) {
                FilterChip(
                    selected = filterStatus == "Outside",
                    onClick = { filterStatus = "Outside" },
                    label = { Text("Outside Syllabus ($outsideCount)", fontSize = 11.sp, color = Color(0xFF8B5CF6)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredMappings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No question mappings match this filter.", fontWeight = FontWeight.Bold)
                    Text("Click 'Map Questions' to run the Phase 3 mapping engine.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMappings, key = { it.id }) { mapping ->
                    val question = questionMap[mapping.questionId]

                    QuestionMappingCard(
                        mapping = mapping,
                        question = question,
                        onApprove = { onApprove(mapping.id) },
                        onReject = { onReject(mapping.id, "Rejected during human review") },
                        onReassign = { onReassign(mapping) },
                        onAddMultiTopic = { if (question != null) onAddMultiTopic(question) },
                        onMarkOutside = { onMarkOutside(mapping.id, "Question concepts fall outside active syllabus") },
                        onMarkUncertain = { onMarkUncertain(mapping.id, "Question concepts ambiguous or multi-disciplinary") },
                        onDelete = { onDelete(mapping.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionMappingCard(
    mapping: QuestionTopicMappingEntity,
    question: ExtractedQuestionEntity?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReassign: () -> Unit,
    onAddMultiTopic: () -> Unit,
    onMarkOutside: () -> Unit,
    onMarkUncertain: () -> Unit,
    onDelete: () -> Unit
) {
    val isApproved = mapping.isApproved()
    val isPending = mapping.isPendingReview()
    val isUncertain = mapping.isUncertain()
    val isOutside = mapping.isOutsideSyllabus

    val statusColor = when {
        isOutside -> Color(0xFF8B5CF6)
        isApproved -> Color(0xFF10B981)
        isUncertain -> Color(0xFFF97316)
        else -> AcademicGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("mapping_card_${mapping.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Header Row: Question number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = PrimaryBlue, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "Q${question?.questionNumber ?: "—"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (question?.marks != null && question.marks > 0) {
                        Surface(color = IntelligenceCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "${question.marks}M",
                                color = IntelligenceCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = question?.questionType ?: "CBSE Question",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            isOutside -> "Outside Syllabus"
                            isApproved -> "Human Verified"
                            isUncertain -> "Uncertain (Review)"
                            else -> "AI Suggestion"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Question Stem Snippet
            Text(
                text = question?.getEffectiveText() ?: "Question text not available",
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mapped Topic & Chapter Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Chapter: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mapping.chapterName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Topic: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = mapping.topicName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    if (!mapping.explanation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Evidence: ${mapping.explanation}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isApproved) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(onClick = onReassign) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reassign", fontSize = 11.sp)
                    }

                    OutlinedButton(onClick = onAddMultiTopic) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Topic", fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onMarkUncertain, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Mark Uncertain", tint = Color(0xFFF97316), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Mapping", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: COVERAGE MATRIX
// =========================================================================

@Composable
fun CoverageMatrixTab(
    stats: List<TopicCoverageStats>,
    version: SyllabusVersionEntity?,
    project: ProjectEntity?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (stats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("No coverage analytics calculated yet.", fontWeight = FontWeight.Bold)
                Text("Map project questions to compute syllabus coverage matrix.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var selectedStatusFilter by remember { mutableStateOf<CoverageStatus?>(null) }
    var selectedChapterFilter by remember { mutableStateOf("All Chapters") }
    var showExportDialog by remember { mutableStateOf(false) }

    val totalTopics = stats.size
    val coveredTopics = stats.count { it.coverageStatus == CoverageStatus.QUESTIONS_FOUND }
    val reviewNeeded = stats.count { it.coverageStatus == CoverageStatus.MAPPING_REVIEW_REQUIRED }
    val notFound = stats.count { it.coverageStatus == CoverageStatus.NO_QUESTIONS_FOUND }
    val excludedCount = stats.count { it.coverageStatus == CoverageStatus.EXCLUDED_FROM_SYLLABUS }

    val distinctChapters = remember(stats) {
        listOf("All Chapters") + stats.map { it.chapterName }.distinct().sorted()
    }

    val filteredStats = stats.filter { item ->
        val matchesStatus = selectedStatusFilter == null || item.coverageStatus == selectedStatusFilter
        val matchesChapter = selectedChapterFilter == "All Chapters" || item.chapterName == selectedChapterFilter
        matchesStatus && matchesChapter
    }

    val hasPreliminaryData = stats.any { it.hasUnverifiedQuestionMarks || it.pendingMappingCount > 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Cards & Export Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Coverage Matrix (${filteredStats.size} of $totalTopics Topics)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            OutlinedButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.testTag("export_coverage_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary Metric Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Total Topics", "$totalTopics", PrimaryBlue, modifier = Modifier.weight(1f))
            MetricCard("Questions Found", "$coveredTopics", Color(0xFF10B981), modifier = Modifier.weight(1f))
            MetricCard("Review Needed", "$reviewNeeded", AcademicGold, modifier = Modifier.weight(1f))
            MetricCard("Not in Sample", "$notFound", Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        }

        // Preliminary Coverage Notice Banner
        if (hasPreliminaryData) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preliminary Coverage Notice: Some questions in this paper collection contain unverified marks or pending topic mappings. Coverage metrics are preliminary until verified.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                label = { Text("All (${stats.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == CoverageStatus.QUESTIONS_FOUND,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == CoverageStatus.QUESTIONS_FOUND) null else CoverageStatus.QUESTIONS_FOUND },
                label = { Text("Questions Found ($coveredTopics)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == CoverageStatus.NO_QUESTIONS_FOUND,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == CoverageStatus.NO_QUESTIONS_FOUND) null else CoverageStatus.NO_QUESTIONS_FOUND },
                label = { Text("Not in Sample ($notFound)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == CoverageStatus.MAPPING_REVIEW_REQUIRED,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == CoverageStatus.MAPPING_REVIEW_REQUIRED) null else CoverageStatus.MAPPING_REVIEW_REQUIRED },
                label = { Text("Review Needed ($reviewNeeded)", fontSize = 11.sp) }
            )
            if (excludedCount > 0) {
                FilterChip(
                    selected = selectedStatusFilter == CoverageStatus.EXCLUDED_FROM_SYLLABUS,
                    onClick = { selectedStatusFilter = if (selectedStatusFilter == CoverageStatus.EXCLUDED_FROM_SYLLABUS) null else CoverageStatus.EXCLUDED_FROM_SYLLABUS },
                    label = { Text("Excluded ($excludedCount)", fontSize = 11.sp) }
                )
            }
        }

        // Chapter Filter Chips
        if (distinctChapters.size > 2) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                distinctChapters.forEach { ch ->
                    FilterChip(
                        selected = selectedChapterFilter == ch,
                        onClick = { selectedChapterFilter = ch },
                        label = { Text(ch, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Topic Matrix List
        if (filteredStats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No topics matching selected filters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStats, key = { it.topicId }) { item ->
                    TopicCoverageCard(item = item)
                }
            }
        }
    }

    // Export Coverage Dialog
    if (showExportDialog) {
        ExportCoverageReportDialog(
            stats = stats,
            version = version,
            project = project,
            onDismiss = { showExportDialog = false },
            onCopy = { content ->
                clipboardManager.setText(AnnotatedString(content))
                Toast.makeText(context, "Coverage report copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ExportCoverageReportDialog(
    stats: List<TopicCoverageStats>,
    version: SyllabusVersionEntity?,
    project: ProjectEntity?,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    var exportFormat by remember { mutableStateOf("markdown") } // "markdown", "csv", "json"

    val reportContent = remember(exportFormat, stats, version, project) {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val totalTopics = stats.size
        val coveredTopics = stats.count { it.coverageStatus == CoverageStatus.QUESTIONS_FOUND }
        val notFound = stats.count { it.coverageStatus == CoverageStatus.NO_QUESTIONS_FOUND }
        val reviewNeeded = stats.count { it.coverageStatus == CoverageStatus.MAPPING_REVIEW_REQUIRED }
        val excludedCount = stats.count { it.coverageStatus == CoverageStatus.EXCLUDED_FROM_SYLLABUS }

        when (exportFormat) {
            "markdown" -> buildString {
                appendLine("# CBSE Class 10 Syllabus Coverage Report: ${version?.subject ?: project?.subject ?: "Mathematics"}")
                appendLine("- **Academic Session:** ${version?.academicSession ?: project?.academicYear ?: "2024-2025"}")
                appendLine("- **Syllabus Version:** ${version?.versionIdentifier ?: "CBSE-10-v1"} (${version?.verificationStatus ?: "Official"})")
                appendLine("- **Source:** ${version?.sourceTitle ?: "Official CBSE Curriculum Document"}")
                appendLine("- **Analysed Collection:** ${project?.name ?: "Current Paper Collection"} (${project?.documentCount ?: 0} papers)")
                appendLine("- **Generated:** $dateStr")
                appendLine()
                appendLine("## Summary Metrics")
                appendLine("- Total Prescribed Topics: $totalTopics")
                appendLine("- Topics with Questions Found: $coveredTopics")
                appendLine("- Untested Topics in Analysed Collection: $notFound")
                appendLine("- Topics Awaiting Mapping Review: $reviewNeeded")
                appendLine("- Excluded Topics: $excludedCount")
                appendLine()
                appendLine("## Chapter & Topic Coverage Matrix")
                appendLine("| Chapter | Topic | Questions | Papers | Verified Marks | Coverage Status |")
                appendLine("|---|---|:---:|:---:|:---:|---|")
                stats.forEach { s ->
                    val marksStr = if (s.verifiedMarksTotal != null) "${s.verifiedMarksTotal}m" else if (s.hasUnverifiedQuestionMarks) "Unverified" else "0m"
                    appendLine("| ${s.chapterName} | ${s.topicName} | ${s.totalMappedQuestions} | ${s.distinctPaperCount} | $marksStr | ${s.coverageStatus.label} |")
                }
                appendLine()
                appendLine("> **Educational Methodology Disclaimer:**")
                appendLine("> Absence from this specific paper collection does NOT mean a topic was omitted from the official CBSE syllabus or will not be tested in future examinations. Students must prepare all prescribed syllabus topics thoroughly.")
            }
            "csv" -> buildString {
                appendLine("Chapter,Topic,Total Mapped Questions,Distinct Papers,Verified Marks,Coverage Status,Is Excluded")
                stats.forEach { s ->
                    val marksStr = s.verifiedMarksTotal?.toString() ?: if (s.hasUnverifiedQuestionMarks) "UNVERIFIED" else "0"
                    appendLine("\"${s.chapterName.replace("\"", "\"\"")}\",\"${s.topicName.replace("\"", "\"\"")}\",${s.totalMappedQuestions},${s.distinctPaperCount},\"$marksStr\",\"${s.coverageStatus.label}\",${s.isExcludedFromSyllabus}")
                }
            }
            "json" -> buildString {
                appendLine("{")
                appendLine("  \"board\": \"CBSE\",")
                appendLine("  \"classLevel\": \"10\",")
                appendLine("  \"subject\": \"${version?.subject ?: project?.subject ?: "Mathematics"}\",")
                appendLine("  \"academicSession\": \"${version?.academicSession ?: "2024-2025"}\",")
                appendLine("  \"syllabusVersion\": \"${version?.versionIdentifier ?: "v1"}\",")
                appendLine("  \"syllabusVerificationStatus\": \"${version?.verificationStatus ?: "VERIFIED"}\",")
                appendLine("  \"projectCollection\": \"${project?.name ?: ""}\",")
                appendLine("  \"paperCount\": ${project?.documentCount ?: 0},")
                appendLine("  \"generatedDate\": \"$dateStr\",")
                appendLine("  \"disclaimer\": \"Absence from this specific paper collection does not imply omission from the official CBSE curriculum.\",")
                appendLine("  \"summary\": {")
                appendLine("    \"totalTopics\": $totalTopics,")
                appendLine("    \"questionsFound\": $coveredTopics,")
                appendLine("    \"notInSample\": $notFound,")
                appendLine("    \"reviewNeeded\": $reviewNeeded,")
                appendLine("    \"excluded\": $excludedCount")
                appendLine("  },")
                appendLine("  \"topicCoverage\": [")
                stats.forEachIndexed { idx, s ->
                    val comma = if (idx < stats.size - 1) "," else ""
                    appendLine("    {")
                    appendLine("      \"chapter\": \"${s.chapterName}\",")
                    appendLine("      \"topic\": \"${s.topicName}\",")
                    appendLine("      \"totalQuestions\": ${s.totalMappedQuestions},")
                    appendLine("      \"distinctPapers\": ${s.distinctPaperCount},")
                    appendLine("      \"verifiedMarks\": ${s.verifiedMarksTotal ?: "null"},")
                    appendLine("      \"hasUnverifiedMarks\": ${s.hasUnverifiedQuestionMarks},")
                    appendLine("      \"coverageStatus\": \"${s.coverageStatus.name}\"")
                    appendLine("    }$comma")
                }
                appendLine("  ]")
                appendLine("}")
            }
            else -> ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Syllabus Coverage Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select format for syllabus coverage and concept gap export:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportFormat == "markdown",
                        onClick = { exportFormat = "markdown" },
                        label = { Text("Markdown (.md)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exportFormat == "csv",
                        onClick = { exportFormat = "csv" },
                        label = { Text("CSV (.csv)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exportFormat == "json",
                        onClick = { exportFormat = "json" },
                        label = { Text("JSON (.json)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preview box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                ) {
                    Box(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = reportContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy(reportContent)
                    onDismiss()
                },
                modifier = Modifier.testTag("copy_coverage_report_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy to Clipboard")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TopicCoverageCard(item: TopicCoverageStats) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("coverage_row_${item.topicId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.chapterName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.topicName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = Color(item.coverageStatus.badgeColorHex).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.coverageStatus.label,
                        color = Color(item.coverageStatus.badgeColorHex),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mapped: ${item.totalMappedQuestions} (${item.verifiedMappingCount} verified, ${item.pendingMappingCount} AI suggested)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Papers: ${item.distinctPaperCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.verifiedMarksTotal != null) {
                    Text(
                        text = "${item.verifiedMarksTotal} Marks verified",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                } else if (item.totalMappedQuestions > 0 && item.hasUnverifiedQuestionMarks) {
                    Text(
                        text = "⚠️ Unverified Marks",
                        fontSize = 10.sp,
                        color = AcademicGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: UNTESTED TOPICS & COLLECTION LIMITATIONS
// =========================================================================

@Composable
fun UntestedTopicsTab(
    stats: List<TopicCoverageStats>,
    version: SyllabusVersionEntity?,
    project: ProjectEntity?
) {
    val untestedTopics = stats.filter { it.coverageStatus == CoverageStatus.NO_QUESTIONS_FOUND }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Educational Collection Caution Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Educational Methodology Notice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No mapped questions found in the currently analysed collection (${project?.name ?: "Current Paper Set"}, ${project?.documentCount ?: 0} papers).",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF78350F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Important: Absence from this specific paper collection does NOT mean a topic was omitted from the official CBSE syllabus or will not be tested in future exams. Students should prepare all prescribed syllabus topics thoroughly.",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Topics Without Mapped Questions in Sample (${untestedTopics.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "Syllabus Version: ${version?.versionIdentifier ?: "CBSE Baseline"} • Board: CBSE Class 10",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (untestedTopics.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comprehensive Question Coverage!", fontWeight = FontWeight.Bold)
                    Text("All syllabus topics have at least one mapped question in the current paper collection.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                untestedTopics.forEach { topic ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = topic.chapterName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = topic.topicName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "0 Questions in Sample",
                                    color = Color(0xFFD97706),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = accentColor)
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

// =========================================================================
// DIALOGS: IMPORT, VERIFY, ADD/EDIT NODE, MERGE, REASSIGN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSyllabusDialog(
    defaultSubject: String,
    defaultSession: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, SyllabusSourceType, String, String?, String?, String?, String?) -> Unit
) {
    var subject by remember { mutableStateOf(defaultSubject) }
    var session by remember { mutableStateOf(defaultSession) }
    var versionIdentifier by remember { mutableStateOf("CBSE-10-${defaultSubject.take(4).uppercase()}-$defaultSession-v1") }
    var sourceType by remember { mutableStateOf(SyllabusSourceType.OFFICIAL_CBSE_DOCUMENT) }
    var sourceTitle by remember { mutableStateOf("CBSE Secondary School Curriculum ($defaultSession)") }
    var sourceUrl by remember { mutableStateOf("https://cbseacademic.nic.in/curriculum_2025.html") }
    var notes by remember { mutableStateOf("") }
    var rawContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import / Scribe Syllabus", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (CBSE Class 10)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    label = { Text("Academic Session (e.g. 2024-2025)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sourceTitle,
                    onValueChange = { sourceTitle = it },
                    label = { Text("Official Source Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("Official Source URL (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawContent,
                    onValueChange = { rawContent = it },
                    label = { Text("Outline or Syllabus Markdown (optional)") },
                    placeholder = { Text("# Chapter 1: Real Numbers\n- 1.1 Fundamental Theorem of Arithmetic\n- 1.2 Irrationality Proofs") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(subject, session, versionIdentifier, sourceType, sourceTitle, sourceUrl.ifBlank { null }, null, notes.ifBlank { null }, rawContent.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Import Syllabus")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VerifySyllabusDialog(
    version: SyllabusVersionEntity,
    onDismiss: () -> Unit,
    onConfirm: (SyllabusVerificationStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(SyllabusVerificationStatus.VERIFIED) }
    var notes by remember { mutableStateOf(version.reviewerNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Syllabus Version", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You are verifying '${version.sourceTitle}' for ${version.subject} (${version.academicSession}).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == SyllabusVerificationStatus.VERIFIED,
                        onClick = { selectedStatus = SyllabusVerificationStatus.VERIFIED },
                        label = { Text("Verified Official") }
                    )
                    FilterChip(
                        selected = selectedStatus == SyllabusVerificationStatus.UNDER_REVIEW,
                        onClick = { selectedStatus = SyllabusVerificationStatus.UNDER_REVIEW },
                        label = { Text("Under Review") }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reviewer Verification Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus, notes.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Verification")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSyllabusNodeDialog(
    existingNodes: List<SyllabusNodeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String?, SyllabusNodeType, String?, String, String?, Boolean, String?) -> Unit
) {
    var nodeType by remember { mutableStateOf(SyllabusNodeType.TOPIC) }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isExcluded by remember { mutableStateOf(false) }
    var exclusionReason by remember { mutableStateOf("") }

    val chapters = existingNodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name || it.nodeType == SyllabusNodeType.UNIT.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Syllabus Node", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = nodeType == SyllabusNodeType.CHAPTER,
                        onClick = {
                            nodeType = SyllabusNodeType.CHAPTER
                            selectedParentId = null
                        },
                        label = { Text("Chapter") }
                    )
                    FilterChip(
                        selected = nodeType == SyllabusNodeType.TOPIC,
                        onClick = { nodeType = SyllabusNodeType.TOPIC },
                        label = { Text("Topic") }
                    )
                }

                if (nodeType == SyllabusNodeType.TOPIC && chapters.isNotEmpty()) {
                    var parentDropdownOpen by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = parentDropdownOpen,
                        onExpandedChange = { parentDropdownOpen = !parentDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = chapters.find { it.id == selectedParentId }?.name ?: "Select Parent Chapter",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent Chapter") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = parentDropdownOpen,
                            onDismissRequest = { parentDropdownOpen = false }
                        ) {
                            chapters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch.name) },
                                    onClick = {
                                        selectedParentId = ch.id
                                        parentDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code (e.g. Ch 1, 1.2)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Learning Objective / Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isExcluded,
                        onCheckedChange = { isExcluded = it }
                    )
                    Text(text = "Mark as Excluded / Rationalized", fontSize = 12.sp)
                }

                if (isExcluded) {
                    OutlinedTextField(
                        value = exclusionReason,
                        onValueChange = { exclusionReason = it },
                        label = { Text("Exclusion Reason") },
                        placeholder = { Text("e.g. Rationalized by NCERT for 2024-25") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(selectedParentId, nodeType, code.ifBlank { null }, name, description.ifBlank { null }, isExcluded, exclusionReason.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add Node")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditSyllabusNodeDialog(
    node: SyllabusNodeEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, Boolean, String?) -> Unit
) {
    var name by remember { mutableStateOf(node.name) }
    var code by remember { mutableStateOf(node.code ?: "") }
    var desc by remember { mutableStateOf(node.description ?: "") }
    var isExcluded by remember { mutableStateOf(node.isExcluded) }
    var reason by remember { mutableStateOf(node.exclusionReason ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Syllabus Node", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isExcluded,
                        onCheckedChange = { isExcluded = it }
                    )
                    Text(text = "Mark Excluded / Rationalized", fontSize = 12.sp)
                }
                if (isExcluded) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Exclusion Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, code.ifBlank { null }, desc.ifBlank { null }, isExcluded, reason.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeSyllabusNodeDialog(
    sourceNode: SyllabusNodeEntity,
    availableTargets: List<SyllabusNodeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedTargetId by remember { mutableStateOf(availableTargets.firstOrNull()?.id) }
    var dropdownOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Syllabus Node", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Merge source node '${sourceNode.name}' into another node. All child subtopics and question mappings will be cleanly transferred.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (availableTargets.isEmpty()) {
                    Text("No candidate nodes of matching type found to merge into.", color = MaterialTheme.colorScheme.error)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownOpen,
                        onExpandedChange = { dropdownOpen = !dropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = availableTargets.find { it.id == selectedTargetId }?.name ?: "Select Target Node",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Node") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownOpen,
                            onDismissRequest = { dropdownOpen = false }
                        ) {
                            availableTargets.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.name) },
                                    onClick = {
                                        selectedTargetId = target.id
                                        dropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTargetId?.let { onConfirm(it) }
                },
                enabled = selectedTargetId != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Confirm Merge")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReassignMappingDialog(
    mapping: QuestionTopicMappingEntity,
    syllabusNodes: List<SyllabusNodeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    val topics = syllabusNodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name && !it.isExcluded }
    val chapters = syllabusNodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

    var selectedTopicId by remember { mutableStateOf(mapping.topicId) }
    var notes by remember { mutableStateOf("") }
    var dropdownOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign Topic Mapping", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select the correct syllabus topic for this question mapping.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownOpen,
                    onExpandedChange = { dropdownOpen = !dropdownOpen }
                ) {
                    OutlinedTextField(
                        value = topics.find { it.id == selectedTopicId }?.let { "${chapters[it.parentId]?.name ?: "Chapter"} ➔ ${it.name}" } ?: "Select Topic",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Topic") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false }
                    ) {
                        topics.forEach { top ->
                            val ch = chapters[top.parentId]?.name ?: "Chapter"
                            DropdownMenuItem(
                                text = { Text("$ch ➔ ${top.name}", fontSize = 12.sp) },
                                onClick = {
                                    selectedTopicId = top.id
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reviewer Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val topic = topics.find { it.id == selectedTopicId }
                    if (topic != null) {
                        val ch = chapters[topic.parentId]
                        onConfirm(
                            ch?.id ?: topic.parentId ?: "ch-unknown",
                            ch?.name ?: "Chapter",
                            topic.id,
                            topic.name,
                            notes.ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Reassignment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMultiTopicMappingDialog(
    question: ExtractedQuestionEntity,
    syllabusNodes: List<SyllabusNodeEntity>,
    projectId: String,
    sourceDocId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    val topics = syllabusNodes.filter { it.nodeType == SyllabusNodeType.TOPIC.name && !it.isExcluded }
    val chapters = syllabusNodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.name }.associateBy { it.id }

    var selectedTopicId by remember { mutableStateOf(topics.firstOrNull()?.id) }
    var notes by remember { mutableStateOf("") }
    var dropdownOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map Additional Topic", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Question Q${question.questionNumber} spans multiple concepts. Select an additional syllabus topic to map.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownOpen,
                    onExpandedChange = { dropdownOpen = !dropdownOpen }
                ) {
                    OutlinedTextField(
                        value = topics.find { it.id == selectedTopicId }?.let { "${chapters[it.parentId]?.name ?: "Chapter"} ➔ ${it.name}" } ?: "Select Topic",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Additional Topic") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false }
                    ) {
                        topics.forEach { top ->
                            val ch = chapters[top.parentId]?.name ?: "Chapter"
                            DropdownMenuItem(
                                text = { Text("$ch ➔ ${top.name}", fontSize = 12.sp) },
                                onClick = {
                                    selectedTopicId = top.id
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. secondary concept or cross-chapter connection)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val topic = topics.find { it.id == selectedTopicId }
                    if (topic != null) {
                        val ch = chapters[topic.parentId]
                        onConfirm(
                            ch?.id ?: topic.parentId ?: "ch-unknown",
                            ch?.name ?: "Chapter",
                            topic.id,
                            topic.name,
                            notes.ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add Mapping")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
