package com.iflytek.aiui.player.core.players

import android.media.AudioManager
import android.media.MediaPlayer
import com.iflytek.aiui.player.core.MetaInfo

typealias MediaPlayerCallBack = (MediaPlayer) -> Unit
typealias MediaPlayerInitializer = (MediaPlayerCallBack) -> Unit

class MetaMediaPlayer: MetaAbstractPlayer() {
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

    override fun play(item: MetaInfo):Boolean {
        if(!canDispose(item)) return false

        mMediaPlayer.reset()
        mMediaPlayer.apply {
            stateChange(MetaState.LOADING)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(item.url)
            prepareAsync()
        }

        return true
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

    private fun canDispose(item: MetaInfo): Boolean {
        if(item.source.isEmpty() || item.source == "iflytek") {
            val url = item.url
            if(!url.isEmpty() && url.contains(Regex("mp3|m4a"))) {
                return true
            }
        }

        return false
    }
}