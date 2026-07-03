package com.aiassistant.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aiassistant.core.data.database.entity.ChatEntity

@Database(
    entities = [ChatMessageEntity::class, ChatEntity::class],
    version = 5
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatDao(): com.aiassistant.core.data.database.dao.ChatDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN currentRequestTokens INTEGER")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN historyTokens INTEGER")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN completionTokens INTEGER")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN branchId TEXT DEFAULT 'main' NOT NULL")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastMessagePreview` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chats ADD COLUMN activeTaskContextId TEXT")
            }
        }
    }
}
