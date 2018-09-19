package com.iflytek.aiui.player.core.players

import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import com.iflytek.aiui.player.common.rpc.storage.Storage
import com.iflytek.aiui.player.core.MetaInfo
import com.kugou.common.utils.KgInfo
import com.kugou.common.utils.MusicJNI

class KuGouAPI {
    private var libraryLoaded = false

    private fun initializeIfNeed() {
        if(!libraryLoaded) {
            System.loadLibrary( "kghwsdk" )
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

    fun retrieveUrl(hash: String, albumId: Int): String {
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

class MetaKGPlayer(rpc: RPC, val storage: Storage) : AbstractMediaPlayer(rpc) {
    private val token_key = "token"
    private val userid_key = "userID"
    private var mKuGouAPI = KuGouAPI()
    private var mRPCRequesting = false

    override fun initialize() {
        super.initialize()
        mKuGouAPI.init()
    }

    override fun retriveURL(item: MetaInfo, callback: URLRetriveCallback) {
        val savedToken = storage.getString(token_key)
        val savedUserID = storage.getInteger(userid_key)

        val retrieveURLAndCallback = {
            val url = mKuGouAPI.retrieveUrl(item.info.optString("itemid"), 0)
            if (!url.isEmpty()) {
                callback(url)
            }
        }

        if (!savedToken.isEmpty() && mKuGouAPI.login(savedUserID, savedToken)) {
            retrieveURLAndCallback()
        } else {
            if(!mRPCRequesting) {
                mRPCRequesting = true
                rpc.request<String>(TokenReq.createFor(SourceType.KuGou), {
                    mRPCRequesting = false
                    val temp = it.split("#")
                    if (temp.size == 2) {
                        val userID = Integer.valueOf(temp[0])
                        val token = temp[1]
                        if (mKuGouAPI.login(userID, token)) {
                            storage.put(token_key, token)
                            storage.put(userid_key, userID)

                            retrieveURLAndCallback()
                        }
                    }
                }, {_, _ ->
                    mRPCRequesting = false
                })
            }
        }
    }

    override fun canDispose(item: MetaInfo): Boolean {
        if(item.source == "kugou") {
            val itemID = item.info.optString("itemid", "")
            if(!itemID.isEmpty()) {
               return true
            }
        }

        return false
    }
}