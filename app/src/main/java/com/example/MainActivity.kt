package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.EsportsRepository
import com.example.model.Tournament
import com.example.ui.navigation.NavScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ErrorBoundaryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MatchDetailsScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CardNavy
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorldWarTheme
import com.example.viewmodel.EsportsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var activeViewModel: EsportsViewModel? = null
    private var pendingPaymentAmount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Checkout.preload(applicationContext)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {}

        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}

        setContent {
            WorldWarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    var startupFatalError by remember { mutableStateOf<String?>(null) }

                    val fatalError = startupFatalError
                    if (fatalError != null) {
                        ErrorBoundaryScreen(
                            errorMessage = fatalError,
                            onRetry = {
                                startupFatalError = null
                            },
                            onContinueOffline = {
                                startupFatalError = null
                            }
                        )
                    } else {
                        AppRootContent(
                            onInitiatePayment = { amount ->
                                startRazorpayCheckout(amount)
                            },
                            onViewModelBound = { vm ->
                                activeViewModel = vm
                            },
                            onFatalError = { errorMsg ->
                                startupFatalError = errorMsg
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        validateFirebaseSession()
    }

    private fun validateFirebaseSession() {
        lifecycleScope.launch {
            val auth = try {
                FirebaseAuth.getInstance()
            } catch (_: Throwable) {
                null
            } ?: return@launch

            val currentUser = try {
                auth.currentUser
            } catch (_: Throwable) {
                null
            }

            if (currentUser == null) {
                clearLocalAuthPreferences()
                return@launch
            }

            try {
                currentUser.reload().await()
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.w("MainActivity", "Firebase user no longer valid; signing out.", e)
                try {
                    auth.signOut()
                } catch (_: Throwable) {}
                clearLocalAuthPreferences()
            } catch (_: Throwable) {
                // Ignore transient reload failures. If the session is actually invalid, the next
                // auth state listener update will redirect the user back to the login screen.
            }
        }
    }

    private fun clearLocalAuthPreferences() {
        val prefsToClear = listOf(
            getSharedPreferences("worldwar_auth_session", MODE_PRIVATE),
            getSharedPreferences("worldwar_session_prefs", MODE_PRIVATE),
            getSharedPreferences("firebase_auth_state", MODE_PRIVATE)
        )

        prefsToClear.forEach { prefs ->
            try {
                prefs.edit().clear().apply()
            } catch (_: Throwable) {}
        }
    }

    private fun startRazorpayCheckout(amount: Int) {
        pendingPaymentAmount = amount
        try {
            val checkout = Checkout()
            val apiKey = try {
                val key = BuildConfig.RAZORPAY_KEY_ID
                if (key.isNullOrBlank()) "rzp_test_1DP5mmOlF5G5ag" else key
            } catch (_: Throwable) {
                "rzp_test_1DP5mmOlF5G5ag"
            }
            checkout.setKeyID(apiKey)

            val currentUser = try {
                Firebase.auth.currentUser
            } catch (_: Throwable) {
                null
            }
            val options = JSONObject().apply {
                put("name", "WorldWar Esports")
                put("description", "Wallet Recharge: ₹$amount")
                put("currency", "INR")
                put("amount", amount * 100)
                put("theme.color", "#00E5FF")

                val prefill = JSONObject().apply {
                    put("email", currentUser?.email ?: "gamer@worldwar.com")
                    put("contact", "9999999999")
                }
                put("prefill", prefill)

                val retry = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retry)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            activeViewModel?.handleRazorpayPaymentError(-1, e.message ?: "Could not open Razorpay Checkout")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: "pay_rzp_${System.currentTimeMillis() % 1000000}"
        activeViewModel?.handleRazorpayPaymentSuccess(paymentId, pendingPaymentAmount)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val errorMsg = response ?: "Payment cancelled or declined"
        activeViewModel?.handleRazorpayPaymentError(code, errorMsg)
    }
}

@Composable
fun AppRootContent(
    onInitiatePayment: (amount: Int) -> Unit = {},
    onViewModelBound: (EsportsViewModel) -> Unit = {},
    onFatalError: (String) -> Unit = {}
) {
    var isGuestMode by remember { mutableStateOf(false) }
    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(
            try {
                Firebase.auth.currentUser
            } catch (e: Throwable) {
                Log.w("MainActivity", "Firebase.auth currentUser check failed: ${e.message}")
                null
            }
        )
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val authInstance = try {
            Firebase.auth
        } catch (e: Throwable) {
            Log.w("MainActivity", "Firebase.auth instance warning: ${e.message}")
            null
        }

        val listener = if (authInstance != null) {
            try {
                val l = FirebaseAuth.AuthStateListener { auth ->
                    try {
                        currentUser = auth.currentUser
                    } catch (_: Throwable) {}
                }
                authInstance.addAuthStateListener(l)
                l
            } catch (_: Throwable) {
                null
            }
        } else null

        onDispose {
            if (authInstance != null && listener != null) {
                try {
                    authInstance.removeAuthStateListener(listener)
                } catch (_: Throwable) {}
            }
        }
    }

    val user = currentUser
    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                try {
                    currentUser = Firebase.auth.currentUser
                } catch (_: Throwable) {
                    isGuestMode = true
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        )
    } else {
        val repository = remember {
            try {
                EsportsRepository(context)
            } catch (e: Throwable) {
                Log.e("MainActivity", "EsportsRepository creation failed: ${e.message}", e)
                null
            }
        }

        if (repository == null) {
            ErrorBoundaryScreen(
                errorMessage = "Database initialization failed. Please check network connectivity and retry.",
                onRetry = {
                    currentUser = null
                    isGuestMode = false
                },
                onContinueOffline = {
                    isGuestMode = true
                }
            )
            return
        }

        val sessionKey = user?.uid ?: "guest_session"
        val viewModel: EsportsViewModel = viewModel(
            key = sessionKey,
            factory = viewModelFactory {
                initializer {
                    EsportsViewModel(repository)
                }
            }
        )

        DisposableEffect(viewModel) {
            onViewModelBound(viewModel)
            onDispose {}
        }

        LaunchedEffect(user?.uid) {
            if (user != null) {
                scope.launch {
                    try {
                        repository.ensureUserProfileExists(user.email, user.displayName)
                    } catch (e: Throwable) {
                        Log.w("MainActivity", "ensureUserProfileExists warning: ${e.message}")
                    }
                }
            }
        }

        MainAppContainer(
            viewModel = viewModel,
            onInitiatePayment = onInitiatePayment,
            onSignOut = {
                try {
                    Firebase.auth.signOut()
                } catch (_: Throwable) {}
                currentUser = null
                isGuestMode = false
            }
        )
    }
}

@Composable
fun MainAppContainer(
    viewModel: EsportsViewModel,
    onInitiatePayment: (amount: Int) -> Unit = {},
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(NavScreen.HOME) }
    var previousScreen by remember { mutableStateOf(NavScreen.HOME) }
    var selectedTournamentForDetails by remember { mutableStateOf<Tournament?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var isAdminRoleLoading by remember { mutableStateOf(true) }
    var isAdminVisible by remember { mutableStateOf(false) }

    val currentUserId = try {
        Firebase.auth.currentUser?.uid
    } catch (_: Throwable) {
        null
    }

    LaunchedEffect(currentUserId) {
        isAdminRoleLoading = true
        isAdminVisible = false

        val uid = currentUserId ?: run {
            isAdminRoleLoading = false
            return@LaunchedEffect
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore
                .collection("users")
                .document(uid)
                .get()
                .await()

            val role = doc.getString("role")?.trim()?.lowercase() ?: "user"
            isAdminVisible = role == "admin"
        } catch (_: Throwable) {
            isAdminVisible = false
        } finally {
            isAdminRoleLoading = false
        }
    }

    LaunchedEffect(currentScreen, isAdminVisible, userProfile.isAdmin) {
        if (currentScreen == NavScreen.ADMIN && !isAdminVisible && !userProfile.isAdmin) {
            currentScreen = NavScreen.HOME
        }
    }

    BackHandler(enabled = currentScreen != NavScreen.HOME) {
        if (currentScreen == NavScreen.MATCH_DETAILS) {
            currentScreen = previousScreen
        } else {
            currentScreen = NavScreen.HOME
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkNavy,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != NavScreen.MATCH_DETAILS) {
                val visibleScreens = remember(isAdminRoleLoading, isAdminVisible, userProfile.isAdmin) {
                    if (isAdminRoleLoading) {
                        NavScreen.entries.filter { screen ->
                            screen.isBottomTab && screen != NavScreen.ADMIN
                        }
                    } else {
                        NavScreen.entries.filter { screen ->
                            screen.isBottomTab &&
                                (screen != NavScreen.ADMIN || isAdminVisible || userProfile.isAdmin)
                        }
                    }
                }

                Surface(
                    color = CardNavy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar"),
                        containerColor = CardNavy,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        tonalElevation = 8.dp
                    ) {
                        visibleScreens.forEach { screen ->
                            val isSelected = currentScreen == screen
                            val activeColor = if (screen == NavScreen.ADMIN) GoldAccent else NeonCyan

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                modifier = Modifier.testTag(screen.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = activeColor,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = activeColor
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            NavScreen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    tournaments = tournaments,
                    walletBalance = walletBalance,
                    selectedCategory = selectedCategory,
                    onNavigateToWallet = { currentScreen = NavScreen.WALLET },
                    onNavigateToMatchDetails = { tournament ->
                        previousScreen = currentScreen
                        selectedTournamentForDetails = tournament
                        currentScreen = NavScreen.MATCH_DETAILS
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            NavScreen.WALLET -> {
                WalletScreen(
                    viewModel = viewModel,
                    walletBalance = walletBalance,
                    transactions = transactions,
                    onInitiateRazorpayPayment = onInitiatePayment,
                    onBack = { currentScreen = NavScreen.HOME },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            NavScreen.MY_MATCHES -> {
                MyMatchesScreen(
                    viewModel = viewModel,
                    tournaments = tournaments,
                    onNavigateToMatchDetails = { tournament ->
                        previousScreen = currentScreen
                        selectedTournamentForDetails = tournament
                        currentScreen = NavScreen.MATCH_DETAILS
                    },
                    onExploreTournaments = { currentScreen = NavScreen.HOME },
                    onBack = { currentScreen = NavScreen.HOME },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            NavScreen.PROFILE -> {
                ProfileScreen(
                    userProfile = userProfile,
                    viewModel = viewModel,
                    onNavigateToWallet = { currentScreen = NavScreen.WALLET },
                    onNavigateToAdmin = { currentScreen = NavScreen.ADMIN },
                    onSignOut = onSignOut,
                    onBack = { currentScreen = NavScreen.HOME },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            NavScreen.ADMIN -> {
                AdminScreen(
                    viewModel = viewModel,
                    userProfile = userProfile,
                    tournaments = tournaments,
                    onBack = { currentScreen = NavScreen.HOME },
                    onNavigateToMatchDetails = { tournament ->
                        previousScreen = currentScreen
                        selectedTournamentForDetails = tournament
                        currentScreen = NavScreen.MATCH_DETAILS
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            NavScreen.MATCH_DETAILS -> {
                val currentTour = selectedTournamentForDetails ?: tournaments.firstOrNull()
                if (currentTour != null) {
                    MatchDetailsScreen(
                        tournament = currentTour,
                        viewModel = viewModel,
                        userProfile = userProfile,
                        onBack = { currentScreen = previousScreen },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                } else {
                    currentScreen = NavScreen.HOME
                }
            }
        }
    }
}
