package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.ProcessingStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.UserEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentsScreen(
    documents: List<DocumentEntity>,
    projects: List<ProjectEntity>,
    currentUser: UserEntity?,
    onDeleteDocument: (String) -> Unit,
    onProcessDocument: (String) -> Unit = {},
    onOpenReviewWorkspace: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDocForModal by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToDeleteId by remember { mutableStateOf<String?>(null) }

    val categories = listOf("All") + DocumentCategory.entries.map { it.label }

    val filteredDocs = documents.filter { doc ->
        val matchesSearch = searchQuery.isBlank() ||
                doc.originalFilename.contains(searchQuery, ignoreCase = true) ||
                doc.documentCategory.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || doc.documentCategory == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Document Repository",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "All staged question papers, sample tests, and answer keys across projects.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by filename or category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("documents_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    modifier = Modifier.testTag("doc_filter_${cat.take(8)}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = IntelligenceCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No documents found", fontWeight = FontWeight.Bold)
                    Text("Stage documents in your project workspace.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    val parentProject = projects.find { it.id == doc.projectId }
                    GlobalDocCard(
                        doc = doc,
                        projectName = parentProject?.name ?: "CBSE Project",
                        onProcess = { onProcessDocument(doc.id) },
                        onReview = { onOpenReviewWorkspace(doc.id) },
                        onInspect = { selectedDocForModal = doc },
                        onDelete = { docToDeleteId = doc.id }
                    )
                }
            }
        }
    }

    if (selectedDocForModal != null) {
        DocumentMetadataModal(
            doc = selectedDocForModal!!,
            projectName = projects.find { it.id == selectedDocForModal?.projectId }?.name ?: "Unknown Project",
            onDismiss = { selectedDocForModal = null }
        )
    }

    if (docToDeleteId != null) {
        ConfirmDeleteDialog(
            title = "Delete Document?",
            message = "Are you sure you want to remove this document from persistent storage?",
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
fun GlobalDocCard(
    doc: DocumentEntity,
    projectName: String,
    onProcess: () -> Unit,
    onReview: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val isReadyForReview = doc.processingStatus == ProcessingStatus.AWAITING_REVIEW.label ||
            doc.processingStatus == ProcessingStatus.PARTIALLY_REVIEWED.label ||
            doc.processingStatus == ProcessingStatus.VERIFIED.label

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_item_${doc.id}"),
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.originalFilename,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = "Project: $projectName",
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
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

            if (isReadyForReview) {
                IconButton(onClick = onReview, modifier = Modifier.testTag("global_review_${doc.id}")) {
                    Icon(Icons.Default.FactCheck, contentDescription = "Review Questions", tint = PrimaryBlue)
                }
            } else {
                IconButton(onClick = onProcess, modifier = Modifier.testTag("global_process_${doc.id}")) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Process Document", tint = Color(0xFF10B981))
                }
            }

            IconButton(onClick = onInspect, modifier = Modifier.testTag("inspect_doc_${doc.id}")) {
                Icon(Icons.Default.Info, contentDescription = "Metadata", tint = PrimaryBlue)
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_doc_${doc.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun DocumentMetadataModal(
    doc: DocumentEntity,
    projectName: String,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document System Metadata", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MetadataRow(label = "Filename", value = doc.originalFilename)
                MetadataRow(label = "Document ID", value = doc.id)
                MetadataRow(label = "Project", value = projectName)
                MetadataRow(label = "Category", value = doc.documentCategory)
                MetadataRow(label = "MIME Type", value = doc.mimeType)
                MetadataRow(label = "File Size", value = "${doc.fileSize} bytes (${doc.formattedFileSize()})")
                MetadataRow(label = "Storage Path", value = doc.storageUri)
                MetadataRow(label = "Uploaded At", value = dateFormat.format(Date(doc.uploadTimestamp)))
                MetadataRow(label = "Processing Status", value = doc.processingStatus)
                if (doc.errorDetails != null) {
                    MetadataRow(label = "Error Details", value = doc.errorDetails)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun MetadataRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
