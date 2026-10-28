package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PaymentSuccessInfo
import com.example.model.WalletTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WorldWar Esports", appName)
    }

    @Test
    fun `razorpay payment models initialize properly`() {
        val paymentInfo = PaymentSuccessInfo(
            paymentId = "pay_test_12345",
            amount = 500,
            newBalance = 1000,
            gateway = "Razorpay Standard (Test Mode)"
        )
        assertEquals("pay_test_12345", paymentInfo.paymentId)
        assertEquals(500, paymentInfo.amount)
        assertEquals(1000, paymentInfo.newBalance)

        val transaction = WalletTransaction(
            id = "tx_rzp_12345",
            userId = "test_user",
            title = "Razorpay Deposit (Test Mode)",
            amount = 500,
            isCredit = true,
            status = "Success",
            paymentId = "pay_test_12345",
            gateway = "Razorpay Standard"
        )
        assertTrue(transaction.isCredit)
        assertEquals("pay_test_12345", transaction.paymentId)
        assertEquals("Razorpay Standard", transaction.gateway)
    }

    @Test
    fun `tournament matchType defaults to Squad and maps format properly`() {
        val defaultTour = com.example.model.Tournament(title = "Default Match")
        assertEquals("Squad", defaultTour.matchType)
        assertEquals(com.example.model.MatchFormat.SQUAD, defaultTour.format)

        val soloTour = com.example.model.Tournament(title = "Solo Showdown", matchType = "Solo")
        assertEquals("Solo", soloTour.matchType)
        assertEquals(com.example.model.MatchFormat.SOLO, soloTour.format)

        val duoTour = com.example.model.Tournament(title = "Duo Clash", matchType = "Duo")
        assertEquals("Duo", duoTour.matchType)
        assertEquals(com.example.model.MatchFormat.DUO, duoTour.format)
    }
}
