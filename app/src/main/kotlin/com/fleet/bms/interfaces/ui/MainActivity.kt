package com.fleet.bms.interfaces.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fleet.bms.BuildConfig
import com.fleet.bms.interfaces.ui.dashboard.DashboardScreen
import com.fleet.bms.interfaces.ui.ecocar.EcoCarShell
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarTheme
import com.fleet.bms.sniffer.presentation.CanSnifferScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity — Standard-Start ist die **EcoCar-Shell** (Pixel-Tablet).
 * Routen `legacy_dashboard` / `legacy_sniffer`: im **Debug**-Build über Settings → Entwickler erreichbar.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EcoCarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_ECOCAR,
                    ) {
                        composable(ROUTE_ECOCAR) {
                            EcoCarShell(
                                onDevNavigateToLegacyDashboard = if (BuildConfig.DEBUG) {
                                    { navController.navigate(ROUTE_LEGACY_DASHBOARD) }
                                } else {
                                    null
                                },
                                onDevNavigateToLegacySniffer = if (BuildConfig.DEBUG) {
                                    { navController.navigate(ROUTE_LEGACY_SNIFFER) }
                                } else {
                                    null
                                },
                            )
                        }
                        composable(ROUTE_LEGACY_DASHBOARD) {
                            DashboardScreen(
                                onNavigateToSniffer = { navController.navigate(ROUTE_LEGACY_SNIFFER) },
                            )
                        }
                        composable(ROUTE_LEGACY_SNIFFER) {
                            CanSnifferScreen(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ROUTE_ECOCAR = "ecocar"
        const val ROUTE_LEGACY_DASHBOARD = "legacy_dashboard"
        const val ROUTE_LEGACY_SNIFFER = "legacy_sniffer"
    }
}
