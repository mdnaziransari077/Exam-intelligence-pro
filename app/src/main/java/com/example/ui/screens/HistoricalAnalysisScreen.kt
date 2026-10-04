package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisScope
import com.example.data.model.ChapterHistoricalMetrics
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.HistoricalAnalysisResult
import com.example.data.model.HistoricalTopicMetrics
import com.example.data.model.MappingVerificationScope
import com.example.data.model.MarksAttributionMethod
import com.example.data.model.ProjectEntity
import com.example.data.model.QuestionTypeMetrics
import com.example.data.model.QuestionVerificationScope
import com.example.data.model.ScopeSummary
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.UserEntity
import com.example.data.model.YearComparisonMetrics
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalAnalysisScreen(
    project: ProjectEntity?,
    allSyllabusVersions: List<SyllabusVersionEntity>,
    selectedVersion: SyllabusVersionEntity?,
    analysisScope: AnalysisScope?,
    scopeSummary: ScopeSummary?,
    analysisResult: HistoricalAnalysisResult?,
    isAnalyzing: Boolean,
    selectedTopicDetail: HistoricalTopicMetrics?,
    topicAssociatedQuestions: List<ExtractedQuestionEntity>,
    currentUser: UserEntity?,
    onNavigateBack: () -> Unit,
    onSelectSyllabusVersion: (String) -> Unit,
    onUpdateQuestionScope: (QuestionVerificationScope) -> Unit,
    onUpdateMappingScope: (MappingVerificationScope) -> Unit,
    onUpdateAttributionMethod: (MarksAttributionMethod) -> Unit,
    onExecuteAnalysis: () -> Unit,
    onOpenTopicDetail: (HistoricalTopicMetrics) -> Unit,
    onCloseTopicDetail: () -> Unit,
    onExportReport: (String, (String) -> Unit) -> Unit,
    onPracticeTopic: (String, String) -> Unit = { _, _ -> },
    onNavigateToPractice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMethodologyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showScopeSettingsDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf(
        "Topic Frequency",
        "Marks Distribution",
        "Question Formats",
        "Year & Set Comparison",
        "Data Quality & Integrity"
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // App Bar / Top Navigation Row
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
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("historical_analysis_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Historical Question Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${project?.subject ?: "CBSE Class 10"} • Topic Frequency & Marks Distribution",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onNavigateToPractice,
                        modifier = Modifier.testTag("historical_to_practice_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Practice Generator",
                            tint = AcademicGold
                        )
                    }

                    IconButton(
                        onClick = { showMethodologyDialog = true },
                        modifier = Modifier.testTag("methodology_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Methodology & Counting Rules",
                            tint = PrimaryBlue
                        )
                    }

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export Analysis Report",
                            tint = PrimaryBlue
                        )
                    }
                }
            }
        }

        // Scope Summary & Recalculate Controls Header
        ScopeSummaryCard(
            scope = analysisScope,
            summary = scopeSummary,
            result = analysisResult,
            isAnalyzing = isAnalyzing,
            onOpenSettings = { showScopeSettingsDialog = true },
            onExecute = onExecuteAnalysis
        )

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue
        ) {
            tabTitles.forEachIndexed { index, title ->
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

        // Tab Contents
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing question corpus & attributing marks...",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Applying multi-topic allocation & distinct paper counts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (analysisResult == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Historical Analysis Not Run Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Run analysis on your eligible CBSE Class 10 questions to calculate chapter frequencies, topic weightages, and paper patterns.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExecuteAnalysis,
                            modifier = Modifier.testTag("run_initial_analysis_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Historical Analysis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                when (selectedTabIndex) {
                    0 -> TopicFrequencyTab(
                        result = analysisResult,
                        onOpenTopicDetail = onOpenTopicDetail
                    )
                    1 -> MarksDistributionTab(
                        result = analysisResult,
                        onOpenTopicDetail = onOpenTopicDetail
                    )
                    2 -> QuestionFormatsTab(
                        result = analysisResult
                    )
                    3 -> YearComparisonTab(
                        result = analysisResult,
                        onOpenTopicDetail = onOpenTopicDetail
                    )
                    4 -> DataQualityTab(
                        result = analysisResult
                    )
                }
            }
        }
    }

    // Topic Details Modal / BottomSheet
    if (selectedTopicDetail != null) {
        TopicDetailDialog(
            topic = selectedTopicDetail,
            associatedQuestions = topicAssociatedQuestions,
            attributionMethod = analysisScope?.attributionMethod ?: MarksAttributionMethod.FULL_OVERLAPPING,
            onPracticeTopic = onPracticeTopic,
            onDismiss = onCloseTopicDetail
        )
    }

    // Methodology & Counting Rules Dialog
    if (showMethodologyDialog) {
        MethodologyRulesDialog(
            onDismiss = { showMethodologyDialog = false }
        )
    }

    // Scope Settings & Filters Dialog
    if (showScopeSettingsDialog) {
        ScopeSettingsDialog(
            scope = analysisScope,
            allVersions = allSyllabusVersions,
            selectedVersion = selectedVersion,
            onSelectVersion = onSelectSyllabusVersion,
            onUpdateQuestionScope = onUpdateQuestionScope,
            onUpdateMappingScope = onUpdateMappingScope,
            onUpdateAttributionMethod = onUpdateAttributionMethod,
            onDismiss = { showScopeSettingsDialog = false }
        )
    }

    // Export Report Dialog
    if (showExportDialog && analysisResult != null) {
        ExportHistoricalReportDialog(
            result = analysisResult,
            onDismiss = { showExportDialog = false },
            onCopy = { content ->
                clipboardManager.setText(AnnotatedString(content))
                Toast.makeText(context, "Historical analysis report copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =========================================================================
// SCOPE SUMMARY & CONTROL BANNER
// =========================================================================

@Composable
fun ScopeSummaryCard(
    scope: AnalysisScope?,
    summary: ScopeSummary?,
    result: HistoricalAnalysisResult?,
    isAnalyzing: Boolean,
    onOpenSettings: () -> Unit,
    onExecute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Analysis Scope & Corpus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Syllabus: ${summary?.syllabusVersionName ?: "Active CBSE Curriculum"} • ${scope?.attributionMethod?.label ?: "Full Overlapping"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("scope_settings_button")
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filters", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onExecute,
                        enabled = !isAnalyzing,
                        modifier = Modifier.testTag("recalculate_analysis_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalculate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Corpus Key Metrics Badges
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricChip("Papers", "${summary?.totalDocuments ?: 0}", PrimaryBlue)
                MiniMetricChip("Verified Questions", "${summary?.verifiedQuestionsCount ?: 0}", Color(0xFF10B981))
                MiniMetricChip("Verified Mappings", "${summary?.questionsWithVerifiedMappingsCount ?: 0}", PrimaryBlue)
                if ((summary?.questionsWithUnknownMarksCount ?: 0) > 0) {
                    MiniMetricChip("Unknown Marks", "${summary?.questionsWithUnknownMarksCount}", AcademicGold)
                }
                if ((summary?.questionsAwaitingReviewCount ?: 0) > 0) {
                    MiniMetricChip("Awaiting Review", "${summary?.questionsAwaitingReviewCount}", Color(0xFFF59E0B))
                }
            }

            // Quality Badges
            if (result?.dataQualityReport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.dataQualityReport.qualityBadges.forEach { badge ->
                        Surface(
                            color = when (badge) {
                                "Verified Historical Data" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "Partial Collection" -> AcademicGold.copy(alpha = 0.15f)
                                "Exploratory AI Suggestions" -> IntelligenceCyan.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (badge) {
                                    "Verified Historical Data" -> Color(0xFF065F46)
                                    "Partial Collection" -> Color(0xFF92400E)
                                    "Exploratory AI Suggestions" -> Color(0xFF0E7490)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMetricChip(label: String, value: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

// =========================================================================
// TAB 0: TOPIC FREQUENCY
// =========================================================================

@Composable
fun TopicFrequencyTab(
    result: HistoricalAnalysisResult,
    onOpenTopicDetail: (HistoricalTopicMetrics) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedChapterFilter by remember { mutableStateOf("All Chapters") }
    var showOnlyTested by remember { mutableStateOf(false) }

    val distinctChapters = remember(result.chapterMetrics) {
        listOf("All Chapters") + result.chapterMetrics.map { it.chapterName }
    }

    val filteredTopics = result.topicMetrics.filter { t ->
        val matchesSearch = searchQuery.isBlank() ||
                t.topicName.contains(searchQuery, ignoreCase = true) ||
                t.chapterName.contains(searchQuery, ignoreCase = true)
        val matchesChapter = selectedChapterFilter == "All Chapters" || t.chapterName == selectedChapterFilter
        val matchesTested = !showOnlyTested || t.questionAppearancesCount > 0
        matchesSearch && matchesChapter && matchesTested
    }.sortedByDescending { it.questionAppearancesCount }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search topics...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).testTag("topic_frequency_search_input"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chapter Chips Filter
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

        Spacer(modifier = Modifier.height(8.dp))

        // Chapter Frequency Visualizer Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Chapter-Wise Appearance Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxAppearances = maxOf(1, result.chapterMetrics.maxOfOrNull { it.totalQuestionAppearances } ?: 1)

                result.chapterMetrics.forEach { ch ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ch.chapterName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ch.totalQuestionAppearances} Qs (${ch.distinctPaperCount} Papers) • ${String.format(Locale.US, "%.1f", ch.attributedMarks)}m",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (ch.totalQuestionAppearances.toFloat() / maxAppearances).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Topic-Wise List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Prescribed Topics (${filteredTopics.size} Topics)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            FilterChip(
                selected = showOnlyTested,
                onClick = { showOnlyTested = !showOnlyTested },
                label = { Text("Tested Only", fontSize = 10.sp) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Topic Items
        if (filteredTopics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No topics matching filters.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTopics, key = { it.topicId }) { topic ->
                    TopicFrequencyCard(
                        topic = topic,
                        onClick = { onOpenTopicDetail(topic) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopicFrequencyCard(
    topic: HistoricalTopicMetrics,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("topic_frequency_card_${topic.topicId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.chapterName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = topic.topicName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (topic.questionAppearancesCount > 0) Color(0xFF10B981).copy(alpha = 0.15f)
                    else if (topic.isExcludedFromSyllabus) Color(0xFF8B5CF6).copy(alpha = 0.15f)
                    else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (topic.isExcludedFromSyllabus) "Excluded"
                        else if (topic.questionAppearancesCount > 0) "${topic.questionAppearancesCount} Questions"
                        else "Untested in Sample",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (topic.isExcludedFromSyllabus) Color(0xFF6D28D9)
                        else if (topic.questionAppearancesCount > 0) Color(0xFF065F46)
                        else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Distinct Papers, Years, Attributed Marks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Distinct Papers: ${topic.distinctPaperCount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Exam Years: ${if (topic.distinctYears.isEmpty()) "None" else topic.distinctYears.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Attributed Marks: ${String.format(Locale.US, "%.1f", topic.attributedMarks)}m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            if (topic.frequencyByQuestionType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topic.frequencyByQuestionType.forEach { (type, count) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$type: $count",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: MARKS DISTRIBUTION
// =========================================================================

@Composable
fun MarksDistributionTab(
    result: HistoricalAnalysisResult,
    onOpenTopicDetail: (HistoricalTopicMetrics) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Attribution Methodology Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Attribution Method: ${result.scope.attributionMethod.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.scope.attributionMethod.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Important: Overlapping topic marks attribute the full question mark to each mapped concept. Topic totals cannot be added together to infer an overall paper total.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Attributed Marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.1f", result.totalAttributedMarks)}m", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                    Text("Across all mapped topics", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Printed Marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${result.totalPrintedMarksAvailable}m", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                    Text("Sum of printed questions", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Candidate Marks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${result.candidateAvailableMarksEstimate}m", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AcademicGold)
                    Text("Internal choice adjusted", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Chapter Marks Distribution Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Marks Weightage by Chapter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val maxMarks = maxOf(1.0, result.chapterMetrics.maxOfOrNull { it.attributedMarks } ?: 1.0)

                result.chapterMetrics.sortedByDescending { it.attributedMarks }.forEach { ch ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ch.chapterName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", ch.attributedMarks)} Marks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (ch.attributedMarks / maxMarks).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Top Topics by Mark Weightage Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Highest Weightage Topics in Analysed Papers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val topTopics = result.topicMetrics.filter { it.attributedMarks > 0 }
                    .sortedByDescending { it.attributedMarks }
                    .take(8)

                topTopics.forEachIndexed { idx, t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTopicDetail(t) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${idx + 1}.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = t.topicName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${t.chapterName} • ${t.distinctPaperCount} Papers",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${String.format(Locale.US, "%.1f", t.attributedMarks)}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    if (idx < topTopics.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: QUESTION FORMATS
// =========================================================================

@Composable
fun QuestionFormatsTab(
    result: HistoricalAnalysisResult
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Question Format Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Observed frequencies across official CBSE Class 10 question structures",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                result.questionTypeMetrics.sortedByDescending { it.questionCount }.forEach { qType ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = qType.questionType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${qType.questionCount} Questions (${String.format(Locale.US, "%.1f%%", qType.percentageOfTotalQuestions)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Distinct Papers: ${qType.distinctPaperCount}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Attributed Marks: ${String.format(Locale.US, "%.1f", qType.totalAttributedMarks)}m",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: YEAR & SET COMPARISON
// =========================================================================

@Composable
fun YearComparisonTab(
    result: HistoricalAnalysisResult,
    onOpenTopicDetail: (HistoricalTopicMetrics) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Year-wise Pattern Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Examination Year Comparison",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                result.yearComparisonMetrics.forEach { ym ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Session / Year: ${ym.year}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "${ym.papersCount} Papers • ${ym.questionsCount} Questions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chapters Tested: ${ym.chaptersCoveredCount} • Topics Tested: ${ym.topicsCoveredCount} • Marks: ${String.format(Locale.US, "%.0f", ym.totalAttributedMarks)}m",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ym.topChapters.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Prominent Chapters: ${ym.topChapters.joinToString(", ")}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Untested Topics Section (with Educational Disclaimer)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Untested Topics in Analysed Collection (${result.untestedTopicsCount})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mandatory Educational Disclaimer
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Methodological Disclaimer: Absence from this specific sample collection does NOT mean a topic was omitted from the official CBSE syllabus or will not be tested in future examinations. Students must prepare all prescribed syllabus topics thoroughly.",
                        fontSize = 10.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                result.untestedTopicsList.forEach { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTopicDetail(topic) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = topic.chapterName, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = topic.topicName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Untested", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: DATA QUALITY & INTEGRITY
// =========================================================================

@Composable
fun DataQualityTab(
    result: HistoricalAnalysisResult
) {
    val report = result.dataQualityReport

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quality Guarantees Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Data Integrity & Verification Completeness",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                CompletenessMeter("Question Verification", report.questionVerificationCompletionPercent)
                CompletenessMeter("Concept Mapping Verification", report.mappingVerificationCompletionPercent)
                CompletenessMeter("Marks Verification", report.marksVerificationCompletionPercent)
            }
        }

        // Duplicate Document Detector
        if (report.suspectedDuplicateDocs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Suspected Duplicate Documents (${report.suspectedDuplicateDocs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    report.suspectedDuplicateDocs.forEach { dup ->
                        Text(
                            text = "• '${dup.doc1Name}' matches '${dup.doc2Name}': ${dup.reason}",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }

        // Methodological Phase Boundaries Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Methodological Guarantees (Phase 4)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Strict Evidence Basis: All statistics originate from verified stored questions and syllabus nodes. No counts or marks are fabricated.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "2. Distinct Paper vs Appearance Distinction: Questions appearing multiple times in a paper count as 1 paper occurrence.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "3. Multi-Topic Non-Additivity: Overlapping topic marks are explicitly isolated and never summed into artificial paper totals.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "4. No Predictive Guarantees: Descriptive historical frequencies do NOT predict or guarantee upcoming CBSE board examination patterns.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CompletenessMeter(title: String, percentage: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = "${String.format(Locale.US, "%.1f%%", percentage)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// =========================================================================
// DIALOGS: TOPIC DETAIL, METHODOLOGY, SCOPE SETTINGS, EXPORT
// =========================================================================

@Composable
fun TopicDetailDialog(
    topic: HistoricalTopicMetrics,
    associatedQuestions: List<ExtractedQuestionEntity>,
    attributionMethod: MarksAttributionMethod,
    onPracticeTopic: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = topic.chapterName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = topic.topicName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMetricChip("Appearances", "${topic.questionAppearancesCount}", PrimaryBlue)
                    MiniMetricChip("Papers", "${topic.distinctPaperCount}", Color(0xFF10B981))
                    MiniMetricChip("Marks", "${String.format(Locale.US, "%.1f", topic.attributedMarks)}m", AcademicGold)
                }

                HorizontalDivider()

                Text(
                    text = "Source Questions (${associatedQuestions.size} Traced Records)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                if (associatedQuestions.isEmpty()) {
                    Text(
                        text = "No questions found for this topic in the currently analyzed collection.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    associatedQuestions.forEach { q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Q${q.questionNumber} • ${q.section}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PrimaryBlue
                                    )
                                    Surface(
                                        color = PrimaryBlue.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (q.marks != null) "${q.marks} Marks" else "Unknown Marks",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.getEffectiveText(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Format: ${q.questionType} • Page ${q.sourcePage} • Status: ${q.verificationStatus}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDismiss()
                        onPracticeTopic(topic.topicId, topic.chapterName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Practice with AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        }
    )
}

@Composable
fun MethodologyRulesDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Phase 4 Methodology & Counting Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleItem("Rule A: Verified Question Frequency", "Only questions whose identity, text, and syllabus mappings are verified are counted in confirmed statistics.")
                RuleItem("Rule B: Distinct Paper Frequency", "A topic appearing 5 times in 1 paper counts as 1 distinct paper, while question frequency is 5.")
                RuleItem("Rule C: Repeated Question Deduplication", "Questions with identical wording across different years or paper sets are tracked to distinguish genuine repeated appearances from unique wording.")
                RuleItem("Rule D: Duplicate Document Detector", "Identical documents uploaded multiple times are flagged to prevent artificial inflation of question frequencies.")
                RuleItem("Rule E: Parent & Sub-Question Counting", "Child question marks are never added to parent question marks if the parent total already encompasses them.")
                RuleItem("Rule F: Internal Choices", "Mutually exclusive alternatives (e.g. OR questions) are not summed as if both were answered. Candidate available marks are tracked separately from printed marks.")
                RuleItem("Rule G: Multi-Topic Attribution", "Under Full Overlapping attribution, each topic receives the full question marks (clearly labelled non-additive). Under Fractional Split, marks are divided equally (Marks / N).")
                RuleItem("Rule H: Unknown Values Preservation", "Missing marks or unverified question types are never guessed or inferred. They are explicitly isolated in data quality reports.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

@Composable
fun RuleItem(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
        Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ScopeSettingsDialog(
    scope: AnalysisScope?,
    allVersions: List<SyllabusVersionEntity>,
    selectedVersion: SyllabusVersionEntity?,
    onSelectVersion: (String) -> Unit,
    onUpdateQuestionScope: (QuestionVerificationScope) -> Unit,
    onUpdateMappingScope: (MappingVerificationScope) -> Unit,
    onUpdateAttributionMethod: (MarksAttributionMethod) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Analysis Scope & Filters", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Question Verification Scope:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scope?.questionVerificationScope == QuestionVerificationScope.VERIFIED_ONLY,
                        onClick = { onUpdateQuestionScope(QuestionVerificationScope.VERIFIED_ONLY) },
                        label = { Text("Verified Only", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scope?.questionVerificationScope == QuestionVerificationScope.ALL_EXTRACTIONS,
                        onClick = { onUpdateQuestionScope(QuestionVerificationScope.ALL_EXTRACTIONS) },
                        label = { Text("All Extractions", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Concept Mapping Scope:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scope?.mappingVerificationScope == MappingVerificationScope.APPROVED_ONLY,
                        onClick = { onUpdateMappingScope(MappingVerificationScope.APPROVED_ONLY) },
                        label = { Text("Approved Only", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scope?.mappingVerificationScope == MappingVerificationScope.INCLUDE_AI_SUGGESTIONS,
                        onClick = { onUpdateMappingScope(MappingVerificationScope.INCLUDE_AI_SUGGESTIONS) },
                        label = { Text("Include AI Suggestions", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Multi-Topic Marks Attribution:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scope?.attributionMethod == MarksAttributionMethod.FULL_OVERLAPPING,
                        onClick = { onUpdateAttributionMethod(MarksAttributionMethod.FULL_OVERLAPPING) },
                        label = { Text("Overlapping", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scope?.attributionMethod == MarksAttributionMethod.FRACTIONAL_SPLIT,
                        onClick = { onUpdateAttributionMethod(MarksAttributionMethod.FRACTIONAL_SPLIT) },
                        label = { Text("Fractional Split", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        }
    )
}

@Composable
fun ExportHistoricalReportDialog(
    result: HistoricalAnalysisResult,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    var exportFormat by remember { mutableStateOf("markdown") }

    val reportText = remember(exportFormat, result) {
        when (exportFormat) {
            "markdown" -> buildMarkdownExport(result)
            "csv" -> buildCsvExport(result)
            "json" -> buildJsonExport(result)
            else -> buildMarkdownExport(result)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Historical Analysis Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = exportFormat == "markdown",
                        onClick = { exportFormat = "markdown" },
                        label = { Text("Markdown", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exportFormat == "csv",
                        onClick = { exportFormat = "csv" },
                        label = { Text("CSV", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exportFormat == "json",
                        onClick = { exportFormat = "json" },
                        label = { Text("JSON", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy(reportText)
                    onDismiss()
                },
                modifier = Modifier.testTag("copy_historical_report_button")
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

private fun buildMarkdownExport(r: HistoricalAnalysisResult): String = buildString {
    appendLine("# CBSE Class 10 Historical Question Analysis Report")
    appendLine("- **Subject:** ${r.scope.subject}")
    appendLine("- **Syllabus:** ${r.scopeSummary.syllabusVersionName}")
    appendLine("- **Analyzed Papers:** ${r.totalEligiblePapers}")
    appendLine("- **Verified Questions:** ${r.totalVerifiedQuestions}")
    appendLine("- **Attribution:** ${r.scope.attributionMethod.label}")
    appendLine()
    appendLine("## Chapter Summary")
    appendLine("| Chapter | Appearances | Distinct Papers | Attributed Marks |")
    appendLine("|---|:---:|:---:|:---:|")
    r.chapterMetrics.forEach { c ->
        appendLine("| ${c.chapterName} | ${c.totalQuestionAppearances} | ${c.distinctPaperCount} | ${String.format(Locale.US, "%.1f", c.attributedMarks)}m |")
    }
}

private fun buildCsvExport(r: HistoricalAnalysisResult): String = buildString {
    appendLine("Chapter,Topic,Appearances,Distinct Papers,Attributed Marks,Status")
    r.topicMetrics.forEach { t ->
        appendLine("\"${t.chapterName.replace("\"", "\"\"")}\",\"${t.topicName.replace("\"", "\"\"")}\",${t.questionAppearancesCount},${t.distinctPaperCount},${t.attributedMarks},${if (t.isExcludedFromSyllabus) "Excluded" else if (t.questionAppearancesCount > 0) "Tested" else "Untested"}")
    }
}

private fun buildJsonExport(r: HistoricalAnalysisResult): String {
    val obj = org.json.JSONObject().apply {
        put("subject", r.scope.subject)
        put("totalPapers", r.totalEligiblePapers)
        put("totalVerifiedQuestions", r.totalVerifiedQuestions)
        put("totalAttributedMarks", r.totalAttributedMarks)
    }
    return obj.toString(2)
}
