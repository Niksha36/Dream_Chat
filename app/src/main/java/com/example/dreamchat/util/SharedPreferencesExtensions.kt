package com.example.dreamchat.util

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> SharedPreferences.putObject(key: String, value: T) {
    val json = Gson().toJson(value)
    edit().putString(key, json).apply()
}

inline fun <reified T> SharedPreferences.getObject(key: String): T? {
    val json = getString(key, null) ?: return null
    return Gson().fromJson(json, object : TypeToken<T>() {}.type)
}