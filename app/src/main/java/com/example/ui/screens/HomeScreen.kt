package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Trophy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: EsportsViewModel,
    tournaments: List<Tournament>,
    walletBalance: Int,
    selectedCategory: GameCategory,
    onNavigateToWallet: () -> Unit,
    onNavigateToMatchDetails: (Tournament) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()

    var tournamentToJoin by remember { mutableStateOf<Tournament?>(null) }
    var tournamentDetail by remember { mutableStateOf<Tournament?>(null) }
    var isJoinLoading by remember { mutableStateOf(false) }
    var joinDialogError by remember { mutableStateOf<String?>(null) }
    var inGameUidInput by remember { mutableStateOf("") }
    var hasRetry by remember { mutableStateOf(false) }

    BackHandler(enabled = tournamentToJoin != null || tournamentDetail != null) {
        if (tournamentToJoin != null) {
            if (!isJoinLoading) {
                tournamentToJoin = null
                joinDialogError = null
            }
        } else if (tournamentDetail != null) {
            tournamentDetail = null
        }
    }

    val filteredTournaments = remember(tournaments, selectedCategory) {
        if (selectedCategory == GameCategory.ALL) {
            tournaments
        } else {
            tournaments.filter { item ->
                item.gameCategory == selectedCategory ||
                    item.gameCategoryName.equals(selectedCategory.name, ignoreCase = true) ||
                    item.gameCategoryName.equals(selectedCategory.displayName, ignoreCase = true) ||
                    item.gameCategoryName.equals(selectedCategory.shortTag, ignoreCase = true) ||
                    (selectedCategory == GameCategory.BGMI && (
                        item.bannerType.equals("bgmi", ignoreCase = true) ||
                            item.title.contains("BGMI", ignoreCase = true) ||
                            item.title.contains("Battlegrounds", ignoreCase = true)
                    )) ||
                    (selectedCategory == GameCategory.FREE_FIRE && (
                        item.bannerType.equals("ff", ignoreCase = true) ||
                            item.title.contains("Free Fire", ignoreCase = true)
                    ))
            }
        }
    }

    val shouldShowComingSoon = filteredTournaments.isEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
    ) {
        item { HomeHeader(walletBalance = walletBalance, onWalletClick = onNavigateToWallet) }
        item {
            HeroPromoBanner(onExploreClick = {
                viewModel.setCategory(GameCategory.FREE_FIRE)
            })
        }
        item {
            GameCategoryRow(
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.setCategory(it) }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                Spacer(modifier = Modifier.width(8.dp))

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

        if (shouldShowComingSoon) {
            item {
                ComingSoonTournamentCard(
                    onRetry = {
                        hasRetry = true
                        viewModel.loadTournaments(forceRefresh = true)
                    },
                    showRetry = hasRetry
                )
            }
        } else {
            items(filteredTournaments, key = { it.id }) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    onJoinClick = {
                        isJoinLoading = false
                        joinDialogError = null
                        inGameUidInput = userProfile.ingameId.ifBlank { "" }
                        tournamentToJoin = tournament
                    },
                    onDetailsClick = { onNavigateToMatchDetails(tournament) },
                    onViewResultsClick = { onNavigateToMatchDetails(tournament) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (tournamentToJoin != null) {
        val item = tournamentToJoin!!
        AlertDialog(
            onDismissRequest = {
                if (!isJoinLoading) {
                    tournamentToJoin = null
                    joinDialogError = null
                }
            },
            modifier = Modifier.imePadding(),
            title = {
                Text(
                    text = "Confirm Tournament Entry",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = item.title,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${item.gameCategory.displayName} • ${item.map} • ${item.matchType.ifBlank { item.format.label }}",
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
                                Text("Entry Fee:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "₹${item.entryFee}",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Wallet Balance:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "₹$walletBalance",
                                    color = if (walletBalance >= item.entryFee) EmeraldGreen else CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    if (walletBalance < item.entryFee) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tournamentToJoin = null
                                    joinDialogError = null
                                    onNavigateToWallet()
                                },
                            color = CrimsonRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚠ Insufficient balance. Need ₹${item.entryFee - walletBalance} more.",
                                    color = CrimsonRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Add Cash →",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "In-Game ID / UID",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inGameUidInput,
                        onValueChange = {
                            inGameUidInput = it
                            if (joinDialogError != null) joinDialogError = null
                        },
                        label = { Text("Enter In-Game ID / UID") },
                        placeholder = { Text("e.g. 5183920194 or Character ID") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .testTag("dialog_ingame_uid_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = NeonCyan
                        )
                    )
                    if (!joinDialogError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CrimsonRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = joinDialogError!!,
                                    color = CrimsonRed,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isJoinLoading) return@Button

                        val trimmedUid = inGameUidInput.trim()
                        if (trimmedUid.isBlank()) {
                            val msg = "Please enter your Game ID"
                            joinDialogError = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (walletBalance < item.entryFee) {
                            val msg = "Insufficient balance to join this tournament."
                            joinDialogError = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        isJoinLoading = true
                        joinDialogError = null

                        coroutineScope.launch {
                            try {
                                val result = viewModel.joinTournamentAsync(item, trimmedUid)
                                if (result.isSuccess) {
                                    val message = result.getOrNull() ?: "Successfully Joined!"
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    tournamentToJoin = null
                                    inGameUidInput = ""
                                } else {
                                    val error = result.exceptionOrNull()?.message ?: "Failed to join tournament."
                                    joinDialogError = error
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Throwable) {
                                val error = e.message ?: "Failed to join tournament."
                                joinDialogError = error
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            } finally {
                                isJoinLoading = false
                            }
                        }
                    },
                    enabled = !isJoinLoading && walletBalance >= item.entryFee,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("dialog_confirm_join_btn")
                ) {
                    if (isJoinLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Joining...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Join Tournament",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tournamentToJoin = null
                        joinDialogError = null
                        isJoinLoading = false
                    },
                    enabled = !isJoinLoading,
                    modifier = Modifier.testTag("dialog_cancel_join_btn")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardNavy
        )
    }

    if (tournamentDetail != null) {
        val item = tournamentDetail!!
        AlertDialog(
            onDismissRequest = { tournamentDetail = null },
            title = {
                Text(item.title, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "${item.gameCategory.displayName} • ${item.map} • ${item.matchType.ifBlank { item.format.label }}",
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
                }
            },
            confirmButton = {
                TextButton(onClick = { tournamentDetail = null }) {
                    Text("Close")
                }
            },
            containerColor = CardNavy
        )
    }
}

@Composable
private fun ComingSoonTournamentCard(
    onRetry: () -> Unit,
    showRetry: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF071827),
                            Color(0xFF0A1D33),
                            Color(0xFF101F3A)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = NeonCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Trophy,
                        contentDescription = "Trophy",
                        tint = NeonCyan,
                        modifier = Modifier
                            .padding(18.dp)
                            .size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "New Tournaments Are Coming Soon!",
                    color = NeonCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Admin is setting up exciting new matches. Stay tuned!",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                if (showRetry) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retry",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "WAR",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
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

        Surface(
            modifier = Modifier
                .clickable { onWalletClick() }
                .testTag("top_bar_wallet_button"),
            shape = RoundedCornerShape(20.dp),
            color = CardNavyElevated,
            border = BorderStroke(1.dp, BorderCyan)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                            Color(0xFF00A6B8),
                            CardNavyElevated,
                            Color(0xFF1A2B66)
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
                        text = "DAILY CLUB SCRIM",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Live Esports Scrims\nReal DB Entry & Winning Prizes",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    Text(
                        text = "Join Scrims Now",
                        fontWeight = FontWeight.Bold
                    )
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
        GameCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectCategory(category) }
                    .testTag("category_chip_${category.name.lowercase()}"),
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
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.displayName,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

