package com.example.model

import com.google.firebase.Timestamp

enum class GameCategory(val displayName: String, val shortTag: String) {
    ALL("All Games", "ALL"),
    FREE_FIRE("Free Fire MAX", "FF"),
    BGMI("BGMI", "BGMI"),
    VALORANT("Valorant", "VAL"),
    COD_MOBILE("COD Mobile", "CODM")
}

enum class MatchFormat(val label: String) {
    SOLO("Solo"),
    DUO("Duo"),
    SQUAD("Squad")
}

// Model for Firestore tournament entity
data class Tournament(
    val id: String = "",
    val title: String = "",
    val gameCategoryName: String = "FREE_FIRE",
    val map: String = "Bermuda",
    val formatName: String = "SQUAD",
    val scheduleTime: String = "07:00 PM",
    val scheduleDate: String = "Today",
    val entryFee: Int = 50,
    val prizePool: Int = 2500,
    val perKill: Int = 10,
    val totalSlots: Int = 48,
    val filledSlots: Int = 0,
    val bannerType: String = "ff", // "ff" or "bgmi"
    val serverType: String = "Asia / TPP",
    val organizer: String = "WorldWar Official",
    val statusText: String = "Open",
    val winnerName: String? = null,
    val isJoined: Boolean = false,
    val roomId: String? = null,
    val roomPassword: String? = null
) {
    val gameCategory: GameCategory
        get() = try {
            GameCategory.valueOf(gameCategoryName)
        } catch (_: Exception) {
            GameCategory.FREE_FIRE
        }

    val format: MatchFormat
        get() = try {
            MatchFormat.valueOf(formatName)
        } catch (_: Exception) {
            MatchFormat.SQUAD
        }

    val isFull: Boolean get() = filledSlots >= totalSlots
    val slotsRemaining: Int get() = (totalSlots - filledSlots).coerceAtLeast(0)
    val progress: Float get() = if (totalSlots > 0) filledSlots.toFloat() / totalSlots.toFloat() else 0f
    val isCompleted: Boolean get() = !winnerName.isNullOrBlank() || statusText.equals("Completed", ignoreCase = true)
}

// User profile model stored in /users/{userId}
data class UserProfile(
    val userId: String = "",
    val username: String = "WorldPlayer",
    val ingameId: String = "WW-1001",
    val email: String = "",
    val role: String = "player", // "admin" or "player"
    val level: Int = 5,
    val tier: String = "PRO WARRIOR",
    val matchesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalKills: Int = 0,
    val winRate: String = "0%",
    val walletBalance: Int = 500,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
}

// Subcollection: /users/{userId}/registrations/{registrationId}
data class UserRegistration(
    val registrationId: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val userId: String = "",
    val entryFee: Int = 0,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val slotNumber: Int = 1,
    val joinedAt: Timestamp? = null
)

// Subcollection: /users/{userId}/transactions/{transactionId}
data class WalletTransaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Int = 0,
    val isCredit: Boolean = true,
    val date: String = "Today",
    val status: String = "Success",
    val paymentId: String? = null,
    val gateway: String? = null,
    val timestamp: Timestamp? = null
)

// Razorpay Payment Receipt model
data class PaymentSuccessInfo(
    val paymentId: String,
    val amount: Int,
    val newBalance: Int,
    val gateway: String = "Razorpay Standard (Test Mode)"
)

// Payout / Withdrawal Receipt model
data class WithdrawalSuccessInfo(
    val transactionId: String,
    val amount: Int,
    val destination: String,
    val newBalance: Int,
    val method: String = "Instant UPI Payout"
)


