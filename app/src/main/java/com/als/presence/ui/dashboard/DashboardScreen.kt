package com.als.presence.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.als.presence.ui.auth.AuthViewModel
import com.als.presence.ui.dashboard.common.EditProfileModal
import com.als.presence.ui.dashboard.owner.modals.EditOrganizationModal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import com.als.presence.ui.dashboard.common.ConnectivityBanner
import com.als.presence.ui.dashboard.common.NotificationsDrawer
import com.als.presence.ui.dashboard.common.NotificationsViewModel
import com.als.presence.ui.dashboard.common.PresenceBottomBar
import com.als.presence.ui.dashboard.common.PresenceBottomItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.als.presence.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.RoleManagerPrimary
import com.als.presence.core.designsystem.RoleOwnerPrimary
import com.als.presence.core.designsystem.RoleStaffPrimary
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.LeaveScreen
import com.als.presence.ui.dashboard.common.SalaryPaymentScreen
import com.als.presence.ui.dashboard.manager.ManagerDashboardScreen
import com.als.presence.ui.dashboard.manager.screens.HolidayManagementScreen
import com.als.presence.ui.dashboard.manager.screens.OvertimeScreen
import com.als.presence.ui.dashboard.manager.screens.ShiftManagementScreen
import com.als.presence.ui.dashboard.owner.OwnerDashboardScreen
import com.als.presence.ui.dashboard.common.MyShiftsScreen
import com.als.presence.ui.dashboard.owner.screens.EmployeesManagementScreen
import com.als.presence.ui.dashboard.owner.screens.OrganizationSettingsScreen
import com.als.presence.ui.dashboard.owner.screens.PremisesManagementScreen
import com.als.presence.ui.dashboard.staff.EmployeeDashboardScreen
import kotlinx.coroutines.launch

private enum class SecondaryDestination(val label: String) {
    MY_SHIFTS("My Shifts"),
    EMPLOYEES_MANAGEMENT("Employees Manager"),
    PREMISES_MANAGEMENT("Premises & Geofencing"),
    SHIFT_MANAGEMENT("Working Shifts"),
    SALARY_PAYMENT("Salary Payments"),
    HOLIDAY_MANAGEMENT("Holiday Calendar"),
    LEAVE("Leave Requests"),
    OVERTIME("Overtime Console"),
    ORGANIZATION_SETTINGS("Organization & Owner")
}

private const val PRIMARY_ROUTE_PREFIX = "dashboard/primary/"
private const val SECONDARY_ROUTE_PREFIX = "dashboard/secondary/"

private fun primaryRoute(tabIndex: Int) = "$PRIMARY_ROUTE_PREFIX$tabIndex"

private fun secondaryRoute(destination: SecondaryDestination) =
    "$SECONDARY_ROUTE_PREFIX${destination.name}"

private fun String?.primaryTabIndex(): Int =
    this?.takeIf { it.startsWith(PRIMARY_ROUTE_PREFIX) }
        ?.removePrefix(PRIMARY_ROUTE_PREFIX)
        ?.toIntOrNull()
        ?: 0

private fun String?.secondaryDestination(): SecondaryDestination? =
    this?.takeIf { it.startsWith(SECONDARY_ROUTE_PREFIX) }
        ?.removePrefix(SECONDARY_ROUTE_PREFIX)
        ?.let { name -> SecondaryDestination.entries.firstOrNull { it.name == name } }

private data class DrawerEntry(
    val destination: SecondaryDestination,
    val icon: ImageVector
)

private data class DrawerSection(
    val title: String,
    val items: List<DrawerEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    notificationsViewModel: NotificationsViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Tabs and secondary screens share the dashboard's ViewModel store, as they
    // did before navigation moved into a NavHost. Otherwise every nav entry would
    // build another copy of the same view models and re-issue their loads.
    val dashboardViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "DashboardScreen requires an ambient ViewModelStoreOwner"
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedBottomTab = currentRoute.primaryTabIndex()
    val activeSecondary = currentRoute.secondaryDestination()

    val notifications by notificationsViewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotifications by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()

    val authViewModel: AuthViewModel = hiltViewModel()
    val updateProfileState by authViewModel.updateProfileState.collectAsStateWithLifecycle()
    val updateOrgState by authViewModel.updateOrgState.collectAsStateWithLifecycle()

    var showNotificationsDrawer by remember { mutableStateOf(false) }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showEditOrgModal by remember { mutableStateOf(false) }

    // The drawer is opened *over* the secondary screen and the back stack only
    // unwinds once it is fully in place. Popping first would expose the tab under
    // the sliding drawer and read as a flash of the wrong screen.
    val returnToMenu: () -> Unit = {
        scope.launch {
            drawerState.open()
            navController.popBackStack()
        }
    }

    // System back on a secondary screen follows the same path as its header back
    // button. The handler lives inside the navigation host content (below) so it
    // takes precedence over the host's own pop handling.
    val roleColor = remember(user.role) { getRoleColor(user.role) }

    val tabs = remember(user.role) {
        when (user.role) {
            Role.OWNER -> listOf(
                PresenceBottomItem("Employees", Icons.Default.People),
                PresenceBottomItem("Locations", Icons.Default.LocationOn),
                PresenceBottomItem("Reports", Icons.Default.BarChart)
            )
            Role.MANAGER -> listOf(
                PresenceBottomItem("Attendance", Icons.Default.EventAvailable),
                PresenceBottomItem("Staff", Icons.Default.People),
                PresenceBottomItem("Locations", Icons.Default.LocationOn)
            )
            Role.STAFF -> listOf(
                PresenceBottomItem("Check-in", Icons.Default.Home),
                PresenceBottomItem("Shifts", Icons.Default.Schedule),
                PresenceBottomItem("Locations", Icons.Default.LocationOn),
                PresenceBottomItem("Earnings", Icons.Default.AccountBalanceWallet)
            )
        }
    }

    val drawerSections = remember(user.role) {
        when (user.role) {
            Role.OWNER -> listOf(
                DrawerSection(
                    title = "ORGANIZATION",
                    items = listOf(
                        DrawerEntry(SecondaryDestination.ORGANIZATION_SETTINGS, Icons.Default.EditNote),
                        DrawerEntry(SecondaryDestination.EMPLOYEES_MANAGEMENT, Icons.Default.Badge),
                        DrawerEntry(SecondaryDestination.PREMISES_MANAGEMENT, Icons.Default.Business),
                        DrawerEntry(SecondaryDestination.SALARY_PAYMENT, Icons.Default.Payments)
                    )
                ),
                DrawerSection(
                    title = "SCHEDULING & LEAVE",
                    items = listOf(
                        DrawerEntry(SecondaryDestination.SHIFT_MANAGEMENT, Icons.Default.Schedule),
                        DrawerEntry(SecondaryDestination.OVERTIME, Icons.Default.VerifiedUser),
                        DrawerEntry(SecondaryDestination.HOLIDAY_MANAGEMENT, Icons.Default.CalendarMonth),
                        DrawerEntry(SecondaryDestination.LEAVE, Icons.Default.EventAvailable)
                    )
                )
            )
            Role.MANAGER -> listOf(
                DrawerSection(
                    title = "PERSONAL WORKSPACE",
                    items = listOf(
                        DrawerEntry(SecondaryDestination.MY_SHIFTS, Icons.Default.Schedule)
                    )
                ),
                DrawerSection(
                    title = "OPERATIONS & LEAVE",
                    items = listOf(
                        DrawerEntry(SecondaryDestination.EMPLOYEES_MANAGEMENT, Icons.Default.Badge),
                        DrawerEntry(SecondaryDestination.PREMISES_MANAGEMENT, Icons.Default.Business),
                        DrawerEntry(SecondaryDestination.SALARY_PAYMENT, Icons.Default.Payments),
                        DrawerEntry(SecondaryDestination.OVERTIME, Icons.Default.VerifiedUser),
                        DrawerEntry(SecondaryDestination.HOLIDAY_MANAGEMENT, Icons.Default.CalendarMonth),
                        DrawerEntry(SecondaryDestination.LEAVE, Icons.Default.EventAvailable)
                    )
                )
            )
            Role.STAFF -> listOf(
                DrawerSection(
                    title = "SCHEDULING & LEAVE",
                    items = listOf(
                        DrawerEntry(SecondaryDestination.MY_SHIFTS, Icons.Default.Schedule),
                        DrawerEntry(SecondaryDestination.OVERTIME, Icons.Default.VerifiedUser),
                        DrawerEntry(SecondaryDestination.HOLIDAY_MANAGEMENT, Icons.Default.CalendarMonth),
                        DrawerEntry(SecondaryDestination.LEAVE, Icons.Default.EventAvailable)
                    )
                )
            )
        }
    }

    val selectTab: (Int) -> Unit = { index ->
        navController.navigate(primaryRoute(index)) {
            // Keep exactly one primary destination under the top of the stack.
            popUpTo(primaryRoute(0)) {
                inclusive = false
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openSecondary: (SecondaryDestination) -> Unit = { destination ->
        scope.launch { drawerState.close() }
        navController.navigate(secondaryRoute(destination)) {
            // A secondary screen always sits on top of the current primary tab;
            // picking another one from the drawer replaces the old one instead of
            // growing an unbounded stack.
            popUpTo(primaryRoute(selectedBottomTab)) { inclusive = false }
            launchSingleTop = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                // The drawer covers the whole window. Backing out of a secondary
                // screen swaps the destination underneath while the drawer is in
                // place, so a partial-width sheet would expose that swap in the
                // uncovered strip and read as a flash of the previous tab.
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { paneTitle = "Navigation menu" },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_logo_presence),
                                        contentDescription = "Presence App Logo",
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    )

                                    Column {
                                        Text(
                                            text = "Presence",
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.4).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "Built by Arman Logical Systems",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { scope.launch { drawerState.close() } },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close navigation menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = roleColor.copy(alpha = 0.1f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        showEditProfileModal = true
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = roleColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(roleColor)
                                                .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!user.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user.avatarUrl,
                                                    contentDescription = user.fullName,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = user.fullName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showEditProfileModal = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = roleColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = roleColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = getRoleIcon(user.role),
                                                contentDescription = null,
                                                tint = roleColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${user.role.name} • ${user.organizationName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = roleColor
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // The drawer is the home of everything the bottom
                            // row deliberately leaves out; the primary tabs stay
                            // on the bottom bar and are not repeated here.
                            drawerSections.forEachIndexed { index, section ->
                                if (index > 0) DrawerDivider()
                                DrawerSectionHeader(section.title)
                                section.items.forEach { entry ->
                                    DrawerActionItem(
                                        label = entry.destination.label,
                                        icon = entry.icon,
                                        selected = activeSecondary == entry.destination,
                                        accentColor = roleColor,
                                        onClick = { openSecondary(entry.destination) }
                                    )
                                }
                            }
                        }

                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        onLogout()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign Out Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                // The workspace bar already includes the status-bar inset and the
                // tab row includes the navigation-bar inset. Without this, the
                // full-bleed secondary screens (which have no bars and apply their
                // own insets) would receive them twice.
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                topBar = {
                    // Secondary screens are full-bleed and draw their own header,
                    // including the status-bar inset, so the workspace bar steps
                    // out while one is open.
                    if (activeSecondary == null) {
                        Column {
                            DashboardTopAppBar(
                                user = user,
                                roleColor = roleColor,
                                onNavigationClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                onNotificationClick = {
                                    showNotificationsDrawer = true
                                },
                                onOrgClick = {
                                    if (user.role == Role.OWNER) {
                                        showEditOrgModal = true
                                    } else {
                                        showEditProfileModal = true
                                    }
                                },
                                unreadCount = unreadNotifications
                            )
                            ConnectivityBanner(isOnline = isOnline)
                        }
                    }
                },
                bottomBar = {
                    // Secondary screens keep their own header and back button,
                    // so the tab row is hidden while one is open; the hamburger
                    // remains the route to every other destination.
                    if (activeSecondary == null) {
                        PresenceBottomBar(
                            items = tabs,
                            selectedIndex = selectedBottomTab,
                            accent = roleColor,
                            onSelect = selectTab
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = primaryRoute(0),
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(220))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(220))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        }
                    ) {
                        composable(primaryRoute(0)) {
                            SharedDashboardDestination(dashboardViewModelStoreOwner) {
                                RoleDashboardContent(
                                    user = user,
                                    tabIndex = 0,
                                    onEditOrgClick = { showEditOrgModal = true }
                                )
                            }
                        }
                        composable(primaryRoute(1)) {
                            SharedDashboardDestination(dashboardViewModelStoreOwner) {
                                RoleDashboardContent(
                                    user = user,
                                    tabIndex = 1,
                                    onEditOrgClick = { showEditOrgModal = true }
                                )
                            }
                        }
                        composable(primaryRoute(2)) {
                            SharedDashboardDestination(dashboardViewModelStoreOwner) {
                                RoleDashboardContent(
                                    user = user,
                                    tabIndex = 2,
                                    onEditOrgClick = { showEditOrgModal = true }
                                )
                            }
                        }
                        composable(primaryRoute(3)) {
                            SharedDashboardDestination(dashboardViewModelStoreOwner) {
                                RoleDashboardContent(
                                    user = user,
                                    tabIndex = 3,
                                    onEditOrgClick = { showEditOrgModal = true }
                                )
                            }
                        }

                        SecondaryDestination.entries.forEach { destination ->
                            composable(secondaryRoute(destination)) {
                                SharedDashboardDestination(dashboardViewModelStoreOwner) {
                                    SecondaryDestinationContent(
                                        user = user,
                                        destination = destination,
                                        onBack = returnToMenu,
                                        onEditProfileClick = { showEditProfileModal = true },
                                        onEditOrgClick = { showEditOrgModal = true }
                                    )
                                }
                            }
                        }
                    }

                    // The drawer is a plain full-bleed surface, so it does not
                    // install Material's sheet back handler: this one closes it
                    // first, then sends a secondary screen back to the menu, and
                    // only after that lets the host pop the tab stack.
                    val drawerShown = drawerState.isOpen ||
                        drawerState.targetValue == DrawerValue.Open
                    BackHandler(enabled = drawerShown || activeSecondary != null) {
                        if (drawerShown) {
                            scope.launch { drawerState.close() }
                        } else {
                            returnToMenu()
                        }
                    }
                }
            }
        }

        if (showEditProfileModal) {
            EditProfileModal(
                user = user,
                updateState = updateProfileState,
                onDismiss = { showEditProfileModal = false },
                onSaveProfile = { fullName, phone, avatarBase64 ->
                    authViewModel.updateProfile(fullName, phone, avatarBase64)
                },
                onResetState = { authViewModel.resetUpdateProfileState() }
            )
        }

        if (showEditOrgModal) {
            EditOrganizationModal(
                user = user,
                updateOrgState = updateOrgState,
                onDismiss = { showEditOrgModal = false },
                onSaveOrg = { name, timezone, currency, logoBase64 ->
                    authViewModel.updateOrganization(name, timezone, currency, logoBase64)
                },
                onResetState = { authViewModel.resetUpdateOrgState() }
            )
        }

        // Kept composed so the drawer can animate back out after dismissal.
        NotificationsDrawer(
            visible = showNotificationsDrawer,
            user = user,
            notifications = notifications,
            onDismiss = { showNotificationsDrawer = false },
            onMarkAllRead = notificationsViewModel::markAllRead,
            onClearAll = notificationsViewModel::clearAll
        )
    }
}

@Composable
private fun SharedDashboardDestination(
    viewModelStoreOwner: ViewModelStoreOwner,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
        content()
    }
}

@Composable
private fun RoleDashboardContent(
    user: User,
    tabIndex: Int,
    onEditOrgClick: () -> Unit = {}
) {
    when (user.role) {
        Role.OWNER -> OwnerDashboardScreen(user = user, selectedTab = tabIndex, onEditOrgClick = onEditOrgClick)
        Role.MANAGER -> ManagerDashboardScreen(user = user, selectedTab = tabIndex)
        Role.STAFF -> EmployeeDashboardScreen(user = user, selectedTab = tabIndex)
    }
}

@Composable
private fun SecondaryDestinationContent(
    user: User,
    destination: SecondaryDestination,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditOrgClick: () -> Unit
) {
    when (destination) {
        SecondaryDestination.MY_SHIFTS -> MyShiftsScreen(user = user, onBack = onBack)
        SecondaryDestination.EMPLOYEES_MANAGEMENT -> EmployeesManagementScreen(
            user = user,
            onBack = onBack,
            canManage = user.role == Role.OWNER || user.role == Role.MANAGER
        )
        SecondaryDestination.PREMISES_MANAGEMENT -> PremisesManagementScreen(
            onBack = onBack,
            canCreate = user.role == Role.OWNER
        )
        SecondaryDestination.SHIFT_MANAGEMENT -> ShiftManagementScreen(
            onBack = onBack,
            canManage = user.role == Role.OWNER || user.role == Role.MANAGER
        )
        SecondaryDestination.SALARY_PAYMENT -> SalaryPaymentScreen(onBack = onBack)
        SecondaryDestination.HOLIDAY_MANAGEMENT -> HolidayManagementScreen(
            onBack = onBack,
            canManage = user.role == Role.OWNER || user.role == Role.MANAGER
        )
        SecondaryDestination.LEAVE -> LeaveScreen(
            role = user.role,
            onBack = onBack
        )
        SecondaryDestination.OVERTIME -> OvertimeScreen(onBack = onBack)
        SecondaryDestination.ORGANIZATION_SETTINGS -> OrganizationSettingsScreen(
            user = user,
            onBack = onBack,
            onEditProfileClick = onEditProfileClick,
            onEditOrgClick = onEditOrgClick
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun DrawerActionItem(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accentColor.copy(alpha = if (selected) 0.2f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getRoleColor(role: Role): Color = when (role) {
    Role.OWNER -> RoleOwnerPrimary
    Role.MANAGER -> RoleManagerPrimary
    Role.STAFF -> RoleStaffPrimary
}

private fun getRoleIcon(role: Role): ImageVector = when (role) {
    Role.OWNER -> Icons.Default.AdminPanelSettings
    Role.MANAGER -> Icons.Default.SupervisorAccount
    Role.STAFF -> Icons.Default.Badge
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopAppBar(
    user: User,
    roleColor: Color,
    onNavigationClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onOrgClick: () -> Unit = {},
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier.clickable(onClick = onOrgClick)
                    ) {
                        OrganizationMark(
                            name = user.organizationName.orEmpty(),
                            logoUrl = user.organizationLogoUrl,
                            accent = roleColor
                        )
                        Text(
                            text = user.organizationName.orEmpty().ifBlank { "Organization" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigationClick,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = roleColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 10.dp)) {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 3.dp, end = 3.dp)
                                .size(if (unreadCount > 0) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (unreadCount > 0) MaterialTheme.colorScheme.error
                                    else roleColor
                                )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
        }
    }
}

/**
 * Identity mark for the signed-in organization shown in the centre of the top bar.
 * The organization is the tenant, so its own mark (not the Presence app icon) sits
 * at the top of the workspace. When no logo asset is available we fall back to a
 * monogram built from the organization name.
 */
@Composable
private fun OrganizationMark(
    name: String,
    logoUrl: String? = null,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$name logo",
            modifier = modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(9.dp)
                ),
            contentScale = ContentScale.Crop
        )
    } else {
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "O"
        Box(
            modifier = modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.14f))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(9.dp)
                )
                .semantics { contentDescription = "$name logo" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}
