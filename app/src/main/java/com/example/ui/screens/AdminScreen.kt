package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TournamentParticipant
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.launch
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
    onNavigateToMatchDetails: (Tournament) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var tournamentToDeclareWinner by remember { mutableStateOf<Tournament?>(null) }
    var tournamentToUpdateRoom by remember { mutableStateOf<Tournament?>(null) }
    var tournamentToViewParticipants by remember { mutableStateOf<Tournament?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val anyDialogOpen = showCreateTournamentDialog ||
            tournamentToDeclareWinner != null ||
            tournamentToUpdateRoom != null ||
            tournamentToViewParticipants != null

    // Physical back button handler: close open dialogs first
    BackHandler(enabled = anyDialogOpen) {
        showCreateTournamentDialog = false
        tournamentToDeclareWinner = null
        tournamentToUpdateRoom = null
        tournamentToViewParticipants = null
    }

    // Physical back button handler: return to previous screen
    BackHandler(enabled = !anyDialogOpen) {
        onBack()
    }

    val isAdmin = userProfile.isAdmin
    val isAdminLoading by viewModel.isAdminActionLoading.collectAsState()

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

                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().testTag("return_home_btn")
                    ) {
                        Text("Return to Home", fontWeight = FontWeight.Bold)
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Studio",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Create tournaments & declare match winners",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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

        // Action Banner: Host / Publish Game Card with Publish Game & Cancel buttons
        item {
            var inlineTitle by remember { mutableStateOf("") }
            var inlineGame by remember { mutableStateOf("Free Fire MAX") }
            var inlineMap by remember { mutableStateOf("Bermuda") }
            var inlineFormat by remember { mutableStateOf("Squad") }
            var inlineDate by remember { mutableStateOf("Today") }
            var inlineTime by remember { mutableStateOf("09:00 PM") }
            var inlineEntryFee by remember { mutableStateOf("50") }
            var inlinePrizePool by remember { mutableStateOf("2500") }
            var inlinePerKill by remember { mutableStateOf("10") }
            var inlineSlots by remember { mutableStateOf("48") }

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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Host a New Tournament",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Set match details and publish live to Firestore",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(
                                onClick = { showCreateTournamentDialog = true },
                                modifier = Modifier.testTag("admin_create_tournament_btn")
                            ) {
                                Text("Open Dialog", color = NeonCyan, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = inlineTitle,
                            onValueChange = { inlineTitle = it },
                            label = { Text("Tournament Title") },
                            placeholder = { Text("e.g. BGMI Championship S1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_tournament_title"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Free Fire MAX", "BGMI").forEach { game ->
                                val isSelected = inlineGame == game
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            inlineGame = game
                                            inlineMap = if (game == "BGMI") "Erangel" else "Bermuda"
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
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inlineEntryFee,
                                onValueChange = { if (it.all { c -> c.isDigit() }) inlineEntryFee = it },
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
                                value = inlinePrizePool,
                                onValueChange = { if (it.all { c -> c.isDigit() }) inlinePrizePool = it },
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inlineTime,
                                onValueChange = { inlineTime = it },
                                label = { Text("Time (e.g. 09:00 PM)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_schedule_time"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = BorderMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = inlineMap,
                                onValueChange = { inlineMap = it },
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
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Publish Game and Cancel Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isLoading) return@Button

                                    val currentUser = try {
                                        FirebaseAuth.getInstance().currentUser
                                    } catch (_: Throwable) {
                                        null
                                    }
                                    if (currentUser == null) {
                                        isLoading = false
                                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isLoading = true
                                    val finalTitle = inlineTitle.ifBlank { "$inlineGame Mega Contest" }
                                    val entry = inlineEntryFee.toIntOrNull() ?: 50
                                    val prize = inlinePrizePool.toIntOrNull() ?: 2500
                                    val kill = inlinePerKill.toIntOrNull() ?: 10
                                    val slots = inlineSlots.toIntOrNull() ?: 48
                                    coroutineScope.launch {
                                        try {
                                            val result = viewModel.createTournamentAsync(
                                                title = finalTitle,
                                                gameCategory = inlineGame,
                                                map = inlineMap,
                                                format = inlineFormat,
                                                scheduleDate = inlineDate,
                                                scheduleTime = inlineTime,
                                                entryFee = entry,
                                                prizePool = prize,
                                                perKill = kill,
                                                totalSlots = slots
                                            )
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Tournament published successfully!", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            } else {
                                                val errorMsg = result.exceptionOrNull()?.localizedMessage
                                                    ?: result.exceptionOrNull()?.message
                                                    ?: "Failed to publish tournament"
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            val errorMsg = e.localizedMessage ?: e.message ?: "Failed to publish tournament"
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        } catch (t: Throwable) {
                                            val errorMsg = t.localizedMessage ?: t.message ?: "Failed to publish tournament"
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                enabled = !isLoading && !isAdminLoading,
                                modifier = Modifier.weight(1f).testTag("admin_publish_game_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isLoading || isAdminLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Text("Publish Game", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    inlineTitle = ""
                                    inlineEntryFee = "50"
                                    inlinePrizePool = "2500"
                                    inlineTime = "09:00 PM"
                                    onBack()
                                },
                                enabled = !isAdminLoading,
                                modifier = Modifier.weight(1f).testTag("admin_cancel_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderMuted),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Tournaments Management (Declare Winners & Update Stats)
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
                    text = "Leaderboard & Results",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        items(tournaments, key = { it.id }) { tournament ->
            AdminTournamentManageCard(
                tournament = tournament,
                onDeclareWinnerClick = { tournamentToDeclareWinner = tournament },
                onManageStatsClick = { onNavigateToMatchDetails(tournament) },
                onUpdateRoomClick = { tournamentToUpdateRoom = tournament },
                onViewParticipantsClick = { tournamentToViewParticipants = tournament }
            )
        }
    }

    // Dialog: Create Tournament
    if (showCreateTournamentDialog) {
        CreateTournamentDialog(
            isLoading = isLoading || isAdminLoading,
            onDismiss = {
                if (!isLoading) {
                    showCreateTournamentDialog = false
                }
            },
            onSubmit = { title, game, map, format, date, time, entryFee, prize, perKill, slots ->
                if (isLoading) return@CreateTournamentDialog

                val currentUser = try {
                    FirebaseAuth.getInstance().currentUser
                } catch (_: Throwable) {
                    null
                }
                if (currentUser == null) {
                    isLoading = false
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@CreateTournamentDialog
                }

                isLoading = true
                coroutineScope.launch {
                    try {
                        val result = viewModel.createTournamentAsync(
                            title = title,
                            gameCategory = game,
                            map = map,
                            format = format,
                            scheduleDate = date,
                            scheduleTime = time,
                            entryFee = entryFee,
                            prizePool = prize,
                            perKill = perKill,
                            totalSlots = slots
                        )
                        if (result.isSuccess) {
                            Toast.makeText(context, "Tournament published successfully!", Toast.LENGTH_SHORT).show()
                            showCreateTournamentDialog = false
                            onBack()
                        } else {
                            val errorMsg = result.exceptionOrNull()?.localizedMessage
                                ?: result.exceptionOrNull()?.message
                                ?: "Failed to publish tournament"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.localizedMessage ?: e.message ?: "Failed to publish tournament"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    } catch (t: Throwable) {
                        val errorMsg = t.localizedMessage ?: t.message ?: "Failed to publish tournament"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
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

    // Dialog: Update Custom Room ID & Password
    tournamentToUpdateRoom?.let { tour ->
        UpdateRoomDialog(
            tournament = tour,
            isLoading = isAdminLoading,
            onDismiss = { tournamentToUpdateRoom = null },
            onSave = { roomId, password ->
                viewModel.updateRoomCredentials(
                    tournamentId = tour.id,
                    roomId = roomId,
                    roomPassword = password,
                    onSuccess = { tournamentToUpdateRoom = null }
                )
            }
        )
    }

    // Dialog: View Registered Players and In-Game UIDs
    tournamentToViewParticipants?.let { tour ->
        ViewParticipantsDialog(
            tournament = tour,
            viewModel = viewModel,
            onDismiss = { tournamentToViewParticipants = null }
        )
    }
}


@Composable
fun AdminTournamentManageCard(
    tournament: Tournament,
    onDeclareWinnerClick: () -> Unit,
    onManageStatsClick: () -> Unit = {},
    onUpdateRoomClick: () -> Unit = {},
    onViewParticipantsClick: () -> Unit = {}
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

            // Room Credentials Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = CardNavyElevated,
                border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Room Credentials:", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = if (!tournament.roomId.isNullOrBlank()) "Room: ${tournament.roomId} | Pass: ${tournament.roomPassword ?: "----"}" else "Awaiting Host (Not Set)",
                            color = if (!tournament.roomId.isNullOrBlank()) EmeraldGreen else GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onUpdateRoomClick,
                        modifier = Modifier.size(30.dp).testTag("admin_edit_room_btn_${tournament.id}")
                    ) {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Edit Room", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buttons: Update Room & View UIDs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUpdateRoomClick,
                    modifier = Modifier.weight(1f).testTag("admin_update_room_btn_${tournament.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Room ID", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onViewParticipantsClick,
                    modifier = Modifier.weight(1f).testTag("admin_view_uids_btn_${tournament.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Player UIDs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onManageStatsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_manage_stats_btn_${tournament.id}"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NeonCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update Stats / Leaderboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun UpdateRoomDialog(
    tournament: Tournament,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (roomId: String, password: String) -> Unit
) {
    var roomId by remember { mutableStateOf(tournament.roomId ?: "") }
    var roomPassword by remember { mutableStateOf(tournament.roomPassword ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text("Update Custom Room Credentials", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tournament: ${tournament.title}",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Entered Room ID & Password will be instantly displayed to all registered players on their My Matches screen.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("Room ID") },
                    placeholder = { Text("e.g. 84920491") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_input_room_id"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = roomPassword,
                    onValueChange = { roomPassword = it },
                    label = { Text("Room Password") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_input_room_password"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(roomId.trim(), roomPassword.trim()) },
                enabled = !isLoading && roomId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("admin_confirm_room_update_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Broadcast to Players", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.testTag("admin_cancel_room_update_btn")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardNavy
    )
}

@Composable
fun ViewParticipantsDialog(
    tournament: Tournament,
    viewModel: EsportsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val participantsFlow = remember(tournament.id) { viewModel.getTournamentParticipants(tournament.id) }
    val participants by participantsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Registered Players & UIDs", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardNavyElevated
                ) {
                    Text(
                        text = "${participants.size} Players",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                Text(
                    text = "Verify player In-Game UIDs inside match lobby:",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (participants.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No players registered yet.\nUIDs will appear here as users join.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(participants, key = { it.userId }) { p ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CardNavyElevated,
                                border = BorderStroke(1.dp, if (p.submissionStatus == "pending") GoldAccent else BorderCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().testTag("participant_row_${p.userId}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Slot #${p.slotNumber} • ${p.username}",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "UID: ",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = p.inGameUid.ifBlank { "N/A" },
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        if (p.inGameUid.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("InGame UID", p.inGameUid)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "UID ${p.inGameUid} copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp).testTag("copy_participant_uid_${p.userId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy UID",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Result submission details (OCR screenshot & verification)
                                    if (p.submissionStatus.isNotBlank() && p.submissionStatus != "none") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (p.submissionStatus) {
                                                "pending" -> GoldAccent.copy(alpha = 0.12f)
                                                "approved" -> EmeraldGreen.copy(alpha = 0.12f)
                                                else -> CrimsonRed.copy(alpha = 0.12f)
                                            },
                                            border = BorderStroke(1.dp, when (p.submissionStatus) {
                                                "pending" -> GoldAccent.copy(alpha = 0.5f)
                                                "approved" -> EmeraldGreen.copy(alpha = 0.5f)
                                                else -> CrimsonRed.copy(alpha = 0.5f)
                                            }),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Status: ${p.submissionStatus.uppercase()}",
                                                        color = when (p.submissionStatus) {
                                                            "pending" -> GoldAccent
                                                            "approved" -> EmeraldGreen
                                                            else -> CrimsonRed
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "Claimed: ${p.kills} Kills • Rank #${p.rank}",
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                if (!p.resultImageUrl.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.fillMaxWidth().height(120.dp)
                                                    ) {
                                                        AsyncImage(
                                                            model = p.resultImageUrl,
                                                            contentDescription = "Scoreboard Screenshot",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }

                                                if (p.submissionStatus == "pending") {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.reviewResultSubmission(
                                                                    tournamentId = tournament.id,
                                                                    participant = p,
                                                                    approved = true,
                                                                    points = p.kills * 10,
                                                                    prizeWon = if (p.rank == 1) tournament.prizePool else 0
                                                                )
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                                            modifier = Modifier.weight(1f).testTag("approve_btn_${p.userId}"),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(vertical = 4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Approve", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }

                                                        OutlinedButton(
                                                            onClick = {
                                                                viewModel.reviewResultSubmission(
                                                                    tournamentId = tournament.id,
                                                                    participant = p,
                                                                    approved = false
                                                                )
                                                            },
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                                                            border = BorderStroke(1.dp, CrimsonRed),
                                                            modifier = Modifier.weight(1f).testTag("reject_btn_${p.userId}"),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(vertical = 4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Reject", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("close_participants_btn")
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardNavy
    )
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
                modifier = Modifier.testTag("admin_publish_game_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Publish Game", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    title = ""
                    entryFeeStr = "50"
                    prizePoolStr = "2500"
                    onDismiss()
                },
                enabled = !isLoading,
                modifier = Modifier.testTag("admin_cancel_btn")
            ) {
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
