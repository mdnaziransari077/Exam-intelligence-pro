package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userEmail: String,
    val action: String, // e.g. "USER_REGISTERED", "PROJECT_CREATED", "DOCUMENT_UPLOADED", "PROJECT_DELETED", "ROLE_CHANGED"
    val resourceType: String, // "USER", "PROJECT", "DOCUMENT", "SYSTEM"
    val resourceId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
