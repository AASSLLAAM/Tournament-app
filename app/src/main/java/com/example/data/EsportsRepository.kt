package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.MatchResult
import com.example.model.Tournament
import com.example.model.UserProfile
import com.example.model.UserRegistration
import com.example.model.WalletTransaction
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "EsportsRepository"

val DEFAULT_TOURNAMENTS = listOf(
    Tournament(
        id = "tour_ff_01",
        title = "Free Fire Pro Scrims Season 4",
        gameCategoryName = "FREE_FIRE",
        map = "Bermuda",
        formatName = "SQUAD",
        scheduleTime = "06:30 PM",
        scheduleDate = "Today",
        entryFee = 50,
        prizePool = 2500,
        perKill = 10,
        totalSlots = 48,
        filledSlots = 24,
        bannerType = "ff",
        serverType = "Asia / TPP",
        organizer = "WorldWar Official",
        statusText = "Open"
    ),
    Tournament(
        id = "tour_bgmi_01",
        title = "BGMI Masters Mega Showdown",
        gameCategoryName = "BGMI",
        map = "Erangel",
        formatName = "SQUAD",
        scheduleTime = "08:00 PM",
        scheduleDate = "Today",
        entryFee = 100,
        prizePool = 5000,
        perKill = 20,
        totalSlots = 100,
        filledSlots = 52,
        bannerType = "bgmi",
        serverType = "Asia / TPP",
        organizer = "WorldWar Official",
        statusText = "Open"
    ),
    Tournament(
        id = "tour_ff_02",
        title = "Free Fire Solo Headshot Championship",
        gameCategoryName = "FREE_FIRE",
        map = "Purgatory",
        formatName = "SOLO",
        scheduleTime = "09:15 PM",
        scheduleDate = "Today",
        entryFee = 30,
        prizePool = 1500,
        perKill = 15,
        totalSlots = 48,
        filledSlots = 18,
        bannerType = "ff",
        serverType = "Asia / TPP",
        organizer = "WorldWar Official",
        statusText = "Open"
    )
)

class EsportsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(createFirestoreInstance(context))

    companion object {
        fun createFirestoreInstance(context: Context): FirebaseFirestore {
            val databaseId = try {
                val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else "ai-studio-android-tourname-a4f8aa2c-79ec-4638-bf9c-a241de520fdd"
            } catch (_: Throwable) {
                "ai-studio-android-tourname-a4f8aa2c-79ec-4638-bf9c-a241de520fdd"
            }
            return try {
                FirebaseFirestore.getInstance(databaseId)
            } catch (_: Throwable) {
                FirebaseFirestore.getInstance()
            }
        }
    }

    private val auth: FirebaseAuth?
        get() = try {
            Firebase.auth
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth instance retrieval warning: ${e.message}")
            null
        }

    fun isGuest(): Boolean {
        return try {
            auth?.currentUser == null || requireUserId() == "player_guest"
        } catch (_: Throwable) {
            true
        }
    }

    private val guestProfileFlow = MutableStateFlow(
        UserProfile(
            userId = "player_guest",
            username = "GuestPlayer",
            ingameId = "WW-GUEST",
            email = "guest@worldwar.com",
            role = "player",
            level = 1,
            tier = "PRO WARRIOR",
            walletBalance = 500
        )
    )

    private val guestRegistrationsFlow = MutableStateFlow<List<UserRegistration>>(emptyList())

    private val guestTransactionsFlow = MutableStateFlow<List<WalletTransaction>>(
        listOf(
            WalletTransaction(
                id = "tx_guest_welcome",
                userId = "player_guest",
                title = "Welcome Bonus (Guest)",
                amount = 500,
                isCredit = true,
                date = "Today",
                status = "Success"
            )
        )
    )

    fun requireUserId(): String {
        return try {
            auth?.currentUser?.uid ?: "player_guest"
        } catch (_: Throwable) {
            "player_guest"
        }
    }

    // 1. Observe all tournaments from Firestore
    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val path = "tournaments"
        try {
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Tournament::class.java)?.copy(id = doc.id)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                        if (list.isEmpty()) DEFAULT_TOURNAMENTS else list
                    }
                    .catch { error ->
                        Log.w(TAG, "observeTournaments stream warning: ${error.message}")
                        emit(DEFAULT_TOURNAMENTS)
                    }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "observeTournaments catch: ${e.message}")
            emit(DEFAULT_TOURNAMENTS)
        }
    }

    // 2. Observe user's joined tournament registrations
    fun observeUserRegistrations(): Flow<List<UserRegistration>> = flow {
        if (isGuest()) {
            emitAll(guestRegistrationsFlow)
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid/registrations"
        try {
            emitAll(
                db.collection("users").document(uid).collection("registrations")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(UserRegistration::class.java)?.copy(registrationId = doc.id)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    }
                    .catch { error ->
                        handleFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, path)
            emit(emptyList())
        }
    }

    // 3. Observe user profile (including wallet balance and admin role)
    fun observeUserProfile(): Flow<UserProfile?> = flow {
        if (isGuest()) {
            emitAll(guestProfileFlow)
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            emitAll(
                db.collection("users").document(uid)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            try {
                                val profile = snapshot.toObject(UserProfile::class.java)
                                val balance = (snapshot.getLong("walletBalance") ?: 500L).toInt()
                                val roleStr = snapshot.getString("role") ?: "player"
                                profile?.copy(walletBalance = balance, role = roleStr)
                            } catch (_: Throwable) {
                                UserProfile(userId = uid, walletBalance = 500, role = "player")
                            }
                        } else {
                            UserProfile(userId = uid, walletBalance = 500, role = "player")
                        }
                    }
                    .catch { error ->
                        handleFirestoreError(error, OperationType.GET, path)
                        emit(UserProfile(userId = uid, walletBalance = 500, role = "player"))
                    }
            )
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.GET, path)
            emit(UserProfile(userId = uid, walletBalance = 500, role = "player"))
        }
    }

    // 4. Observe user transactions
    fun observeTransactions(): Flow<List<WalletTransaction>> = flow {
        if (isGuest()) {
            emitAll(guestTransactionsFlow)
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        try {
            emitAll(
                db.collection("users").document(uid).collection("transactions")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    }
                    .catch { error ->
                        handleFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, path)
            emit(emptyList())
        }
    }

    // In-memory cache for match results (used for guest mode and offline preview)
    private val localMatchResults = mutableMapOf<String, MutableStateFlow<List<MatchResult>>>()

    fun getDefaultMatchResults(tournamentId: String): List<MatchResult> {
        return listOf(
            MatchResult(
                id = "${tournamentId}_res_1",
                tournamentId = tournamentId,
                rank = 1,
                teamName = "Team Soul",
                playerName = "Soul_Mortal",
                kills = 14,
                points = 29,
                prizeWon = 1200
            ),
            MatchResult(
                id = "${tournamentId}_res_2",
                tournamentId = tournamentId,
                rank = 2,
                teamName = "GodLike Esports",
                playerName = "GodL_Jonathan",
                kills = 11,
                points = 23,
                prizeWon = 700
            ),
            MatchResult(
                id = "${tournamentId}_res_3",
                tournamentId = tournamentId,
                rank = 3,
                teamName = "Total Gaming",
                playerName = "TG_Ajjubhai",
                kills = 8,
                points = 18,
                prizeWon = 400
            ),
            MatchResult(
                id = "${tournamentId}_res_4",
                tournamentId = tournamentId,
                rank = 4,
                teamName = "Blind Esports",
                playerName = "Blind_Spower",
                kills = 6,
                points = 14,
                prizeWon = 200
            ),
            MatchResult(
                id = "${tournamentId}_res_5",
                tournamentId = tournamentId,
                rank = 5,
                teamName = "Revenant X",
                playerName = "RNT_Sensei",
                kills = 4,
                points = 10,
                prizeWon = 0
            )
        )
    }

    // 5. Observe Match Leaderboard / Results from tournaments/{tournamentId}/results
    fun observeMatchResults(tournamentId: String): Flow<List<MatchResult>> = flow {
        val defaultList = getDefaultMatchResults(tournamentId)
        if (isGuest()) {
            val flow = localMatchResults.getOrPut(tournamentId) { MutableStateFlow(defaultList) }
            emitAll(flow)
            return@flow
        }
        val path = "tournaments/$tournamentId/results"
        try {
            emitAll(
                db.collection("tournaments").document(tournamentId).collection("results")
                    .snapshots()
                    .map { snapshot ->
                        val results = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(MatchResult::class.java)?.copy(id = doc.id)
                            } catch (_: Throwable) {
                                null
                            }
                        }.sortedBy { it.rank }

                        if (results.isEmpty()) defaultList else results
                    }
                    .catch { error ->
                        Log.w(TAG, "observeMatchResults fallback: ${error.message}")
                        emit(defaultList)
                    }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "observeMatchResults catch fallback: ${e.message}")
            emit(defaultList)
        }
    }

    // Admin: Add or update Match Result / Leaderboard entry
    suspend fun saveMatchResult(tournamentId: String, matchResult: MatchResult): Result<Unit> {
        val resultId = matchResult.id.ifBlank { "res_${System.currentTimeMillis() % 100000}" }
        val toSave = matchResult.copy(id = resultId, tournamentId = tournamentId)

        if (isGuest()) {
            val flow = localMatchResults.getOrPut(tournamentId) { MutableStateFlow(getDefaultMatchResults(tournamentId)) }
            val current = flow.value.filter { it.id != resultId && it.rank != toSave.rank }
            val updated = (current + toSave).sortedBy { it.rank }
            flow.value = updated
            return Result.success(Unit)
        }

        return try {
            val map = mapOf(
                "id" to resultId,
                "tournamentId" to tournamentId,
                "rank" to toSave.rank,
                "teamName" to toSave.teamName,
                "playerName" to toSave.playerName,
                "kills" to toSave.kills,
                "points" to toSave.points,
                "prizeWon" to toSave.prizeWon
            )
            db.collection("tournaments").document(tournamentId)
                .collection("results").document(resultId)
                .set(map).await()

            // Update local in-memory cache as well
            val flow = localMatchResults.getOrPut(tournamentId) { MutableStateFlow(getDefaultMatchResults(tournamentId)) }
            val current = flow.value.filter { it.id != resultId && it.rank != toSave.rank }
            flow.value = (current + toSave).sortedBy { it.rank }

            Result.success(Unit)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/$tournamentId/results/$resultId")
            Result.failure(e)
        }
    }

    // Seed default tournaments if empty
    suspend fun seedDefaultTournamentsIfEmpty() {
        try {
            val snapshot = db.collection("tournaments").limit(1).get().await()
            if (snapshot.isEmpty) {
                val seedData = listOf(
                    Tournament(
                        id = "tour_ff_01",
                        title = "Free Fire Pro Scrims Season 4",
                        gameCategoryName = "FREE_FIRE",
                        map = "Bermuda",
                        formatName = "SQUAD",
                        scheduleTime = "06:30 PM",
                        scheduleDate = "Today",
                        entryFee = 50,
                        prizePool = 2500,
                        perKill = 10,
                        totalSlots = 48,
                        filledSlots = 24,
                        bannerType = "ff",
                        serverType = "Asia / TPP",
                        organizer = "WorldWar Official",
                        statusText = "Open"
                    ),
                    Tournament(
                        id = "tour_bgmi_01",
                        title = "BGMI Masters Mega Showdown",
                        gameCategoryName = "BGMI",
                        map = "Erangel",
                        formatName = "SQUAD",
                        scheduleTime = "08:00 PM",
                        scheduleDate = "Today",
                        entryFee = 100,
                        prizePool = 5000,
                        perKill = 20,
                        totalSlots = 100,
                        filledSlots = 52,
                        bannerType = "bgmi",
                        serverType = "Asia / TPP",
                        organizer = "WorldWar Official",
                        statusText = "Open"
                    ),
                    Tournament(
                        id = "tour_ff_02",
                        title = "Free Fire Solo Headshot Championship",
                        gameCategoryName = "FREE_FIRE",
                        map = "Purgatory",
                        formatName = "SOLO",
                        scheduleTime = "09:15 PM",
                        scheduleDate = "Today",
                        entryFee = 30,
                        prizePool = 1500,
                        perKill = 15,
                        totalSlots = 48,
                        filledSlots = 18,
                        bannerType = "ff",
                        serverType = "Asia / TPP",
                        organizer = "WorldWar Official",
                        statusText = "Open"
                    )
                )

                for (tour in seedData) {
                    try {
                        val map = mapOf(
                            "id" to tour.id,
                            "title" to tour.title,
                            "gameCategoryName" to tour.gameCategoryName,
                            "map" to tour.map,
                            "formatName" to tour.formatName,
                            "scheduleTime" to tour.scheduleTime,
                            "scheduleDate" to tour.scheduleDate,
                            "entryFee" to tour.entryFee,
                            "prizePool" to tour.prizePool,
                            "perKill" to tour.perKill,
                            "totalSlots" to tour.totalSlots,
                            "filledSlots" to tour.filledSlots,
                            "bannerType" to tour.bannerType,
                            "serverType" to tour.serverType,
                            "organizer" to tour.organizer,
                            "statusText" to tour.statusText
                        )
                        db.collection("tournaments").document(tour.id).set(map).await()
                    } catch (_: Throwable) {}
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments")
        }
    }

    // Ensure user profile document exists with role
    suspend fun ensureUserProfileExists(userEmail: String?, displayName: String?): UserProfile {
        if (isGuest()) {
            return guestProfileFlow.value
        }
        val uid = requireUserId()
        val defaultProfile = UserProfile(
            userId = uid,
            username = displayName?.ifBlank { null } ?: userEmail?.substringBefore("@")?.ifBlank { null } ?: "WorldPlayer",
            ingameId = "WW-${(1000..9999).random()}",
            email = userEmail ?: "",
            role = "player",
            level = 1,
            tier = "PRO WARRIOR",
            walletBalance = 500
        )

        try {
            val userDocRef = db.collection("users").document(uid)
            val snapshot = try { userDocRef.get().await() } catch (_: Throwable) { null }

            // Check if user is registered in admins collection or matches bootstrap email
            val isUserAdmin = try {
                val adminDoc = db.collection("admins").document(uid).get().await()
                adminDoc.exists() || (userEmail != null && (
                    userEmail.equals("ah296629@gmail.com", ignoreCase = true) ||
                    userEmail.equals("aslamhussain096@gmail.com", ignoreCase = true)
                ))
            } catch (_: Throwable) {
                userEmail != null && (
                    userEmail.equals("ah296629@gmail.com", ignoreCase = true) ||
                    userEmail.equals("aslamhussain096@gmail.com", ignoreCase = true)
                )
            }

            val role = if (isUserAdmin) "admin" else "player"

            if (snapshot == null || !snapshot.exists()) {
                val initialBalance = 500
                val username = displayName?.ifBlank { null }
                    ?: userEmail?.substringBefore("@")?.ifBlank { null }
                    ?: "Player_${uid.take(4)}"

                val newProfile = UserProfile(
                    userId = uid,
                    username = username,
                    ingameId = "WW-${(1000..9999).random()}",
                    email = userEmail ?: "",
                    role = role,
                    level = 5,
                    tier = if (isUserAdmin) "TOURNAMENT ADMIN" else "PRO WARRIOR",
                    walletBalance = initialBalance
                )

                val profileMap = mapOf(
                    "userId" to uid,
                    "username" to newProfile.username,
                    "ingameId" to newProfile.ingameId,
                    "email" to newProfile.email,
                    "role" to role,
                    "level" to newProfile.level,
                    "tier" to newProfile.tier,
                    "matchesPlayed" to 0,
                    "totalWins" to 0,
                    "totalKills" to 0,
                    "winRate" to "0%",
                    "walletBalance" to initialBalance,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                try {
                    userDocRef.set(profileMap).await()
                } catch (_: Throwable) {}

                // If user is admin, guarantee presence in /admins collection
                if (isUserAdmin) {
                    try {
                        db.collection("admins").document(uid).set(mapOf("uid" to uid, "email" to (userEmail ?: ""))).await()
                    } catch (_: Throwable) {}
                }

                // Record initial welcome bonus
                try {
                    val txId = "tx_welcome_${System.currentTimeMillis() % 100000}"
                    val txMap = mapOf(
                        "id" to txId,
                        "userId" to uid,
                        "title" to "Sign-up Welcome Bonus",
                        "amount" to initialBalance,
                        "isCredit" to true,
                        "date" to "Just now",
                        "status" to "Success",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    userDocRef.collection("transactions").document(txId).set(txMap).await()
                } catch (_: Throwable) {}

                return newProfile
            } else {
                val balance = (snapshot.getLong("walletBalance") ?: 500L).toInt()
                val existingRole = snapshot.getString("role") ?: role

                // Update role if user is recognized as admin
                if (isUserAdmin && existingRole != "admin") {
                    try {
                        userDocRef.update("role", "admin").await()
                        db.collection("admins").document(uid).set(mapOf("uid" to uid, "email" to (userEmail ?: ""))).await()
                    } catch (_: Throwable) {}
                }

                return try {
                    snapshot.toObject(UserProfile::class.java)?.copy(
                        walletBalance = balance,
                        role = if (isUserAdmin) "admin" else existingRole
                    ) ?: defaultProfile.copy(walletBalance = balance, role = role)
                } catch (_: Throwable) {
                    defaultProfile.copy(walletBalance = balance, role = role)
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            return defaultProfile
        }
    }

    // Toggle admin role for testing convenience
    suspend fun toggleAdminRole(enable: Boolean): Result<Unit> {
        if (isGuest()) {
            val current = guestProfileFlow.value
            guestProfileFlow.value = current.copy(role = if (enable) "admin" else "player")
            return Result.success(Unit)
        }
        val uid = requireUserId()
        return try {
            val userDocRef = db.collection("users").document(uid)
            val adminDocRef = db.collection("admins").document(uid)
            val newRole = if (enable) "admin" else "player"

            userDocRef.update("role", newRole).await()
            if (enable) {
                adminDocRef.set(mapOf("uid" to uid, "email" to (auth?.currentUser?.email ?: ""))).await()
            } else {
                adminDocRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Admin: Create a new Tournament
    suspend fun createTournament(
        title: String,
        gameCategory: String,
        map: String,
        format: String,
        scheduleDate: String,
        scheduleTime: String,
        entryFee: Int,
        prizePool: Int,
        perKill: Int,
        totalSlots: Int
    ): Result<String> {
        return try {
            val tourId = "tour_${System.currentTimeMillis() % 100000}"
            val banner = if (gameCategory.contains("BGMI", ignoreCase = true)) "bgmi" else "ff"

            val tourMap = mapOf(
                "id" to tourId,
                "title" to title,
                "gameCategoryName" to gameCategory,
                "map" to map,
                "formatName" to format,
                "scheduleTime" to scheduleTime,
                "scheduleDate" to scheduleDate,
                "entryFee" to entryFee,
                "prizePool" to prizePool,
                "perKill" to perKill,
                "totalSlots" to totalSlots,
                "filledSlots" to 0,
                "bannerType" to banner,
                "serverType" to "Asia / TPP",
                "organizer" to "WorldWar Admin",
                "statusText" to "Open"
            )

            db.collection("tournaments").document(tourId).set(tourMap).await()
            Result.success(tourId)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.CREATE, "tournaments")
            Result.failure(e)
        }
    }

    // Admin: Declare Winner of a match
    suspend fun declareWinner(tournamentId: String, winnerName: String): Result<Unit> {
        return try {
            val tourDocRef = db.collection("tournaments").document(tournamentId)
            tourDocRef.update(
                mapOf(
                    "winnerName" to winnerName,
                    "statusText" to "Completed",
                    "declaredWinnerAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId")
            Result.failure(e)
        }
    }

    // Add money to wallet
    suspend fun addMoney(amount: Int): Result<Unit> {
        if (isGuest()) {
            val current = guestProfileFlow.value
            val newBalance = current.walletBalance + amount
            guestProfileFlow.value = current.copy(walletBalance = newBalance)
            val txId = "tx_guest_dep_${System.currentTimeMillis() % 100000}"
            val newTx = WalletTransaction(
                id = txId,
                userId = "player_guest",
                title = "Deposit (Guest UPI/Card)",
                amount = amount,
                isCredit = true,
                date = "Just now",
                status = "Success"
            )
            guestTransactionsFlow.value = listOf(newTx) + guestTransactionsFlow.value
            return Result.success(Unit)
        }
        val uid = requireUserId()
        return try {
            val userDocRef = db.collection("users").document(uid)
            val snapshot = userDocRef.get().await()
            val currentBalance = (snapshot.getLong("walletBalance") ?: 0L).toInt()
            val newBalance = currentBalance + amount

            userDocRef.update("walletBalance", newBalance).await()

            // Record transaction
            val txId = "tx_dep_${System.currentTimeMillis() % 100000}"
            val txMap = mapOf(
                "id" to txId,
                "userId" to uid,
                "title" to "Deposit (UPI/Card)",
                "amount" to amount,
                "isCredit" to true,
                "date" to "Just now",
                "status" to "Success",
                "timestamp" to FieldValue.serverTimestamp()
            )
            userDocRef.collection("transactions").document(txId).set(txMap).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Add money after successful Razorpay Payment
    suspend fun addMoneyFromRazorpay(amount: Int, paymentId: String): Result<Unit> {
        if (isGuest()) {
            val current = guestProfileFlow.value
            val newBalance = current.walletBalance + amount
            guestProfileFlow.value = current.copy(walletBalance = newBalance)
            val cleanId = paymentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val txId = "tx_rzp_${cleanId.take(40)}"
            val newTx = WalletTransaction(
                id = txId,
                userId = "player_guest",
                title = "Razorpay Deposit (Test Mode)",
                amount = amount,
                isCredit = true,
                date = "Just now",
                status = "Success",
                paymentId = paymentId
            )
            guestTransactionsFlow.value = listOf(newTx) + guestTransactionsFlow.value
            return Result.success(Unit)
        }
        val uid = requireUserId()
        return try {
            val userDocRef = db.collection("users").document(uid)
            val snapshot = userDocRef.get().await()
            val currentBalance = (snapshot.getLong("walletBalance") ?: 0L).toInt()
            val newBalance = currentBalance + amount

            userDocRef.update("walletBalance", newBalance).await()

            // Record transaction in Firestore
            val cleanId = paymentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val txId = "tx_rzp_${cleanId.take(40)}"
            val txMap = mapOf(
                "id" to txId,
                "userId" to uid,
                "title" to "Razorpay Deposit (Test Mode)",
                "amount" to amount,
                "isCredit" to true,
                "date" to "Just now",
                "status" to "Success",
                "paymentId" to paymentId,
                "gateway" to "Razorpay Standard",
                "timestamp" to FieldValue.serverTimestamp()
            )
            userDocRef.collection("transactions").document(txId).set(txMap).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Withdraw money from wallet to UPI or Bank
    suspend fun withdrawMoney(amount: Int, destination: String, method: String = "UPI"): Result<String> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Withdrawal amount must be greater than zero."))
        }
        if (isGuest()) {
            val current = guestProfileFlow.value
            if (current.walletBalance < amount) {
                return Result.failure(IllegalStateException("Insufficient balance! Available: ₹${current.walletBalance}, Requested: ₹$amount"))
            }
            val newBalance = current.walletBalance - amount
            guestProfileFlow.value = current.copy(walletBalance = newBalance)
            val txId = "tx_guest_wd_${System.currentTimeMillis() % 100000}"
            val cleanDest = destination.trim().take(40)
            val newTx = WalletTransaction(
                id = txId,
                userId = "player_guest",
                title = "Withdrawal ($method: $cleanDest)",
                amount = amount,
                isCredit = false,
                date = "Just now",
                status = "Success",
                paymentId = txId
            )
            guestTransactionsFlow.value = listOf(newTx) + guestTransactionsFlow.value
            return Result.success(txId)
        }
        val uid = requireUserId()
        return try {
            val userDocRef = db.collection("users").document(uid)
            val snapshot = userDocRef.get().await()
            val currentBalance = (snapshot.getLong("walletBalance") ?: 0L).toInt()

            if (currentBalance < amount) {
                return Result.failure(IllegalStateException("Insufficient balance! Available: ₹$currentBalance, Requested: ₹$amount"))
            }

            val newBalance = currentBalance - amount
            userDocRef.update("walletBalance", newBalance).await()

            // Record transaction in Firestore
            val cleanDest = destination.trim().take(40)
            val txId = "tx_wd_${System.currentTimeMillis() % 100000}"
            val txMap = mapOf(
                "id" to txId,
                "userId" to uid,
                "title" to "Withdrawal ($method: $cleanDest)",
                "amount" to amount,
                "isCredit" to false,
                "date" to "Just now",
                "status" to "Success",
                "paymentId" to txId,
                "gateway" to "Instant $method Payout",
                "timestamp" to FieldValue.serverTimestamp()
            )
            userDocRef.collection("transactions").document(txId).set(txMap).await()
            Result.success(txId)
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Join Tournament
    suspend fun joinTournament(tournament: Tournament): Result<String> {
        if (isGuest()) {
            val alreadyJoined = guestRegistrationsFlow.value.any { it.tournamentId == tournament.id }
            if (alreadyJoined) {
                return Result.failure(IllegalStateException("You have already joined this tournament!"))
            }
            val current = guestProfileFlow.value
            if (current.walletBalance < tournament.entryFee) {
                return Result.failure(IllegalStateException("Insufficient balance! You need ₹${tournament.entryFee - current.walletBalance} more."))
            }
            val newBalance = current.walletBalance - tournament.entryFee
            val currentMatches = current.matchesPlayed
            guestProfileFlow.value = current.copy(
                walletBalance = newBalance,
                matchesPlayed = currentMatches + 1
            )
            val roomId = "ROOM-${(1000..9999).random()}"
            val roomPassword = "${(100..999).random()}"
            val newReg = UserRegistration(
                registrationId = tournament.id,
                tournamentId = tournament.id,
                tournamentTitle = tournament.title,
                userId = "player_guest",
                entryFee = tournament.entryFee,
                roomId = roomId,
                roomPassword = roomPassword,
                slotNumber = tournament.filledSlots + 1
            )
            guestRegistrationsFlow.value = listOf(newReg) + guestRegistrationsFlow.value
            if (tournament.entryFee > 0) {
                val txId = "tx_fee_${System.currentTimeMillis() % 100000}"
                val newTx = WalletTransaction(
                    id = txId,
                    userId = "player_guest",
                    title = "Entry Fee: ${tournament.title}",
                    amount = tournament.entryFee,
                    isCredit = false,
                    date = "Just now",
                    status = "Success"
                )
                guestTransactionsFlow.value = listOf(newTx) + guestTransactionsFlow.value
            }
            return Result.success("Joined ${tournament.title}! Room ID: $roomId, Pass: $roomPassword")
        }
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val tourDocRef = db.collection("tournaments").document(tournament.id)
        val regDocRef = userDocRef.collection("registrations").document(tournament.id)

        try {
            val existingReg = try { regDocRef.get().await() } catch (_: Throwable) { null }
            if (existingReg != null && existingReg.exists()) {
                return Result.failure(IllegalStateException("You have already joined this tournament!"))
            }

            val userSnap = userDocRef.get().await()
            val currentBalance = (userSnap.getLong("walletBalance") ?: 0L).toInt()

            if (currentBalance < tournament.entryFee) {
                return Result.failure(IllegalStateException("Insufficient balance! You need ₹${tournament.entryFee - currentBalance} more."))
            }

            val tourSnap = tourDocRef.get().await()
            val filledSlots = (tourSnap.getLong("filledSlots") ?: 0L).toInt()
            val totalSlots = (tourSnap.getLong("totalSlots") ?: 48L).toInt()

            if (filledSlots >= totalSlots) {
                return Result.failure(IllegalStateException("Tournament slots are completely full!"))
            }

            // Deduct entry fee
            val newBalance = currentBalance - tournament.entryFee
            val currentMatches = (userSnap.getLong("matchesPlayed") ?: 0L).toInt()
            userDocRef.update(
                mapOf(
                    "walletBalance" to newBalance,
                    "matchesPlayed" to currentMatches + 1
                )
            ).await()

            // Increment slots
            tourDocRef.update("filledSlots", filledSlots + 1).await()

            // Save Registration
            val roomId = "ROOM-${(1000..9999).random()}"
            val roomPassword = "${(100..999).random()}"
            val slotNumber = filledSlots + 1

            val regMap = mapOf(
                "registrationId" to tournament.id,
                "tournamentId" to tournament.id,
                "tournamentTitle" to tournament.title,
                "userId" to uid,
                "entryFee" to tournament.entryFee,
                "roomId" to roomId,
                "roomPassword" to roomPassword,
                "slotNumber" to slotNumber,
                "joinedAt" to FieldValue.serverTimestamp()
            )
            regDocRef.set(regMap).await()

            if (tournament.entryFee > 0) {
                val txId = "tx_fee_${System.currentTimeMillis() % 100000}"
                val txMap = mapOf(
                    "id" to txId,
                    "userId" to uid,
                    "title" to "Entry Fee: ${tournament.title}",
                    "amount" to tournament.entryFee,
                    "isCredit" to false,
                    "date" to "Just now",
                    "status" to "Success",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                try {
                    userDocRef.collection("transactions").document(txId).set(txMap).await()
                } catch (_: Throwable) {}
            }

            return Result.success("Joined ${tournament.title}! Room ID: $roomId, Pass: $roomPassword")
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/registrations/${tournament.id}")
            return Result.failure(e)
        }
    }
}
