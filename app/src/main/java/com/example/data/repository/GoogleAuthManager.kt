package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.GoogleUser
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class GoogleAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kesan_google_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_CURRENT_NAME = "current_name"
        private const val KEY_CURRENT_PHOTO = "current_photo"
        private const val KEY_SAVED_ACCOUNTS = "saved_accounts_json"
        private const val KEY_RESET_FLAG = "accounts_cleared_by_user_req_v5"

        const val GOOGLE_CLIENT_ID = "306468857348-kesan.apps.googleusercontent.com"
        const val GOOGLE_REDIRECT_URI = "https://ais-dev-mz5ghyodpeyoxqjg7v6a6y-306468857348.asia-southeast1.run.app/oauth2callback"
    }

    init {
        // Enforce "kosongkan semua akun" - reset and wipe all cached accounts
        if (!prefs.getBoolean(KEY_RESET_FLAG, false)) {
            prefs.edit().clear().putBoolean(KEY_RESET_FLAG, true).apply()
        }
    }

    fun getGoogleAuthorizationUrl(emailHint: String? = null): String {
        val scope = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
        val builder = StringBuilder("https://accounts.google.com/o/oauth2/v2/auth")
            .append("?client_id=").append(GOOGLE_CLIENT_ID)
            .append("&redirect_uri=").append(URLEncoder.encode(GOOGLE_REDIRECT_URI, "UTF-8"))
            .append("&response_type=code")
            .append("&scope=").append(URLEncoder.encode(scope, "UTF-8"))
            .append("&prompt=select_account")
            .append("&access_type=offline")

        if (!emailHint.isNullOrBlank()) {
            builder.append("&login_hint=").append(URLEncoder.encode(emailHint.trim(), "UTF-8"))
        }
        return builder.toString()
    }

    fun getLoggedInUser(): GoogleUser? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val email = prefs.getString(KEY_CURRENT_EMAIL, "") ?: ""
        if (email.isBlank()) return null

        val name = prefs.getString(KEY_CURRENT_NAME, "Pengguna Google") ?: "Pengguna Google"
        val photoUrl = prefs.getString(KEY_CURRENT_PHOTO, null)

        val accounts = getSavedAccounts()
        val match = accounts.firstOrNull { it.email.equals(email, ignoreCase = true) }

        return GoogleUser(
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            lastSyncTimestamp = match?.lastSyncTimestamp ?: System.currentTimeMillis()
        )
    }

    fun getSavedAccounts(): List<GoogleUser> {
        val jsonStr = prefs.getString(KEY_SAVED_ACCOUNTS, "[]") ?: "[]"
        val list = mutableListOf<GoogleUser>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val email = obj.optString("email", "")
                val name = obj.optString("name", "Akun Google")
                val photo = if (obj.has("photo") && !obj.isNull("photo")) obj.getString("photo") else null
                val lastSync = obj.optLong("lastSync", System.currentTimeMillis())
                if (email.isNotBlank()) {
                    list.add(GoogleUser(name, email, photo, lastSync))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveLogin(name: String, email: String, photoUrl: String? = null): GoogleUser {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank {
            cleanEmail.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
        val now = System.currentTimeMillis()

        val accounts = getSavedAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.email.equals(cleanEmail, ignoreCase = true) }

        val user = GoogleUser(
            displayName = cleanName,
            email = cleanEmail,
            photoUrl = photoUrl,
            lastSyncTimestamp = now
        )

        if (existingIndex >= 0) {
            accounts[existingIndex] = user
        } else {
            accounts.add(0, user)
        }
        saveAccountsList(accounts)

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_NAME, cleanName)
            .putString(KEY_CURRENT_EMAIL, cleanEmail)
            .putString(KEY_CURRENT_PHOTO, photoUrl)
            .apply()

        return user
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_NAME)
            .remove(KEY_CURRENT_EMAIL)
            .remove(KEY_CURRENT_PHOTO)
            .apply()
    }

    fun clearAllAccounts() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_NAME)
            .remove(KEY_CURRENT_EMAIL)
            .remove(KEY_CURRENT_PHOTO)
            .remove(KEY_SAVED_ACCOUNTS)
            .apply()
    }

    fun removeSavedAccount(email: String) {
        val accounts = getSavedAccounts().filterNot { it.email.equals(email, ignoreCase = true) }
        saveAccountsList(accounts)
        val currentEmail = prefs.getString(KEY_CURRENT_EMAIL, "")
        if (currentEmail.equals(email, ignoreCase = true)) {
            logout()
        }
    }

    fun updateLastSyncTimestamp(): Long {
        val now = System.currentTimeMillis()
        val currentEmail = prefs.getString(KEY_CURRENT_EMAIL, "") ?: ""
        if (currentEmail.isNotBlank()) {
            val accounts = getSavedAccounts().toMutableList()
            val idx = accounts.indexOfFirst { it.email.equals(currentEmail, ignoreCase = true) }
            if (idx >= 0) {
                accounts[idx] = accounts[idx].copy(lastSyncTimestamp = now)
                saveAccountsList(accounts)
            }
        }
        return now
    }

    private fun saveAccountsList(accounts: List<GoogleUser>) {
        try {
            val arr = JSONArray()
            for (acc in accounts) {
                val obj = JSONObject()
                obj.put("name", acc.displayName)
                obj.put("email", acc.email)
                obj.put("photo", acc.photoUrl)
                obj.put("lastSync", acc.lastSyncTimestamp)
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_ACCOUNTS, arr.toString()).apply()
        } catch (_: Exception) {}
    }
}
