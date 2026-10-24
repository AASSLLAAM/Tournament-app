package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.MatchResult
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

@Composable
fun MatchDetailsScreen(
    tournament: Tournament,
    viewModel: EsportsViewModel,
    userProfile: UserProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddStatDialog by remember { mutableStateOf(false) }

    // Physical back button handler: close dialog first, else return
    BackHandler(enabled = showAddStatDialog) {
        showAddStatDialog = false
    }

    BackHandler(enabled = !showAddStatDialog) {
        onBack()
    }

    val resultsFlow = remember(tournament.id) { viewModel.getMatchResults(tournament.id) }
    val resultsList by resultsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val participantsFlow = remember(tournament.id) { viewModel.getTournamentParticipants(tournament.id) }
    val participantsList by participantsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingSubmissions = remember(participantsList) { participantsList.filter { it.submissionStatus == "pending" } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavy)
            .testTag("match_details_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top App Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("match_details_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Match Leaderboard",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tournament.title,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (userProfile.isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldAccent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("btn_admin_add_stat_header")
                        ) {
                            Text(
                                text = "ADMIN",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Tournament Overview Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    border = BorderStroke(1.dp, BorderCyan)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF003847), CardNavyElevated)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tournament.bannerType == "bgmi") Color(0xFF1B5E20) else Color(0xFFE65100)
                                ) {
                                    Text(
                                        text = tournament.gameCategory.displayName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tournament.isCompleted) GoldAccent.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (tournament.isCompleted) GoldAccent else NeonCyan)
                                ) {
                                    Text(
                                        text = if (tournament.isCompleted) "COMPLETED" else "LIVE RESULTS",
                                        color = if (tournament.isCompleted) GoldAccent else NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = tournament.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tournament.map, color = TextSecondary, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tournament.format.label, color = TextSecondary, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tournament.scheduleTime, color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Prize & Per Kill metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CardNavy,
                                    border = BorderStroke(1.dp, BorderMuted),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Prize Pool", color = TextMuted, fontSize = 10.sp)
                                        Text("₹${tournament.prizePool}", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CardNavy,
                                    border = BorderStroke(1.dp, BorderMuted),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Per Kill", color = TextMuted, fontSize = 10.sp)
                                        Text("₹${tournament.perKill}", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Champion Banner if declared
                            if (!tournament.winnerName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GoldAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Tournament Champion", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(tournament.winnerName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Admin: Pending Result Submissions (OCR verified screenshots)
            if (userProfile.isAdmin && pendingSubmissions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("pending_submissions_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pending Player Results",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${pendingSubmissions.size} Submissions",
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Verify screenshots submitted by players. Approving will automatically update the leaderboard below.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                pendingSubmissions.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = CardNavyElevated,
                                        border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().testTag("pending_submission_item_${sub.userId}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${sub.username} (Slot #${sub.slotNumber})",
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = "UID: ${sub.inGameUid.ifBlank { "N/A" }}",
                                                        color = NeonCyan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = EmeraldGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Claimed: ${sub.kills} Kills • #${sub.rank}",
                                                        color = EmeraldGreen,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            if (!sub.resultImageUrl.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(140.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = sub.resultImageUrl,
                                                        contentDescription = "Match Scoreboard Screenshot",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.reviewResultSubmission(
                                                            tournamentId = tournament.id,
                                                            participant = sub,
                                                            approved = true,
                                                            points = sub.kills * 10,
                                                            prizeWon = if (sub.rank == 1) tournament.prizePool else 0
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                                    modifier = Modifier.weight(1f).testTag("admin_approve_submission_${sub.userId}"),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approve & Publish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.reviewResultSubmission(
                                                            tournamentId = tournament.id,
                                                            participant = sub,
                                                            approved = false
                                                        )
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                                                    border = BorderStroke(1.dp, CrimsonRed),
                                                    modifier = Modifier.weight(1f).testTag("admin_reject_submission_${sub.userId}"),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reject", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

            // Leaderboard Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Standings & Kill Leaderboard",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (userProfile.isAdmin) {
                        Button(
                            onClick = { showAddStatDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_add_stat_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Stat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Results Table / Cards
            if (resultsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        border = BorderStroke(1.dp, BorderMuted)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No match results posted yet", color = TextSecondary, fontSize = 14.sp)
                            Text("Live stats will appear once the custom scrim concludes.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(resultsList, key = { it.id.ifBlank { "${it.rank}_${it.teamName}" } }) { result ->
                    MatchResultItemCard(result = result)
                }
            }
        }

        // Admin Add/Update Stats Dialog
        if (showAddStatDialog) {
            AddUpdateMatchResultDialog(
                tournamentId = tournament.id,
                existingResult = null,
                isLoading = false,
                onDismiss = { showAddStatDialog = false },
                onSave = { rank, team, player, kills, points, prize ->
                    viewModel.saveMatchResult(
                        tournamentId = tournament.id,
                        rank = rank,
                        teamName = team,
                        playerName = player,
                        kills = kills,
                        points = points,
                        prizeWon = prize,
                        onSuccess = { showAddStatDialog = false }
                    )
                }
            )
        }
    }
}

@Composable
fun MatchResultItemCard(
    result: MatchResult,
    modifier: Modifier = Modifier
) {
    val rankColor = when (result.rank) {
        1 -> GoldAccent
        2 -> Color(0xFFE0E0E0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> NeonCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_result_card_rank_${result.rank}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        border = BorderStroke(1.dp, if (result.rank == 1) GoldAccent.copy(alpha = 0.6f) else BorderMuted)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                shape = CircleShape,
                color = rankColor.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, rankColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = result.rankBadge,
                        color = rankColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Team and Player Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.teamName.ifBlank { "Team #${result.rank}" },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.playerName.ifBlank { "Unknown Player" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Kills Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardNavyElevated,
                border = BorderStroke(1.dp, BorderMuted)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${result.kills} Kills",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Points Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (result.rank == 1) GoldAccent.copy(alpha = 0.2f) else CardNavyElevated,
                border = BorderStroke(1.dp, if (result.rank == 1) GoldAccent else BorderMuted)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${result.points} pts",
                        color = if (result.rank == 1) GoldAccent else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun AddUpdateMatchResultDialog(
    tournamentId: String,
    existingResult: MatchResult? = null,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (rank: Int, teamName: String, playerName: String, kills: Int, points: Int, prizeWon: Int) -> Unit
) {
    var rankStr by remember { mutableStateOf(existingResult?.rank?.toString() ?: "1") }
    var teamName by remember { mutableStateOf(existingResult?.teamName ?: "") }
    var playerName by remember { mutableStateOf(existingResult?.playerName ?: "") }
    var killsStr by remember { mutableStateOf(existingResult?.kills?.toString() ?: "5") }
    var pointsStr by remember { mutableStateOf(existingResult?.points?.toString() ?: "15") }
    var prizeStr by remember { mutableStateOf(existingResult?.prizeWon?.toString() ?: "0") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text("Update Match Stats / Leaderboard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = rankStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) rankStr = it },
                        label = { Text("Rank (1, 2..)") },
                        modifier = Modifier.weight(1f).testTag("dialog_stat_rank_input"),
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = killsStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) killsStr = it },
                        label = { Text("Kills") },
                        modifier = Modifier.weight(1f).testTag("dialog_stat_kills_input"),
                        singleLine = true
                    )
                }

                androidx.compose.material3.OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    placeholder = { Text("e.g. Team Soul / GodLike") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_stat_team_input"),
                    singleLine = true
                )

                androidx.compose.material3.OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Player Name (Gamer Tag)") },
                    placeholder = { Text("e.g. Mortal / Jonathan") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_stat_player_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = pointsStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pointsStr = it },
                        label = { Text("Points") },
                        modifier = Modifier.weight(1f).testTag("dialog_stat_points_input"),
                        singleLine = true
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = prizeStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) prizeStr = it },
                        label = { Text("Prize (₹)") },
                        modifier = Modifier.weight(1f).testTag("dialog_stat_prize_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = rankStr.toIntOrNull() ?: 1
                    val k = killsStr.toIntOrNull() ?: 0
                    val pts = pointsStr.toIntOrNull() ?: (k * 2)
                    val pz = prizeStr.toIntOrNull() ?: 0
                    onSave(r, teamName, playerName, k, pts, pz)
                },
                enabled = !isLoading && (teamName.isNotBlank() || playerName.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_save_stat_confirm_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Save to Firestore", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardNavy
    )
}
