package com.blahmonster.prooflab

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blahmonster.prooflab.core.AlertScheduler
import com.blahmonster.prooflab.ui.LocalPalette
import com.blahmonster.prooflab.ui.ProofLabTheme
import com.blahmonster.prooflab.ui.ProofType
import com.blahmonster.prooflab.ui.screens.BatchDetailScreen
import com.blahmonster.prooflab.ui.screens.LogScreen
import com.blahmonster.prooflab.ui.screens.MixSheetScreen
import com.blahmonster.prooflab.ui.screens.NewBatchScreen
import com.blahmonster.prooflab.ui.screens.ProofingScreen
import com.blahmonster.prooflab.ui.screens.ReviewScreen
import com.blahmonster.prooflab.ui.screens.SettingsScreen
import com.blahmonster.prooflab.ui.screens.StyleDetailScreen
import com.blahmonster.prooflab.ui.screens.StylesScreen

class MainActivity : ComponentActivity() {
	private val model: AppViewModel by viewModels()

	private val permissionLauncher =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
			model.onPermissionResult(granted)
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			ProofLabTheme {
				ProofLabApp(
					model = model,
					onRequestNotifications = { requestNotificationPermission() },
				)
			}
		}
	}

	override fun onResume() {
		super.onResume()
		// Stages elapse while the app is away; the count is only right after a refresh.
		model.refresh()
	}

	private fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		} else {
			model.onPermissionResult(true)
		}
	}
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
	Tab("proofing", "Proofing", Icons.Outlined.Timer),
	Tab("styles", "Styles", Icons.Outlined.Science),
	Tab("log", "Log", Icons.AutoMirrored.Outlined.MenuBook),
	Tab("settings", "Settings", Icons.Outlined.Settings),
)

@Composable
fun ProofLabApp(model: AppViewModel, onRequestNotifications: () -> Unit) {
	val palette = LocalPalette.current
	val navController = rememberNavController()
	val backStack by navController.currentBackStackEntryAsState()
	val currentRoute = backStack?.destination
	val batches by model.batches.collectAsStateWithLifecycle()
	val dueCount = AlertScheduler.currentBadge(batches, System.currentTimeMillis())
	val onTab = tabs.any { tab -> currentRoute?.hierarchy?.any { it.route == tab.route } == true }

	Scaffold(
		containerColor = palette.paper,
		bottomBar = {
			if (onTab) {
				NavigationBar(containerColor = palette.card) {
					tabs.forEach { tab ->
						val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
						NavigationBarItem(
							selected = selected,
							onClick = {
								navController.navigate(tab.route) {
									popUpTo(navController.graph.findStartDestination().id) {
										saveState = true
									}
									launchSingleTop = true
									restoreState = true
								}
							},
							icon = {
								if (tab.route == "proofing" && dueCount > 0) {
									BadgedBox(badge = { Badge { Text("$dueCount") } }) {
										Icon(tab.icon, contentDescription = tab.label)
									}
								} else {
									Icon(tab.icon, contentDescription = tab.label)
								}
							},
							label = { Text(tab.label, style = ProofType.label) },
						)
					}
				}
			}
		},
		floatingActionButton = {
			if (currentRoute?.route == "proofing") {
				FloatingActionButton(
					onClick = { navController.navigate("new") },
					containerColor = palette.hot,
				) {
					Icon(Icons.Filled.Add, contentDescription = "New batch", tint = palette.paper)
				}
			}
		},
	) { padding ->
		Box(
			Modifier
				.fillMaxSize()
				.padding(padding)
				.background(palette.paper),
		) {
			NavHost(navController = navController, startDestination = "proofing") {
				composable("proofing") {
					ProofingScreen(
						model = model,
						onOpenBatch = { navController.navigate("batch/$it") },
						onRequestNotifications = onRequestNotifications,
					)
				}
				composable("styles") {
					StylesScreen(onOpenStyle = { navController.navigate("style/$it") })
				}
				composable("log") {
					LogScreen(model = model, onOpenBatch = { navController.navigate("batch/$it") })
				}
				composable("settings") {
					SettingsScreen(model = model, onRequestNotifications = onRequestNotifications)
				}
				composable("new") {
					NewBatchScreen(model = model, onDone = { navController.popBackStack() })
				}
				composable("batch/{id}") { entry ->
					BatchDetailScreen(
						model = model,
						batchId = entry.arguments?.getString("id").orEmpty(),
						onOpenMixSheet = { navController.navigate("mix/$it") },
						onOpenReview = { navController.navigate("review/$it") },
						onGone = { navController.popBackStack() },
					)
				}
				composable("mix/{id}") { entry ->
					MixSheetScreen(model = model, batchId = entry.arguments?.getString("id").orEmpty())
				}
				composable("review/{id}") { entry ->
					ReviewScreen(
						model = model,
						batchId = entry.arguments?.getString("id").orEmpty(),
						onDone = { navController.popBackStack() },
					)
				}
				composable("style/{id}") { entry ->
					StyleDetailScreen(
						model = model,
						styleId = entry.arguments?.getString("id").orEmpty(),
						onStarted = { navController.navigate("batch/$it") },
					)
				}
			}
		}
	}
}
