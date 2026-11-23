package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameLocalization
import com.example.game.modes.GameModeId
import com.example.game.ui.GameViewModel
import com.example.game.ui.screens.ArchitectureDocsScreen
import com.example.game.ui.screens.DistrictsScreen
import com.example.game.ui.screens.GamePlayScreen
import com.example.game.ui.screens.InventoryScreen
import com.example.game.ui.screens.LobbyScreen
import com.example.game.ui.screens.ModesScreen
import com.example.game.ui.screens.MultiplayerHubScreen
import com.example.game.ui.screens.ProfileScreen
import com.example.game.ui.screens.SettingsScreen
import com.example.game.ui.screens.SocialAndAccountScreen
import com.example.ui.theme.DistrictWarriorsTheme

object GameRoutes {
    const val LOBBY = "lobby"
    const val DISTRICTS = "districts"
    const val MODES = "modes"
    const val INVENTORY = "inventory"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val DOCS = "docs"
    const val MULTIPLAYER_HUB = "multiplayer_hub"
    const val SOCIAL_ACCOUNT = "social_account"
    const val GAMEPLAY_SOLO = "gameplay_solo"
    const val GAMEPLAY_TRAINING = "gameplay_training"
    const val GAMEPLAY_MODE_PATTERN = "gameplay/{modeId}"

    fun gameplayForMode(mode: GameModeId): String = "gameplay/${mode.id}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val progress by viewModel.progressState.collectAsStateWithLifecycle()
            val lang = AppLanguage.fromCode(progress.save.languageCode)
            val strings = GameLocalization.strings(lang)

            DistrictWarriorsTheme(isBengali = lang == AppLanguage.BENGALI) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = GameRoutes.LOBBY
                ) {
                    composable(GameRoutes.LOBBY) {
                        LobbyScreen(
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onPlaySolo = { navController.navigate(GameRoutes.GAMEPLAY_SOLO) },
                            onPlayTrainingGround = { navController.navigate(GameRoutes.GAMEPLAY_TRAINING) },
                            onOpenDistricts = { navController.navigate(GameRoutes.DISTRICTS) },
                            onOpenModes = { navController.navigate(GameRoutes.MODES) },
                            onOpenInventory = { navController.navigate(GameRoutes.INVENTORY) },
                            onOpenSettings = { navController.navigate(GameRoutes.SETTINGS) },
                            onOpenProfile = { navController.navigate(GameRoutes.PROFILE) },
                            onOpenDocs = { navController.navigate(GameRoutes.DOCS) },
                            onOpenMultiplayer = { navController.navigate(GameRoutes.MULTIPLAYER_HUB) },
                            onOpenSocial = { navController.navigate(GameRoutes.SOCIAL_ACCOUNT) }
                        )
                    }

                    composable(GameRoutes.MULTIPLAYER_HUB) {
                        MultiplayerHubScreen(
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onEnterMultiplayerMatch = { mode ->
                                navController.navigate(GameRoutes.gameplayForMode(mode)) {
                                    popUpTo(GameRoutes.LOBBY)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.SOCIAL_ACCOUNT) {
                        SocialAndAccountScreen(
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onJoinFriendRoom = {
                                navController.navigate(GameRoutes.MULTIPLAYER_HUB) {
                                    popUpTo(GameRoutes.LOBBY)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.DISTRICTS) {
                        DistrictsScreen(
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onTravelToDistrict = { districtId, deploy ->
                                viewModel.travelToDistrict(districtId, alsoStreamIntoWorld = deploy) {
                                    if (deploy) {
                                        navController.navigate(GameRoutes.GAMEPLAY_SOLO) {
                                            popUpTo(GameRoutes.LOBBY)
                                        }
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.MODES) {
                        ModesScreen(
                            strings = strings,
                            lang = lang,
                            onLaunchMode = { mode ->
                                val target = when {
                                    mode == GameModeId.TRAINING_GROUND -> GameRoutes.GAMEPLAY_TRAINING
                                    mode == GameModeId.SOLO -> GameRoutes.GAMEPLAY_SOLO
                                    mode.requiresMultiplayer -> GameRoutes.MULTIPLAYER_HUB
                                    else -> GameRoutes.GAMEPLAY_SOLO
                                }
                                navController.navigate(target) {
                                    popUpTo(GameRoutes.LOBBY)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.INVENTORY) {
                        InventoryScreen(
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onSelectWeapon = { weaponId, slot -> viewModel.selectWeapon(weaponId, slot) },
                            onUpdateAttachments = { optic, muzzle, mag, grip ->
                                viewModel.updateAttachments(optic, muzzle, mag, grip)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.SETTINGS) {
                        SettingsScreen(
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.PROFILE) {
                        ProfileScreen(
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.DOCS) {
                        ArchitectureDocsScreen(
                            strings = strings,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(GameRoutes.GAMEPLAY_SOLO) {
                        GamePlayScreen(
                            mode = GameModeId.SOLO,
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onExitToLobby = {
                                navController.popBackStack(GameRoutes.LOBBY, inclusive = false)
                            }
                        )
                    }

                    composable(GameRoutes.GAMEPLAY_TRAINING) {
                        GamePlayScreen(
                            mode = GameModeId.TRAINING_GROUND,
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onExitToLobby = {
                                navController.popBackStack(GameRoutes.LOBBY, inclusive = false)
                            }
                        )
                    }

                    composable(
                        route = GameRoutes.GAMEPLAY_MODE_PATTERN,
                        arguments = listOf(navArgument("modeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val modeIdStr = backStackEntry.arguments?.getString("modeId").orEmpty()
                        val resolvedMode = GameModeId.entries.firstOrNull { it.id == modeIdStr } ?: GameModeId.FOUR_PLAYER_SQUAD
                        GamePlayScreen(
                            mode = resolvedMode,
                            viewModel = viewModel,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onExitToLobby = {
                                viewModel.socialDelegate.leaveRoom()
                                navController.popBackStack(GameRoutes.LOBBY, inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
