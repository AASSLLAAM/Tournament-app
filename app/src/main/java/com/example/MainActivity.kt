package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.EsportsRepository
import com.example.ui.navigation.NavScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.ArenaWarTheme
import com.example.ui.theme.CardNavy
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EsportsViewModel
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var activeViewModel: EsportsViewModel? = null
    private var pendingPaymentAmount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Checkout.preload(applicationContext)
            } catch (_: Throwable) {
            }
        }
        enableEdgeToEdge()
        setContent {
            ArenaWarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    AppRootContent(
                        onInitiatePayment = { amount ->
                            startRazorpayCheckout(amount)
                        },
                        onViewModelBound = { vm ->
                            activeViewModel = vm
                        }
                    )
                }
            }
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

            val currentUser = Firebase.auth.currentUser
            val options = JSONObject().apply {
                put("name", "ArenaWar Esports")
                put("description", "Wallet Recharge: ₹$amount")
                put("currency", "INR")
                put("amount", amount * 100) // in paise (e.g. 100 INR = 10000 paise)
                put("theme.color", "#00E5FF")

                val prefill = JSONObject().apply {
                    put("email", currentUser?.email ?: "gamer@arenawar.com")
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
    onViewModelBound: (EsportsViewModel) -> Unit = {}
) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Auth Gated Sign-In Screen
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Authenticated Session: Inject Database ID and ViewModel keyed by authenticated user ID
        val databaseId = context.getString(R.string.firestore_database_id)
        val db = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
        val repository = remember(db) { EsportsRepository(db) }

        val viewModel: EsportsViewModel = viewModel(
            key = user.uid,
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

        // Ensure user document exists with initial balance ₹500 in Firestore
        LaunchedEffect(user.uid) {
            scope.launch {
                repository.ensureUserProfileExists(user.email, user.displayName)
            }
        }

        MainAppContainer(
            viewModel = viewModel,
            onInitiatePayment = onInitiatePayment,
            onSignOut = {
                currentUser = null
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
    val snackbarHostState = remember { SnackbarHostState() }

    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Handle back button when on secondary screens
    BackHandler(enabled = currentScreen != NavScreen.HOME) {
        currentScreen = NavScreen.HOME
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = CardNavy,
                tonalElevation = 8.dp
            ) {
                NavScreen.entries.forEach { screen ->
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
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
    ) { innerPadding ->
        when (currentScreen) {
            NavScreen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    tournaments = tournaments,
                    walletBalance = walletBalance,
                    selectedCategory = selectedCategory,
                    onNavigateToWallet = { currentScreen = NavScreen.WALLET },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.WALLET -> {
                WalletScreen(
                    viewModel = viewModel,
                    walletBalance = walletBalance,
                    transactions = transactions,
                    onInitiateRazorpayPayment = onInitiatePayment,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.MY_MATCHES -> {
                MyMatchesScreen(
                    tournaments = tournaments,
                    onExploreTournaments = { currentScreen = NavScreen.HOME },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.PROFILE -> {
                ProfileScreen(
                    userProfile = userProfile,
                    viewModel = viewModel,
                    onNavigateToWallet = { currentScreen = NavScreen.WALLET },
                    onNavigateToAdmin = { currentScreen = NavScreen.ADMIN },
                    onSignOut = onSignOut,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.ADMIN -> {
                AdminScreen(
                    viewModel = viewModel,
                    userProfile = userProfile,
                    tournaments = tournaments,
                    onBack = { currentScreen = NavScreen.HOME },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
