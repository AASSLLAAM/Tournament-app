package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Tournament
import com.example.model.UserProfile
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.CardNavy
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EsportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: EsportsViewModel,
    userProfile: UserProfile,
    tournaments: List<Tournament>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = userProfile.isAdmin
    val isAdminLoading by viewModel.isAdminActionLoading.collectAsState()

    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var tournamentToDeclareWinner by remember { mutableStateOf<Tournament?>(null) }

    if (!isAdmin) {
        // Access Denied Screen for Non-Admins
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkNavy)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("admin_access_denied_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = CrimsonRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Admin Access Required",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Only authorized administrators with the 'admin' role can create tournaments and declare winners.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Developer/Tester Toggle for Admin Access
                    OutlinedButton(
                        onClick = { viewModel.setAdminRole(true) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.testTag("request_admin_role_btn")
                    ) {
                        Text("Switch to Admin Role (Dev Mode)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onBack) {
                        Text("Return to Home", color = TextMuted)
                    }
                }
            }
        }
        return
    }

    // Admin Dashboard
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Studio",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Create tournaments & declare match winners",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldAccent.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = "ADMIN ACTIVE",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Action Banner: Create Tournament Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("admin_action_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderCyan)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00495C), CardNavyElevated)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Host a New Tournament",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set game (Free Fire / BGMI), entry fee, prize pool, schedule, and publish live to Firestore.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showCreateTournamentDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("admin_create_tournament_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create New Tournament", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Tournaments Management (Declare Winners)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Matches (${tournaments.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to declare winner",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        items(tournaments, key = { it.id }) { tournament ->
            AdminTournamentManageCard(
                tournament = tournament,
                onDeclareWinnerClick = { tournamentToDeclareWinner = tournament }
            )
        }
    }

    // Dialog: Create Tournament
    if (showCreateTournamentDialog) {
        CreateTournamentDialog(
            isLoading = isAdminLoading,
            onDismiss = { showCreateTournamentDialog = false },
            onSubmit = { title, game, map, format, date, time, entryFee, prize, perKill, slots ->
                viewModel.createTournament(
                    title = title,
                    gameCategory = game,
                    map = map,
                    format = format,
                    scheduleDate = date,
                    scheduleTime = time,
                    entryFee = entryFee,
                    prizePool = prize,
                    perKill = perKill,
                    totalSlots = slots,
                    onSuccess = { showCreateTournamentDialog = false }
                )
            }
        )
    }

    // Dialog: Declare Winner
    tournamentToDeclareWinner?.let { tour ->
        DeclareWinnerDialog(
            tournament = tour,
            isLoading = isAdminLoading,
            onDismiss = { tournamentToDeclareWinner = null },
            onConfirmWinner = { winnerName ->
                viewModel.declareWinner(
                    tournamentId = tour.id,
                    winnerName = winnerName,
                    onSuccess = { tournamentToDeclareWinner = null }
                )
            }
        )
    }
}

@Composable
fun AdminTournamentManageCard(
    tournament: Tournament,
    onDeclareWinnerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_manage_card_${tournament.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        border = BorderStroke(1.dp, if (tournament.isCompleted) EmeraldGreen.copy(alpha = 0.5f) else BorderMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardNavyElevated,
                    border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${tournament.gameCategory.displayName} • ${tournament.map}",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (tournament.isCompleted) EmeraldGreen.copy(alpha = 0.2f) else CardNavyElevated
                ) {
                    Text(
                        text = if (tournament.isCompleted) "WINNER DECLARED" else "SLOTS: ${tournament.filledSlots}/${tournament.totalSlots}",
                        color = if (tournament.isCompleted) EmeraldGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tournament.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Entry: ₹${tournament.entryFee} | Prize: ₹${tournament.prizePool} | Time: ${tournament.scheduleDate}, ${tournament.scheduleTime}",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (tournament.winnerName != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Champion / Winner:", color = TextMuted, fontSize = 10.sp)
                            Text(tournament.winnerName ?: "", color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onDeclareWinnerClick,
                    modifier = Modifier.fillMaxWidth().testTag("btn_declare_winner_${tournament.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Declare Winner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, game: String, map: String, format: String, date: String, time: String, entryFee: Int, prizePool: Int, perKill: Int, totalSlots: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("Free Fire MAX") }
    var mapName by remember { mutableStateOf("Bermuda") }
    var matchFormat by remember { mutableStateOf("Squad") }
    var scheduleDate by remember { mutableStateOf("Today") }
    var scheduleTime by remember { mutableStateOf("09:00 PM") }
    var entryFeeStr by remember { mutableStateOf("50") }
    var prizePoolStr by remember { mutableStateOf("2500") }
    var perKillStr by remember { mutableStateOf("10") }
    var totalSlotsStr by remember { mutableStateOf("48") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text("Create New Tournament", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tournament Title") },
                        placeholder = { Text("e.g. BGMI Pro Championship S1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_tournament_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Game selection buttons
                        listOf("Free Fire MAX", "BGMI").forEach { game ->
                            val isSelected = selectedGame == game
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedGame = game
                                        mapName = if (game == "BGMI") "Erangel" else "Bermuda"
                                    },
                                color = if (isSelected) NeonCyan else CardNavyElevated,
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else BorderMuted),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = game,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mapName,
                            onValueChange = { mapName = it },
                            label = { Text("Map") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = matchFormat,
                            onValueChange = { matchFormat = it },
                            label = { Text("Format (Solo/Squad)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = entryFeeStr,
                            onValueChange = { if (it.all { c -> c.isDigit() }) entryFeeStr = it },
                            label = { Text("Entry Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_entry_fee"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = prizePoolStr,
                            onValueChange = { if (it.all { c -> c.isDigit() }) prizePoolStr = it },
                            label = { Text("Prize Pool (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_prize_pool"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scheduleDate,
                            onValueChange = { scheduleDate = it },
                            label = { Text("Date (e.g. Today)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = scheduleTime,
                            onValueChange = { scheduleTime = it },
                            label = { Text("Time (e.g. 08:30 PM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_schedule_time"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "$selectedGame Mega Contest" }
                    val entry = entryFeeStr.toIntOrNull() ?: 50
                    val prize = prizePoolStr.toIntOrNull() ?: 2500
                    val kill = perKillStr.toIntOrNull() ?: 10
                    val slots = totalSlotsStr.toIntOrNull() ?: 48
                    onSubmit(finalTitle, selectedGame, mapName, matchFormat, scheduleDate, scheduleTime, entry, prize, kill, slots)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_create_tournament_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Publish to Firestore", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardNavy
    )
}

@Composable
fun DeclareWinnerDialog(
    tournament: Tournament,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmWinner: (String) -> Unit
) {
    var winnerInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text("Declare Match Winner", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = tournament.title,
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prize pool of ₹${tournament.prizePool} will be marked won for this custom tournament.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = winnerInput,
                    onValueChange = { winnerInput = it },
                    label = { Text("Winner Player/Team Name") },
                    placeholder = { Text("e.g. Soul_Mortal / ShadowGamer_99") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_winner_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Team Soul", "ShadowGamer_99", "GodL_Jonathan").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardNavyElevated,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { winnerInput = preset }
                        ) {
                            Text(
                                text = preset,
                                color = GoldAccent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmWinner(winnerInput) },
                enabled = !isLoading && winnerInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_declare_winner_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Declare Winner", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardNavy
    )
}
