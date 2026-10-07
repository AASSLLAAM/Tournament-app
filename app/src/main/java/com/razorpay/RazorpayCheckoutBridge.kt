package com.razorpay

import android.app.Activity
import android.content.Intent
import org.json.JSONObject

object RazorpayCheckoutBridge {

    private var activeListener: PaymentResultWithDataListener? = null
    private var activeFallbackListener: PaymentResultListener? = null

    fun registerListener(listener: PaymentResultWithDataListener) {
        this.activeListener = listener
    }

    fun registerFallbackListener(listener: PaymentResultListener) {
        this.activeFallbackListener = listener
    }

    fun open(activity: Activity, keyId: String, options: JSONObject) {
        if (activity is PaymentResultWithDataListener) {
            activeListener = activity
        } else if (activity is PaymentResultListener) {
            activeFallbackListener = activity
        }

        val intent = Intent(activity, RazorpayCheckoutActivity::class.java).apply {
            putExtra("key", keyId)
            putExtra("amount", options.optInt("amount", 10000))
            putExtra("currency", options.optString("currency", "INR"))
            putExtra("name", options.optString("name", "ArenaWar Esports"))
            putExtra("description", options.optString("description", "Wallet Deposit"))
            putExtra("theme_color", options.optString("theme.color", "#00E5FF"))

            val prefill = options.optJSONObject("prefill")
            putExtra("email", prefill?.optString("email") ?: "")
            putExtra("contact", prefill?.optString("contact") ?: "")
        }

        activity.startActivity(intent)
    }

    fun notifySuccess(paymentId: String, data: PaymentData) {
        activeListener?.onPaymentSuccess(paymentId, data)
        activeFallbackListener?.onPaymentSuccess(paymentId)
    }

    fun notifyError(code: Int, message: String, data: PaymentData?) {
        activeListener?.onPaymentError(code, message, data)
        activeFallbackListener?.onPaymentError(code, message)
    }
}
