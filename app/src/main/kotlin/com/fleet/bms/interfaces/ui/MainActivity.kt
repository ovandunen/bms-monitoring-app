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
import com.fleet.bms.interfaces.ui.dashboard.DashboardScreen
import com.fleet.bms.interfaces.ui.theme.BmsTheme
import com.fleet.bms.sniffer.presentation.CanSnifferScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity
 *
 * Entry point for the BMS Monitoring App.
 * Navigation: Dashboard | CAN Sniffer
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BmsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToSniffer = { navController.navigate("sniffer") }
                            )
                        }
                        composable("sniffer") {
                            CanSnifferScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
