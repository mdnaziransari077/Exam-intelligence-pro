package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiReviewStatus
import com.example.data.model.PracticeAttemptEntity
import com.example.data.model.PracticeDifficulty
import com.example.data.model.PracticeGenerationRequest
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.PracticeQuestionFormat
import com.example.data.model.PracticeQuestionSetEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusNodeType
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.TopicPracticeSuggestion
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPracticeScreen(
    project: ProjectEntity?,
    allSyllabusVersions: List<SyllabusVersionEntity>,
    selectedVersion: SyllabusVersionEntity?,
    syllabusNodes: List<SyllabusNodeEntity>,
    activeSet: PracticeQuestionSetEntity?,
    activeQuestions: List<PracticeQuestionEntity>,
    savedSets: List<PracticeQuestionSetEntity>,
    suggestions: List<TopicPracticeSuggestion>,
    attempts: List<PracticeAttemptEntity>,
    isGenerating: Boolean,
    currentUser: UserEntity?,
    onNavigateBack: () -> Unit,
    onNavigateToEstimates: () -> Unit,
    onGenerateQuestions: (PracticeGenerationRequest) -> Unit,
    onSelectSet: (String) -> Unit,
    onToggleSaveSet: (String, Boolean) -> Unit,
    onRecordAttempt: (questionId: String, setId: String, selectedOption: String, isCorrect: Boolean) -> Unit,
    onReviewQuestion: (questionId: String, newStatus: AiReviewStatus, correctedText: String?, notes: String?) -> Unit,
    onExportSet: (String, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = if (currentUser?.getRoleEnum()?.canApproveQuestions() == true) {
        listOf("Practice Generator", "Evidence Suggestions", "Saved Sets", "Content Review")
    } else {
        listOf("Practice Generator", "Evidence Suggestions", "Saved Sets")
    }

    // Generator Form State
    val chapters = remember(syllabusNodes) {
        syllabusNodes.filter { it.nodeType == SyllabusNodeType.CHAPTER.label }
    }

    var selectedChapter by remember { mutableStateOf<SyllabusNodeEntity?>(chapters.firstOrNull()) }
    val topicsForChapter = remember(selectedChapter, syllabusNodes) {
        if (selectedChapter != null) {
            syllabusNodes.filter { it.parentId == selectedChapter?.id && it.nodeType == SyllabusNodeType.TOPIC.label }
        } else emptyList()
    }

    var selectedTopic by remember { mutableStateOf<SyllabusNodeEntity?>(null) }
    var selectedFormat by remember { mutableStateOf(PracticeQuestionFormat.MCQ) }
    var selectedDifficulty by remember { mutableStateOf(PracticeDifficulty.MEDIUM) }
    var questionCount by remember { mutableIntStateOf(3) }

    var editingQuestion by remember { mutableStateOf<PracticeQuestionEntity?>(null) }

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
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_practice_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AcademicGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Practice Questions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${project?.subject ?: "CBSE Class 10"} • Syllabus Aligned Practice & Insights",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dedicated button to open Separate AI Exam Estimates Module
                Button(
                    onClick = onNavigateToEstimates,
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligenceCyan),
                    modifier = Modifier.testTag("open_ai_estimates_nav_button")
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Exam Estimates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

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

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when (selectedTabIndex) {
                0 -> PracticeGeneratorTab(
                    project = project,
                    selectedVersion = selectedVersion,
                    chapters = chapters,
                    selectedChapter = selectedChapter,
                    onSelectChapter = {
                        selectedChapter = it
                        selectedTopic = null
                    },
                    topics = topicsForChapter,
                    selectedTopic = selectedTopic,
                    onSelectTopic = { selectedTopic = it },
                    selectedFormat = selectedFormat,
                    onSelectFormat = { selectedFormat = it },
                    selectedDifficulty = selectedDifficulty,
                    onSelectDifficulty = { selectedDifficulty = it },
                    questionCount = questionCount,
                    onCountChange = { questionCount = it },
                    isGenerating = isGenerating,
                    onGenerate = {
                        if (project != null && selectedVersion != null && selectedChapter != null) {
                            onGenerateQuestions(
                                PracticeGenerationRequest(
                                    projectId = project.id,
                                    subject = project.subject,
                                    syllabusVersionId = selectedVersion.id,
                                    chapterId = selectedChapter!!.id,
                                    chapterName = selectedChapter!!.name,
                                    topicId = selectedTopic?.id,
                                    topicName = selectedTopic?.name,
                                    format = selectedFormat,
                                    difficulty = selectedDifficulty,
                                    questionCount = questionCount,
                                    syllabusContext = selectedChapter!!.description ?: ""
                                )
                            )
                        } else {
                            Toast.makeText(context, "Please select an active chapter.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    activeSet = activeSet,
                    activeQuestions = activeQuestions,
                    attempts = attempts,
                    onRecordAttempt = onRecordAttempt,
                    onToggleSaveSet = onToggleSaveSet,
                    onExportSet = { setId ->
                        onExportSet(setId) { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Practice questions copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> EvidenceSuggestionsTab(
                    suggestions = suggestions,
                    onPracticeTopic = { sug ->
                        // Pre-populate generator and switch to tab 0
                        val ch = chapters.find { it.id == sug.chapterId }
                        if (ch != null) {
                            selectedChapter = ch
                            selectedTopic = syllabusNodes.find { it.id == sug.topicId }
                            selectedTabIndex = 0
                            Toast.makeText(context, "Loaded topic '${sug.topicName}' into generator.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> SavedSetsTab(
                    savedSets = savedSets,
                    onSelectSet = { setId ->
                        onSelectSet(setId)
                        selectedTabIndex = 0
                    }
                )
                3 -> ContentReviewTab(
                    questions = activeQuestions,
                    currentUser = currentUser,
                    onApprove = { q ->
                        onReviewQuestion(q.id, AiReviewStatus.VERIFIED, null, "Approved by ${currentUser?.email}")
                    },
                    onReject = { q ->
                        onReviewQuestion(q.id, AiReviewStatus.REJECTED, null, "Rejected by ${currentUser?.email}")
                    },
                    onEdit = { q -> editingQuestion = q }
                )
            }
        }
    }

    // Edit Question Dialog
    if (editingQuestion != null) {
        EditPracticeQuestionDialog(
            question = editingQuestion!!,
            onDismiss = { editingQuestion = null },
            onSave = { correctedText, notes ->
                onReviewQuestion(editingQuestion!!.id, AiReviewStatus.EDITED, correctedText, notes)
                editingQuestion = null
                Toast.makeText(context, "Question updated & approved.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =========================================================================
// TAB 0: PRACTICE GENERATOR & INTERACTIVE QUESTIONS
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeGeneratorTab(
    project: ProjectEntity?,
    selectedVersion: SyllabusVersionEntity?,
    chapters: List<SyllabusNodeEntity>,
    selectedChapter: SyllabusNodeEntity?,
    onSelectChapter: (SyllabusNodeEntity) -> Unit,
    topics: List<SyllabusNodeEntity>,
    selectedTopic: SyllabusNodeEntity?,
    onSelectTopic: (SyllabusNodeEntity?) -> Unit,
    selectedFormat: PracticeQuestionFormat,
    onSelectFormat: (PracticeQuestionFormat) -> Unit,
    selectedDifficulty: PracticeDifficulty,
    onSelectDifficulty: (PracticeDifficulty) -> Unit,
    questionCount: Int,
    onCountChange: (Int) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    activeSet: PracticeQuestionSetEntity?,
    activeQuestions: List<PracticeQuestionEntity>,
    attempts: List<PracticeAttemptEntity>,
    onRecordAttempt: (String, String, String, Boolean) -> Unit,
    onToggleSaveSet: (String, Boolean) -> Unit,
    onExportSet: (String) -> Unit
) {
    var isConfigExpanded by remember { mutableStateOf(activeQuestions.isEmpty()) }

    var chapterDropdownExpanded by remember { mutableStateOf(false) }
    var topicDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Configuration Collapsible Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isConfigExpanded = !isConfigExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Practice Generator Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = { isConfigExpanded = !isConfigExpanded }) {
                        Icon(
                            if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }

                if (isConfigExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Chapter Selector Dropdown
                    Text("Select Chapter:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = chapterDropdownExpanded,
                        onExpandedChange = { chapterDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedChapter?.name ?: "Select a Chapter",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("practice_chapter_selector"),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = chapterDropdownExpanded,
                            onDismissRequest = { chapterDropdownExpanded = false }
                        ) {
                            chapters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch.name, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectChapter(ch)
                                        chapterDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topic Selector Dropdown
                    Text("Topic (Optional):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = topicDropdownExpanded,
                        onExpandedChange = { topicDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedTopic?.name ?: "All Topics in Chapter",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("practice_topic_selector"),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = topicDropdownExpanded,
                            onDismissRequest = { topicDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Topics in Chapter", fontSize = 12.sp) },
                                onClick = {
                                    onSelectTopic(null)
                                    topicDropdownExpanded = false
                                }
                            )
                            topics.forEach { top ->
                                DropdownMenuItem(
                                    text = { Text(top.name, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectTopic(top)
                                        topicDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Question Format Chips
                    Text("Question Format:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            PracticeQuestionFormat.MCQ,
                            PracticeQuestionFormat.ASSERTION_REASON,
                            PracticeQuestionFormat.SHORT_ANSWER_1,
                            PracticeQuestionFormat.SHORT_ANSWER_2,
                            PracticeQuestionFormat.LONG_ANSWER,
                            PracticeQuestionFormat.CASE_BASED,
                            PracticeQuestionFormat.MIXED
                        ).forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { onSelectFormat(fmt) },
                                label = { Text(fmt.label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Difficulty Chips
                    Text("Difficulty Level (AI-Estimated):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PracticeDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = selectedDifficulty == diff,
                                onClick = { onSelectDifficulty(diff) },
                                label = { Text(diff.label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Question Count Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Question Count:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$questionCount Questions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                    Slider(
                        value = questionCount.toFloat(),
                        onValueChange = { onCountChange(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth().testTag("practice_count_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Generate Button
                    Button(
                        onClick = onGenerate,
                        enabled = !isGenerating && selectedChapter != null,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("generate_practice_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Questions with Gemini AI...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Original Practice Questions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Questions Display
        if (activeQuestions.isNotEmpty() && activeSet != null) {
            // Set Header & Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = IntelligenceCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "AI-Generated Practice",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0E7490),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Model: ${activeSet.modelTag}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeSet.chapterName} • ${activeSet.questionFormat}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onToggleSaveSet(activeSet.id, !activeSet.isSaved) }) {
                            Icon(
                                if (activeSet.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save Set",
                                tint = PrimaryBlue
                            )
                        }
                        IconButton(onClick = { onExportSet(activeSet.id) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Questions", tint = PrimaryBlue)
                        }
                    }
                }
            }

            // Questions List
            activeQuestions.forEach { q ->
                InteractiveQuestionCard(
                    question = q,
                    userAttempts = attempts.filter { it.questionId == q.id },
                    onAttempt = { opt, isCorrect ->
                        onRecordAttempt(q.id, q.setId, opt, isCorrect)
                    }
                )
            }
        }
    }
}

@Composable
fun InteractiveQuestionCard(
    question: PracticeQuestionEntity,
    userAttempts: List<PracticeAttemptEntity>,
    onAttempt: (String, Boolean) -> Unit
) {
    var isAnswerRevealed by remember { mutableStateOf(false) }
    val latestAttempt = userAttempts.lastOrNull()

    Card(
        modifier = Modifier.fillMaxWidth().testTag("practice_question_card_${question.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Q Number, Type, Marks, Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Q${question.questionNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${question.marks ?: 1} Mark(s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = question.difficulty,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question Text
            Text(
                text = question.getEffectiveText(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // MCQ Options if present
            if (question.getOptionsList().isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                question.getOptionsList().forEach { opt ->
                    val isSelected = latestAttempt?.selectedOption == opt
                    val isCorrectOption = question.correctAnswer?.contains(opt.take(3), ignoreCase = true) == true ||
                            question.correctAnswer?.equals(opt, ignoreCase = true) == true

                    val bgColor = when {
                        isSelected && latestAttempt?.isCorrect == true -> Color(0xFF10B981).copy(alpha = 0.15f)
                        isSelected && latestAttempt?.isCorrect == false -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        isAnswerRevealed && isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    val borderColor = when {
                        isSelected && latestAttempt?.isCorrect == true -> Color(0xFF10B981)
                        isSelected && latestAttempt?.isCorrect == false -> Color(0xFFEF4444)
                        isAnswerRevealed && isCorrectOption -> Color(0xFF10B981)
                        else -> Color.Transparent
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                val isCorrect = question.correctAnswer?.contains(opt.take(3), ignoreCase = true) == true ||
                                        question.correctAnswer?.equals(opt, ignoreCase = true) == true
                                onAttempt(opt, isCorrect)
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                if (latestAttempt?.isCorrect == true) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Answer & Explanation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAnswerRevealed = !isAnswerRevealed }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AcademicGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAnswerRevealed) "Hide Answer & Explanation" else "Reveal Answer & Explanation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Icon(
                    if (isAnswerRevealed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isAnswerRevealed) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (!question.correctAnswer.isNullOrBlank()) {
                            Text(
                                text = "Correct Answer: ${question.correctAnswer}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (!question.explanation.isNullOrBlank()) {
                            Text("Explanation:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(question.explanation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (!question.modelAnswerGuidance.isNullOrBlank()) {
                            Text("Suggested Model Answer Guidance (Not official CBSE marking scheme):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AcademicGold)
                            Text(question.modelAnswerGuidance, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: EVIDENCE-BASED PRACTICE SUGGESTIONS
// =========================================================================

@Composable
fun EvidenceSuggestionsTab(
    suggestions: List<TopicPracticeSuggestion>,
    onPracticeTopic: (TopicPracticeSuggestion) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Educational Caveat Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Evidence-Based Topic Practice Suggestions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "“Practice suggestions are based on the available verified papers and selected syllabus. Historical frequency does not guarantee future examination content.”",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (suggestions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Insufficient verified data to compute suggestions.\nPlease ensure at least one verified question paper is processed and mapped to the syllabus.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            suggestions.forEach { sug ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("suggestion_card_${sug.topicId}"),
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
                                Text(sug.chapterName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sug.topicName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Surface(
                                color = when (sug.category) {
                                    "HIGH_FREQUENCY" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "UNTESTED_GAP" -> AcademicGold.copy(alpha = 0.15f)
                                    else -> IntelligenceCyan.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when (sug.category) {
                                        "HIGH_FREQUENCY" -> "High Weightage"
                                        "UNTESTED_GAP" -> "Curriculum Gap"
                                        else -> "Format Focus"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (sug.category) {
                                        "HIGH_FREQUENCY" -> Color(0xFF065F46)
                                        "UNTESTED_GAP" -> Color(0xFF92400E)
                                        else -> Color(0xFF0E7490)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sug.suggestionRationale,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onPracticeTopic(sug) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Practice This Topic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: SAVED PRACTICE SETS
// =========================================================================

@Composable
fun SavedSetsTab(
    savedSets: List<PracticeQuestionSetEntity>,
    onSelectSet: (String) -> Unit
) {
    if (savedSets.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Saved Practice Sets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("When practicing, tap the bookmark icon to save question sets to your account.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedSets, key = { it.id }) { set ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectSet(set.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(set.chapterName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${set.questionCount} Questions", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Format: ${set.questionFormat} • Difficulty: ${set.difficulty}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: ADMIN CONTENT REVIEW WORKSPACE
// =========================================================================

@Composable
fun ContentReviewTab(
    questions: List<PracticeQuestionEntity>,
    currentUser: UserEntity?,
    onApprove: (PracticeQuestionEntity) -> Unit,
    onReject: (PracticeQuestionEntity) -> Unit,
    onEdit: (PracticeQuestionEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("AI Practice Content Review & Verification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Teacher & Administrator moderation workspace. Review, verify, or edit AI-generated practice questions before formal approval.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (questions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No practice questions in active set to review.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            questions.forEach { q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Q${questionNumberString(q)} • ${q.questionType}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(
                                color = when (q.reviewStatus) {
                                    AiReviewStatus.VERIFIED.label -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    AiReviewStatus.EDITED.label -> PrimaryBlue.copy(alpha = 0.15f)
                                    AiReviewStatus.REJECTED.label -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else -> AcademicGold.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = q.reviewStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (q.reviewStatus) {
                                        AiReviewStatus.VERIFIED.label -> Color(0xFF065F46)
                                        AiReviewStatus.EDITED.label -> PrimaryBlue
                                        AiReviewStatus.REJECTED.label -> Color(0xFF991B1B)
                                        else -> Color(0xFF92400E)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(q.getEffectiveText(), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApprove(q) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onEdit(q) },
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onReject(q) },
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun questionNumberString(q: PracticeQuestionEntity): String = "${q.questionNumber}"

@Composable
fun EditPracticeQuestionDialog(
    question: PracticeQuestionEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var editedText by remember { mutableStateOf(question.getEffectiveText()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Practice Question (Q${question.questionNumber})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Question Text:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )

                Text("Reviewer Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Reason for edit / correction notes...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedText, notes) },
                enabled = editedText.isNotBlank()
            ) {
                Text("Save & Verify")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
