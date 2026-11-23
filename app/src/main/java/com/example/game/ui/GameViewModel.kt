package com.example.game.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.ai.AiDifficulty
import com.example.game.challenges.ChallengeCatalog
import com.example.game.challenges.ChallengeOutcome
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.EmoteAction
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.districts.DistrictCatalog
import com.example.game.engine.GameSimulation
import com.example.game.engine.GlTrig
import com.example.game.engine.HudSnapshot
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.InventoryCatalog
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponSlotType
import com.example.game.inventory.WeaponSpec
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameLocalization
import com.example.game.localization.GameStrings
import com.example.game.modes.GameModeId
import com.example.game.multiplayer.ConnectionState
import com.example.game.multiplayer.MatchPhase
import com.example.game.multiplayer.MultiplayerSessionCoordinator
import com.example.game.save.GameDatabase
import com.example.game.save.GraphicsQuality
import com.example.game.save.PlayerProgressRepository
import com.example.game.save.PlayerProgressState
import com.example.game.social.SocialAndAccountRepository
import com.example.game.world.StreamingTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GameDatabase.getInstance(application)
    private val repository = PlayerProgressRepository(db.saveDao())
    private val socialRepo = SocialAndAccountRepository(db.socialAccountDao())
    val multiplayerCoordinator = MultiplayerSessionCoordinator()
    val socialDelegate = SocialAccountDelegate(
        scope = viewModelScope,
        progressRepo = repository,
        socialRepo = socialRepo,
        coordinator = multiplayerCoordinator
    )

    val progressState: StateFlow<PlayerProgressState> = repository.progressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProgressState()
        )

    val simulation = GameSimulation()
    val streamingTelemetry: StateFlow<StreamingTelemetry> = simulation.streamingManager.telemetry

    private val _hudState = MutableStateFlow(HudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    private val _worldBanner = MutableStateFlow<String?>(null)
    val worldBanner: StateFlow<String?> = _worldBanner.asStateFlow()

    private var challengeRewardGrantedThisRun = false
    private var lastHudEmitMs = 0L
    private var lastNetMoveEmitMs = 0L

    init {
        viewModelScope.launch {
            multiplayerCoordinator.worldStateFlow.collect { ws ->
                if (ws != null && simulation.isMultiplayerSession) {
                    val localPid = multiplayerCoordinator.primaryClient.localPlayerId
                    val localSnap = ws.players.firstOrNull { it.playerId == localPid }
                    simulation.updateRemotePlayers(
                        players = ws.players,
                        localPlayerId = localPid,
                        authoritativeLocalHealth = localSnap?.health
                    )
                    if (ws.phase == MatchPhase.FINISHED) {
                        val won = localSnap != null && (localSnap.team.id == ws.winningTeam || localSnap.callsign == ws.winningTeam)
                        synchronized(simulation.lock) {
                            if (simulation.outcome == ChallengeOutcome.IN_PROGRESS) {
                                simulation.outcome = if (won) ChallengeOutcome.VICTORY else ChallengeOutcome.SPECTATOR_DEFEAT
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            multiplayerCoordinator.verifiedRewardFlow.collect { reward ->
                if (reward != null) {
                    val worldPlayers = multiplayerCoordinator.worldStateFlow.value?.players.orEmpty()
                    val localPid = multiplayerCoordinator.primaryClient.localPlayerId
                    val localSnap = worldPlayers.firstOrNull { it.playerId == localPid }
                    val kills = localSnap?.kills ?: 0
                    val winner = multiplayerCoordinator.worldStateFlow.value?.winningTeam
                    val won = localSnap != null && (localSnap.team.id == winner || localSnap.callsign == winner)
                    val districtId = multiplayerCoordinator.activeRoomFlow.value?.districtId ?: reward.unlockedDistrictId
                    repository.applyServerVerifiedMatchReward(
                        districtId = districtId,
                        xpEarned = reward.xp,
                        takaEarned = reward.taka,
                        eliminations = kills
                    )
                    socialRepo.applyMatchStats(
                        xpEarned = reward.xp,
                        won = won,
                        kills = kills
                    )
                    multiplayerCoordinator.primaryClient.clearReward()
                }
            }
        }
    }

    fun currentLanguage(): AppLanguage =
        AppLanguage.fromCode(progressState.value.save.languageCode)

    fun currentStrings(): GameStrings = GameLocalization.strings(currentLanguage())

    fun clearWorldBanner() {
        _worldBanner.value = null
    }

    fun syncShowcaseState() {
        val save = progressState.value.save
        synchronized(simulation.lock) {
            simulation.activeCharacter = CharacterCatalog.getById(save.selectedCharacterId)
            simulation.activeOutfit = OutfitStyle.fromId(save.selectedOutfitId)
            simulation.activeHeadwear = HeadwearOption.fromId(save.selectedHeadwearId)
            simulation.activeAccessory = AccessoryOption.fromId(save.selectedAccessoryId)
            simulation.activeColorTint = CharacterColorTint.fromId(save.selectedColorTintId)
            simulation.activeEmote = EmoteAction.fromId(save.selectedEmoteId)
            simulation.primaryWeapon = InventoryCatalog.getWeapon(save.selectedWeaponId)
            simulation.secondaryWeapon = InventoryCatalog.getWeapon(save.secondaryWeaponId)
            simulation.meleeWeapon = InventoryCatalog.getWeapon(save.meleeWeaponId)
            simulation.attachments = save.attachments()
            simulation.soundEngine.enabled = save.audioEnabled
        }
    }

    fun selectCharacter(characterId: String) {
        val slot = CharacterCatalog.getById(characterId)
        viewModelScope.launch { repository.selectCharacter(slot.id, slot.defaultOutfit.id) }
        synchronized(simulation.lock) {
            simulation.activeCharacter = slot
            simulation.activeOutfit = slot.defaultOutfit
        }
        if (multiplayerCoordinator.connectionFlow.value == ConnectionState.IN_ROOM) {
            multiplayerCoordinator.primaryClient.setCharacterAndWeapon(slot.id, progressState.value.save.selectedWeaponId)
        }
    }

    fun selectOutfit(outfit: OutfitStyle) {
        viewModelScope.launch { repository.selectOutfit(outfit.id) }
        synchronized(simulation.lock) { simulation.activeOutfit = outfit }
    }

    fun selectHeadwear(headwear: HeadwearOption) {
        viewModelScope.launch { repository.selectHeadwear(headwear.id) }
        synchronized(simulation.lock) { simulation.activeHeadwear = headwear }
    }

    fun selectAccessory(accessory: AccessoryOption) {
        viewModelScope.launch { repository.selectAccessory(accessory.id) }
        synchronized(simulation.lock) { simulation.activeAccessory = accessory }
    }

    fun selectColorTint(tint: CharacterColorTint) {
        viewModelScope.launch { repository.selectColorTint(tint.id) }
        synchronized(simulation.lock) { simulation.activeColorTint = tint }
    }

    fun selectEmote(emote: EmoteAction) {
        viewModelScope.launch { repository.selectEmote(emote.id) }
        synchronized(simulation.lock) { simulation.activeEmote = emote }
    }

    fun selectWeapon(weaponId: String, slot: WeaponSlotType = WeaponSlotType.PRIMARY) {
        viewModelScope.launch { repository.selectWeapon(weaponId, slot) }
        val w = InventoryCatalog.getWeapon(weaponId)
        synchronized(simulation.lock) {
            when (slot) {
                WeaponSlotType.PRIMARY -> simulation.primaryWeapon = w
                WeaponSlotType.SECONDARY -> simulation.secondaryWeapon = w
                WeaponSlotType.MELEE -> simulation.meleeWeapon = w
            }
        }
        if (slot == WeaponSlotType.PRIMARY && multiplayerCoordinator.connectionFlow.value == ConnectionState.IN_ROOM) {
            multiplayerCoordinator.primaryClient.setCharacterAndWeapon(progressState.value.save.selectedCharacterId, weaponId)
        }
    }

    fun updateAttachments(
        optic: OpticAttachment? = null,
        muzzle: MuzzleAttachment? = null,
        magazine: MagazineAttachment? = null,
        underbarrel: UnderbarrelAttachment? = null
    ) {
        viewModelScope.launch { repository.updateAttachments(optic, muzzle, magazine, underbarrel) }
    }

    fun toggleLanguage() {
        val next = if (currentLanguage() == AppLanguage.ENGLISH) AppLanguage.BENGALI else AppLanguage.ENGLISH
        viewModelScope.launch { repository.setLanguage(next.code) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language.code) }
    }

    fun updateSettings(
        sensitivity: Float? = null,
        graphicsQuality: GraphicsQuality? = null,
        invertY: Boolean? = null,
        hapticsEnabled: Boolean? = null,
        aiDifficulty: AiDifficulty? = null,
        audioEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateSettings(sensitivity, graphicsQuality, invertY, hapticsEnabled, aiDifficulty, audioEnabled)
        }
        if (graphicsQuality != null) {
            simulation.streamingManager.configureQuality(graphicsQuality)
        }
        synchronized(simulation.lock) {
            if (sensitivity != null) simulation.cameraSensitivity = sensitivity
            if (invertY != null) simulation.invertYAxis = invertY
            if (aiDifficulty != null) simulation.aiDifficulty = aiDifficulty
            if (audioEnabled != null) simulation.soundEngine.enabled = audioEnabled
        }
    }

    fun resetLocalSave() {
        viewModelScope.launch {
            repository.resetSave()
            val dhaka = simulation.streamingManager.streamToDistrict("dhaka", GraphicsQuality.fromId(progressState.value.save.graphicsQualityId))
            simulation.loadStreamedWorld(dhaka)
        }
    }

    fun travelToDistrict(districtId: String, alsoStreamIntoWorld: Boolean = false, onReady: (() -> Unit)? = null) {
        val unlocked = progressState.value.unlockedDistrictIds
        if (districtId !in unlocked) return
        val entry = DistrictCatalog.getById(districtId)
        val quality = GraphicsQuality.fromId(progressState.value.save.graphicsQualityId)
        viewModelScope.launch {
            repository.travelToDistrict(districtId)
            if (alsoStreamIntoWorld && entry.isEnvironmentReady) {
                val streamedGeo = simulation.streamingManager.streamToDistrict(districtId, quality)
                simulation.loadStreamedWorld(streamedGeo, resetSpawn = true)
                _worldBanner.value = "${entry.name(currentLanguage())} • ${entry.biome.label(currentLanguage())}"
            }
            onReady?.invoke()
        }
    }

    fun startPlayableMode(mode: GameModeId, isMultiplayer: Boolean = mode.requiresMultiplayer) {
        val save = progressState.value.save
        val targetDistrict = DistrictCatalog.getById(save.currentDistrictId).let {
            if (it.isEnvironmentReady) it.id else "dhaka"
        }
        val quality = GraphicsQuality.fromId(save.graphicsQualityId)
        challengeRewardGrantedThisRun = false

        if (simulation.world.districtId != targetDistrict) {
            viewModelScope.launch {
                val streamedGeo = simulation.streamingManager.streamToDistrict(targetDistrict, quality)
                simulation.loadStreamedWorld(streamedGeo, resetSpawn = true)
                startSessionOnCurrentWorld(mode, isMultiplayer)
            }
        } else {
            startSessionOnCurrentWorld(mode, isMultiplayer)
        }
    }

    private fun startSessionOnCurrentWorld(mode: GameModeId, isMultiplayer: Boolean) {
        val save = progressState.value.save
        val progress = progressState.value
        simulation.startSession(
            mode = mode,
            character = CharacterCatalog.getById(save.selectedCharacterId),
            outfit = OutfitStyle.fromId(save.selectedOutfitId),
            headwear = HeadwearOption.fromId(save.selectedHeadwearId),
            accessory = AccessoryOption.fromId(save.selectedAccessoryId),
            colorTint = CharacterColorTint.fromId(save.selectedColorTintId),
            primary = InventoryCatalog.getWeapon(save.selectedWeaponId),
            secondary = InventoryCatalog.getWeapon(save.secondaryWeaponId),
            melee = InventoryCatalog.getWeapon(save.meleeWeaponId),
            loadoutAttachments = save.attachments(),
            difficulty = AiDifficulty.fromId(save.aiDifficultyId),
            sensitivity = save.cameraSensitivity,
            invertY = save.invertYAxis,
            audioEnabled = save.audioEnabled,
            multiplayerSession = isMultiplayer
        )
        val alreadyMissionDone = progress.isMissionDone(simulation.world.districtId)
        synchronized(simulation.lock) {
            simulation.districtMissionCompleted = alreadyMissionDone
        }
        _hudState.value = simulation.snapshot()
    }

    fun completeDistrictMissionObjective() {
        val districtId = simulation.world.districtId
        val newlyDone = simulation.completeDistrictMissionObjective()
        if (!newlyDone) return
        val entry = DistrictCatalog.getById(districtId)
        val lang = currentLanguage()
        viewModelScope.launch {
            val unlockedNext = repository.recordDistrictMissionComplete(
                districtId = districtId,
                xpEarned = entry.missionXpReward,
                takaEarned = entry.missionTakaReward
            )
            _worldBanner.value = if (unlockedNext != null) {
                val nextEntry = DistrictCatalog.getById(unlockedNext)
                "${entry.missionTitle(lang)} ✓ • Unlocked: ${nextEntry.name(lang)}"
            } else {
                "${entry.missionTitle(lang)} ✓ (+${entry.missionXpReward} XP)"
            }
        }
        _hudState.value = simulation.snapshot()
    }

    fun onRenderTick() {
        val now = System.currentTimeMillis()
        if (simulation.isMultiplayerSession &&
            multiplayerCoordinator.connectionFlow.value == ConnectionState.IN_ROOM &&
            now - lastNetMoveEmitMs >= 85L
        ) {
            lastNetMoveEmitMs = now
            val (px, py, pz, yaw, wepId) = synchronized(simulation.lock) {
                Tuple5(
                    simulation.playerPos.x,
                    simulation.playerPos.y,
                    simulation.playerPos.z,
                    simulation.playerYawRad,
                    simulation.activeWeapon().id
                )
            }
            multiplayerCoordinator.primaryClient.sendMove(px, py, pz, yaw, wepId)
        }

        if (now - lastHudEmitMs >= 75L) {
            lastHudEmitMs = now
            val snap = simulation.snapshot()
            _hudState.value = snap
            if (!snap.isMultiplayerSession && snap.outcome == ChallengeOutcome.VICTORY && !challengeRewardGrantedThisRun) {
                challengeRewardGrantedThisRun = true
                val def = ChallengeCatalog.forDistrict(snap.activeDistrictId)
                viewModelScope.launch {
                    val unlockedNext = repository.recordDistrictChallengeComplete(
                        districtId = def.districtId,
                        challengeId = def.id,
                        xpEarned = def.xpReward,
                        takaEarned = def.takaReward,
                        targetsEliminated = snap.enemiesEliminated
                    )
                    socialRepo.applyMatchStats(def.xpReward, won = true, kills = snap.enemiesEliminated)
                    if (unlockedNext != null) {
                        val nextEntry = DistrictCatalog.getById(unlockedNext)
                        _worldBanner.value = "100% Cleared • Unlocked ${nextEntry.name(currentLanguage())}!"
                    }
                }
            }
        }
    }

    fun fireWeapon() {
        val newTrainingHits = simulation.fireWeapon()
        if (newTrainingHits > 0) {
            viewModelScope.launch { repository.recordTrainingHits(newTrainingHits) }
        }
        if (simulation.isMultiplayerSession && multiplayerCoordinator.connectionFlow.value == ConnectionState.IN_ROOM) {
            val forward = synchronized(simulation.lock) {
                GlTrig.forwardFromYawPitch(simulation.playerYawRad, simulation.playerPitchRad)
            }
            multiplayerCoordinator.primaryClient.sendFire(forward.x, forward.y, forward.z)
        }
        _hudState.value = simulation.snapshot()
    }

    fun toggleCameraMode() { simulation.toggleCameraMode(); _hudState.value = simulation.snapshot() }
    fun toggleAim() { simulation.toggleAim(); _hudState.value = simulation.snapshot() }
    fun cycleFireMode() { simulation.cycleFireMode(); _hudState.value = simulation.snapshot() }
    fun switchWeaponSlot(slot: WeaponSlotType) { simulation.switchWeaponSlot(slot); _hudState.value = simulation.snapshot() }
    fun switchTestingWeapon(weapon: WeaponSpec) { simulation.switchTestingWeapon(weapon); _hudState.value = simulation.snapshot() }
    fun toggleCrouch() { simulation.toggleCrouch(); _hudState.value = simulation.snapshot() }
    fun setSprint(enabled: Boolean) { simulation.setSprint(enabled); _hudState.value = simulation.snapshot() }
    fun jumpOrVault() { simulation.jumpOrVault(); _hudState.value = simulation.snapshot() }
    fun reloadWeapon() {
        simulation.reloadWeapon()
        if (simulation.isMultiplayerSession && multiplayerCoordinator.connectionFlow.value == ConnectionState.IN_ROOM) {
            multiplayerCoordinator.primaryClient.sendReload()
        }
        _hudState.value = simulation.snapshot()
    }
    fun enterOrExitVehicle() { simulation.enterOrExitVehicle(); _hudState.value = simulation.snapshot() }
    fun pickupNearbyLoot() { simulation.pickupNearbyLoot(); _hudState.value = simulation.snapshot() }
    fun useConsumable(type: ConsumableType) { simulation.useConsumable(type); _hudState.value = simulation.snapshot() }
    fun dropActiveItem() { simulation.dropActiveItem(); _hudState.value = simulation.snapshot() }
    fun spawnPracticeBot() { simulation.spawnPracticeBot(); _hudState.value = simulation.snapshot() }
    fun refillAmmoAndHealth() { simulation.refillAmmoAndHealth(); _hudState.value = simulation.snapshot() }

    override fun onCleared() {
        super.onCleared()
        multiplayerCoordinator.shutdown()
    }

    private data class Tuple5(val x: Float, val y: Float, val z: Float, val yaw: Float, val wepId: String)
}
