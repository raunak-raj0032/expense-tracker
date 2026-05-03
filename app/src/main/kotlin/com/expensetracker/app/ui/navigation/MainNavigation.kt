package com.expensetracker.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.auth.AuthViewModel
import com.expensetracker.app.ui.screens.login.EmailAuthScreen
import com.expensetracker.app.ui.screens.login.LoginScreen
import com.expensetracker.app.ui.screens.onboarding.OnboardingScreen
import com.expensetracker.app.ui.screens.profile.ProfileScreen
import com.expensetracker.app.ui.screens.budget.BudgetSetupScreen
import com.expensetracker.app.ui.screens.analytics.AnalyticsScreen
import com.expensetracker.app.ui.screens.calendar.CalendarScreen
import com.expensetracker.app.ui.screens.capture.CaptureReviewScreen
import com.expensetracker.app.ui.screens.home.HomeScreen
import com.expensetracker.app.ui.screens.imports.StatementImportScreen
import com.expensetracker.app.ui.screens.ledger.LedgerScreen
import com.expensetracker.app.ui.screens.settings.SettingsScreen
import com.expensetracker.app.ui.screens.tags.TagsManagementScreen
import com.expensetracker.app.ui.screens.transaction.AddEditTransactionScreen
import com.expensetracker.app.ui.mascot.TutorialTarget
import com.expensetracker.app.ui.theme.AuroraBackground
import com.expensetracker.app.ui.theme.ScreenEdgePadding

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home      : BottomNavItem("home",      Icons.Default.Home,                    "Home")
    data object Ledger    : BottomNavItem("ledger",    Icons.AutoMirrored.Filled.List,        "Ledger")
    data object Calendar  : BottomNavItem("calendar",  Icons.Default.CalendarMonth,           "Calendar")
    data object Analytics : BottomNavItem("analytics", Icons.Default.BarChart,                "Analytics")
    data object Settings  : BottomNavItem("settings",  Icons.Default.Settings,               "Settings")
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Ledger,
    BottomNavItem.Calendar,
    BottomNavItem.Analytics,
    BottomNavItem.Settings
)

@Composable
fun MainNavigation(
    darkThemeEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    captureInboxRequest: Int = 0
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val gateViewModel: AppGateViewModel = hiltViewModel()
    val onboardingSeen by gateViewModel.onboardingSeen.collectAsStateWithLifecycle()
    val homeCurrency by gateViewModel.homeCurrency.collectAsStateWithLifecycle()
    val firstRunPermissionsPrompted by gateViewModel.firstRunPermissionsPrompted.collectAsStateWithLifecycle()
    val tutorialSeen by gateViewModel.tutorialSeen.collectAsStateWithLifecycle()
    val tutorialTargets = remember { mutableStateMapOf<TutorialTarget, Rect>() }
    val onTutorialTargetPositioned: (TutorialTarget, Rect) -> Unit = { target, bounds ->
        tutorialTargets[target] = bounds
    }

    LaunchedEffect(authState, onboardingSeen) {
        val seen = onboardingSeen ?: return@LaunchedEffect
        val target = when {
            !seen -> Screen.Onboarding.route
            authState is AuthState.SignedOut -> Screen.Login.route
            authState is AuthState.SignedIn -> Screen.Home.route
            else -> null
        } ?: return@LaunchedEffect
        val gatedRoutes = setOf(Screen.Onboarding.route, Screen.Login.route, Screen.EmailAuth.route)
        val current = currentDestination?.route
        val needsRedirect = when (target) {
            Screen.Onboarding.route -> current != target
            Screen.Login.route -> current != Screen.Login.route && current != Screen.EmailAuth.route
            else -> current in gatedRoutes
        }
        if (needsRedirect) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(captureInboxRequest, authState) {
        if (captureInboxRequest > 0 && authState is AuthState.SignedIn && currentDestination?.route != Screen.CaptureInbox.route) {
            navController.navigate(Screen.CaptureInbox.route) { launchSingleTop = true }
        }
    }

    val shouldShowBottomBar = authState is AuthState.SignedIn && bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val startDestination = when {
        onboardingSeen == false -> Screen.Onboarding.route
        authState is AuthState.SignedIn -> Screen.Home.route
        else -> Screen.Login.route
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowBottomBar,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(380, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(380)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(280))
                ) {
                    PremiumNavBar(
                        items = bottomNavItems,
                        currentDestination = currentDestination,
                        modifier = Modifier.onGloballyPositioned {
                            tutorialTargets[TutorialTarget.BottomBar] = it.boundsInRoot()
                        },
                        onItemClick = { item ->
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
        ) { innerPadding ->
            androidx.compose.runtime.CompositionLocalProvider(
                com.expensetracker.app.ui.money.LocalHomeCurrency provides homeCurrency
            ) {
            NavHost(
                navController    = navController,
                startDestination = startDestination,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(onFinish = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onContinueWithEmail = { navController.navigate(Screen.EmailAuth.route) }
                    )
                }
                composable(Screen.EmailAuth.route) {
                    EmailAuthScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = authViewModel
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        authViewModel = authViewModel
                    )
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        onAddTransaction     = { navController.navigate(Screen.AddTransaction.route) },
                        onViewLedger         = { navController.navigate(Screen.LedgerDetail.route) },
                        onOpenBudget         = { navController.navigate(Screen.Budgets.route) },
                        onOpenCaptureInbox   = { navController.navigate(Screen.CaptureInbox.route) },
                        onOpenStatementImport= { navController.navigate(Screen.Import.route) },
                        onTransactionClick   = { navController.navigate(Screen.EditTransaction.createRoute(it)) },
                        onOpenProfile        = { navController.navigate(Screen.Profile.route) },
                        onTutorialTargetPositioned = onTutorialTargetPositioned
                    )
                }
                composable(Screen.Ledger.route) {
                    LedgerScreen(
                        onTransactionClick = { navController.navigate(Screen.EditTransaction.createRoute(it)) }
                    )
                }
                composable(Screen.LedgerDetail.route) {
                    LedgerScreen(
                        onTransactionClick = { navController.navigate(Screen.EditTransaction.createRoute(it)) },
                        onNavigateBack     = { navController.popBackStack() }
                    )
                }
                composable(Screen.Calendar.route) {
                    CalendarScreen(
                        onDayClick         = { },
                        onTransactionClick = { navController.navigate(Screen.EditTransaction.createRoute(it)) }
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onOpenCaptureInbox    = { navController.navigate(Screen.CaptureInbox.route) },
                        onOpenBudget          = { navController.navigate(Screen.Budgets.route) },
                        onOpenStatementImport = { navController.navigate(Screen.Import.route) },
                        onOpenTags            = { navController.navigate(Screen.Tags.route) },
                        onReplayTutorial      = {
                            gateViewModel.replayTutorial()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        isDarkModeEnabled     = darkThemeEnabled,
                        onDarkModeChange      = onDarkThemeChange
                    )
                }
                composable(Screen.Tags.route) {
                    TagsManagementScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Budgets.route) {
                    BudgetSetupScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.CaptureInbox.route) {
                    CaptureReviewScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.Import.route) {
                    StatementImportScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(Screen.AddTransaction.route) {
                    AddEditTransactionScreen(
                        transactionId = null,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route     = Screen.EditTransaction.route,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
                ) { backStack ->
                    AddEditTransactionScreen(
                        transactionId  = backStack.arguments?.getLong("transactionId"),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            }
        }

        if (authState is AuthState.SignedIn && !firstRunPermissionsPrompted) {
            FirstRunPermissionPrompt(
                onPromptHandled = gateViewModel::markFirstRunPermissionsPrompted
            )
        }

        // Penny's guided tour — runs once after sign-in, after the perms prompt, while on Home.
        val onHome = currentDestination?.route == Screen.Home.route
        val showTutorial = authState is AuthState.SignedIn &&
            firstRunPermissionsPrompted &&
            !tutorialSeen &&
            onHome
        AnimatedVisibility(
            visible = showTutorial,
            enter = fadeIn(tween(420)),
            exit = fadeOut(tween(280))
        ) {
            com.expensetracker.app.ui.mascot.TutorialOverlay(
                onFinish = gateViewModel::markTutorialSeen,
                targetBounds = tutorialTargets
            )
        }
    }
}

@Composable
private fun FirstRunPermissionPrompt(
    onPromptHandled: () -> Unit
) {
    val context = LocalContext.current
    val missingPermissions = remember(context) {
        firstRunRuntimePermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    var showPrompt by remember(missingPermissions) { mutableStateOf(missingPermissions.isNotEmpty()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPromptHandled()
    }

    LaunchedEffect(missingPermissions.isEmpty()) {
        if (missingPermissions.isEmpty()) {
            onPromptHandled()
        }
    }

    if (!showPrompt || missingPermissions.isEmpty()) return

    AlertDialog(
        onDismissRequest = {
            showPrompt = false
            onPromptHandled()
        },
        title = { Text("Allow app permissions") },
        text = {
            Text(
                "Pocket Pulse can scan transaction SMS messages and send budget or capture alerts. You can change these permissions later in Settings."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    showPrompt = false
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                }
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showPrompt = false
                    onPromptHandled()
                }
            ) {
                Text("Not now")
            }
        }
    )
}

private fun firstRunRuntimePermissions(): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions
}

@Composable
private fun PremiumNavBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    modifier: Modifier = Modifier,
    onItemClick: (BottomNavItem) -> Unit
) {
    val primary       = MaterialTheme.colorScheme.primary
    val surface       = MaterialTheme.colorScheme.surface
    val surfaceVariant= MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenEdgePadding, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.35f),
                            primary.copy(alpha = 0.08f),
                            surfaceVariant.copy(alpha = 0.4f),
                            primary.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.95f),
                            surfaceVariant.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavBarItem(
                        modifier = Modifier.weight(1f),
                        item     = item,
                        selected = selected,
                        onClick  = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    modifier: Modifier = Modifier,
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val iconScale by animateFloatAsState(
        targetValue   = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )
    val dotWidth by animateDpAsState(
        targetValue   = if (selected) 20.dp else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "dotWidth"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) primary.copy(alpha = 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector    = item.icon,
                contentDescription = item.label,
                tint           = if (selected) primary else onSurfaceVariant,
                modifier       = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
        }

        Text(
            text       = item.label,
            style      = MaterialTheme.typography.labelSmall,
            color      = if (selected) primary else onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines   = 1,
            softWrap   = false,
            overflow   = TextOverflow.Visible
        )

        // Active dot indicator
        Box(
            modifier = Modifier
                .width(dotWidth)
                .height(2.dp)
                .clip(CircleShape)
                .background(primary)
        )
    }
}
