package com.aiassistant.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 2
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN currentRequestTokens INTEGER")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN historyTokens INTEGER")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN completionTokens INTEGER")
            }
        }
    }
}