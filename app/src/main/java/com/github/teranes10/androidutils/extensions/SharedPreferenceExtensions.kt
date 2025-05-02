package com.github.teranes10.androidutils.extensions

import android.content.SharedPreferences

object SharedPreferenceExtensions {

    fun SharedPreferences.Editor.safePut(key: String, value: String?) {
        if (value == null) remove(key) else putString(key, value)
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Float?) {
        if (value == null) remove(key) else putFloat(key, value)
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Long?) {
        if (value == null) remove(key) else putLong(key, value)
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Boolean?) {
        if (value == null) remove(key) else putBoolean(key, value)
    }

    fun SharedPreferences.Editor.safePut(key: String, value: Set<String>?) {
        if (value == null) remove(key) else putStringSet(key, value)
    }

    fun SharedPreferences.Editor.safePutIntSet(key: String, value: Set<Int>?) {
        if (value == null) {
            remove(key)
        } else {
            putStringSet(key, value.map { it.toString() }.toSet())
        }
    }

    fun SharedPreferences.getIntSet(key: String): Set<Int> {
        return getStringSet(key, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }
}