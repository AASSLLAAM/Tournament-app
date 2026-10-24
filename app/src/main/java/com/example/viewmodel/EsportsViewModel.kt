package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EsportsRepository
import com.example.model.GameCategory
import com.example.model.MatchResult
import com.example.model.PaymentSuccessInfo
import com.example.model.Tournament
import com.example.model.UserProfile
import com.example.model.UserRegistration
import com.example.model.WalletTransaction
import com.example.model.WithdrawalSuccessInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EsportsViewModel(
    private val repository: EsportsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(GameCategory.ALL)
    val selectedCategory: StateFlow<GameCategory> = _selectedCategory.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _isAdminActionLoading = MutableStateFlow(false)
    val isAdminActionLoading: StateFlow<Boolean> = _isAdminActionLoading.asStateFlow()

    private val _isPaymentProcessing = MutableStateFlow(false)
    val isPaymentProcessing: StateFlow<Boolean> = _isPaymentProcessing.asStateFlow()

    private val _paymentSuccessInfo = MutableStateFlow<PaymentSuccessInfo?>(null)
    val paymentSuccessInfo: StateFlow<PaymentSuccessInfo?> = _paymentSuccessInfo.asStateFlow()

    private val _isWithdrawalProcessing = MutableStateFlow(false)
    val isWithdrawalProcessing: StateFlow<Boolean> = _isWithdrawalProcessing.asStateFlow()

    private val _withdrawalSuccessInfo = MutableStateFlow<WithdrawalSuccessInfo?>(null)
    val withdrawalSuccessInfo: StateFlow<WithdrawalSuccessInfo?> = _withdrawalSuccessInfo.asStateFlow()

    // 1. Observe Tournaments from Firestore merged with User Registrations
    val tournaments: StateFlow<List<Tournament>> = combine(
        repository.observeTournaments(),
        repository.observeUserRegistrations()
    ) { allTournaments, userRegistrations ->
        val regMap = userRegistrations.associateBy { it.tournamentId }
        allTournaments.map { tour ->
            val reg = regMap[tour.id]
            if (reg != null) {
                tour.copy(
                    isJoined = true,
                    roomId = reg.roomId ?: tour.roomId,
                    roomPassword = reg.roomPassword ?: tour.roomPassword
                )
            } else {
                tour.copy(isJoined = false)
            }
        }
    }.catch {
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // 2. Observe User Profile from Firestore
    val userProfile: StateFlow<UserProfile> = repository.observeUserProfile()
        .catch { emit(null) }
        .combine(MutableStateFlow(UserProfile())) { remote, default ->
            remote ?: default
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserProfile()
        )

    // 3. Dynamic Wallet balance from Firestore
    val walletBalance: StateFlow<Int> = combine(userProfile) { profileArray ->
        profileArray[0].walletBalance
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 500
    )

    // 4. Observe User Transactions from Firestore
    val transactions: StateFlow<List<WalletTransaction>> = repository.observeTransactions()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedDefaultTournamentsIfEmpty()
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun clearPaymentSuccessInfo() {
        _paymentSuccessInfo.value = null
    }

    fun clearWithdrawalSuccessInfo() {
        _withdrawalSuccessInfo.value = null
    }

    fun setCategory(category: GameCategory) {
        _selectedCategory.value = category
    }

    fun withdrawMoney(
        amount: Int,
        destination: String,
        method: String = "UPI",
        onSuccess: () -> Unit = {}
    ) {
        if (amount <= 0) {
            _snackbarMessage.value = "Please enter a valid withdrawal amount."
            return
        }
        if (destination.isBlank()) {
            _snackbarMessage.value = "Please enter your UPI ID or Bank account details."
            return
        }
        if (amount > walletBalance.value) {
            _snackbarMessage.value = "Insufficient balance! Available: ₹${walletBalance.value}"
            return
        }

        _isWithdrawalProcessing.value = true
        viewModelScope.launch {
            try {
                val result = repository.withdrawMoney(amount, destination, method)
                if (result.isSuccess) {
                    val txId = result.getOrThrow()
                    val newBal = (walletBalance.value - amount).coerceAtLeast(0)
                    _withdrawalSuccessInfo.value = WithdrawalSuccessInfo(
                        transactionId = txId,
                        amount = amount,
                        destination = destination.trim(),
                        newBalance = newBal,
                        method = if (method == "UPI") "Instant UPI Payout" else "Bank Transfer (IMPS)"
                    )
                    _snackbarMessage.value = "Withdrawal of ₹$amount to $destination successful!"
                    onSuccess()
                } else {
                    _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Withdrawal request failed."
                }
            } finally {
                _isWithdrawalProcessing.value = false
            }
        }
    }

    fun addMoney(amount: Int) {
        if (amount <= 0) return
        viewModelScope.launch {
            val result = repository.addMoney(amount)
            if (result.isSuccess) {
                _snackbarMessage.value = "Successfully added ₹$amount to Wallet!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to add money"
            }
        }
    }

    fun handleRazorpayPaymentSuccess(paymentId: String, amount: Int) {
        if (amount <= 0) return
        _isPaymentProcessing.value = true
        viewModelScope.launch {
            try {
                val result = repository.addMoneyFromRazorpay(amount, paymentId)
                if (result.isSuccess) {
                    val updatedBal = walletBalance.value + amount
                    _paymentSuccessInfo.value = PaymentSuccessInfo(
                        paymentId = paymentId,
                        amount = amount,
                        newBalance = updatedBal,
                        gateway = "Razorpay Standard (Test Mode)"
                    )
                    _snackbarMessage.value = "Payment Successful! ₹$amount added to Wallet (ID: $paymentId)"
                } else {
                    _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to update wallet after payment"
                }
            } finally {
                _isPaymentProcessing.value = false
            }
        }
    }

    fun handleRazorpayPaymentError(code: Int, message: String) {
        _isPaymentProcessing.value = false
        _snackbarMessage.value = "Razorpay Payment Failed: $message (Error code: $code)"
    }

    suspend fun joinTournamentAsync(tournament: Tournament): Result<String> {
        _isJoining.value = true
        return try {
            val result = repository.joinTournament(tournament)
            if (result.isSuccess) {
                _snackbarMessage.value = result.getOrNull() ?: "Joined ${tournament.title}!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to join tournament"
            }
            result
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to join tournament"
            _snackbarMessage.value = errorMsg
            Result.failure(e)
        } finally {
            _isJoining.value = false
        }
    }

    fun joinTournament(
        tournament: Tournament,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isJoining.value) return
        _isJoining.value = true

        viewModelScope.launch {
            try {
                val result = repository.joinTournament(tournament)
                if (result.isSuccess) {
                    val msg = result.getOrNull() ?: "Joined ${tournament.title}!"
                    _snackbarMessage.value = msg
                    onSuccess(msg)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to join tournament"
                    _snackbarMessage.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to join tournament"
                _snackbarMessage.value = errorMsg
                onError(errorMsg)
            } finally {
                _isJoining.value = false
            }
        }
    }

    suspend fun createTournamentAsync(
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
        _isAdminActionLoading.value = true
        return try {
            val result = repository.createTournament(
                title = title,
                gameCategory = gameCategory,
                map = map,
                format = format,
                scheduleDate = scheduleDate,
                scheduleTime = scheduleTime,
                entryFee = entryFee,
                prizePool = prizePool,
                perKill = perKill,
                totalSlots = totalSlots
            )
            if (result.isSuccess) {
                _snackbarMessage.value = "Tournament '$title' published to Firestore successfully!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to create tournament"
            }
            result
        } catch (e: Throwable) {
            val errorMsg = e.message ?: "Failed to create tournament"
            _snackbarMessage.value = errorMsg
            Result.failure(e)
        } finally {
            _isAdminActionLoading.value = false
        }
    }

    // Admin: Create new tournament
    fun createTournament(
        title: String,
        gameCategory: String,
        map: String,
        format: String,
        scheduleDate: String,
        scheduleTime: String,
        entryFee: Int,
        prizePool: Int,
        perKill: Int,
        totalSlots: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _isAdminActionLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.createTournament(
                    title = title,
                    gameCategory = gameCategory,
                    map = map,
                    format = format,
                    scheduleDate = scheduleDate,
                    scheduleTime = scheduleTime,
                    entryFee = entryFee,
                    prizePool = prizePool,
                    perKill = perKill,
                    totalSlots = totalSlots
                )
                if (result.isSuccess) {
                    _snackbarMessage.value = "Tournament '$title' published to Firestore successfully!"
                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create tournament"
                    _snackbarMessage.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Throwable) {
                val errorMsg = e.message ?: "Failed to create tournament"
                _snackbarMessage.value = errorMsg
                onError(errorMsg)
            } finally {
                _isAdminActionLoading.value = false
            }
        }
    }

    // Admin: Declare winner
    fun declareWinner(
        tournamentId: String,
        winnerName: String,
        onSuccess: () -> Unit = {}
    ) {
        if (winnerName.isBlank()) return
        _isAdminActionLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.declareWinner(tournamentId, winnerName.trim())
                if (result.isSuccess) {
                    _snackbarMessage.value = "Winner '$winnerName' declared for tournament!"
                    onSuccess()
                } else {
                    _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to declare winner"
                }
            } finally {
                _isAdminActionLoading.value = false
            }
        }
    }

    // Test helper: Switch admin role
    fun setAdminRole(enable: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleAdminRole(enable)
            if (result.isSuccess) {
                _snackbarMessage.value = if (enable) "Admin access granted!" else "Switched to standard player mode"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to update role"
            }
        }
    }

    // Observe Match Leaderboard for a tournament
    fun getMatchResults(tournamentId: String): Flow<List<MatchResult>> {
        return repository.observeMatchResults(tournamentId)
    }

    // Admin: Save / Update Match Result in leaderboard
    fun saveMatchResult(
        tournamentId: String,
        resultId: String = "",
        rank: Int,
        teamName: String,
        playerName: String,
        kills: Int,
        points: Int,
        prizeWon: Int = 0,
        onSuccess: () -> Unit = {}
    ) {
        if (teamName.isBlank() && playerName.isBlank()) {
            _snackbarMessage.value = "Please enter team or player name."
            return
        }
        _isAdminActionLoading.value = true
        viewModelScope.launch {
            try {
                val matchResult = MatchResult(
                    id = resultId,
                    tournamentId = tournamentId,
                    rank = rank,
                    teamName = teamName.trim(),
                    playerName = playerName.trim(),
                    kills = kills,
                    points = points,
                    prizeWon = prizeWon
                )
                val res = repository.saveMatchResult(tournamentId, matchResult)
                if (res.isSuccess) {
                    _snackbarMessage.value = "Saved stats for rank #$rank ($teamName)!"
                    onSuccess()
                } else {
                    _snackbarMessage.value = res.exceptionOrNull()?.message ?: "Failed to save match result"
                }
            } finally {
                _isAdminActionLoading.value = false
            }
        }
    }
}
