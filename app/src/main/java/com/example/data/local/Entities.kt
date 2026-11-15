package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recent_entries")
data class RecentEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pathOrUri: String,
    val isDirectory: Boolean,
    val isSaf: Boolean,
    val safTreeUri: String?,
    val safDocumentId: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val lastAccessedMillis: Long
)

@Entity(tableName = "batch_undo_groups")
data class BatchUndoGroupEntity(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
    val operationType: String,
    val summaryText: String,
    val timestampMillis: Long,
    val isReversible: Boolean,
    val isUndone: Boolean = false
)

@Entity(
    tableName = "batch_undo_items",
    foreignKeys = [
        ForeignKey(
            entity = BatchUndoGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class BatchUndoItemEntity(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val groupId: Long,
    val originalName: String,
    val newName: String,
    val originalPathOrUri: String,
    val newPathOrUri: String,
    val parentPathOrUri: String,
    val isSaf: Boolean,
    val safTreeUri: String?
)
