package com.iflytek.aiui.player.players

import android.content.Context
import com.iflytek.aiui.player.common.player.AbstractMediaPlayer
import com.iflytek.aiui.player.common.player.MetaItem
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.player.URLRetrieveCallback
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import com.iflytek.aiui.player.common.storage.Storage
import com.kugou.common.utils.KgInfo
import com.kugou.common.utils.MusicJNI
import timber.log.Timber

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

class MetaKGPlayer(context: Context, rpc: RPC, storage: Storage) : AbstractMediaPlayer(context, rpc, storage) {
    private val tokenKey = "token"
    private val userIDKey = "userID"
    private var mKuGouAPI = KuGouAPI()
    private var mRPCRequesting = false
    private var currentItem: MetaItem? = null

    override fun initialize() {
        super.initialize()
        mKuGouAPI.init()
    }

    override fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback) {
        currentItem = item
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

    private fun onTokenExpire(item: MetaItem, callback: URLRetrieveCallback) {
        val savedToken = storage.getString(tokenKey)
        val savedUserID = storage.getInteger(userIDKey)
        //首先用本地缓存token进行登录
        if (!savedToken.isEmpty() && mKuGouAPI.login(savedUserID, savedToken)) {
            retrieveURL(item, callback)
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

                            retrieveURL(currentItem!!, callback)
                        }
                    }
                }, { error, description ->
                    mRPCRequesting = false
                    onError(error, description)
                    Timber.e("MetaKuGouPlayer On RPC Error $error $description")
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