package com.iflytek.aiui.player.init

import android.content.Context

class PlayerInitializer {
    companion object {
        init {
            System.loadLibrary("player_init")
        }

        @JvmStatic
        external fun initQTFM(context: Context)
    }
}