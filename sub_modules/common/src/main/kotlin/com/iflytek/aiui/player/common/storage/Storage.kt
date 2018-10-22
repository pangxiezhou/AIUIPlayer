package com.iflytek.aiui.player.common.storage

import android.content.Context

class Storage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("player_storage", Context.MODE_PRIVATE)

    fun put(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).commit()
    }

    fun put(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).commit()
    }

    fun getString(key: String): String {
        return sharedPreferences.getString(key, "")
    }

    fun getInteger(key: String): Int {
        return sharedPreferences.getInt(key, 0)
    }


}