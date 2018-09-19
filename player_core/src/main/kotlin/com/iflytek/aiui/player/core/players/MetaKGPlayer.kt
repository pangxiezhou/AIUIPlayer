package com.iflytek.aiui.player.core.players

import android.media.AudioManager
import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.method.GetToken
import com.iflytek.aiui.player.core.MetaInfo
import com.kugou.common.utils.KgInfo
import com.kugou.common.utils.MusicJNI

class KuGouAPI {
    private var libraryLoaded = false

    private fun initializeIfNeed() {
        if(!libraryLoaded) {
            System.loadLibrary( "kghwsdk" );
            libraryLoaded = true
        }
    }

    fun init() {
        initializeIfNeed()
        MusicJNI.KgNativeInit()
    }

    fun login(userID: Int, token: String): Boolean {
        initializeIfNeed()
        val ret = KgInfo()
        MusicJNI.KgHwsdkLogin(token, ret, userID)
        return ret.errorNo == 0
    }

    fun retriveUrl(hash: String, albumId: Int): String {
        initializeIfNeed()
        val ret = KgInfo()
        MusicJNI.KgHwsdkGetUrl(hash, ret, 0, 0)
        return if (ret.errorNo == 0) {
            ret.Info
        } else {
            ""
        }
    }
}

class MetaKGPlayer(rpc: RPC) : AbstractMediaPlayer(rpc) {
    private var mKuGouAPI = KuGouAPI()

    override fun initialize() {
        super.initialize()
        mKuGouAPI.init()
    }

    override fun retriveURL(item: MetaInfo, callback: URLRetriveCallback) {
        rpc.request<String>(GetToken.forSource("kugou")) {
            val temp = it.split("#")
            if (temp.size == 2) {
                val userID = Integer.valueOf(temp[0])
                val token = temp[1]
                if (mKuGouAPI.login(userID, token)) {
                    val url = mKuGouAPI.retriveUrl(item.info.optString("itemId"), 0)
                    if (!url.isEmpty()) {
                        callback(url)
                    }
                }
            }
        }
    }

    override fun canDispose(item: MetaInfo): Boolean {
        if(item.source == "kugou") {
            val itemID = item.info.optString("itemId", "")
            if(!itemID.isEmpty()) {
               return true
            }
        }

        return false
    }
}