package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.provider.MediaStore
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.GameCategory
import com.example.model.Tournament
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Parsed OCR Match Result data holder with extracted in-game IDs
data class ParsedMatchResult(
    val kills: Int,
    val rank: Int,
    val isBooyahOrWinner: Boolean,
    val extractedUids: List<String> = emptyList(),
    val rawText: String
)

// Regex-based OCR parser for Free Fire and BGMI match result scoreboards
fun parseScreenshotText(text: String): ParsedMatchResult {
    val clean = text.replace("\r", " ")
    val lower = clean.lowercase()

    val isBooyahOrWinner = lower.contains("booyah") ||
            lower.contains("chicken dinner") ||
            lower.contains("winner winner") ||
            lower.contains("victory") ||
            lower.contains("#1/") ||
            lower.contains("# 1/")

    // 1. Kills regex matching (handles Kills: 5, Finishes: 5, 5 Kills, Eliminations - 5)
    var detectedKills: Int? = null
    val killsPrefixRegex = Regex("""(?i)(?:kills?|finishes?|eliminations?|total\s*kills?)\s*[:#\-]?\s*(\d{1,2})""")
    val killsPrefixMatch = killsPrefixRegex.find(clean)
    if (killsPrefixMatch != null) {
        detectedKills = killsPrefixMatch.groupValues[1].toIntOrNull()
    }

    if (detectedKills == null) {
        val killsSuffixRegex = Regex("""(?i)(\d{1,2})\s*(?:kills?|finishes?|eliminations?)""")
        val killsSuffixMatch = killsSuffixRegex.find(clean)
        if (killsSuffixMatch != null) {
            detectedKills = killsSuffixMatch.groupValues[1].toIntOrNull()
        }
    }

    // 2. Rank regex matching (handles #1/50, Rank: 2, Standing #3, # 4)
    var detectedRank: Int? = null
    val rankSlashRegex = Regex("""#\s*(\d{1,2})\s*/\s*\d{1,3}""")
    val rankSlashMatch = rankSlashRegex.find(clean)
    if (rankSlashMatch != null) {
        detectedRank = rankSlashMatch.groupValues[1].toIntOrNull()
    }

    if (detectedRank == null) {
        val rankPrefixRegex = Regex("""(?i)(?:rank|standing|place|position)\s*[:#\-]?\s*#?\s*(\d{1,2})""")
        val rankPrefixMatch = rankPrefixRegex.find(clean)
        if (rankPrefixMatch != null) {
            detectedRank = rankPrefixMatch.groupValues[1].toIntOrNull()
        }
    }

    if (detectedRank == null) {
        val hashRankRegex = Regex("""#\s*([1-9]\d?)""")
        val hashRankMatch = hashRankRegex.find(clean)
        if (hashRankMatch != null) {
            detectedRank = hashRankMatch.groupValues[1].toIntOrNull()
        }
    }

    // Default rank if victory/booyah detected
    if (detectedRank == null && isBooyahOrWinner) {
        detectedRank = 1
    }

    // 3. In-Game UID extraction (Free Fire & BGMI UIDs are typically 7-14 digits, or labeled UID/ID)
    val extractedUids = mutableListOf<String>()
    val labeledUidRegex = Regex("""(?i)(?:uid|player\s*id|character\s*id|in-?game\s*id|game\s*uid|id)\s*[:#\-]?\s*([a-zA-Z0-9_\-]{4,20})""")
    labeledUidRegex.findAll(clean).forEach { match ->
        val uidValue = match.groupValues[1].trim()
        if (uidValue.isNotBlank() && !extractedUids.contains(uidValue)) {
            extractedUids.add(uidValue)
        }
    }

    val numericUidRegex = Regex("""\b(\d{7,14})\b""")
    numericUidRegex.findAll(clean).forEach { match ->
        val numValue = match.groupValues[1].trim()
        if (!extractedUids.contains(numValue)) {
            extractedUids.add(numValue)
        }
    }

    return ParsedMatchResult(
        kills = detectedKills ?: 0,
        rank = detectedRank ?: 1,
        isBooyahOrWinner = isBooyahOrWinner,
        extractedUids = extractedUids,
        rawText = clean
    )
}

// Anti-Cheat: Cross-Match user's registered In-Game UID against extracted screenshot text
fun verifyInGameUidMatch(expectedUid: String, parsedResult: ParsedMatchResult): Boolean {
    val cleanExpected = expectedUid.trim()
    if (cleanExpected.isBlank()) {
        return true // No registered UID constraint provided
    }

    // Check direct equality in extracted UIDs (case-insensitive)
    if (parsedResult.extractedUids.any { it.equals(cleanExpected, ignoreCase = true) }) {
        return true
    }

    // Check case-insensitive substring in raw OCR text
    if (parsedResult.rawText.contains(cleanExpected, ignoreCase = true)) {
        return true
    }

    // Normalized alphanumeric check (ignoring hyphens, underscores, spaces)
    val alnumExpected = cleanExpected.filter { it.isLetterOrDigit() }
    if (alnumExpected.length >= 4) {
        val alnumRaw = parsedResult.rawText.filter { it.isLetterOrDigit() }
        if (alnumRaw.contains(alnumExpected, ignoreCase = true)) {
            return true
        }
        if (parsedResult.extractedUids.any { uid ->
                uid.filter { it.isLetterOrDigit() }.equals(alnumExpected, ignoreCase = true)
            }) {
            return true
        }
    }

    return false
}

// Compute SHA-256 hash of image bytes for duplicate detection
fun computeImageHash(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } ?: ""
    } catch (e: Throwable) {
        Log.w("ImageHash", "computeImageHash error: ${e.message}")
        ""
    }
}

// Extract timestamp from MediaStore or ExifInterface
fun getImageTimestamp(context: Context, uri: Uri): Long? {
    try {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val takenIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (takenIdx >= 0) {
                    val dateTaken = cursor.getLong(takenIdx)
                    if (dateTaken > 0) return dateTaken
                }
                val modIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (modIdx >= 0) {
                    val dateMod = cursor.getLong(modIdx)
                    if (dateMod > 0) return dateMod * 1000L
                }
            }
        }
    } catch (_: Throwable) {}

    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = android.media.ExifInterface(stream)
            val dateStr = exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(android.media.ExifInterface.TAG_DATETIME)
            if (!dateStr.isNullOrBlank()) {
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            }
        }
    } catch (_: Throwable) {}

    return null
}

// Parse tournament schedule date & time into epoch milliseconds
fun parseTournamentScheduleMillis(scheduleDate: String, scheduleTime: String): Long {
    val cal = Calendar.getInstance()
    val dateLower = scheduleDate.trim().lowercase()

    when {
        dateLower.contains("tomorrow") -> cal.add(Calendar.DAY_OF_YEAR, 1)
        dateLower.contains("yesterday") -> cal.add(Calendar.DAY_OF_YEAR, -1)
        dateLower.contains("today") || dateLower.isBlank() -> { /* keep today */ }
        else -> {
            val dateFormats = listOf("dd MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "MMM dd, yyyy")
            for (pattern in dateFormats) {
                try {
                    val parsed = SimpleDateFormat(pattern, Locale.US).parse(scheduleDate.trim())
                    if (parsed != null) {
                        val temp = Calendar.getInstance().apply { time = parsed }
                        cal.set(Calendar.YEAR, temp.get(Calendar.YEAR))
                        cal.set(Calendar.MONTH, temp.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, temp.get(Calendar.DAY_OF_MONTH))
                        break
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    var hour = 19
    var minute = 0
    val timePatterns = listOf(
        SimpleDateFormat("hh:mm a", Locale.US),
        SimpleDateFormat("h:mm a", Locale.US),
        SimpleDateFormat("HH:mm", Locale.US),
        SimpleDateFormat("H:mm", Locale.US)
    )
    for (tf in timePatterns) {
        try {
            val parsedTime = tf.parse(scheduleTime.trim())
            if (parsedTime != null) {
                val temp = Calendar.getInstance().apply { time = parsedTime }
                hour = temp.get(Calendar.HOUR_OF_DAY)
                minute = temp.get(Calendar.MINUTE)
                break
            }
        } catch (_: Throwable) {}
    }

    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// Anti-Cheat: Validate that image was not taken prior to tournament schedule or older than end time
fun verifyScreenshotTime(
    context: Context,
    uri: Uri,
    scheduleDate: String,
    scheduleTime: String
): Pair<Boolean, String> {
    val imageTime = getImageTimestamp(context, uri) ?: return true to ""
    val scheduleTimeMillis = parseTournamentScheduleMillis(scheduleDate, scheduleTime)

    // Allowed early tolerance: 15 minutes before scheduled match start
    val earliestAllowed = scheduleTimeMillis - (15 * 60 * 1000L)
    if (imageTime < earliestAllowed) {
        val diffHours = (scheduleTimeMillis - imageTime) / (1000 * 3600)
        val timeDesc = if (diffHours >= 24) "${diffHours / 24} day(s) before match" else "$diffHours hour(s) before match"
        return false to "Time Check Failed: Image was captured before the tournament scheduled start time (taken $timeDesc). Please submit a screenshot taken during or after the match."
    }

    // Future check (5 minutes tolerance for clock skew)
    if (imageTime > System.currentTimeMillis() + (5 * 60 * 1000L)) {
        return false to "Time Check Failed: Screenshot has a future timestamp. Please ensure your device clock is correct."
    }

    return true to ""
}

@Composable
fun MyMatchesScreen(
    tournaments: List<Tournament>,
    onExploreTournaments: () -> Unit,
    onNavigateToMatchDetails: (Tournament) -> Unit = {},
    onBack: () -> Unit = onExploreTournaments,
    viewModel: EsportsViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val joinedMatches = remember(tournaments) { tournaments.filter { it.isJoined } }

    var selectedMatchForUpload by remember { mutableStateOf<Tournament?>(null) }
    var pendingScreenshotUri by remember { mutableStateOf<Uri?>(null) }
    var pendingScreenshotHash by remember { mutableStateOf("") }
    var isScanningOcr by remember { mutableStateOf(false) }
    var ocrDetectedSummary by remember { mutableStateOf("") }
    var verifiedUidSummary by remember { mutableStateOf("") }
    var detectedKills by remember { mutableStateOf(0) }
    var detectedRank by remember { mutableStateOf(1) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var antiCheatErrorMessage by remember { mutableStateOf("") }
    var showAntiCheatDialog by remember { mutableStateOf(false) }

    // Physical back button handler: close open dialogs first
    BackHandler(enabled = showConfirmDialog || isScanningOcr || showAntiCheatDialog) {
        showConfirmDialog = false
        showAntiCheatDialog = false
        isScanningOcr = false
        selectedMatchForUpload = null
        pendingScreenshotUri = null
        pendingScreenshotHash = ""
        antiCheatErrorMessage = ""
    }

    BackHandler(enabled = !showConfirmDialog && !isScanningOcr && !showAntiCheatDialog) {
        onBack()
    }

    // Gallery Image Picker contract using ActivityResultContracts.GetContent()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedMatchForUpload != null) {
            val match = selectedMatchForUpload!!
            pendingScreenshotUri = uri

            // 1. Time Check: Reject images older than the tournament schedule
            val (timeValid, timeError) = verifyScreenshotTime(context, uri, match.scheduleDate, match.scheduleTime)
            if (!timeValid) {
                antiCheatErrorMessage = timeError
                showAntiCheatDialog = true
                pendingScreenshotUri = null
                selectedMatchForUpload = null
                return@rememberLauncherForActivityResult
            }

            // 2. Generate SHA-256 Image Hash for duplicate prevention
            val hash = computeImageHash(context, uri)
            pendingScreenshotHash = hash

            // 3. OCR Text Recognition & UID Cross-Matching
            isScanningOcr = true
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        isScanningOcr = false
                        val parsed = parseScreenshotText(visionText.text)
                        detectedKills = parsed.kills
                        detectedRank = parsed.rank

                        // Determine expected In-Game UID
                        val expectedUid = match.registeredInGameUid.ifBlank {
                            viewModel?.userProfile?.value?.ingameId.orEmpty()
                        }.trim()

                        // Anti-Cheat Check 1: UID Cross-Matching
                        // If UID does not match registered In-Game UID, reject immediately with exact error
                        val uidMatches = verifyInGameUidMatch(expectedUid, parsed)
                        if (!uidMatches) {
                            antiCheatErrorMessage = "UID mismatch. Fake screenshot detected."
                            showAntiCheatDialog = true
                            pendingScreenshotUri = null
                            pendingScreenshotHash = ""
                            selectedMatchForUpload = null
                            Toast.makeText(context, "UID mismatch. Fake screenshot detected.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        verifiedUidSummary = if (expectedUid.isNotBlank()) "UID Verified: $expectedUid" else "UID Verified"
                        ocrDetectedSummary = if (parsed.isBooyahOrWinner) {
                            "Booyah / Victory Detected! ${parsed.kills} Kills, Rank #${parsed.rank}"
                        } else {
                            "Detected: ${parsed.kills} Kills, Rank #${parsed.rank}"
                        }
                        showConfirmDialog = true
                    }
                    .addOnFailureListener { e ->
                        isScanningOcr = false
                        val expectedUid = match.registeredInGameUid.ifBlank {
                            viewModel?.userProfile?.value?.ingameId.orEmpty()
                        }.trim()
                        if (expectedUid.isNotBlank() && expectedUid != "WW-PLAYER" && expectedUid != "WW-GUEST") {
                            antiCheatErrorMessage = "UID mismatch. Fake screenshot detected."
                            showAntiCheatDialog = true
                            pendingScreenshotUri = null
                            selectedMatchForUpload = null
                        } else {
                            detectedKills = 0
                            detectedRank = 1
                            ocrDetectedSummary = "OCR could not scan automatically. Please verify numbers manually."
                            showConfirmDialog = true
                        }
                    }
            } catch (t: Throwable) {
                isScanningOcr = false
                antiCheatErrorMessage = "Image processing error: ${t.message}"
                showAntiCheatDialog = true
                pendingScreenshotUri = null
                selectedMatchForUpload = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_matches_screen")
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
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "My Matches",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your registered cloud tournaments, room keys & result submissions",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardNavyElevated,
                    border = BorderStroke(1.dp, BorderCyan)
                ) {
                    Text(
                        text = "${joinedMatches.size} Active",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (joinedMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_matches_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    border = BorderStroke(1.dp, BorderMuted)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CardNavyElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Joined Tournaments Yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Browse tournaments on Home, click 'Join Tournament' to register and deduct entry fee.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onExploreTournaments,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Explore Tournaments", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(joinedMatches, key = { it.id }) { match ->
                JoinedMatchCard(
                    match = match,
                    onCopyCredentials = { roomId, password ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Room Details", "Room ID: $roomId, Password: $password")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Room credentials copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onViewResults = { onNavigateToMatchDetails(match) },
                    onUploadScreenshotClick = {
                        selectedMatchForUpload = match
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    // OCR Scanning Progress Dialog
    if (isScanningOcr) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning Result Screenshot...", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                    Text(
                        text = "Google ML Kit is extracting Kills and Rank from your match scoreboard...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {},
            containerColor = CardNavy
        )
    }

    // Confirm & Submit Dialog with Image Preview and Editable Kills/Rank
    if (showConfirmDialog && selectedMatchForUpload != null) {
        val match = selectedMatchForUpload!!
        var editKillsStr by remember(detectedKills) { mutableStateOf(detectedKills.toString()) }
        var editRankStr by remember(detectedRank) { mutableStateOf(detectedRank.toString()) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    showConfirmDialog = false
                    selectedMatchForUpload = null
                    pendingScreenshotUri = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Match Result", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = match.title,
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (pendingScreenshotUri != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().height(160.dp)
                            ) {
                                AsyncImage(
                                    model = pendingScreenshotUri,
                                    contentDescription = "Match Scoreboard Screenshot",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ocrDetectedSummary.ifBlank { "Detected: $detectedKills Kills, Rank #$detectedRank" },
                                    color = EmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardNavyElevated,
                            border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = verifiedUidSummary.ifBlank { "In-Game UID Verified" },
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (pendingScreenshotHash.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SHA-256: ${pendingScreenshotHash.take(16)}...",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editKillsStr,
                                onValueChange = { if (it.all { c -> c.isDigit() }) editKillsStr = it },
                                label = { Text("Kills") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("input_ocr_kills"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = BorderMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = editRankStr,
                                onValueChange = { if (it.all { c -> c.isDigit() }) editRankStr = it },
                                label = { Text("Rank (e.g. 1)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("input_ocr_rank"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = BorderMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardNavyElevated,
                            border = BorderStroke(1.dp, BorderMuted),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Anti-Cheat Policy: Result is submitted with status 'pending' (Pending Admin Verification). Kills and rank will only appear on the public leaderboard after manual Admin approval.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalKills = editKillsStr.toIntOrNull() ?: detectedKills
                        val finalRank = editRankStr.toIntOrNull() ?: detectedRank
                        if (viewModel != null) {
                            isSubmitting = true
                            viewModel.submitMatchResult(
                                tournamentId = match.id,
                                kills = finalKills,
                                rank = finalRank,
                                imageUri = pendingScreenshotUri,
                                imageHash = pendingScreenshotHash,
                                onSuccess = {
                                    isSubmitting = false
                                    showConfirmDialog = false
                                    selectedMatchForUpload = null
                                    pendingScreenshotUri = null
                                    pendingScreenshotHash = ""
                                    Toast.makeText(context, "Submitted! Status: Pending Admin Verification.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    isSubmitting = false
                                    showConfirmDialog = false
                                    antiCheatErrorMessage = errorMsg
                                    showAntiCheatDialog = true
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Result saved: $finalKills Kills, Rank #$finalRank", Toast.LENGTH_SHORT).show()
                            showConfirmDialog = false
                            selectedMatchForUpload = null
                            pendingScreenshotUri = null
                            pendingScreenshotHash = ""
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_submit_result_btn")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Confirm & Submit", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedMatchForUpload = null
                        pendingScreenshotUri = null
                        pendingScreenshotHash = ""
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardNavy
        )
    }

    // Anti-Cheat Security Dialog
    if (showAntiCheatDialog) {
        AlertDialog(
            onDismissRequest = {
                showAntiCheatDialog = false
                antiCheatErrorMessage = ""
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CrimsonRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Anti-Cheat Alert",
                        tint = CrimsonRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Anti-Cheat Alert",
                    color = CrimsonRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = antiCheatErrorMessage.ifBlank { "UID mismatch. Fake screenshot detected." },
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "To protect fair tournament play, screenshots must display your registered In-Game ID and valid match schedule. Mismatched UIDs, duplicate screenshots from other players, or fake images are immediately rejected.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAntiCheatDialog = false
                        antiCheatErrorMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("anti_cheat_dismiss_btn")
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardNavy,
            modifier = Modifier.testTag("anti_cheat_error_dialog")
        )
    }
}

fun launchGameDirectly(context: Context, match: Tournament) {
    val pm = context.packageManager
    val isBgmi = match.gameCategory == GameCategory.BGMI ||
            match.gameCategoryName.contains("BGMI", ignoreCase = true) ||
            match.title.contains("BGMI", ignoreCase = true) ||
            match.bannerType.equals("bgmi", ignoreCase = true)

    val (packagesToCheck, gameName) = if (isBgmi) {
        listOf("com.pubg.imobile") to "BGMI"
    } else {
        listOf("com.dts.freefiremax", "com.dts.freefireth") to "Free Fire"
    }

    var launchIntent: Intent? = null
    for (pkg in packagesToCheck) {
        try {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                launchIntent = intent
                break
            }
        } catch (_: Exception) {}
    }

    if (launchIntent != null) {
        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch $gameName: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(
            context,
            "$gameName is not installed on this phone. Please download it from Play Store.",
            Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
fun JoinedMatchCard(
    match: Tournament,
    onCopyCredentials: (String, String) -> Unit,
    onViewResults: () -> Unit = {},
    onUploadScreenshotClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bannerRes = if (match.bannerType == "bgmi" || match.gameCategory == GameCategory.BGMI) {
        R.drawable.img_banner_bgmi
    } else {
        R.drawable.img_banner_ff
    }

    val hasRoomUpdated = !match.roomId.isNullOrBlank() &&
            !match.roomId.equals("Awaiting Host", ignoreCase = true) &&
            !match.roomId.equals("N/A", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("joined_match_card_${match.id}")
            .clickable { onViewResults() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        border = BorderStroke(1.dp, BorderCyan)
    ) {
        Column {
            // Header Image with game tag & schedule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = match.title,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CardNavy.copy(alpha = 0.95f))
                            )
                        )
                )

                // Status Tag
                Surface(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopEnd),
                    color = EmeraldGreen,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "REGISTERED",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${match.scheduleDate} • ${match.scheduleTime}",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomStart)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = match.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${match.gameCategory.displayName} • ${match.map} • ${match.matchType.ifBlank { match.format.label }}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Room ID & Password box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardNavyElevated,
                    border = BorderStroke(1.dp, if (hasRoomUpdated) EmeraldGreen.copy(alpha = 0.7f) else BorderCyan.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    tint = if (hasRoomUpdated) EmeraldGreen else NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasRoomUpdated) "Custom Room Credentials (LIVE)" else "Custom Room Credentials",
                                    color = if (hasRoomUpdated) EmeraldGreen else NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasRoomUpdated) {
                                IconButton(
                                    onClick = {
                                        onCopyCredentials(
                                            match.roomId ?: "N/A",
                                            match.roomPassword ?: "N/A"
                                        )
                                    },
                                    modifier = Modifier.size(28.dp).testTag("copy_room_btn_${match.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Both Details",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (!hasRoomUpdated) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Admin will update the Room ID & Password 15 minutes before the match start time.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Room ID with dedicated Copy Icon
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Room ID", color = TextMuted, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = match.roomId ?: "Awaiting Host",
                                        color = if (hasRoomUpdated) TextPrimary else TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (hasRoomUpdated) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Room ID", match.roomId ?: "")
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Room ID copied: ${match.roomId}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp).testTag("copy_room_id_btn_${match.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Room ID",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Password with dedicated Copy Icon
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Password", color = TextMuted, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = match.roomPassword ?: "----",
                                        color = GoldAccent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (hasRoomUpdated && !match.roomPassword.isNullOrBlank() && match.roomPassword != "----") {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Password", match.roomPassword ?: "")
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Password copied: ${match.roomPassword}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp).testTag("copy_password_btn_${match.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Password",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Slot Number
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your Slot", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = "#${match.filledSlots.coerceAtLeast(1)}",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prize Pool: ₹${match.prizePool} | Kill: ₹${match.perKill}",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "Cloud Verified",
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play Game button (Deep Linking Intent to Free Fire / BGMI)
                Button(
                    onClick = { launchGameDirectly(context, match) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_game_btn_${match.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Play Game",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isBgmi = match.gameCategory == GameCategory.BGMI || match.bannerType == "bgmi"
                    val gameLabel = if (isBgmi) "Play BGMI" else "Play Free Fire"
                    Text("$gameLabel (Launch Game)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Result Submission Section (OCR Scan & Upload)
                val statusLower = match.submissionStatus.lowercase()
                when {
                    statusLower == "pending" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("pending_result_card_${match.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = GoldAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Result Pending Verification",
                                            color = GoldAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${match.submittedKills} Kills • Rank #${match.submittedRank}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pending Admin approval",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    TextButton(
                                        onClick = onUploadScreenshotClick,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("reupload_result_btn_${match.id}")
                                    ) {
                                        Text("Re-upload", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    statusLower == "approved" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("approved_result_card_${match.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Result Approved & Verified",
                                        color = EmeraldGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${match.submittedKills} Kills • Rank #${match.submittedRank}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    statusLower == "rejected" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("rejected_result_card_${match.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = CrimsonRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Result Rejected by Host",
                                        color = CrimsonRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = onUploadScreenshotClick,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Re-submit", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onUploadScreenshotClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_result_btn_${match.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Result Screenshot",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Result Screenshot (OCR Scan)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Results / Leaderboard button
                OutlinedButton(
                    onClick = onViewResults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_view_results_${match.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan
                    )
                ) {
                    Text("View Results & Leaderboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
