package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.isMaaryaSuperAdmin
import com.example.data.repository.MaaryaRepository
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreatorStudioScreen
import com.example.ui.screens.MaaryaFeedScreen
import com.example.ui.screens.MembershipTiersScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldDark
import com.example.ui.theme.MaaryaTheme
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.VelvetObsidian
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.WarmTaupe
import com.example.ui.viewmodel.MaaryaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaaryaTheme {
                AppContent()
            }
        }
    }
}

private enum class MaaryaTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FEED("feed", "Feed", Icons.Filled.Home, Icons.Outlined.Home),
    CLUB("club", "Club", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    MESSAGES("messages", "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    STUDIO("studio", "Studio", Icons.Filled.DashboardCustomize, Icons.Outlined.DashboardCustomize),
    ACCOUNT("account", "Account", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun AppContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    val playBillingManager = remember(context) {
        com.example.billing.PlayBillingManager(context = context)
    }

    DisposableEffect(playBillingManager) {
        playBillingManager.startConnection()
        onDispose { playBillingManager.endConnection() }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        val isAdmin = user.isMaaryaSuperAdmin()
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: if (isAdmin) "Maarya" else "Member"
        val email = user.email?.takeIf { it.isNotBlank() } ?: "$uid@member.maarya"

        key(uid) {
            val viewModel: MaaryaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MaaryaViewModel(
                            repository = MaaryaRepository(db),
                            currentUserId = uid,
                            isAdmin = isAdmin,
                            currentUserName = displayName,
                            currentUserEmail = email,
                            playBillingManager = playBillingManager
                        )
                    }
                }
            )
            MaaryaAuthenticatedShell(
                viewModel = viewModel,
                playBillingManager = playBillingManager,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun MaaryaAuthenticatedShell(
    viewModel: MaaryaViewModel,
    playBillingManager: com.example.billing.PlayBillingManager,
    onSignedOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MaaryaTab.FEED.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val visibleTabs = remember(viewModel.isAdmin) {
        MaaryaTab.entries.filter { tab ->
            tab != MaaryaTab.STUDIO || viewModel.isAdmin
        }
    }

    val navigateToTab: (MaaryaTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetObsidian)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = VelvetObsidian,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = VelvetSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        visibleTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ChampagneGold,
                                    selectedTextColor = ChampagneGold,
                                    indicatorColor = ChampagneGoldDark,
                                    unselectedIconColor = WarmTaupe,
                                    unselectedTextColor = WarmTaupe
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = VelvetSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        visibleTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, color = if (selected) ChampagneGold else PearlIvory) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ChampagneGold,
                                    selectedTextColor = ChampagneGold,
                                    indicatorColor = ChampagneGoldDark,
                                    unselectedIconColor = WarmTaupe,
                                    unselectedTextColor = WarmTaupe
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = MaaryaTab.FEED.route
                    ) {
                        composable(MaaryaTab.FEED.route) {
                            MaaryaFeedScreen(
                                viewModel = viewModel,
                                onNavigateToClub = { navigateToTab(MaaryaTab.CLUB) },
                                onNavigateToMessages = { navigateToTab(MaaryaTab.MESSAGES) }
                            )
                        }
                        composable(MaaryaTab.CLUB.route) {
                            MembershipTiersScreen(
                                viewModel = viewModel,
                                playBillingManager = playBillingManager
                            )
                        }
                        composable(MaaryaTab.MESSAGES.route) {
                            MessagesScreen(
                                viewModel = viewModel,
                                onNavigateToClub = { navigateToTab(MaaryaTab.CLUB) }
                            )
                        }
                        if (viewModel.isAdmin) {
                            composable(MaaryaTab.STUDIO.route) {
                                CreatorStudioScreen(viewModel = viewModel)
                            }
                        }
                        composable(MaaryaTab.ACCOUNT.route) {
                            AccountScreen(
                                viewModel = viewModel,
                                onSignedOut = onSignedOut
                            )
                        }
                    }
                }
            }
        }
    }
}
