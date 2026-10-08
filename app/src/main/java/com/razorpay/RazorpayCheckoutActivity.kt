package com.razorpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class RazorpayCheckoutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val key = intent.getStringExtra("key") ?: Checkout.RZP_TEST_KEY_DEFAULT
        val amountPaise = intent.getIntExtra("amount", 10000)
        val amountInRupees = amountPaise / 100
        val merchantName = intent.getStringExtra("name") ?: "WorldWar Esports"
        val description = intent.getStringExtra("description") ?: "Wallet Deposit"
        val userEmail = intent.getStringExtra("email") ?: ""
        val userContact = intent.getStringExtra("contact") ?: ""

        setContent {
            RazorpayCheckoutScreen(
                merchantName = merchantName,
                description = description,
                amountInRupees = amountInRupees,
                keyId = key,
                userEmail = userEmail,
                userContact = userContact,
                onSuccess = { paymentId ->
                    val data = PaymentData(
                        paymentId = paymentId,
                        orderId = "order_${System.currentTimeMillis() % 100000}",
                        userContact = userContact,
                        userEmail = userEmail,
                        data = JSONObject().apply {
                            put("razorpay_payment_id", paymentId)
                        }
                    )
                    RazorpayCheckoutBridge.notifySuccess(paymentId, data)
                    finish()
                },
                onCancel = {
                    RazorpayCheckoutBridge.notifyError(
                        Checkout.PAYMENT_CANCELED,
                        "Payment cancelled by user",
                        null
                    )
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RazorpayCheckoutScreen(
    merchantName: String,
    description: String,
    amountInRupees: Int,
    keyId: String,
    userEmail: String,
    userContact: String,
    onSuccess: (paymentId: String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var upiId by remember { mutableStateOf("success@razorpay") }
    var cardNumber by remember { mutableStateOf("4111 1111 1111 1111") }
    var expiryDate by remember { mutableStateOf("12/28") }
    var cvv by remember { mutableStateOf("123") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStatusText by remember { mutableStateOf("Authorizing payment with Razorpay...") }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = !isProcessing) {
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0C2340)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Razorpay",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Text(
                                text = "TEST MODE",
                                color = Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !isProcessing) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A111E)
                )
            )
        },
        containerColor = Color(0xFF070B14)
    ) { innerPadding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = processingStatusText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Secured by 256-bit encryption",
                            color = Color(0xFF8FA1B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Merchant Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726)),
                    border = BorderStroke(1.dp, Color(0xFF1B2B44))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = merchantName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = description,
                                color = Color(0xFF8FA1B8),
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹$amountInRupees",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "INR",
                                color = Color(0xFF8FA1B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Mode Warning Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2818),
                    border = BorderStroke(1.dp, Color(0xFF388E3C))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Test Mode Active • No real amount will be deducted. All test credentials succeed.",
                            color = Color(0xFFC8E6C9),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SELECT PAYMENT METHOD",
                    color = Color(0xFF8FA1B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method Tabs
                val paymentMethods = listOf(
                    Triple("UPI", Icons.Default.QrCode, "GPay, PhonePe, Paytm"),
                    Triple("Cards", Icons.Default.CreditCard, "Visa, MasterCard, RuPay"),
                    Triple("Netbanking", Icons.Default.AccountBalance, "SBI, HDFC, ICICI, Axis"),
                    Triple("Wallets", Icons.Default.AccountBalanceWallet, "Paytm, Mobikwik")
                )

                paymentMethods.forEach { (method, icon, subtitle) ->
                    val isSelected = selectedMethod == method
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedMethod = method },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF14243B) else Color(0xFF0E1726)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF1B2B44)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8FA1B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = method,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        color = Color(0xFF8FA1B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Input for Selected Method
                when (selectedMethod) {
                    "UPI" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726)),
                            border = BorderStroke(1.dp, Color(0xFF1B2B44))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Virtual Payment Address (VPA)", color = Color(0xFF8FA1B8), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = upiId,
                                    onValueChange = { upiId = it },
                                    label = { Text("UPI ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF1B2B44),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF101B2E),
                                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { upiId = "success@razorpay" }
                                    ) {
                                        Text(
                                            text = "Use Test UPI (success@razorpay)",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Cards" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726)),
                            border = BorderStroke(1.dp, Color(0xFF1B2B44))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it },
                                    label = { Text("Card Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF1B2B44),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = expiryDate,
                                        onValueChange = { expiryDate = it },
                                        label = { Text("MM/YY") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF00E5FF),
                                            unfocusedBorderColor = Color(0xFF1B2B44),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    OutlinedTextField(
                                        value = cvv,
                                        onValueChange = { cvv = it },
                                        label = { Text("CVV") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF00E5FF),
                                            unfocusedBorderColor = Color(0xFF1B2B44),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726)),
                            border = BorderStroke(1.dp, Color(0xFF1B2B44))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Simulated ${selectedMethod} Gateway",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Ready to authorize instant deposit in Test Mode.",
                                    color = Color(0xFF8FA1B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pay Button
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            processingStatusText = "Connecting to Razorpay PG..."
                            delay(700)
                            processingStatusText = "Verifying test payment..."
                            delay(600)
                            val cleanRand = (100000..999999).random()
                            val paymentId = "pay_rzp_test_$cleanRand"
                            onSuccess(paymentId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("razorpay_pay_now_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay ₹$amountInRupees (Test Mode)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8FA1B8)),
                    border = BorderStroke(1.dp, Color(0xFF1B2B44))
                ) {
                    Text("Cancel Payment")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Razorpay Standard Checkout • PCI DSS Certified",
                        color = Color(0xFF8FA1B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
