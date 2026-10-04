package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.PrimaryBlue

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false,
    val isAiHighlight: Boolean = false
)

val NAV_ITEMS = listOf(
    NavItem("dashboard", "Overview", Icons.Default.Home),
    NavItem("projects", "Projects", Icons.Default.Folder),
    NavItem("documents", "Documents", Icons.Default.Description),
    NavItem("ai_practice", "AI Practice", Icons.Default.AutoAwesome, isAiHighlight = true),
    NavItem("reports", "Reports", Icons.Default.Assessment),
    NavItem("admin", "Admin", Icons.Default.AdminPanelSettings, adminOnly = true),
    NavItem("settings", "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamTopAppBar(
    currentUser: UserEntity?,
    isGuest: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("main_top_app_bar"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "App Logo",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Exam Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (currentUser != null) {
                RoleBadge(role = currentUser.getRoleEnum(), modifier = Modifier.padding(end = 4.dp))
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.testTag("appbar_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isGuest) {
                RoleBadge(role = UserRole.GUEST, modifier = Modifier.padding(end = 6.dp))
                IconButton(
                    onClick = onLoginClick,
                    modifier = Modifier.testTag("appbar_signin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Sign In",
                        tint = PrimaryBlue
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ExamBottomNavBar(
    currentRoute: String,
    currentUser: UserEntity?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser != null && currentUser.getRoleEnum().canAccessAdminArea()
    val visibleItems = NAV_ITEMS.filter { !it.adminOnly || isAdmin }

    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        visibleItems.forEach { item ->
            val selected = currentRoute == item.route || (item.route == "projects" && currentRoute == "project_detail")
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item.isAiHighlight) {
                        BadgedBox(badge = {
                            Badge(containerColor = AcademicGold) {
                                Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("nav_item_${item.route}")
            )
        }
    }
}

@Composable
fun ExamNavigationRail(
    currentRoute: String,
    currentUser: UserEntity?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser != null && currentUser.getRoleEnum().canAccessAdminArea()
    val visibleItems = NAV_ITEMS.filter { !it.adminOnly || isAdmin }

    NavigationRail(
        modifier = modifier.fillMaxHeight().testTag("side_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        visibleItems.forEach { item ->
            val selected = currentRoute == item.route || (item.route == "projects" && currentRoute == "project_detail")
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { Text(item.title, fontSize = 10.sp) },
                modifier = Modifier.testTag("rail_item_${item.route}")
            )
        }
    }
}
