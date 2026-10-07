package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameCategory
import com.example.model.Tournament
import com.example.ui.components.TournamentCard
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.CardNavy
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EsportsViewModel

@Composable
fun HomeScreen(
    viewModel: EsportsViewModel,
    tournaments: List<Tournament>,
    walletBalance: Int,
    selectedCategory: GameCategory,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tournamentToJoin by remember { mutableStateOf<Tournament?>(null) }
    var tournamentDetail by remember { mutableStateOf<Tournament?>(null) }
    val isJoining by viewModel.isJoining.collectAsState()

    val filteredTournaments = remember(tournaments, selectedCategory) {
        if (selectedCategory == GameCategory.ALL) {
            tournaments
        } else {
            tournaments.filter { it.gameCategory == selectedCategory }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // App Top Bar Header
        item {
            HomeHeader(
                walletBalance = walletBalance,
                onWalletClick = onNavigateToWallet
            )
        }

        // Hero Promotional Card
        item {
            HeroPromoBanner(onExploreClick = { viewModel.setCategory(GameCategory.FREE_FIRE) })
        }

        // Game Category Filters (Pills)
        item {
            GameCategoryRow(
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.setCategory(it) }
            )
        }

        // Section Title: Dynamic Upcoming Tournaments (Fetched from Firestore)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = CrimsonRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live Tournaments (Firestore)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardNavyElevated,
                    border = BorderStroke(1.dp, BorderMuted)
                ) {
                    Text(
                        text = "${filteredTournaments.size} Available",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (filteredTournaments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavy)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading dynamic tournaments from Firestore...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredTournaments, key = { it.id }) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    onJoinClick = { tournamentToJoin = tournament },
                    onDetailsClick = { tournamentDetail = tournament },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Join Tournament Confirmation Dialog
    tournamentToJoin?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isJoining) tournamentToJoin = null },
            title = {
                Text(
                    text = "Confirm Tournament Entry",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = item.title,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Format: ${item.format.label} • Map: ${item.map}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Time: ${item.scheduleDate} at ${item.scheduleTime}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardNavyElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Entry Fee (Deducted from DB Wallet):", color = TextSecondary, fontSize = 12.sp)
                                Text("₹${item.entryFee}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Wallet Balance:", color = TextSecondary, fontSize = 12.sp)
                                Text("₹$walletBalance", color = if (walletBalance >= item.entryFee) EmeraldGreen else CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (walletBalance < item.entryFee) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠ Insufficient balance. Please top up ₹${item.entryFee - walletBalance} in Wallet.",
                            color = CrimsonRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.joinTournament(item)
                        tournamentToJoin = null
                    },
                    enabled = !isJoining && walletBalance >= item.entryFee,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("dialog_confirm_join_btn")
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("Deduct ₹${item.entryFee} & Join", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tournamentToJoin = null },
                    enabled = !isJoining,
                    modifier = Modifier.testTag("dialog_cancel_join_btn")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardNavy
        )
    }

    // Tournament Detail Dialog
    tournamentDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { tournamentDetail = null },
            title = {
                Text(item.title, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Game: ${item.gameCategory.displayName} (${item.format.label})",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Map: ${item.map}", color = TextSecondary, fontSize = 13.sp)
                    Text("• Schedule: ${item.scheduleDate}, ${item.scheduleTime}", color = TextSecondary, fontSize = 13.sp)
                    Text("• Prize Pool: ₹${item.prizePool}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Per Kill Bonus: ₹${item.perKill}", color = TextSecondary, fontSize = 13.sp)
                    Text("• Server / Perspective: ${item.serverType}", color = TextSecondary, fontSize = 13.sp)
                    Text("• Organizer: ${item.organizer}", color = TextSecondary, fontSize = 13.sp)

                    if (item.isJoined) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardNavyElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderCyan)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Room Credentials (Joined in Cloud DB):", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Room ID: ${item.roomId ?: "Awaiting Host"}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Password: ${item.roomPassword ?: "----"}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Credentials become active 15 mins before match.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { tournamentDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardNavy
        )
    }
}

@Composable
fun HomeHeader(
    walletBalance: Int,
    onWalletClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Logo",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WORLD",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "WAR",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Esports Tournaments",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Wallet Balance Quick Chip
        Surface(
            modifier = Modifier
                .clickable { onWalletClick() }
                .testTag("top_bar_wallet_button"),
            shape = RoundedCornerShape(20.dp),
            color = CardNavyElevated,
            border = BorderStroke(1.dp, BorderCyan)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "₹$walletBalance",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroPromoBanner(
    onExploreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        border = BorderStroke(1.dp, BorderCyan)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00364A),
                            CardNavyElevated,
                            Color(0xFF1D263B)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeonCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "DAILY CLOUD SCRIMS",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Live Esports Scrims\nReal DB Entry & Winnings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tournaments are synced live with Firebase Firestore. Entry fees auto-deduct from your cloud wallet.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onExploreClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("hero_explore_btn")
                ) {
                    Text("Join Scrims Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameCategoryRow(
    selectedCategory: GameCategory,
    onSelectCategory: (GameCategory) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        GameCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectCategory(category) }
                    .testTag("category_chip_${category.name}"),
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) NeonCyan else CardNavy,
                border = BorderStroke(1.dp, if (isSelected) NeonCyan else BorderMuted)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = category.displayName,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}
