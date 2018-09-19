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

class MetaKGPlayer(rpc: RPC) : MetaAbstractPlayer(rpc) {
    private lateinit var mMediaPlayer: MediaPlayer
    private var mKuGouAPI = KuGouAPI()
    private var mInitializer: MediaPlayerInitializer = { callback ->
        callback(MediaPlayer())
    }

    override fun initialize() {
        super.initialize()
        mInitializer.invoke {
            mKuGouAPI.init()

            mMediaPlayer = it
            mMediaPlayer.setOnCompletionListener {
                stateChange(MetaState.COMPLETE)
            }

            mMediaPlayer.setOnErrorListener { _, _, _ -> true }

            mMediaPlayer.setOnPreparedListener {
                // 仅在处于播放状态时，缓冲后立即播放
                if (state() == MetaState.LOADING) {
                    stateChange(MetaState.PLAYING)
                    it.start()
                }
            }

            stateChange(MetaState.READY)
        }
    }

    override fun play(item: MetaInfo) {
        mMediaPlayer.reset()
        mMediaPlayer.apply {
            stateChange(MetaState.LOADING)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            rpc.request<String>(GetToken.forSource("kugou")) {
                val temp = it.split("#")
                if (temp.size == 2) {
                    val userID = Integer.valueOf(temp[0])
                    val token = temp[1]
                    if (mKuGouAPI.login(userID, token)) {
                        val url = mKuGouAPI.retriveUrl(item.info.optString("itemId"), 0)
                        if (!url.isEmpty()) {
                            setDataSource(url)
                            prepareAsync()
                        }
                    }
                }
            }
        }
    }

    override fun pause() {
        try {
            mMediaPlayer.pause()
        } catch (e: Exception) {
            //ignore IllegalStateException
        }
        stateChange(MetaState.PAUSED)
    }

    override fun resume() {
        try {
            mMediaPlayer.start()
        } catch (e: Exception) {
            //ignore IllegalStateException
        }
        stateChange(MetaState.PLAYING)
    }

    override fun release() {
        mMediaPlayer.release()
        stateChange(MetaState.IDLE)
        super.release()
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