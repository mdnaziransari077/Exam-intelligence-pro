package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    GUEST,
    STUDENT,
    TEACHER,
    ADMIN,
    SUPER_ADMIN;

    fun displayName(): String = when (this) {
        GUEST -> "Guest Explorer"
        STUDENT -> "Student"
        TEACHER -> "Teacher / Educator"
        ADMIN -> "Platform Admin"
        SUPER_ADMIN -> "Super Administrator"
    }

    fun canManageAllProjects(): Boolean = this == ADMIN || this == SUPER_ADMIN
    fun canAccessAdminArea(): Boolean = this == ADMIN || this == SUPER_ADMIN
    fun canManageSyllabus(): Boolean = this == TEACHER || this == ADMIN || this == SUPER_ADMIN
    fun canApproveSyllabus(): Boolean = this == TEACHER || this == ADMIN || this == SUPER_ADMIN
    fun canApproveQuestions(): Boolean = this == TEACHER || this == ADMIN || this == SUPER_ADMIN
    fun canReviewQuestions(): Boolean = this == TEACHER || this == ADMIN || this == SUPER_ADMIN
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // from UserRole.name
    val schoolOrOrg: String = "CBSE Affiliated School",
    val targetExamYear: String = "2025-2026",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun getRoleEnum(): UserRole = try {
        UserRole.valueOf(role)
    } catch (_: Exception) {
        UserRole.STUDENT
    }
}
