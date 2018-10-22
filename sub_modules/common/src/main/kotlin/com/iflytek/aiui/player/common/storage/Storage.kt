package com.iflytek.aiui.player.common.storage

import android.content.Context

class Storage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("player_storage", Context.MODE_PRIVATE)

    fun put(key: String, value: String) {
        with(sharedPreferences.edit()){
            putString(key, value)
            apply()
        }
    }

    fun put(key: String, value: Int) {
        with(sharedPreferences.edit()){
            putInt(key, value)
            apply()
        }
    }

    fun getString(key: String): String {
        return sharedPreferences.getString(key, "")
    }

    fun getInteger(key: String): Int {
        return sharedPreferences.getInt(key, 0)
    }


}