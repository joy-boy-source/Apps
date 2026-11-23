package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameScoreEntity::class,
        StudioImageEntity::class,
        StudySqlLogEntity::class,
        ExerciseSheetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class QuizzySqlDatabase : RoomDatabase() {
    abstract fun quizzySqlDao(): QuizzySqlDao

    companion object {
        @Volatile
        private var INSTANCE: QuizzySqlDatabase? = null

        fun getInstance(context: Context): QuizzySqlDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuizzySqlDatabase::class.java,
                    "quizzy_local_sql.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
