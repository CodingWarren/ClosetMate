package com.closetmate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Closet : BottomNavItem(
        route = "closet",
        title = "衣橱",
        selectedIcon = Icons.Filled.Checkroom,
        unselectedIcon = Icons.Outlined.Checkroom
    )

    object Outfit : BottomNavItem(
        route = "outfit",
        title = "搭配",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style
    )

    object Stats : BottomNavItem(
        route = "stats",
        title = "统计",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Closet,
    BottomNavItem.Outfit,
    BottomNavItem.Stats,
    BottomNavItem.Settings
)
