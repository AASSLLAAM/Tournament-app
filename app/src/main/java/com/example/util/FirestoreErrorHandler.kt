package com.example.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Throwable) {
        null
    }
    val currentUser = try {
        auth?.currentUser
    } catch (_: Throwable) {
        null
    }

    val providerInfoList = try {
        currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    val authInfoJson = JSONObject().apply {
        try {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        } catch (_: Throwable) {}
    }

    val errorInfoJson = JSONObject().apply {
        try {
            put("error", exception.message ?: exception.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        } catch (_: Throwable) {}
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
