package com.lumen.reader.data

import android.content.Context

object AuthSession {
    private const val PREFS = "lumen_auth"
    private const val KEY_EMAIL = "email"
    private const val KEY_USER = "username"
    private const val KEY_SIGNED = "signed_in"

    fun isSignedIn(context: Context): Boolean {
        if (!SupabaseClient.accessToken.isNullOrBlank()) return true
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SIGNED, false)
    }

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""

    fun username(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, "") ?: ""

    fun markSignedIn(context: Context, email: String, username: String = "") {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SIGNED, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER, username)
            .apply()
    }

    fun signOut(context: Context) {
        SupabaseClient.clearToken()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
