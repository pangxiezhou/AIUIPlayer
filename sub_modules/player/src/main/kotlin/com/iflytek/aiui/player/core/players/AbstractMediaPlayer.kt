package com.iflytek.aiui.player.core.players

import android.media.AudioManager
import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.core.MetaInfo

typealias MediaPlayerCallBack = (MediaPlayer) -> Unit
typealias MediaPlayerInitializer = (MediaPlayerCallBack) -> Unit

typealias URLRetriveCallback = (String) -> Unit

abstract class AbstractMediaPlayer(rpc: RPC): MetaAbstractPlayer(rpc) {
    private lateinit var mMediaPlayer:MediaPlayer
    private var mInitializer:MediaPlayerInitializer = { callback ->
        callback(MediaPlayer())
    }

    override fun initialize() {
        super.initialize()
        mInitializer.invoke {
            mMediaPlayer = it
            mMediaPlayer.setOnCompletionListener {
                stateChange(MetaState.COMPLETE)
            }

            mMediaPlayer.setOnErrorListener { _, _, _ -> true }

            mMediaPlayer.setOnPreparedListener {
                // 仅在处于播放状态时，缓冲后立即播放
                if(state() == MetaState.LOADING) {
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
            retriveURL(item) {
                setDataSource(it)
                prepareAsync()
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

    abstract fun retriveURL(item: MetaInfo, callback: URLRetriveCallback)
}