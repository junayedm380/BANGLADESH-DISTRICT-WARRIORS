package com.example.game.save

import com.example.game.ai.AiDifficulty
import com.example.game.districts.DistrictCatalog
import com.example.game.districts.SequentialUnlockConfig
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponSlotType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

enum class CloudSyncStatus {
    LOCAL_ONLY_MILESTONE_2,
    SYNCED,
    PENDING_UPLOAD
}

interface IBackendSyncGateway {
    val syncStatus: CloudSyncStatus
    suspend fun pushSaveSnapshot(save: PlayerSaveEntity): Result<Unit>
    suspend fun pullSaveSnapshot(playerId: String): Result<PlayerSaveEntity?>
}

class LocalOnlyBackendGateway : IBackendSyncGateway {
    override val syncStatus: CloudSyncStatus = CloudSyncStatus.LOCAL_ONLY_MILESTONE_2
    override suspend fun pushSaveSnapshot(save: PlayerSaveEntity): Result<Unit> = Result.success(Unit)
    override suspend fun pullSaveSnapshot(playerId: String): Result<PlayerSaveEntity?> = Result.success(null)
}

data class PlayerProgressState(
    val save: PlayerSaveEntity = PlayerSaveEntity(),
    val unlockedDistrictIds: Set<String> = setOf("dhaka"),
    val completedDistrictIds: Set<String> = emptySet(),
    val completedChallengeIds: Set<String> = emptySet(),
    val completedMissionDistrictIds: Set<String> = emptySet(),
    val discoveredTravelNodes: Set<String> = setOf("dhaka"),
    val currentDistrictId: String = "dhaka",
    val districtOrder: List<String> = SequentialUnlockConfig.defaultOrder,
    val syncStatus: CloudSyncStatus = CloudSyncStatus.LOCAL_ONLY_MILESTONE_2
) {
    fun completionPercent(districtId: String): Int {
        val completedChallengeDistricts = completedChallengeIds
            .map { it.removePrefix("challenge_") }
            .toSet() + completedDistrictIds
        return SequentialUnlockConfig.completionPercent(
            districtId = districtId,
            completedMissionDistricts = completedMissionDistrictIds,
            completedChallengeDistricts = completedChallengeDistricts
        )
    }

    fun isChallengeDone(districtId: String): Boolean =
        "challenge_$districtId" in completedChallengeIds || districtId in completedDistrictIds

    fun isMissionDone(districtId: String): Boolean =
        districtId in completedMissionDistrictIds || districtId in completedDistrictIds
}

class PlayerProgressRepository(
    private val saveDao: SaveDao,
    private val backendGateway: IBackendSyncGateway = LocalOnlyBackendGateway()
) {
    val progressFlow: Flow<PlayerProgressState> = saveDao.observeSave()
        .onStart {
            if (saveDao.getSaveOnce() == null) {
                saveDao.upsertSave(PlayerSaveEntity())
            }
        }
        .map { entity ->
            val current = entity ?: PlayerSaveEntity()
            val orderList = SequentialUnlockConfig.normalizeOrder(parseOrderedCsv(current.configurableDistrictOrderCsv))
            PlayerProgressState(
                save = current,
                unlockedDistrictIds = parseCsv(current.unlockedDistrictIdsCsv).ifEmpty { setOf("dhaka") },
                completedDistrictIds = parseCsv(current.completedDistrictIdsCsv),
                completedChallengeIds = parseCsv(current.completedChallengeIdsCsv),
                completedMissionDistrictIds = parseCsv(current.completedMissionDistrictIdsCsv),
                discoveredTravelNodes = parseCsv(current.discoveredTravelNodesCsv).ifEmpty { setOf("dhaka") },
                currentDistrictId = current.currentDistrictId.ifBlank { "dhaka" },
                districtOrder = orderList,
                syncStatus = backendGateway.syncStatus
            )
        }

    suspend fun updateSave(transform: (PlayerSaveEntity) -> PlayerSaveEntity) {
        val current = saveDao.getSaveOnce() ?: PlayerSaveEntity()
        val updated = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
        saveDao.upsertSave(updated)
    }

    suspend fun selectCharacter(characterId: String, defaultOutfitId: String) {
        updateSave {
            it.copy(
                selectedCharacterId = characterId,
                selectedOutfitId = defaultOutfitId
            )
        }
    }

    suspend fun selectOutfit(outfitId: String) {
        updateSave { it.copy(selectedOutfitId = outfitId) }
    }

    suspend fun selectHeadwear(headwearId: String) {
        updateSave { it.copy(selectedHeadwearId = headwearId) }
    }

    suspend fun selectAccessory(accessoryId: String) {
        updateSave { it.copy(selectedAccessoryId = accessoryId) }
    }

    suspend fun selectColorTint(colorTintId: String) {
        updateSave { it.copy(selectedColorTintId = colorTintId) }
    }

    suspend fun selectEmote(emoteId: String) {
        updateSave { it.copy(selectedEmoteId = emoteId) }
    }

    suspend fun selectWeapon(weaponId: String, slot: WeaponSlotType = WeaponSlotType.PRIMARY) {
        updateSave { cur ->
            when (slot) {
                WeaponSlotType.PRIMARY -> cur.copy(selectedWeaponId = weaponId)
                WeaponSlotType.SECONDARY -> cur.copy(secondaryWeaponId = weaponId)
                WeaponSlotType.MELEE -> cur.copy(meleeWeaponId = weaponId)
            }
        }
    }

    suspend fun updateAttachments(
        optic: OpticAttachment? = null,
        muzzle: MuzzleAttachment? = null,
        magazine: MagazineAttachment? = null,
        underbarrel: UnderbarrelAttachment? = null
    ) {
        updateSave { cur ->
            cur.copy(
                opticAttachmentId = optic?.id ?: cur.opticAttachmentId,
                muzzleAttachmentId = muzzle?.id ?: cur.muzzleAttachmentId,
                magazineAttachmentId = magazine?.id ?: cur.magazineAttachmentId,
                underbarrelAttachmentId = underbarrel?.id ?: cur.underbarrelAttachmentId
            )
        }
    }

    suspend fun setLanguage(languageCode: String) {
        updateSave { it.copy(languageCode = languageCode) }
    }

    suspend fun updateSettings(
        sensitivity: Float? = null,
        graphicsQuality: GraphicsQuality? = null,
        invertY: Boolean? = null,
        hapticsEnabled: Boolean? = null,
        aiDifficulty: AiDifficulty? = null,
        audioEnabled: Boolean? = null
    ) {
        updateSave { cur ->
            cur.copy(
                cameraSensitivity = sensitivity ?: cur.cameraSensitivity,
                graphicsQualityId = graphicsQuality?.id ?: cur.graphicsQualityId,
                invertYAxis = invertY ?: cur.invertYAxis,
                hapticsEnabled = hapticsEnabled ?: cur.hapticsEnabled,
                aiDifficultyId = aiDifficulty?.id ?: cur.aiDifficultyId,
                audioEnabled = audioEnabled ?: cur.audioEnabled
            )
        }
    }

    suspend fun recordTrainingHits(hits: Int) {
        if (hits <= 0) return
        updateSave { cur ->
            cur.copy(
                xp = cur.xp + (hits * 10),
                targetsEliminatedCount = cur.targetsEliminatedCount + hits
            )
        }
    }

    /**
     * Records sequential Mission completion for [districtId].
     * Unlocks the next district in configurable order ONLY once both Mission AND Challenge are done.
     */
    suspend fun recordDistrictMissionComplete(
        districtId: String,
        xpEarned: Int,
        takaEarned: Int
    ): String? {
        var unlockedNextId: String? = null
        updateSave { cur ->
            val unlocked = parseCsv(cur.unlockedDistrictIdsCsv).toMutableSet().apply { add("dhaka") }
            val completedMissions = parseCsv(cur.completedMissionDistrictIdsCsv).toMutableSet()
            val newlyAdded = completedMissions.add(districtId)
            val completedChallenges = parseCsv(cur.completedChallengeIdsCsv)
            val completedDistricts = parseCsv(cur.completedDistrictIdsCsv).toMutableSet()
            val order = SequentialUnlockConfig.normalizeOrder(parseOrderedCsv(cur.configurableDistrictOrderCsv))

            if ("challenge_$districtId" in completedChallenges || districtId in completedDistricts) {
                completedDistricts.add(districtId)
                val nextId = SequentialUnlockConfig.nextDistrictIdAfter(districtId, order)
                if (nextId != null) {
                    unlocked.add(nextId)
                    unlockedNextId = nextId
                }
            }

            cur.copy(
                xp = cur.xp + if (newlyAdded) xpEarned else (xpEarned / 4),
                takaCredits = cur.takaCredits + if (newlyAdded) takaEarned else (takaEarned / 4),
                unlockedDistrictIdsCsv = unlocked.joinToString(","),
                completedMissionDistrictIdsCsv = completedMissions.joinToString(","),
                completedDistrictIdsCsv = completedDistricts.joinToString(",")
            )
        }
        return unlockedNextId
    }

    /**
     * Records sequential Challenge completion for [districtId].
     * Unlocks the next district in configurable order ONLY once both Mission AND Challenge are done.
     */
    suspend fun recordDistrictChallengeComplete(
        districtId: String,
        challengeId: String,
        xpEarned: Int,
        takaEarned: Int,
        targetsEliminated: Int
    ): String? {
        var unlockedNextId: String? = null
        updateSave { cur ->
            val unlocked = parseCsv(cur.unlockedDistrictIdsCsv).toMutableSet().apply { add("dhaka") }
            val completedChallenges = parseCsv(cur.completedChallengeIdsCsv).toMutableSet().apply { add(challengeId) }
            val completedMissions = parseCsv(cur.completedMissionDistrictIdsCsv)
            val completedDistricts = parseCsv(cur.completedDistrictIdsCsv).toMutableSet()
            val order = SequentialUnlockConfig.normalizeOrder(parseOrderedCsv(cur.configurableDistrictOrderCsv))

            if (districtId in completedMissions) {
                completedDistricts.add(districtId)
                val nextId = SequentialUnlockConfig.nextDistrictIdAfter(districtId, order)
                if (nextId != null) {
                    unlocked.add(nextId)
                    unlockedNextId = nextId
                }
            }

            cur.copy(
                xp = cur.xp + xpEarned,
                takaCredits = cur.takaCredits + takaEarned,
                unlockedDistrictIdsCsv = unlocked.joinToString(","),
                completedDistrictIdsCsv = completedDistricts.joinToString(","),
                completedChallengeIdsCsv = completedChallenges.joinToString(","),
                challengesClearedCount = cur.challengesClearedCount + 1,
                targetsEliminatedCount = cur.targetsEliminatedCount + targetsEliminated
            )
        }
        return unlockedNextId
    }

    /**
     * Milestone 1-3 solo challenge victory entry point (marks district challenge + mission cleared).
     */
    suspend fun recordChallengeVictory(
        districtId: String,
        challengeId: String,
        xpEarned: Int,
        takaEarned: Int,
        targetsEliminated: Int
    ) {
        updateSave { cur ->
            val unlocked = parseCsv(cur.unlockedDistrictIdsCsv).toMutableSet().apply { add("dhaka") }
            val completedMissions = parseCsv(cur.completedMissionDistrictIdsCsv).toMutableSet().apply { add(districtId) }
            val completedDistricts = parseCsv(cur.completedDistrictIdsCsv).toMutableSet().apply { add(districtId) }
            val completedChallenges = parseCsv(cur.completedChallengeIdsCsv).toMutableSet().apply { add(challengeId) }
            val order = SequentialUnlockConfig.normalizeOrder(parseOrderedCsv(cur.configurableDistrictOrderCsv))
            DistrictCatalog.nextDistrictAfter(districtId, order)?.let { next ->
                unlocked.add(next.id)
            }
            cur.copy(
                xp = cur.xp + xpEarned,
                takaCredits = cur.takaCredits + takaEarned,
                unlockedDistrictIdsCsv = unlocked.joinToString(","),
                completedMissionDistrictIdsCsv = completedMissions.joinToString(","),
                completedDistrictIdsCsv = completedDistricts.joinToString(","),
                completedChallengeIdsCsv = completedChallenges.joinToString(","),
                challengesClearedCount = cur.challengesClearedCount + 1,
                targetsEliminatedCount = cur.targetsEliminatedCount + targetsEliminated
            )
        }
    }

    suspend fun travelToDistrict(districtId: String): Boolean {
        val current = saveDao.getSaveOnce() ?: PlayerSaveEntity()
        val unlocked = parseCsv(current.unlockedDistrictIdsCsv).ifEmpty { setOf("dhaka") }
        if (districtId !in unlocked) return false

        val nodes = parseCsv(current.discoveredTravelNodesCsv).toMutableSet().apply {
            add("dhaka")
            add(districtId)
        }
        updateSave { cur ->
            cur.copy(
                currentDistrictId = districtId,
                discoveredTravelNodesCsv = nodes.joinToString(",")
            )
        }
        return true
    }

    suspend fun setConfigurableDistrictOrder(order: List<String>) {
        val normalized = SequentialUnlockConfig.normalizeOrder(order)
        updateSave { cur ->
            cur.copy(configurableDistrictOrderCsv = normalized.joinToString(","))
        }
    }

    suspend fun updateCallsign(newCallsign: String) {
        if (newCallsign.isBlank()) return
        updateSave { it.copy(callsign = newCallsign.trim().take(20)) }
    }

    suspend fun applyServerVerifiedMatchReward(
        districtId: String,
        xpEarned: Int,
        takaEarned: Int,
        eliminations: Int
    ) {
        updateSave { cur ->
            val unlocked = parseCsv(cur.unlockedDistrictIdsCsv).toMutableSet().apply { add("dhaka") }
            if (districtId.isNotBlank()) unlocked.add(districtId)
            cur.copy(
                xp = cur.xp + xpEarned.coerceAtLeast(0),
                takaCredits = cur.takaCredits + takaEarned.coerceAtLeast(0),
                unlockedDistrictIdsCsv = unlocked.joinToString(","),
                challengesClearedCount = cur.challengesClearedCount + 1,
                targetsEliminatedCount = cur.targetsEliminatedCount + eliminations.coerceAtLeast(0)
            )
        }
    }

    suspend fun resetSave() {
        val currentLang = saveDao.getSaveOnce()?.languageCode ?: "en"
        saveDao.upsertSave(PlayerSaveEntity(languageCode = currentLang))
    }

    private fun parseCsv(csv: String): Set<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun parseOrderedCsv(csv: String): List<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
