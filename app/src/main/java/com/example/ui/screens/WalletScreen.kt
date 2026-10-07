package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WalletTransaction
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

@Composable
fun WalletScreen(
    viewModel: EsportsViewModel,
    walletBalance: Int,
    transactions: List<WalletTransaction>,
    onInitiateRazorpayPayment: (amount: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var prefillAmount by remember { mutableStateOf("100") }

    val paymentSuccessInfo by viewModel.paymentSuccessInfo.collectAsStateWithLifecycle()
    val isPaymentProcessing by viewModel.isPaymentProcessing.collectAsStateWithLifecycle()
    val withdrawalSuccessInfo by viewModel.withdrawalSuccessInfo.collectAsStateWithLifecycle()
    val isWithdrawalProcessing by viewModel.isWithdrawalProcessing.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wallet_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wallet & Payments",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Razorpay Gateway (Test Mode) • Firestore Synced",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardNavyElevated,
                    border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Mode",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Main Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderCyan)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00455B),
                                    CardNavyElevated
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BALANCE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verified Wallet", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current Balance Highlight
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹$walletBalance",
                                color = TextPrimary,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INR Coins",
                                color = GoldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick stats breakdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardNavy.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Deposit Balance", color = TextMuted, fontSize = 11.sp)
                                Text("₹$walletBalance", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Winnings", color = TextMuted, fontSize = 11.sp)
                                Text("₹0", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Gateway", color = TextMuted, fontSize = 11.sp)
                                Text("Razorpay", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Dual Action Buttons: Add Money & Withdraw
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Add Money Button
                            Button(
                                onClick = {
                                    prefillAmount = "100"
                                    showAddMoneyDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("add_money_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                enabled = !isPaymentProcessing
                            ) {
                                if (isPaymentProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Processing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add Money",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // 2. Withdraw Button
                            Button(
                                onClick = {
                                    showWithdrawDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("withdraw_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = Color.Black
                                ),
                                enabled = !isWithdrawalProcessing
                            ) {
                                if (isWithdrawalProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sending...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Withdraw",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Withdraw",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Top Up presets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderMuted)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Recharge Presets",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to pay",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 200, 500).forEach { amt ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        prefillAmount = amt.toString()
                                        showAddMoneyDialog = true
                                    }
                                    .testTag("quick_topup_$amt"),
                                shape = RoundedCornerShape(10.dp),
                                color = CardNavyElevated,
                                border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("+₹$amt", color = NeonCyan, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recent Transactions",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${transactions.size} records",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Transaction list items
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavy)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No transactions yet. Add money to get started!", color = TextMuted)
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    border = BorderStroke(1.dp, BorderMuted)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tx.isCredit) EmeraldGreen.copy(alpha = 0.15f)
                                        else CrimsonRed.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (tx.isCredit) EmeraldGreen else CrimsonRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tx.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${tx.date} • ${tx.status}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    if (tx.paymentId != null) {
                                        Text(
                                            text = " • ${tx.paymentId.take(12)}...",
                                            color = NeonCyan,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = if (tx.isCredit) "+₹${tx.amount}" else "-₹${tx.amount}",
                            color = if (tx.isCredit) EmeraldGreen else CrimsonRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Money Dialog with Razorpay Test Mode & Simulator
    if (showAddMoneyDialog) {
        var inputAmount by remember { mutableStateOf(prefillAmount) }

        AlertDialog(
            onDismissRequest = { showAddMoneyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Money via Razorpay",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Deposit money directly into your tournament wallet using Razorpay Standard SDK.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) inputAmount = it },
                        label = { Text("Deposit Amount (₹)") },
                        leadingIcon = {
                            Text("₹", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_money_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 250, 500, 1000).forEach { chipAmt ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { inputAmount = chipAmt.toString() },
                                shape = RoundedCornerShape(6.dp),
                                color = if (inputAmount == chipAmt.toString()) NeonCyan.copy(alpha = 0.2f) else CardNavyElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (inputAmount == chipAmt.toString()) NeonCyan else BorderMuted
                                )
                            ) {
                                Text(
                                    text = "₹$chipAmt",
                                    color = if (inputAmount == chipAmt.toString()) NeonCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test mode info box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1A2C),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Razorpay Test Mode Active",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Test UPI: success@razorpay\n• Test Card: 4111 1111 1111 1111\n• Any OTP / CVV succeeds in Test Mode.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val amount = inputAmount.toIntOrNull() ?: 0

                    // 1. Primary: Razorpay Standard SDK Checkout
                    Button(
                        onClick = {
                            if (amount > 0) {
                                showAddMoneyDialog = false
                                onInitiateRazorpayPayment(amount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_submit_razorpay_btn"),
                        enabled = amount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pay ₹${inputAmount.ifEmpty { "0" }} with Razorpay",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Secondary Simulator: Instant Test Simulator Fallback
                    OutlinedButton(
                        onClick = {
                            if (amount > 0) {
                                val simPaymentId = "pay_test_${System.currentTimeMillis() % 1000000}"
                                viewModel.handleRazorpayPaymentSuccess(simPaymentId, amount)
                                showAddMoneyDialog = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmeraldGreen
                        ),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_simulate_payment_btn"),
                        enabled = amount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Instant Test Simulate (+₹${inputAmount.ifEmpty { "0" }})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { showAddMoneyDialog = false },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("dialog_cancel_add_money_btn")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = CardNavy
        )
    }

    // Payment Success Receipt Modal
    paymentSuccessInfo?.let { receipt ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPaymentSuccessInfo() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Payment Successful!",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Firestore Database Updated",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_success_receipt_card")
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardNavyElevated,
                        border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "AMOUNT DEPOSITED",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${receipt.amount}",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderMuted)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment ID", color = TextMuted, fontSize = 11.sp)
                                Text(receipt.paymentId, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gateway", color = TextMuted, fontSize = 11.sp)
                                Text(receipt.gateway, color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("New Balance", color = TextMuted, fontSize = 11.sp)
                                Text("₹${receipt.newBalance}", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearPaymentSuccessInfo() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_done_button")
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardNavy
        )
    }

    // Withdraw Money Dialog
    if (showWithdrawDialog) {
        var withdrawAmountText by remember { mutableStateOf("100") }
        var withdrawMethod by remember { mutableStateOf("UPI") } // "UPI" or "Bank"
        var destinationId by remember { mutableStateOf("gamer@paytm") }
        var ifscCode by remember { mutableStateOf("HDFC0001234") }
        var localError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Withdraw to Bank / UPI",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    // Available Balance Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = CardNavyElevated,
                        border = BorderStroke(1.dp, BorderMuted)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available Balance:", color = TextMuted, fontSize = 12.sp)
                            Text("₹$walletBalance", color = EmeraldGreen, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payout method tabs (UPI vs Bank)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { withdrawMethod = "UPI" },
                            color = if (withdrawMethod == "UPI") GoldAccent.copy(alpha = 0.2f) else CardNavyElevated,
                            border = BorderStroke(1.dp, if (withdrawMethod == "UPI") GoldAccent else BorderMuted),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Instant UPI",
                                color = if (withdrawMethod == "UPI") GoldAccent else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { withdrawMethod = "Bank" },
                            color = if (withdrawMethod == "Bank") GoldAccent.copy(alpha = 0.2f) else CardNavyElevated,
                            border = BorderStroke(1.dp, if (withdrawMethod == "Bank") GoldAccent else BorderMuted),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Bank Account",
                                color = if (withdrawMethod == "Bank") GoldAccent else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (withdrawMethod == "UPI") {
                        OutlinedTextField(
                            value = destinationId,
                            onValueChange = { destinationId = it },
                            label = { Text("UPI ID (VPA)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("withdraw_upi_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick UPI suffix chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("user@paytm", "gamer@oksbi", "player@ybl").forEach { preset ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { destinationId = preset },
                                    shape = RoundedCornerShape(4.dp),
                                    color = CardNavyElevated,
                                    border = BorderStroke(1.dp, BorderMuted)
                                ) {
                                    Text(
                                        text = preset,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = destinationId,
                            onValueChange = { destinationId = it },
                            label = { Text("Bank Account Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("withdraw_account_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase() },
                            label = { Text("IFSC Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("withdraw_ifsc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = withdrawAmountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) withdrawAmountText = it },
                        label = { Text("Withdraw Amount (₹)") },
                        leadingIcon = {
                            Text("₹", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 200, 500).forEach { chipAmt ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { withdrawAmountText = chipAmt.toString() },
                                shape = RoundedCornerShape(6.dp),
                                color = if (withdrawAmountText == chipAmt.toString()) GoldAccent.copy(alpha = 0.2f) else CardNavyElevated,
                                border = BorderStroke(1.dp, if (withdrawAmountText == chipAmt.toString()) GoldAccent else BorderMuted)
                            ) {
                                Text(
                                    text = "₹$chipAmt",
                                    color = if (withdrawAmountText == chipAmt.toString()) GoldAccent else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Max Balance Chip
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { withdrawAmountText = walletBalance.toString() },
                            shape = RoundedCornerShape(6.dp),
                            color = CardNavyElevated,
                            border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Max",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localError ?: "",
                            color = CrimsonRed,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                val amount = withdrawAmountText.toIntOrNull() ?: 0
                Button(
                    onClick = {
                        if (amount <= 0) {
                            localError = "Please enter an amount greater than ₹0."
                            return@Button
                        }
                        if (amount > walletBalance) {
                            localError = "Insufficient balance! Available: ₹$walletBalance"
                            return@Button
                        }
                        val dest = if (withdrawMethod == "UPI") destinationId else "$destinationId (IFSC: $ifscCode)"
                        if (dest.isBlank()) {
                            localError = "Please provide payment destination details."
                            return@Button
                        }

                        localError = null
                        viewModel.withdrawMoney(
                            amount = amount,
                            destination = dest,
                            method = withdrawMethod,
                            onSuccess = {
                                showWithdrawDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_submit_withdraw_btn"),
                    enabled = !isWithdrawalProcessing && amount > 0
                ) {
                    if (isWithdrawalProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processing Payout...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = "Withdraw ₹${withdrawAmountText.ifEmpty { "0" }}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWithdrawDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_withdraw_btn")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardNavy
        )
    }

    // Withdrawal Success Receipt Modal
    withdrawalSuccessInfo?.let { receipt ->
        AlertDialog(
            onDismissRequest = { viewModel.clearWithdrawalSuccessInfo() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Withdrawal Successful!",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Instant Payout Processed",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdrawal_success_receipt_card")
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardNavyElevated,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "AMOUNT TRANSFERRED",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${receipt.amount}",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderMuted)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Destination", color = TextMuted, fontSize = 11.sp)
                                Text(receipt.destination, color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payout ID", color = TextMuted, fontSize = 11.sp)
                                Text(receipt.transactionId, color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Method", color = TextMuted, fontSize = 11.sp)
                                Text(receipt.method, color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Balance", color = TextMuted, fontSize = 11.sp)
                                Text("₹${receipt.newBalance}", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearWithdrawalSuccessInfo() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdrawal_done_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardNavy
        )
    }
}
