package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectVisibility
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.AdminSystemStats
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
    currentUser: UserEntity?,
    stats: AdminSystemStats?,
    users: List<UserEntity>,
    projects: List<ProjectEntity> = emptyList(),
    documents: List<DocumentEntity> = emptyList(),
    questions: List<ExtractedQuestionEntity> = emptyList(),
    syllabusVersions: List<SyllabusVersionEntity> = emptyList(),
    mappings: List<QuestionTopicMappingEntity> = emptyList(),
    practiceQuestions: List<PracticeQuestionEntity> = emptyList(),
    estimates: List<AiExamEstimateEntity> = emptyList(),
    reports: List<GeneratedReportEntity> = emptyList(),
    auditLogs: List<AuditLogEntity>,
    onToggleUserStatus: (String) -> Unit,
    onUpdateUserRole: (String, UserRole) -> Unit = { _, _ -> },
    onChangeProjectVisibility: (String, ProjectVisibility) -> Unit = { _, _ -> },
    onDeleteProject: (String) -> Unit = {},
    onDeleteDocument: (String) -> Unit = {},
    onApproveQuestion: (String) -> Unit = {},
    onDeleteReport: (String) -> Unit = {},
    onRefreshStats: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAuthorized = currentUser != null && currentUser.getRoleEnum().canAccessAdminArea()

    if (!isAuthorized) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Access Denied",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Access Denied",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The Admin Dashboard is restricted to authorized platform administrators. Your current account role (${currentUser?.role ?: "Guest"}) is not permitted to access this area.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNavigateToSignIn,
                        modifier = Modifier.testTag("admin_denied_login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Sign In with Admin Account")
                    }
                }
            }
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Users & Roles",
        "Projects & Access",
        "Documents Repository",
        "Extracted Questions",
        "Syllabus & Curricula",
        "Concept Mappings",
        "AI Practice Review",
        "AI Trend Estimates",
        "System Reports",
        "Audit Trail"
    )

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    var userRoleToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var projectToConfirmVisibility by remember { mutableStateOf<Pair<ProjectEntity, ProjectVisibility>?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Admin Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Administration Console",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CBSE Class 10 Governance & Evidence Control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRefreshStats, modifier = Modifier.testTag("admin_refresh_button")) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Stats", tint = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Stats Overview Bar
        if (stats != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCardMini("Users", "${stats.totalUsers}", Icons.Default.People, PrimaryBlue)
                StatCardMini("Projects", "${stats.totalProjects}", Icons.Default.Folder, IntelligenceCyan)
                StatCardMini("Documents", "${stats.totalDocuments}", Icons.Default.Description, AcademicGold)
                StatCardMini("Verified Qs", "${stats.verifiedQuestions}", Icons.Default.CheckCircle, StatusGreen)
                StatCardMini("Mappings", "${stats.approvedMappings}", Icons.Default.Layers, Color(0xFF8B5CF6))
                StatCardMini("Reports", "${stats.totalReports}", Icons.Default.Assessment, Color(0xFFEC4899))
                StatCardMini("Audit Logs", "${stats.totalAuditLogs}", Icons.Default.Security, Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue,
            edgePadding = 0.dp
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

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content Views
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedTabIndex) {
                0 -> UsersManagementSection(
                    users = users,
                    currentUser = currentUser,
                    onToggleUserStatus = onToggleUserStatus,
                    onEditRole = { userRoleToEdit = it }
                )
                1 -> ProjectsManagementSection(
                    projects = projects,
                    onChangeVisibility = { proj, vis -> projectToConfirmVisibility = Pair(proj, vis) },
                    onDelete = { projectToDelete = it }
                )
                2 -> DocumentsRepositorySection(
                    documents = documents,
                    onDelete = onDeleteDocument
                )
                3 -> QuestionsReviewSection(
                    questions = questions,
                    onApprove = onApproveQuestion
                )
                4 -> SyllabusCurriculaSection(
                    versions = syllabusVersions
                )
                5 -> ConceptMappingsSection(
                    mappings = mappings
                )
                6 -> PracticeQuestionsQueueSection(
                    questions = practiceQuestions
                )
                7 -> AiEstimatesSection(
                    estimates = estimates
                )
                8 -> SystemReportsSection(
                    reports = reports,
                    dateFormat = dateFormat,
                    onDelete = onDeleteReport
                )
                9 -> AuditTrailSection(
                    logs = auditLogs,
                    dateFormat = dateFormat
                )
            }
        }
    }

    // Role Edit Dialog
    if (userRoleToEdit != null) {
        val target = userRoleToEdit!!
        var chosenRole by remember { mutableStateOf(target.getRoleEnum()) }

        AlertDialog(
            onDismissRequest = { userRoleToEdit = null },
            title = { Text("Update Role for ${target.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new platform role for ${target.email}:", fontSize = 11.sp)
                    UserRole.entries.forEach { r ->
                        val isSel = chosenRole == r
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenRole = r },
                            color = if (isSel) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue) else null
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSel) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isSel) PrimaryBlue else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(r.displayName(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(r.name, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserRole(target.id, chosenRole)
                        userRoleToEdit = null
                    }
                ) {
                    Text("Apply Role Change")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userRoleToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Confirm Visibility Dialog
    if (projectToConfirmVisibility != null) {
        val (proj, newVis) = projectToConfirmVisibility!!
        AlertDialog(
            onDismissRequest = { projectToConfirmVisibility = null },
            title = { Text("Confirm Visibility Change") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (newVis == ProjectVisibility.PUBLIC)
                            "Making '${proj.name}' PUBLIC will allow all students and visitors to see the project's syllabus coverage and public report summaries. Private documents and internal annotations remain protected."
                        else
                            "Returning '${proj.name}' to PRIVATE will restrict access exclusively to the project owner and administrators.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangeProjectVisibility(proj.id, newVis)
                        projectToConfirmVisibility = null
                    }
                ) {
                    Text("Confirm ${newVis.name}")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { projectToConfirmVisibility = null }) { Text("Cancel") }
            }
        )
    }

    // Confirm Project Delete Dialog
    if (projectToDelete != null) {
        val proj = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project '${proj.name}'?") },
            text = { Text("This will permanently delete the project, all uploaded documents, and extracted questions.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Project")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { projectToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StatCardMini(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Card(
        modifier = Modifier.width(105.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 0: USERS & ROLES
// -------------------------------------------------------------------------
@Composable
fun UsersManagementSection(
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onToggleUserStatus: (String) -> Unit,
    onEditRole: (UserEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = users.filter {
        searchQuery.isBlank() ||
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by name, email, or role...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { u ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = u.getRoleEnum())
                            }
                            Text(u.email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${u.schoolOrOrg} • Target Year: ${u.targetExamYear}", fontSize = 9.sp, color = Color(0xFF64748B))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onEditRole(u) },
                                modifier = Modifier.height(30.dp),
                                enabled = u.id != currentUser?.id
                            ) {
                                Text("Role", fontSize = 9.sp)
                            }

                            IconButton(
                                onClick = { onToggleUserStatus(u.id) },
                                enabled = u.id != currentUser?.id,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    if (u.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                    contentDescription = "Status",
                                    tint = if (u.isActive) StatusGreen else StatusRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 1: PROJECTS & VISIBILITY
// -------------------------------------------------------------------------
@Composable
fun ProjectsManagementSection(
    projects: List<ProjectEntity>,
    onChangeVisibility: (ProjectEntity, ProjectVisibility) -> Unit,
    onDelete: (ProjectEntity) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(projects, key = { it.id }) { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (p.isPublic()) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (p.isPublic()) "PUBLIC" else "PRIVATE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.isPublic()) Color(0xFF065F46) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("${p.subject} • ${p.className} • Owner: ${p.ownerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Documents: ${p.documentCount} • Status: ${p.status}", fontSize = 9.sp, color = Color(0xFF64748B))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val targetVis = if (p.isPublic()) ProjectVisibility.PRIVATE else ProjectVisibility.PUBLIC
                            onChangeVisibility(p, targetVis)
                        }) {
                            Icon(
                                if (p.isPublic()) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = "Toggle Visibility",
                                tint = if (p.isPublic()) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onDelete(p) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 2: DOCUMENTS
// -------------------------------------------------------------------------
@Composable
fun DocumentsRepositorySection(
    documents: List<DocumentEntity>,
    onDelete: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(documents, key = { it.id }) { d ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(d.customFilename, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Status: ${d.processingStatus} • Method: ${d.extractionMethod}", fontSize = 10.sp, color = PrimaryBlue)
                        Text("Questions: ${d.questionCount} extracted (${d.verifiedCount} verified) • Category: ${d.category}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(d.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 3: EXTRACTED QUESTIONS
// -------------------------------------------------------------------------
@Composable
fun QuestionsReviewSection(
    questions: List<ExtractedQuestionEntity>,
    onApprove: (String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    val filtered = when (statusFilter) {
        "APPROVED" -> questions.filter { it.isVerified() }
        "PENDING" -> questions.filter { !it.isVerified() }
        else -> questions
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = statusFilter == "ALL",
                onClick = { statusFilter = "ALL" },
                label = { Text("All (${questions.size})", fontSize = 10.sp) }
            )
            FilterChip(
                selected = statusFilter == "PENDING",
                onClick = { statusFilter = "PENDING" },
                label = { Text("Pending Review (${questions.count { !it.isVerified() }})", fontSize = 10.sp) }
            )
            FilterChip(
                selected = statusFilter == "APPROVED",
                onClick = { statusFilter = "APPROVED" },
                label = { Text("Approved (${questions.count { it.isVerified() }})", fontSize = 10.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered.take(50), key = { it.id }) { q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Q${q.questionNumber} (${q.section}) • ${q.marks ?: 1}m", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                            Surface(
                                color = if (q.isVerified()) Color(0xFF10B981).copy(alpha = 0.15f) else AcademicGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = q.verificationStatus,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (q.isVerified()) Color(0xFF065F46) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(q.getEffectiveText(), fontSize = 11.sp, maxLines = 2)
                        if (!q.isVerified()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onApprove(q.id) },
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify Question", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 4: SYLLABUS CURRICULA
// -------------------------------------------------------------------------
@Composable
fun SyllabusCurriculaSection(versions: List<SyllabusVersionEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(versions, key = { it.id }) { v ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${v.subject} — ${v.academicSession}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Surface(
                            color = if (v.isVerified()) Color(0xFF10B981).copy(alpha = 0.15f) else AcademicGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(v.verificationStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text("Source: ${v.sourceType} • ${v.sourceTitle}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ID: ${v.versionIdentifier} • Class: ${v.classLevel}", fontSize = 9.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 5: CONCEPT MAPPINGS
// -------------------------------------------------------------------------
@Composable
fun ConceptMappingsSection(mappings: List<QuestionTopicMappingEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(mappings.take(50), key = { it.id }) { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${m.chapterName} > ${m.topicName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                        Text(m.reviewStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Question ID: ${m.questionId} • Confidence: ${String.format(Locale.US, "%.0f%%", m.mappingConfidence * 100)}", fontSize = 10.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 6: AI PRACTICE REVIEW
// -------------------------------------------------------------------------
@Composable
fun PracticeQuestionsQueueSection(questions: List<PracticeQuestionEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(questions.take(50), key = { it.id }) { pq ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Q${pq.questionNumber}: ${pq.questionType} • ${pq.difficulty}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(pq.reviewStatus, fontSize = 9.sp, color = AcademicGold, fontWeight = FontWeight.Bold)
                    }
                    Text(pq.getEffectiveText(), fontSize = 10.sp, maxLines = 2)
                    Text("Chapter: ${pq.chapterName} • Model: ${pq.modelTag}", fontSize = 9.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 7: AI ESTIMATES
// -------------------------------------------------------------------------
@Composable
fun AiEstimatesSection(estimates: List<AiExamEstimateEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(estimates, key = { it.id }) { est ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Session: ${est.subject} (Based on ${est.analyzedPaperCount} papers)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IntelligenceCyan)
                    Text("Years: ${est.analyzedYears}", fontSize = 10.sp)
                    Text(est.disclaimer, fontSize = 9.sp, color = Color(0xFF92400E))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 8: SYSTEM REPORTS
// -------------------------------------------------------------------------
@Composable
fun SystemReportsSection(
    reports: List<GeneratedReportEntity>,
    dateFormat: SimpleDateFormat,
    onDelete: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(reports, key = { it.id }) { rep ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rep.title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Author: ${rep.userEmail} • Format: ${rep.format} • ${rep.fileSizeBytes / 1024} KB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${dateFormat.format(Date(rep.createdAt))} • Visibility: ${if (rep.isPublic) "PUBLIC" else "PRIVATE"}", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = { onDelete(rep.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SECTION 9: AUDIT TRAIL
// -------------------------------------------------------------------------
@Composable
fun AuditTrailSection(
    logs: List<AuditLogEntity>,
    dateFormat: SimpleDateFormat
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(log.action, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Actor: ${log.userEmail} • ${log.resourceType}: ${log.resourceId}", fontSize = 9.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}
