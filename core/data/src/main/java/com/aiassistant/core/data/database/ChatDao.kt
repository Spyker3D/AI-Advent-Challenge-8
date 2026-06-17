package com.aiassistant.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiassistant.core.data.database.entity.ChatEntity

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getChats(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun getChat(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    fun deleteChat(chatId: String)

    @Query("""
        UPDATE chats 
        SET title = :title, updatedAt = :updatedAt, lastMessagePreview = :preview 
        WHERE id = :chatId
    """)
    fun updateChatMeta(
        chatId: String,
        title: String,
        updatedAt: Long,
        preview: String
    )

    @Query("""
        UPDATE chats
        SET activeTaskContextId = :taskContextId, updatedAt = :updatedAt
        WHERE id = :chatId
    """)
    fun updateChatActiveTaskContext(
        chatId: String,
        taskContextId: String?,
        updatedAt: Long
    )
}
