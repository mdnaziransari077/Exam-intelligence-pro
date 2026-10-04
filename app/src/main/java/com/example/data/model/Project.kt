package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProjectVisibility {
    PRIVATE,
    PUBLIC
}

enum class ProjectStatus(val label: String) {
    SETUP("Setup"),
    DOCUMENTS_ADDED("Documents Added"),
    READY_FOR_REVIEW("Ready for Review"),
    ANALYSIS_NOT_STARTED("Analysis Not Started");

    companion object {
        fun fromLabel(label: String): ProjectStatus {
            return entries.find { it.label.equals(label, ignoreCase = true) } ?: SETUP
        }
    }
}

val CBSE_CLASS_10_SUBJECTS = listOf(
    "Mathematics (Standard)",
    "Mathematics (Basic)",
    "Science (086)",
    "Social Science (087)",
    "English Language & Literature (184)",
    "Hindi Course-A (002)",
    "Hindi Course-B (085)",
    "Computer Applications (165)",
    "Information Technology (402)"
)

val ACADEMIC_YEARS = listOf(
    "2025-2026",
    "2024-2025",
    "2023-2024",
    "2022-2023"
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val name: String,
    val board: String = "CBSE",
    val className: String = "Class 10",
    val subject: String,
    val academicYear: String = "2024-2025",
    val description: String = "",
    val visibility: String = ProjectVisibility.PRIVATE.name,
    val status: String = ProjectStatus.SETUP.label,
    val documentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isPrivate(): Boolean = visibility == ProjectVisibility.PRIVATE.name
    fun isPublic(): Boolean = visibility == ProjectVisibility.PUBLIC.name
}
