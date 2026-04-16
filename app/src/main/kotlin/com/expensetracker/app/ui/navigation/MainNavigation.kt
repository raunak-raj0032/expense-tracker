package com.expensetracker.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensetracker.app.ui.screens.budget.BudgetSetupScreen
import com.expensetracker.app.ui.screens.analytics.AnalyticsScreen
import com.expensetracker.app.ui.screens.calendar.CalendarScreen
import com.expensetracker.app.ui.screens.capture.CaptureReviewScreen
import com.expensetracker.app.ui.screens.home.HomeScreen
import com.expensetracker.app.ui.screens.imports.StatementImportScreen
import com.expensetracker.app.ui.screens.ledger.LedgerScreen
import com.expensetracker.app.ui.screens.settings.SettingsScreen
import com.expensetracker.app.ui.screens.transaction.AddEditTransactionScreen
import com.expensetracker.app.ui.theme.AuroraBackground
import com.expensetracker.app.ui.theme.ScreenEdgePadding

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    data object Ledger : BottomNavItem("ledger", Icons.AutoMirrored.Filled.List, "Ledger")
    data object Calendar : BottomNavItem("calendar", Icons.Default.CalendarMonth, "Calendar")
    data object Analytics : BottomNavItem("analytics", Icons.Default.BarChart, "Analytics")
    data object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Ledger,
    BottomNavItem.Calendar,
    BottomNavItem.Analytics,
    BottomNavItem.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    darkThemeEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shouldShowBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (shouldShowBottomBar) {
                    Surface(
                        modifier = Modifier.padding(horizontal = ScreenEdgePadding, vertical = 10.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ),
                        shadowElevation = 10.dp
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(
                                                    width = if (selected) 1.dp else 0.dp,
                                                    color = if (selected) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(18.dp)
                                                ),
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            } else {
                                                Color.Transparent
                                            }
                                        ) {
                                            androidx.compose.foundation.layout.Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (selected) {
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        } else {
                                                            Brush.verticalGradient(
                                                                colors = listOf(Color.Transparent, Color.Transparent)
                                                            )
                                                        }
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    item.icon,
                                                    contentDescription = item.label,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onViewLedger = { navController.navigate(Screen.LedgerDetail.route) },
                        onOpenBudget = { navController.navigate(Screen.Budgets.route) },
                        onOpenCaptureInbox = { navController.navigate(Screen.CaptureInbox.route) },
                        onOpenStatementImport = { navController.navigate(Screen.Import.route) },
                        onTransactionClick = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        }
                    )
                }
                composable(Screen.Ledger.route) {
                    LedgerScreen(
                        onTransactionClick = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        }
                    )
                }
                composable(Screen.LedgerDetail.route) {
                    LedgerScreen(
                        onTransactionClick = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Calendar.route) {
                    CalendarScreen(
                        onDayClick = { /* Stay on current day */ },
                        onTransactionClick = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        }
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onOpenCaptureInbox = { navController.navigate(Screen.CaptureInbox.route) },
                        onOpenBudget = { navController.navigate(Screen.Budgets.route) },
                        onOpenStatementImport = { navController.navigate(Screen.Import.route) },
                        isDarkModeEnabled = darkThemeEnabled,
                        onDarkModeChange = onDarkThemeChange
                    )
                }
                composable(Screen.Budgets.route) {
                    BudgetSetupScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.CaptureInbox.route) {
                    CaptureReviewScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Import.route) {
                    StatementImportScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.AddTransaction.route) {
                    AddEditTransactionScreen(
                        transactionId = null,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.EditTransaction.route,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val transactionId = backStackEntry.arguments?.getLong("transactionId")
                    AddEditTransactionScreen(
                        transactionId = transactionId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
