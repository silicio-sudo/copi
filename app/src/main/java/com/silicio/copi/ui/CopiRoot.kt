package com.silicio.copi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silicio.copi.nav.Copi
import com.silicio.copi.ui.screens.AboutScreen
import com.silicio.copi.ui.screens.DashboardScreen
import com.silicio.copi.ui.screens.HelpScreen
import com.silicio.copi.ui.screens.MapScreen
import com.silicio.copi.ui.screens.PlannerScreen
import com.silicio.copi.ui.screens.RoadbooksScreen
import com.silicio.copi.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    INICIO("Inicio", Icons.Filled.Speed),
    TRAMO("Tramo", Icons.Filled.Map),
    MAPA("Mapa", Icons.Filled.Navigation),
    RUTA("Ruta", Icons.Filled.Explore),
    AJUSTES("Ajustes", Icons.Filled.Tune)
}

private enum class InfoPage { HELP, ABOUT }

@Composable
fun CopiRoot(
    hasLocation: Boolean,
    onRequestPermission: () -> Unit
) {
    val brand = LocalBrand.current
    val state by Copi.engine.state.collectAsStateWithLifecycle()
    val settings by Copi.settings.state.collectAsStateWithLifecycle()
    val roadbooks by Copi.roadbooks.collectAsStateWithLifecycle()
    val importing by Copi.importing.collectAsStateWithLifecycle()
    val error by Copi.lastError.collectAsStateWithLifecycle()
    val message by Copi.message.collectAsStateWithLifecycle()
    val pending by Copi.pendingDestination.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.INICIO) }
    var infoPage by rememberSaveable { mutableStateOf<InfoPage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pending) {
        if (!pending.isNullOrBlank()) tab = Tab.RUTA
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            Copi.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            Copi.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = brand.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (infoPage == null) {
                NavigationBar(
                    containerColor = brand.surface,
                    tonalElevation = 0.dp
                ) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = brand.primary,
                                selectedTextColor = brand.primary,
                                indicatorColor = brand.primary.copy(alpha = 0.14f),
                                unselectedIconColor = brand.muted,
                                unselectedTextColor = brand.muted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            val page = infoPage
            if (page != null) {
                when (page) {
                    InfoPage.HELP -> HelpScreen(onBack = { infoPage = null })
                    InfoPage.ABOUT -> AboutScreen(onBack = { infoPage = null })
                }
            } else {
                when (tab) {
                    Tab.INICIO -> DashboardScreen(
                        state = state,
                        settings = settings,
                        hasLocation = hasLocation,
                        onRequestPermission = onRequestPermission,
                        onOpenRoadbooks = { tab = Tab.TRAMO },
                        onOpenPlanner = { tab = Tab.RUTA }
                    )
                    Tab.TRAMO -> RoadbooksScreen(
                        roadbooks = roadbooks,
                        settings = settings,
                        state = state,
                        importing = importing,
                        onNavigateHome = { tab = Tab.INICIO }
                    )
                    Tab.MAPA -> MapScreen(
                        state = state,
                        settings = settings,
                        onOpenRoadbooks = { tab = Tab.TRAMO }
                    )
                    Tab.RUTA -> PlannerScreen(
                        settings = settings,
                        state = state,
                        onGenerated = { tab = Tab.INICIO }
                    )
                    Tab.AJUSTES -> SettingsScreen(
                        settings = settings,
                        onOpenHelp = {
                            infoPage = InfoPage.HELP
                            tab = Tab.AJUSTES
                        },
                        onOpenAbout = {
                            infoPage = InfoPage.ABOUT
                            tab = Tab.AJUSTES
                        }
                    )
                }
            }
        }
    }
}
