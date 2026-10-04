package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TopicEstimateItem
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.IntelligenceCyan
import com.example.ui.theme.PrimaryBlue
import org.json.JSONArray

@Composable
fun AiEstimatesScreen(
    project: ProjectEntity?,
    latestEstimate: AiExamEstimateEntity?,
    isGenerating: Boolean,
    onNavigateBack: () -> Unit,
    onGenerateEstimates: () -> Unit,
    onPracticeTopic: (String, String) -> Unit, // topicId, chapterName
    modifier: Modifier = Modifier
) {
    val estimateItems = remember(latestEstimate) {
        val list = mutableListOf<TopicEstimateItem>()
        if (latestEstimate != null) {
            try {
                val arr = JSONArray(latestEstimate.estimatesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        TopicEstimateItem(
                            topicId = obj.optString("topicId", ""),
                            topicName = obj.optString("topicName", ""),
                            chapterName = obj.optString("chapterName", ""),
                            attentionLevel = obj.optString("attentionLevel", "Recommended Focus"),
                            rationale = obj.optString("rationale", ""),
                            historicalFrequencyNotice = obj.optString("historicalFrequencyNotice", ""),
                            syllabusStatus = obj.optString("syllabusStatus", "Official Prescribed Topic"),
                            caveat = obj.optString("caveat", "")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // App Bar
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
                        modifier = Modifier.testTag("ai_estimates_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IntelligenceCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Exam Estimates & Revision Focus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${project?.subject ?: "CBSE Class 10"} • Tentative Practice-Oriented Trend Insights",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onGenerateEstimates,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = IntelligenceCyan),
                    modifier = Modifier.testTag("refresh_ai_estimates_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-estimate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mandatory Disclaimer & Isolation Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = IntelligenceCyan.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, IntelligenceCyan.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0E7490), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Estimate — Not an Official Prediction",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0E7490)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This module provides tentative, qualitative revision guidance synthesized from verified historical papers and the official CBSE syllabus. It does NOT guarantee question appearance or marks. The CBSE board examination may test any topic in the official prescribed syllabus.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Estimate Status & Scope Info
            if (latestEstimate != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Evidence Basis & Analysis Scope",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Analysed Papers: ${latestEstimate.analyzedPaperCount} eligible verified papers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Examination Sessions: ${latestEstimate.analyzedYears}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Limitations: ${latestEstimate.dataLimitations}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Content List
            if (isGenerating) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IntelligenceCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Synthesizing curriculum patterns & generating estimates...", fontSize = 12.sp)
                    }
                }
            } else if (estimateItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IntelligenceCyan, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No AI Exam Estimates Generated", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Re-estimate' above to synthesize tentative practice recommendations based on verified historical papers and syllabus coverage.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "Tentative Recommended Focus Areas (${estimateItems.size} Topics)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                estimateItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("estimate_item_${item.topicId}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.chapterName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.topicName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Surface(
                                    color = when {
                                        item.attentionLevel.contains("High", true) -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        item.attentionLevel.contains("Gap", true) -> AcademicGold.copy(alpha = 0.15f)
                                        else -> IntelligenceCyan.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.attentionLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            item.attentionLevel.contains("High", true) -> Color(0xFF065F46)
                                            item.attentionLevel.contains("Gap", true) -> Color(0xFF92400E)
                                            else -> Color(0xFF0E7490)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.rationale, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Evidence: ${item.historicalFrequencyNotice}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Caveat: ${item.caveat}",
                                fontSize = 10.sp,
                                color = AcademicGold,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onPracticeTopic(item.topicId, item.chapterName) },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate Practice for This Topic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
