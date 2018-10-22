package com.iflytek.aiui.player.core.players

import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import com.iflytek.aiui.player.common.rpc.storage.Storage
import com.iflytek.aiui.player.core.MetaItem
import com.kugou.common.utils.KgInfo
import com.kugou.common.utils.MusicJNI

class KuGouAPI {
    private var libraryLoaded = false

    private fun initializeIfNeed() {
        if (!libraryLoaded) {
            System.loadLibrary("kghwsdk")
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

    fun retrieveUrl(hash: String, albumId: Int): KgInfo {
        initializeIfNeed()
        val ret = KgInfo()
        MusicJNI.KgHwsdkGetUrl(hash, ret, 0, 0)
        return ret
    }
}

class MetaKGPlayer(rpc: RPC, val storage: Storage) : AbstractMediaPlayer(rpc) {
    private val tokenKey = "token"
    private val userIDKey = "userID"
    private var mKuGouAPI = KuGouAPI()
    private var mRPCRequesting = false

    override fun initialize() {
        super.initialize()
        mKuGouAPI.init()
    }

    override fun retriveURL(item: MetaItem, callback: URLRetriveCallback) {
        val url = mKuGouAPI.retrieveUrl(item.info.optString("itemid"), 0)
        when (url.errorNo) {
            0 -> {
                if (url.Info?.isEmpty() == false) {
                    callback(url.Info)
                } else {
                    //酷狗SDK问题, 在未登录情况下依旧返回0
                    //故认为error = 0 info = null的情况下做未授权的处理
                    onTokenExpire(item, callback)
                }
            }

            //Token过期
            -2 -> {
                onTokenExpire(item, callback)
            }
        }
    }

    private fun onTokenExpire(item: MetaItem, callback: URLRetriveCallback) {
        val savedToken = storage.getString(tokenKey)
        val savedUserID = storage.getInteger(userIDKey)
        //首先用本地缓存token进行登录
        if (!savedToken.isEmpty() && mKuGouAPI.login(savedUserID, savedToken)) {
            retriveURL(item, callback)
        } else {
            //当前正在请求中，不重复请求
            if (!mRPCRequesting) {
                mRPCRequesting = true
                rpc.request<String>(TokenReq.createFor(SourceType.KuGou), {
                    mRPCRequesting = false
                    val temp = it.split("#")
                    if (temp.size == 2) {
                        val userID = Integer.valueOf(temp[0])
                        val token = temp[1]
                        if (mKuGouAPI.login(userID, token)) {
                            storage.put(tokenKey, token)
                            storage.put(userIDKey, userID)

                            retriveURL(item, callback)
                        }
                    }
                }, { error, description ->
                    mRPCRequesting = false
                    onError(error, description)
                })
            }
        }
    }

    override fun canDispose(item: MetaItem): Boolean {
        if (item.source == "kugou") {
            val itemID = item.info.optString("itemid", "")
            if (!itemID.isEmpty()) {
                return true
            }
        }

        return false
    }
}