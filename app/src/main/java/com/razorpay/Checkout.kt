package com.razorpay

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.json.JSONObject

/**
 * Standard Razorpay Checkout SDK interfaces and models for Android (Test Mode).
 */
interface PaymentResultListener {
    fun onPaymentSuccess(razorpayPaymentId: String?)
    fun onPaymentError(code: Int, response: String?)
}

interface PaymentResultWithDataListener {
    fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?)
    fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?)
}

data class PaymentData(
    val paymentId: String? = null,
    val orderId: String? = null,
    val signature: String? = null,
    val userContact: String? = null,
    val userEmail: String? = null,
    val data: JSONObject? = null
)

class Checkout {
    private var keyId: String = RZP_TEST_KEY_DEFAULT

    fun setKeyID(key: String) {
        if (key.isNotBlank()) {
            this.keyId = key
        }
    }

    fun open(activity: Activity, options: JSONObject) {
        RazorpayCheckoutBridge.open(activity, keyId, options)
    }

    companion object {
        const val RZP_TEST_KEY_DEFAULT = "rzp_test_1DP5mmOlF5G5ag"
        const val PAYMENT_CANCELED = 0
        const val TLS_ERROR = 1
        const val INCOMPATIBLE_PLUGIN = 2
        const val NETWORK_ERROR = 3
        const val INVALID_OPTIONS = 4

        @JvmStatic
        fun preload(context: Context) {
            // Preload checkout engine assets
        }

        @JvmStatic
        fun clearUserData(context: Context) {
        }
    }
}
