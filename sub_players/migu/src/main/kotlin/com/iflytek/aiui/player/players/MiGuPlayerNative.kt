package com.iflytek.aiui.player.players

import android.content.Context

class MiGuPlayerNative {
    companion object {
        init {
            System.loadLibrary("migu_player_init")
        }

        @JvmStatic
        external fun initMiGu(context: Context)
    }
}