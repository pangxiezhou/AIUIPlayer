package com.iflytek.aiui.player.init

import android.content.Context
import com.kugou.kgmusicsdk.KGMusicSDK

class ThirdPartyPlayers {
    companion object {
        init {
            System.loadLibrary("player_init")
        }

        @JvmStatic
        external fun initQTFM(context: Context)

        @JvmStatic
        external fun initKuGouMusic()
    }
}