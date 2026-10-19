package com.example.model

// Model for Firestore subcollection: /tournaments/{tournamentId}/results/{resultId}
data class MatchResult(
    val id: String = "",
    val tournamentId: String = "",
    val rank: Int = 1,
    val teamName: String = "",
    val playerName: String = "",
    val kills: Int = 0,
    val points: Int = 0,
    val prizeWon: Int = 0
) {
    val rankBadge: String
        get() = when (rank) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${rank}th"
        }
}
