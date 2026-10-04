package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AuditLogDao
import com.example.data.local.UserDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(
    context: Context,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("exam_intel_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    // Valid admin security bootstrap key
    val adminSecurityBootstrapKey = "EXAMINTEL-ADMIN-2026"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialAccountsIfNeeded()
            restoreSavedSession()
        }
    }

    private suspend fun seedInitialAccountsIfNeeded() {
        if (userDao.getUserCount() == 0) {
            val adminUser = UserEntity(
                id = "usr-admin-bootstrap",
                email = "admin@examintel.org",
                passwordHash = hashPassword("admin2026"),
                fullName = "Dr. Ananya Sharma (Admin)",
                role = UserRole.ADMIN.name,
                schoolOrOrg = "Exam Intelligence Academic Board",
                targetExamYear = "2025-2026"
            )

            val studentUser = UserEntity(
                id = "usr-student-demo",
                email = "student@cbse.org",
                passwordHash = hashPassword("cbse10"),
                fullName = "Aarav Patel",
                role = UserRole.STUDENT.name,
                schoolOrOrg = "Delhi Public School, R.K. Puram",
                targetExamYear = "2025"
            )

            val teacherUser = UserEntity(
                id = "usr-teacher-demo",
                email = "teacher@cbse.org",
                passwordHash = hashPassword("cbse10"),
                fullName = "Sunita Verma (Educator)",
                role = UserRole.TEACHER.name,
                schoolOrOrg = "Kendriya Vidyalaya No. 1",
                targetExamYear = "2025"
            )

            userDao.insertUser(adminUser)
            userDao.insertUser(studentUser)
            userDao.insertUser(teacherUser)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = adminUser.id,
                    userEmail = adminUser.email,
                    action = "SYSTEM_INITIALIZED",
                    resourceType = "SYSTEM",
                    resourceId = "BOOTSTRAP",
                    details = "Platform initialized with security roles and seed accounts"
                )
            )
        }
    }

    private suspend fun restoreSavedSession() {
        val savedUserId = prefs.getString("active_user_id", null)
        val guestFlag = prefs.getBoolean("is_guest", false)
        if (guestFlag) {
            _isGuest.value = true
            _currentUser.value = null
        } else if (savedUserId != null) {
            val user = userDao.getUserById(savedUserId)
            if (user != null && user.isActive) {
                _currentUser.value = user
                _isGuest.value = false
            } else {
                prefs.edit().clear().apply()
            }
        }
    }

    suspend fun login(email: String, rawPassword: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            val trimmedEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(trimmedEmail)
                ?: return@withContext Result.failure(Exception("Account not found with this email address"))

            if (!user.isActive) {
                return@withContext Result.failure(Exception("Account has been suspended by an administrator"))
            }

            val inputHash = hashPassword(rawPassword)
            if (inputHash != user.passwordHash) {
                return@withContext Result.failure(Exception("Invalid email or password"))
            }

            prefs.edit().putString("active_user_id", user.id).putBoolean("is_guest", false).apply()
            _currentUser.value = user
            _isGuest.value = false

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "USER_LOGIN",
                    resourceType = "USER",
                    resourceId = user.id,
                    details = "User logged in with role: ${user.role}"
                )
            )

            Result.success(user)
        }

    suspend fun register(
        email: String,
        rawPassword: String,
        fullName: String,
        role: UserRole,
        adminCode: String?,
        schoolOrOrg: String,
        targetExamYear: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(Exception("Please enter a valid email address"))
        }

        if (rawPassword.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters long"))
        }

        if (fullName.isBlank()) {
            return@withContext Result.failure(Exception("Full name cannot be blank"))
        }

        // Server-enforced role escalation security check
        if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) {
            if (adminCode.isNullOrBlank() || adminCode.trim() != adminSecurityBootstrapKey) {
                return@withContext Result.failure(
                    Exception("Security check failed: Valid Admin Invitation Code is required to create an Admin account.")
                )
            }
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext Result.failure(Exception("An account with this email already exists"))
        }

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            email = trimmedEmail,
            passwordHash = hashPassword(rawPassword),
            fullName = fullName.trim(),
            role = role.name,
            schoolOrOrg = schoolOrOrg.trim().ifBlank { "CBSE Affiliated School" },
            targetExamYear = targetExamYear.trim().ifBlank { "2025-2026" },
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        userDao.insertUser(newUser)
        prefs.edit().putString("active_user_id", newUser.id).putBoolean("is_guest", false).apply()
        _currentUser.value = newUser
        _isGuest.value = false

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                userId = newUser.id,
                userEmail = newUser.email,
                action = "USER_REGISTERED",
                resourceType = "USER",
                resourceId = newUser.id,
                details = "Registered new account with role: ${newUser.role}"
            )
        )

        Result.success(newUser)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val trimmedEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(trimmedEmail)
                ?: return@withContext Result.failure(Exception("No account registered with this email"))

            if (newPassword.length < 6) {
                return@withContext Result.failure(Exception("Password must be at least 6 characters"))
            }

            val updated = user.copy(passwordHash = hashPassword(newPassword))
            userDao.updateUser(updated)

            auditLogDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userEmail = user.email,
                    action = "PASSWORD_RESET",
                    resourceType = "USER",
                    resourceId = user.id,
                    details = "Password was reset for user ${user.email}"
                )
            )

            Result.success(Unit)
        }

    fun continueAsGuest() {
        prefs.edit().putBoolean("is_guest", true).remove("active_user_id").apply()
        _isGuest.value = true
        _currentUser.value = null
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
        _isGuest.value = false
    }

    private fun hashPassword(password: String): String {
        val salt = "ExamIntelligence2026SecretSalt"
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
