package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.game.ai.AiDifficulty
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.EmoteAction
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponSlotType
import com.example.game.multiplayer.ReportReason
import com.example.game.multiplayer.ServerAuthorityValidator
import com.example.game.save.GameDatabase
import com.example.game.save.GraphicsQuality
import com.example.game.save.PlayerProgressRepository
import com.example.game.social.AchievementEvaluator
import com.example.game.social.FriendStatus
import com.example.game.social.SocialAndAccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("District Warriors", appName)
    }

    @Test
    fun `local Room v2 save repository persists slots, attachments, customization, difficulty, and unlocks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = PlayerProgressRepository(db.saveDao())
            val initial = repo.progressFlow.first()
            assertEquals("hero_alam", initial.save.selectedCharacterId)
            assertEquals("ar_dhaka_m4", initial.save.selectedWeaponId)
            assertEquals("pistol_p92", initial.save.secondaryWeaponId)
            assertEquals("melee_knife", initial.save.meleeWeaponId)
            assertTrue("dhaka" in initial.unlockedDistrictIds)

            val speedSlot = CharacterCatalog.getById("ishowspeed")
            repo.selectCharacter(speedSlot.id, speedSlot.defaultOutfit.id)
            repo.selectOutfit(OutfitStyle.PADMA_CAMO.id)
            repo.selectHeadwear(HeadwearOption.COMBAT_HELMET.id)
            repo.selectAccessory(AccessoryOption.TACTICAL_BACKPACK.id)
            repo.selectColorTint(CharacterColorTint.BENGAL_GREEN.id)
            repo.selectEmote(EmoteAction.COMBAT_WARMUP.id)

            repo.selectWeapon("sr_awm", WeaponSlotType.PRIMARY)
            repo.selectWeapon("pistol_deagle", WeaponSlotType.SECONDARY)
            repo.selectWeapon("melee_pan", WeaponSlotType.MELEE)

            repo.updateAttachments(
                optic = OpticAttachment.SCOPE_8X,
                muzzle = MuzzleAttachment.SUPPRESSOR,
                magazine = MagazineAttachment.EXTENDED,
                underbarrel = UnderbarrelAttachment.FOREGRIP
            )

            repo.setLanguage("bn")
            repo.updateSettings(
                sensitivity = 1.6f,
                graphicsQuality = GraphicsQuality.HIGH,
                invertY = true,
                aiDifficulty = AiDifficulty.HARD,
                audioEnabled = false
            )
            repo.recordChallengeVictory(
                districtId = "dhaka",
                challengeId = "challenge_dhaka",
                xpEarned = 250,
                takaEarned = 1000,
                targetsEliminated = 6
            )

            val updated = repo.progressFlow.first()
            assertEquals("ishowspeed", updated.save.selectedCharacterId)
            assertEquals(OutfitStyle.PADMA_CAMO.id, updated.save.selectedOutfitId)
            assertEquals(HeadwearOption.COMBAT_HELMET.id, updated.save.selectedHeadwearId)
            assertEquals(AccessoryOption.TACTICAL_BACKPACK.id, updated.save.selectedAccessoryId)
            assertEquals(CharacterColorTint.BENGAL_GREEN.id, updated.save.selectedColorTintId)
            assertEquals(EmoteAction.COMBAT_WARMUP.id, updated.save.selectedEmoteId)

            assertEquals("sr_awm", updated.save.selectedWeaponId)
            assertEquals("pistol_deagle", updated.save.secondaryWeaponId)
            assertEquals("melee_pan", updated.save.meleeWeaponId)

            val attachments = updated.save.attachments()
            assertEquals(OpticAttachment.SCOPE_8X, attachments.optic)
            assertEquals(MuzzleAttachment.SUPPRESSOR, attachments.muzzle)
            assertEquals(MagazineAttachment.EXTENDED, attachments.magazine)
            assertEquals(UnderbarrelAttachment.FOREGRIP, attachments.underbarrel)

            assertEquals("bn", updated.save.languageCode)
            assertEquals(1.6f, updated.save.cameraSensitivity, 0.01f)
            assertEquals(GraphicsQuality.HIGH.id, updated.save.graphicsQualityId)
            assertTrue(updated.save.invertYAxis)
            assertEquals(AiDifficulty.HARD.id, updated.save.aiDifficultyId)
            assertFalse(updated.save.audioEnabled)
            assertEquals(250, updated.save.xp)
            assertEquals(1500, updated.save.takaCredits)
            assertTrue("dhaka" in updated.completedDistrictIds)
            assertTrue("gazipur" in updated.unlockedDistrictIds)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Room v3 social and account repository hashes passwords, manages friends, logs moderation reports, and clamps rewards`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val socialRepo = SocialAndAccountRepository(db.socialAccountDao())

            // Short/weak password rejected cleanly
            val badPass = socialRepo.registerAccount("Dhaka_Tester", "123")
            assertTrue(badPass.isFailure)

            // Valid registration hashes password with salt (never stored in plaintext)
            val reg = socialRepo.registerAccount("Dhaka_Tester", "SecretPass99")
            assertTrue(reg.isSuccess)
            val acc = reg.getOrThrow()
            assertTrue(acc.playerId.startsWith("BD-"))
            val storedEntity = db.socialAccountDao().findAccountByUsername("Dhaka_Tester")
            assertNotNull(storedEntity)
            assertNotEquals("SecretPass99", storedEntity!!.passwordHash)

            // Sign out, verify bad password fails, valid password signs in
            socialRepo.logoutAccount()
            assertTrue(socialRepo.loginAccount("Dhaka_Tester", "WrongPassword").isFailure)
            assertTrue(socialRepo.loginAccount("Dhaka_Tester", "SecretPass99").isSuccess)

            // Seeded incoming request acceptance, directory search, friend request, mute, remove
            socialRepo.acceptFriendRequest("bot_host_1")
            val search = socialRepo.searchPlayers("Chawk")
            assertTrue(search.any { it.username == "ChawkRanger" })
            socialRepo.sendFriendRequest("bot_host_2", "ChawkRanger")
            socialRepo.toggleMuteFriend("bot_host_1")

            val friends = socialRepo.friendsFlow.first()
            val leader = friends.first { it.playerId == "bot_host_1" }
            assertEquals(FriendStatus.ACCEPTED, leader.status)
            assertTrue(leader.isMuted)

            socialRepo.rejectOrRemoveFriend("bot_host_2")
            assertFalse(socialRepo.friendsFlow.first().any { it.playerId == "bot_host_2" })

            // Moderation report saved cleanly
            socialRepo.reportPlayer("bad_user_1", "ToxicBot", ReportReason.ABUSIVE_CHAT, "Spamming plaza")
            assertEquals(1, socialRepo.reportsFlow.first().size)

            // Match stats & achievements
            socialRepo.applyMatchStats(xpEarned = 280, won = true, kills = 6)
            val updatedAcc = socialRepo.activeAccountFlow.first()!!
            val achievements = AchievementEvaluator.evaluate(
                xp = updatedAcc.xp,
                matchesPlayed = updatedAcc.matchesPlayed,
                matchesWon = updatedAcc.matchesWon,
                eliminations = updatedAcc.eliminations,
                unlockedDistrictsCount = 2,
                acceptedFriendsCount = 1
            )
            assertTrue(achievements.all { it.isUnlocked })

            // Server reward clamp prevents forged massive XP/Taka
            val (clampedXp, clampedTaka) = ServerAuthorityValidator.clampVerifiedMatchReward(999999, 999999, won = true)
            assertEquals(ServerAuthorityValidator.MAX_REWARD_XP_PER_MATCH, clampedXp)
            assertEquals(ServerAuthorityValidator.MAX_REWARD_TAKA_PER_MATCH, clampedTaka)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Room v4 sequential unlock gates next district behind both mission and challenge and persists custom order`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = PlayerProgressRepository(db.saveDao())
            val start = repo.progressFlow.first()
            assertEquals("dhaka", start.currentDistrictId)
            assertEquals(0, start.completionPercent("dhaka"))

            // Custom order: dhaka -> chattogram -> rest
            val customOrder = listOf("dhaka", "chattogram") +
                start.districtOrder.filter { it != "dhaka" && it != "chattogram" }
            repo.setConfigurableDistrictOrder(customOrder)

            // Cannot fast-travel to locked district
            assertFalse(repo.travelToDistrict("chattogram"))

            // Mission alone sets completion to 50% and does NOT unlock Chattogram yet
            val afterMissionUnlocked = repo.recordDistrictMissionComplete("dhaka", xpEarned = 120, takaEarned = 450)
            assertEquals(null, afterMissionUnlocked)
            val midState = repo.progressFlow.first()
            assertEquals(50, midState.completionPercent("dhaka"))
            assertFalse("chattogram" in midState.unlockedDistrictIds)

            // Challenge completes Dhaka -> 100% and unlocks Chattogram per custom order
            val unlockedNext = repo.recordDistrictChallengeComplete(
                districtId = "dhaka",
                challengeId = "challenge_dhaka",
                xpEarned = 250,
                takaEarned = 1000,
                targetsEliminated = 6
            )
            assertEquals("chattogram", unlockedNext)
            assertTrue(repo.travelToDistrict("chattogram"))

            val finalState = repo.progressFlow.first()
            assertEquals(100, finalState.completionPercent("dhaka"))
            assertTrue("dhaka" in finalState.completedDistrictIds)
            assertTrue("chattogram" in finalState.unlockedDistrictIds)
            assertEquals("chattogram", finalState.currentDistrictId)
            assertTrue("chattogram" in finalState.discoveredTravelNodes)
        } finally {
            db.close()
        }
    }
}

