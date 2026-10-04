package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AiExamEstimateEntity
import com.example.data.model.AiReviewLogEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.ExtractedQuestionEntity
import com.example.data.model.GeneratedReportEntity
import com.example.data.model.HistoricalAnalysisRunEntity
import com.example.data.model.MappingReviewLogEntity
import com.example.data.model.PracticeAttemptEntity
import com.example.data.model.PracticeQuestionEntity
import com.example.data.model.PracticeQuestionSetEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.QuestionReviewLogEntity
import com.example.data.model.QuestionTopicMappingEntity
import com.example.data.model.SyllabusNodeEntity
import com.example.data.model.SyllabusVersionEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        DocumentEntity::class,
        AuditLogEntity::class,
        ExtractedQuestionEntity::class,
        QuestionReviewLogEntity::class,
        SyllabusVersionEntity::class,
        SyllabusNodeEntity::class,
        QuestionTopicMappingEntity::class,
        MappingReviewLogEntity::class,
        HistoricalAnalysisRunEntity::class,
        PracticeQuestionSetEntity::class,
        PracticeQuestionEntity::class,
        PracticeAttemptEntity::class,
        AiReviewLogEntity::class,
        AiExamEstimateEntity::class,
        GeneratedReportEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ExamDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun documentDao(): DocumentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun extractedQuestionDao(): ExtractedQuestionDao
    abstract fun questionReviewLogDao(): QuestionReviewLogDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun questionTopicMappingDao(): QuestionTopicMappingDao
    abstract fun historicalAnalysisDao(): HistoricalAnalysisDao
    abstract fun practiceQuestionDao(): PracticeQuestionDao
    abstract fun aiExamEstimateDao(): AiExamEstimateDao
    abstract fun generatedReportDao(): GeneratedReportDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        fun getInstance(context: Context): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_intelligence_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
