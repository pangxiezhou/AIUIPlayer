package com.iflytek.aiui.player.core.players

import android.media.AudioManager
import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.error.ErrorDef
import com.iflytek.aiui.player.core.MetaItem
import timber.log.Timber

typealias MediaPlayerCallBack = (MediaPlayer) -> Unit
typealias MediaPlayerInitializer = (MediaPlayerCallBack) -> Unit

typealias URLRetrieveCallback = (String) -> Unit

abstract class AbstractMediaPlayer(rpc: RPC): MetaAbstractPlayer(rpc) {
    private lateinit var mMediaPlayer:MediaPlayer
    private var mInitializer:MediaPlayerInitializer = { callback ->
        callback(MediaPlayer())
    }

    override fun initialize() {
        super.initialize()
        mInitializer.invoke {
            mMediaPlayer = it
            mMediaPlayer.setOnCompletionListener { _ ->
                stateChange(MetaState.COMPLETE)
            }

            mMediaPlayer.setOnErrorListener { _, _, error ->
                if(error != 0) {
                    onError(ErrorDef.ERROR_MEDIA_PLAYER_ERROR, "Media Player Error $error")
                    Timber.e("Media Player On Error $error")
                }
               true
            }

            mMediaPlayer.setOnPreparedListener {_ ->
                // 仅在处于播放状态时，缓冲后立即播放
                if(state() == MetaState.LOADING) {
                    stateChange(MetaState.PLAYING)
                    it.start()
                }
            }

            stateChange(MetaState.READY)
        }
    }

    override fun play(item: MetaItem) {
        mMediaPlayer.reset()
        mMediaPlayer.apply {
            stateChange(MetaState.LOADING)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            retrieveURL(item) {
                setDataSource(it)
                prepareAsync()
            }
        }
    }

    override fun pause() {
        try {
            if(mMediaPlayer.isPlaying) {
                mMediaPlayer.pause()
            }
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

    override fun getDuration(): Int {
        return if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.duration else 0
    }

    override fun getCurrentPos(): Int {
        return if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.currentPosition else 0
    }

    override fun seekTo(msec: Int) {
        if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.seekTo(msec)

    }

    abstract fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback)
}