package com.iflytek.aiui.player.players

import android.content.Context

class QTPlayerNative {
    companion object {
        init {
            System.loadLibrary("qt_player_init")
        }

        @JvmStatic
        external fun initQTFM(context: Context)
    }
}