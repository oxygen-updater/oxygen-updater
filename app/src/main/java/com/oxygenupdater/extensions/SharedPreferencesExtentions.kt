@file:Suppress("NOTHING_TO_INLINE")

package com.oxygenupdater.extensions

import android.content.SharedPreferences
import androidx.core.content.edit

//region Float
inline operator fun SharedPreferences.get(key: String, default: String) = getString(key, default) ?: default
inline operator fun SharedPreferences.set(key: String, value: String) = edit { putString(key, value) }
//endregion

//region Set<String>
inline operator fun SharedPreferences.get(key: String, default: Set<String>) = getStringSet(key, default) ?: default
inline operator fun SharedPreferences.set(key: String, value: Set<String>) = edit { putStringSet(key, value) }
//endregion

//region Int
inline operator fun SharedPreferences.get(key: String, default: Int) = getInt(key, default)
inline operator fun SharedPreferences.set(key: String, value: Int) = edit { putInt(key, value) }
inline fun SharedPreferences.incrementInt(key: String) = set(key, get(key, 0) + 1)
//endregion

//region Long
inline operator fun SharedPreferences.get(key: String, default: Long) = getLong(key, default)
inline operator fun SharedPreferences.set(key: String, value: Long) = edit { putLong(key, value) }
//endregion

//region Float
inline operator fun SharedPreferences.get(key: String, default: Float) = getFloat(key, default)
inline operator fun SharedPreferences.set(key: String, value: Float) = edit { putFloat(key, value) }
//endregion

//region Boolean
inline operator fun SharedPreferences.get(key: String, default: Boolean) = getBoolean(key, default)
inline operator fun SharedPreferences.set(key: String, value: Boolean) = edit { putBoolean(key, value) }
//endregion

inline fun SharedPreferences.remove(key: String) = edit { remove(key) }
inline fun SharedPreferences.remove(key1: String, key2: String) = edit {
    remove(key1)
    remove(key2)
}

/**
 * Convenience function to persist [com.oxygenupdater.models.SelectableModel].
 *
 * [key] is used directly for persisting [name], while for [id] "_id" is appended.
 */
inline fun SharedPreferences.setIdAndName(key: String, id: Long, name: String?) = edit {
    putLong("${key}_id", id)
    putString(key, name)
}
