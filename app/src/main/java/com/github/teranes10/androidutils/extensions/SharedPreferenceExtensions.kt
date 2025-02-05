package com.example.mytaxy.extensions

import android.content.SharedPreferences

object SharedPreferenceExtensions {
    fun SharedPreferences.Editor.safePut(key: String, value: String?) {
        value?.let { putString(key, it) }
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Float?) {
        value?.let { putFloat(key, it) }
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Int?) {
        value?.let { putInt(key, it) }
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Long?) {
        value?.let { putLong(key, it) }
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Boolean?) {
        value?.let { putBoolean(key, it) }
    }
}