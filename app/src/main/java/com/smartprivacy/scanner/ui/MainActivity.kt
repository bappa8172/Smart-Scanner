package com.smartprivacy.scanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartprivacy.scanner.ui.theme.SmartPrivacyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPrivacyTheme {
                val viewModel: ScannerViewModel = viewModel()
                val state by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        if (state.isScanning) {
                            ScanProgressScreen(state = state)
                        } else {
                            HomeScreen(
                                state = state,
                                onStartScan = { viewModel.startScan() },
                                onCleanCache = { viewModel.onBoostClicked() },
                                onNavigateToAppList = { navController.navigate("app_list") },
                                onNavigateToRiskyApps = { navController.navigate("app_list?tab=0&riskyOnly=true") },
                                onDismissPermissionDialog = { viewModel.dismissPermissionDialog() },
                                onRequestPermission = { viewModel.requestPermission() },
                                onOpenSystemCleaner = { viewModel.openSystemDeepClean() }
                            )
                        }
                    }
                    composable(
                        route = "app_list?tab={tab}&riskyOnly={riskyOnly}",
                        arguments = listOf(
                            navArgument("tab") { 
                                type = NavType.StringType
                                defaultValue = "0"
                                nullable = true 
                            },
                            navArgument("riskyOnly") { 
                                type = NavType.StringType
                                defaultValue = "false"
                                nullable = true 
                            }
                        )
                    ) { backStackEntry ->
                        val initialTab = backStackEntry.arguments?.getString("tab")?.toIntOrNull() ?: 0
                        val riskyOnly = backStackEntry.arguments?.getString("riskyOnly")?.toBoolean() ?: false
                        
                        AppListScreen(
                            systemApps = state.systemApps,
                            installedApps = state.installedApps,
                            storageStats = state.appStorageDetails,
                            showRiskyOnly = riskyOnly,
                            onAppClick = { packageName -> navController.navigate("app_detail/$packageName") },
                            onBack = { navController.popBackStack() },
                            initialTab = initialTab
                        )
                    }
                    composable(
                        route = "app_detail/{packageName}",
                        arguments = listOf(
                            navArgument("packageName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val packageName = backStackEntry.arguments?.getString("packageName")
                        val app = state.apps.find { it.packageName == packageName }
                        if (app != null) {
                            AppDetailScreen(
                                app = app,
                                storageStats = state.appStorageDetails[app.packageName],
                                vtLoading = state.vtLoading,
                                onCheckVT = { viewModel.checkVirusTotal(app) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
