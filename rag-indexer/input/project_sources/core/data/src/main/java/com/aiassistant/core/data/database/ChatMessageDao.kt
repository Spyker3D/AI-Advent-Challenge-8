package com.aiassistant.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE branchId = :branchId ORDER BY timestamp ASC")
    fun getMessages(branchId: String = "main"): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE branchId = :branchId")
    fun clearMessages(branchId: String = "main")
}