package com.example.data.engine

import com.example.model.ExtensionCaseMode
import com.example.model.FileItem
import com.example.model.RenameConfig
import com.example.model.RenameConflictStatus
import com.example.model.RenameMode
import com.example.model.RenamePreviewItem
import com.example.model.SequencePosition

object BatchRenameEngine {

    private val invalidCharsRegex = Regex("[/\\\\:*?\"<>|\\u0000]")

    fun computeNewFileName(
        item: FileItem,
        index: Int,
        config: RenameConfig,
        overrideExtension: String? = null
    ): String {
        val rawStem = when (config.mode) {
            RenameMode.REPLACE_NAME -> config.customBaseName
            RenameMode.MODIFY_ORIGINAL -> {
                val base = item.nameWithoutExtension
                if (config.findText.isNotEmpty()) {
                    base.replace(
                        oldValue = config.findText,
                        newValue = config.replaceText,
                        ignoreCase = !config.caseSensitiveFind
                    )
                } else {
                    base
                }
            }
        }

        val seqStr = if (config.useSequence) {
            val number = (config.startNumber + index * config.stepNumber).coerceAtLeast(0)
            val width = config.digits.coerceIn(1, 8)
            number.toString().padStart(width, '0')
        } else {
            ""
        }

        val body = when {
            !config.useSequence -> "${config.prefix}${rawStem}${config.suffix}"
            config.sequencePosition == SequencePosition.SUFFIX -> "${config.prefix}${rawStem}${seqStr}${config.suffix}"
            else -> "${config.prefix}${seqStr}${rawStem}${config.suffix}"
        }

        if (item.isDirectory && overrideExtension == null) {
            return body
        }

        val baseExt = when {
            overrideExtension != null -> overrideExtension.trimStart('.')
            config.preserveExtension -> item.extension
            else -> config.customExtension.trimStart('.')
        }

        val finalExt = when (config.extensionCaseMode) {
            ExtensionCaseMode.PRESERVE -> baseExt
            ExtensionCaseMode.LOWERCASE -> baseExt.lowercase()
            ExtensionCaseMode.UPPERCASE -> baseExt.uppercase()
        }

        return if (finalExt.isNotBlank()) "$body.$finalExt" else body
    }

    /**
     * Generates live preview items with intra-batch duplicate detection and
     * destination-folder collision detection.
     *
     * @param items Ordered items to rename.
     * @param config Current rename configuration.
     * @param existingNamesByParent Map of parentId -> Set of all existing file/folder names in that parent.
     * @param overrideExtension Optional forced extension (used when chained in Batch Pipeline).
     */
    fun generatePreviews(
        items: List<FileItem>,
        config: RenameConfig,
        existingNamesByParent: Map<String, Set<String>>,
        overrideExtension: String? = null
    ): List<RenamePreviewItem> {
        if (items.isEmpty()) return emptyList()

        val proposedNames = items.mapIndexed { index, item ->
            computeNewFileName(item, index, config, overrideExtension)
        }

        // Count normalized targets per parent directory to catch intra-batch duplicates
        val targetCountsPerParent = mutableMapOf<String, MutableMap<String, Int>>()
        val batchOriginalNamesPerParent = mutableMapOf<String, MutableSet<String>>()

        items.forEachIndexed { index, item ->
            val targetNorm = proposedNames[index].lowercase()
            val parentCounts = targetCountsPerParent.getOrPut(item.parentId) { mutableMapOf() }
            parentCounts[targetNorm] = (parentCounts[targetNorm] ?: 0) + 1

            val origSet = batchOriginalNamesPerParent.getOrPut(item.parentId) { mutableSetOf() }
            origSet.add(item.name.lowercase())
        }

        return items.mapIndexed { index, item ->
            val newName = proposedNames[index]
            val normNew = newName.lowercase()
            val parentExistingNorm = existingNamesByParent[item.parentId]
                ?.map { it.lowercase() }
                ?.toSet()
                ?: emptySet()
            val batchSourcesNorm = batchOriginalNamesPerParent[item.parentId] ?: emptySet()
            val targetCount = targetCountsPerParent[item.parentId]?.get(normNew) ?: 0

            val (status, reason) = when {
                newName.isBlank() || newName == "." || newName == ".." -> {
                    RenameConflictStatus.INVALID_NAME to "Filename cannot be empty"
                }
                invalidCharsRegex.containsMatchIn(newName) -> {
                    RenameConflictStatus.INVALID_NAME to "Contains invalid characters"
                }
                targetCount > 1 -> {
                    RenameConflictStatus.DUPLICATE_IN_BATCH to "Duplicate name in batch ($targetCount files)"
                }
                newName == item.name -> {
                    RenameConflictStatus.UNCHANGED to null
                }
                normNew in parentExistingNorm && normNew !in batchSourcesNorm -> {
                    RenameConflictStatus.EXISTS_IN_FOLDER to "Already exists in folder"
                }
                else -> {
                    RenameConflictStatus.READY to null
                }
            }

            RenamePreviewItem(
                source = item,
                originalName = item.name,
                newName = newName,
                conflictStatus = status,
                conflictReason = reason
            )
        }
    }

    fun hasBlockingConflicts(previews: List<RenamePreviewItem>): Boolean {
        return previews.any {
            it.conflictStatus == RenameConflictStatus.DUPLICATE_IN_BATCH ||
                it.conflictStatus == RenameConflictStatus.EXISTS_IN_FOLDER ||
                it.conflictStatus == RenameConflictStatus.INVALID_NAME
        }
    }

    /**
     * Resolves cyclic / overlapping renames inside the same folder using a two-phase plan:
     * 1. Any file whose target name matches another file's current name in the batch is first
     *    renamed to a unique temporary name.
     * 2. Then all files are renamed to their final target names.
     */
    data class RenamePlan(
        val phaseOneTemps: List<Pair<FileItem, String>>,
        val phaseTwoFinals: List<Triple<FileItem, String, String>> // source, currentNameBeforePhase2, targetName
    )

    fun buildTwoPhasePlan(previews: List<RenamePreviewItem>, uniqueToken: String): RenamePlan {
        val active = previews.filter {
            it.conflictStatus == RenameConflictStatus.READY && it.originalName != it.newName
        }
        val originalNamesByParent = active.groupBy { it.source.parentId }
            .mapValues { (_, list) -> list.map { it.originalName.lowercase() }.toSet() }

        val phase1 = mutableListOf<Pair<FileItem, String>>()
        val phase2 = mutableListOf<Triple<FileItem, String, String>>()

        // If any target name collides with ANY active original name in the same folder,
        // move ALL active items whose original name is targeted (or simply all colliding ones) to temp first.
        val targetedNormsByParent = active.groupBy { it.source.parentId }
            .mapValues { (_, list) -> list.map { it.newName.lowercase() }.toSet() }

        active.forEachIndexed { idx, preview ->
            val parentTargeted = targetedNormsByParent[preview.source.parentId] ?: emptySet()
            val parentOriginals = originalNamesByParent[preview.source.parentId] ?: emptySet()
            val needsTemp = preview.originalName.lowercase() in parentTargeted ||
                preview.newName.lowercase() in parentOriginals

            if (needsTemp) {
                val ext = if (preview.source.extension.isNotBlank() && !preview.source.isDirectory) {
                    ".${preview.source.extension}"
                } else ""
                val tempName = ".__fm_tmp_${uniqueToken}_${idx}$ext"
                phase1.add(preview.source to tempName)
                phase2.add(Triple(preview.source, tempName, preview.newName))
            } else {
                phase2.add(Triple(preview.source, preview.originalName, preview.newName))
            }
        }

        return RenamePlan(phaseOneTemps = phase1, phaseTwoFinals = phase2)
    }
}
