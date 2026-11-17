package com.autodub.edge.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autodub.edge.ui.screens.AboutScreen
import com.autodub.edge.ui.screens.CharacterManagerScreen
import com.autodub.edge.ui.screens.DashboardScreen
import com.autodub.edge.ui.screens.RoiConfigScreen
import com.autodub.edge.ui.screens.TranslationSettingsScreen
import com.autodub.edge.ui.screens.VoiceSettingsScreen
import com.autodub.edge.ui.theme.CyberColors
import com.autodub.edge.viewmodel.CharacterViewModel
import com.autodub.edge.viewmodel.MainViewModel

sealed class AutoDubRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : AutoDubRoute("dashboard", "Home", Icons.Default.Dashboard)
    data object RoiConfig : AutoDubRoute("roi_config", "ROI", Icons.Default.CropFree)
    data object Characters : AutoDubRoute("characters", "Karakter", Icons.Default.People)
    data object Voice : AutoDubRoute("voice_settings", "Voice", Icons.Default.RecordVoiceOver)
    data object Translation : AutoDubRoute("translation_settings", "Translate", Icons.Default.Translate)
    data object About : AutoDubRoute("about", "About", Icons.Default.Info)
}

@Composable
fun AutoDubAppScaffold(
    mainViewModel: MainViewModel,
    characterViewModel: CharacterViewModel,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartCaptureFlow: () -> Unit,
    onStopCaptureAndOverlay: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val runtimeState by mainViewModel.runtimeState.collectAsState()
    val roiLayout by mainViewModel.roiLayoutState.collectAsState()
    val overlayConfig by mainViewModel.overlayConfigState.collectAsState()
    val dubbingSettings by mainViewModel.dubbingSettingsState.collectAsState()
    val recentDialogues by mainViewModel.recentDialogues.collectAsState()
    val availableVoices by mainViewModel.availableVoices.collectAsState()
    val cacheSizeBytes by mainViewModel.cacheSizeBytes.collectAsState()
    val feedback by mainViewModel.feedbackMessage.collectAsState()

    val characters by characterViewModel.characters.collectAsState()
    val providerVoices by characterViewModel.providerVoices.collectAsState()
    val offlineVoices by characterViewModel.offlineVoices.collectAsState()
    val characterStatus by characterViewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearFeedback()
        }
    }

    LaunchedEffect(characterStatus) {
        characterStatus?.let {
            snackbarHostState.showSnackbar(it)
            characterViewModel.clearStatus()
        }
    }

    val navItems = listOf(
        AutoDubRoute.Dashboard,
        AutoDubRoute.RoiConfig,
        AutoDubRoute.Characters,
        AutoDubRoute.Voice,
        AutoDubRoute.Translation
    )

    Scaffold(
        containerColor = CyberColors.DarkNavy,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = CyberColors.SurfaceNavy) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberColors.CyberCyan,
                            selectedTextColor = CyberColors.CyberCyan,
                            unselectedIconColor = CyberColors.TextSecondary,
                            unselectedTextColor = CyberColors.TextSecondary,
                            indicatorColor = CyberColors.CardNavy
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AutoDubRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AutoDubRoute.Dashboard.route) {
                DashboardScreen(
                    runtimeState = runtimeState,
                    recentDialogues = recentDialogues,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onStartTranslation = onStartCaptureFlow,
                    onStopTranslation = onStopCaptureAndOverlay,
                    onDownloadModel = { mainViewModel.downloadOfflineModel() },
                    onNavigateRoi = { navController.navigate(AutoDubRoute.RoiConfig.route) },
                    onNavigateCharacters = { navController.navigate(AutoDubRoute.Characters.route) },
                    onNavigateVoiceSettings = { navController.navigate(AutoDubRoute.Voice.route) },
                    onNavigateTranslationSettings = { navController.navigate(AutoDubRoute.Translation.route) },
                    onNavigateAbout = { navController.navigate(AutoDubRoute.About.route) }
                )
            }
            composable(AutoDubRoute.RoiConfig.route) {
                RoiConfigScreen(
                    roiLayout = roiLayout,
                    onSaveLayout = { mainViewModel.saveRoiLayout(it) },
                    onResetDefault = { mainViewModel.resetRoiLayout() },
                    onToggleOverlaySetupMode = { mainViewModel.setRoiMode(it) }
                )
            }
            composable(AutoDubRoute.Characters.route) {
                CharacterManagerScreen(
                    characters = characters,
                    availableVoices = providerVoices,
                    offlineVoices = offlineVoices,
                    statusMessage = characterStatus,
                    onLoadVoicesForProvider = { characterViewModel.loadVoices(it) },
                    onSaveCharacter = { characterViewModel.saveCharacter(it) },
                    onDeleteCharacter = { characterViewModel.deleteCharacter(it) },
                    onTestVoice = { characterViewModel.testVoice(it) },
                    onBuildVoicePromptPreview = { profile ->
                        characterViewModel.buildVoicePromptPreview(profile)
                    },
                    onBuildSsmlPreview = { profile, sample ->
                        characterViewModel.buildSsmlPreview(profile, sample)
                    }
                )
            }
            composable(AutoDubRoute.Voice.route) {
                VoiceSettingsScreen(
                    settings = dubbingSettings,
                    availableVoices = availableVoices,
                    cacheSizeBytes = cacheSizeBytes,
                    onProviderChanged = { mainViewModel.loadVoicesForProvider(it.id) },
                    onSaveSettings = { mainViewModel.saveDubbingSettings(it) },
                    onClearAudioCache = { mainViewModel.clearAudioCache() },
                    onTestVoice = { mainViewModel.testGlobalVoice() },
                    onSkipCurrent = { mainViewModel.skipAudio() }
                )
            }
            composable(AutoDubRoute.Translation.route) {
                TranslationSettingsScreen(
                    dubbingSettings = dubbingSettings,
                    overlayConfig = overlayConfig,
                    modelState = runtimeState.translationModelState,
                    onSaveDubbingSettings = { mainViewModel.saveDubbingSettings(it) },
                    onSaveOverlayConfig = { mainViewModel.saveOverlayConfig(it) },
                    onDownloadOfflineModel = { mainViewModel.downloadOfflineModel() }
                )
            }
            composable(AutoDubRoute.About.route) {
                AboutScreen()
            }
        }
    }
}
