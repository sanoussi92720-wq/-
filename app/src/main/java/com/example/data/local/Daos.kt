package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class BatchUndoGroupWithItems(
    val group: BatchUndoGroupEntity,
    val items: List<BatchUndoItemEntity>
)

@Dao
interface RecentEntryDao {
    @Query("SELECT * FROM recent_entries WHERE isDirectory = 0 ORDER BY lastAccessedMillis DESC LIMIT 20")
    fun observeRecentFiles(): Flow<List<RecentEntryEntity>>

    @Query("SELECT * FROM recent_entries WHERE isDirectory = 1 ORDER BY lastAccessedMillis DESC LIMIT 12")
    fun observeRecentFolders(): Flow<List<RecentEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentEntryEntity)

    @Query("DELETE FROM recent_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_entries")
    suspend fun clearAll()
}

@Dao
interface BatchHistoryDao {
    @Query("SELECT * FROM batch_undo_groups ORDER BY timestampMillis DESC LIMIT 30")
    fun observeGroups(): Flow<List<BatchUndoGroupEntity>>

    @Insert
    suspend fun insertGroup(group: BatchUndoGroupEntity): Long

    @Insert
    suspend fun insertItems(items: List<BatchUndoItemEntity>)

    @Query("SELECT * FROM batch_undo_groups WHERE groupId = :groupId")
    suspend fun getGroup(groupId: Long): BatchUndoGroupEntity?

    @Query("SELECT * FROM batch_undo_items WHERE groupId = :groupId")
    suspend fun getItemsForGroup(groupId: Long): List<BatchUndoItemEntity>

    @Query("UPDATE batch_undo_groups SET isUndone = 1 WHERE groupId = :groupId")
    suspend fun markUndone(groupId: Long)

    @Query("DELETE FROM batch_undo_groups")
    suspend fun clearAll()

    @Transaction
    suspend fun saveUndoBatch(group: BatchUndoGroupEntity, items: List<BatchUndoItemEntity>): Long {
        val id = insertGroup(group)
        if (items.isNotEmpty()) {
            insertItems(items.map { it.copy(groupId = id) })
        }
        return id
    }
}
