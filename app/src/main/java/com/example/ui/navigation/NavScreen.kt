package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val isBottomTab: Boolean = true
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home", true),
    WALLET("Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_item_wallet", true),
    MY_MATCHES("My Matches", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_item_my_matches", true),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_item_profile", true),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_item_admin", true),
    MATCH_DETAILS("Match Details", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_item_match_details", false)
}
