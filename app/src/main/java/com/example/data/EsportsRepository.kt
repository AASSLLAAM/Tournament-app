package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Tournament
import com.example.model.UserProfile
import com.example.model.UserRegistration
import com.example.model.WalletTransaction
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EsportsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // 1. Observe all tournaments from Firestore
    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val path = "tournaments"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Tournament::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // 2. Observe user's joined tournament registrations
    fun observeUserRegistrations(): Flow<List<UserRegistration>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/registrations"
        emitAll(
            db.collection("users").document(uid).collection("registrations")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserRegistration::class.java)?.copy(registrationId = doc.id)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // 3. Observe user profile (including wallet balance and admin role)
    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        val profile = snapshot.toObject(UserProfile::class.java)
                        val balance = (snapshot.getLong("walletBalance") ?: 500L).toInt()
                        val roleStr = snapshot.getString("role") ?: "player"
                        profile?.copy(walletBalance = balance, role = roleStr)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    // 4. Observe user transactions
    fun observeTransactions(): Flow<List<WalletTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
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
                        organizer = "ArenaWar Official",
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
                        organizer = "ArenaWar Official",
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
                        organizer = "ArenaWar Official",
                        statusText = "Open"
                    )
                )

                for (tour in seedData) {
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
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments")
        }
    }

    // Ensure user profile document exists with role
    suspend fun ensureUserProfileExists(userEmail: String?, displayName: String?): UserProfile {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val snapshot = userDocRef.get().await()

        // Check if user is registered in admins collection or matches bootstrap email
        val adminDoc = db.collection("admins").document(uid).get().await()
        val isUserAdmin = adminDoc.exists() ||
                (userEmail != null && userEmail.equals("aslamhussain096@gmail.com", ignoreCase = true))

        val role = if (isUserAdmin) "admin" else "player"

        return if (!snapshot.exists()) {
            val initialBalance = 500
            val username = displayName?.ifBlank { null }
                ?: userEmail?.substringBefore("@")?.ifBlank { null }
                ?: "Player_${uid.take(4)}"

            val newProfile = UserProfile(
                userId = uid,
                username = username,
                ingameId = "AW-${(1000..9999).random()}",
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
            userDocRef.set(profileMap).await()

            // If user is admin, guarantee presence in /admins collection
            if (isUserAdmin) {
                db.collection("admins").document(uid).set(mapOf("uid" to uid, "email" to (userEmail ?: ""))).await()
            }

            // Record initial welcome bonus
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

            newProfile
        } else {
            val balance = (snapshot.getLong("walletBalance") ?: 500L).toInt()
            val existingRole = snapshot.getString("role") ?: role

            // Update role if user is recognized as admin
            if (isUserAdmin && existingRole != "admin") {
                userDocRef.update("role", "admin").await()
                db.collection("admins").document(uid).set(mapOf("uid" to uid, "email" to (userEmail ?: ""))).await()
            }

            snapshot.toObject(UserProfile::class.java)?.copy(
                walletBalance = balance,
                role = if (isUserAdmin) "admin" else existingRole
            ) ?: UserProfile(userId = uid, walletBalance = balance, role = role)
        }
    }

    // Toggle admin role for testing convenience
    suspend fun toggleAdminRole(enable: Boolean): Result<Unit> {
        val uid = requireUserId()
        return try {
            val userDocRef = db.collection("users").document(uid)
            val adminDocRef = db.collection("admins").document(uid)
            val newRole = if (enable) "admin" else "player"

            userDocRef.update("role", newRole).await()
            if (enable) {
                adminDocRef.set(mapOf("uid" to uid, "email" to (auth.currentUser?.email ?: ""))).await()
            } else {
                adminDocRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
                "organizer" to "ArenaWar Admin",
                "statusText" to "Open"
            )

            db.collection("tournaments").document(tourId).set(tourMap).await()
            Result.success(tourId)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId")
            Result.failure(e)
        }
    }

    // Add money to wallet
    suspend fun addMoney(amount: Int): Result<Unit> {
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
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Add money after successful Razorpay Payment
    suspend fun addMoneyFromRazorpay(amount: Int, paymentId: String): Result<Unit> {
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
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Withdraw money from wallet to UPI or Bank
    suspend fun withdrawMoney(amount: Int, destination: String, method: String = "UPI"): Result<String> {
        val uid = requireUserId()
        return try {
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("Withdrawal amount must be greater than zero."))
            }
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
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // Join Tournament
    suspend fun joinTournament(tournament: Tournament): Result<String> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val tourDocRef = db.collection("tournaments").document(tournament.id)
        val regDocRef = userDocRef.collection("registrations").document(tournament.id)

        try {
            val existingReg = regDocRef.get().await()
            if (existingReg.exists()) {
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
                userDocRef.collection("transactions").document(txId).set(txMap).await()
            }

            return Result.success("Joined ${tournament.title}! Room ID: $roomId, Pass: $roomPassword")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/registrations/${tournament.id}")
            return Result.failure(e)
        }
    }
}
