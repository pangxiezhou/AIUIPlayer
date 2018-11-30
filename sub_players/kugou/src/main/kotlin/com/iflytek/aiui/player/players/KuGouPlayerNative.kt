package com.iflytek.aiui.player.players

class KuGouPlayerNative {
    companion object {
        init {
            System.loadLibrary("kugou_player_init")
        }

        @JvmStatic
        external fun initKuGouMusic()
    }
}