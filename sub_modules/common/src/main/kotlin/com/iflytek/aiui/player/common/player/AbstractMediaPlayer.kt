package com.iflytek.aiui.player.common.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import com.iflytek.aiui.player.common.error.ErrorDef
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.storage.Storage
import timber.log.Timber
import java.io.File

typealias MediaPlayerCallBack = (ExoPlayer) -> Unit
typealias MediaPlayerInitializer = (Context, MediaPlayerCallBack) -> Unit

typealias URLRetrieveCallback = (String) -> Unit


abstract class AbstractMediaPlayer(context: Context, rpc: RPC, storage: Storage): MetaAbstractPlayer(context, rpc, storage) {
    @Volatile private var mCurrentItem: MetaItem? = null
    private lateinit var mMediaPlayer:ExoPlayer
    private var mIsLiveStream = false
    private var mInitializer: MediaPlayerInitializer = { context, callback->
        callback(ExoPlayerFactory.newSimpleInstance(context))
    }

    override fun initialize() {
        super.initialize()
        mInitializer.invoke(context) {
            mMediaPlayer = it
            mMediaPlayer.playWhenReady = true
            mMediaPlayer.addListener(object: Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    when(playbackState) {
                        Player.STATE_ENDED -> {
                            stateChange(MetaState.COMPLETE)
                        }

                        Player.STATE_READY -> {
                           if(state() == MetaState.LOADING && playWhenReady) {
                               stateChange(MetaState.PLAYING)
                           }
                        }
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    onError(ErrorDef.ERROR_MEDIA_PLAYER_ERROR, "ExoPlayer Error $error")
                    Timber.e("ExoPlayer On Error $error")
                }
            })

            stateChange(MetaState.READY)
        }
    }

    override fun play(item: MetaItem) {
        Timber.d("$this play ${item.title}")
        mCurrentItem = item
        mMediaPlayer.stop(true)
        stateChange(MetaState.LOADING)

        retrieveURL(item) { url ->
            //避免URL回调时已不是当前项目
            if(item == mCurrentItem) {
                val uri = Uri.parse(url)
                val type = Util.inferContentType(uri)
                when(type) {
                    C.TYPE_HLS -> {
                        val  factory = HlsMediaSource.Factory(DefaultDataSourceFactory(context, "AIUIPlayer"))
                        //忽略Ts文件中的h264 stream
                        factory.setExtractorFactory(DefaultHlsExtractorFactory(DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM))
                        mMediaPlayer.prepare(factory.createMediaSource(uri))
                        mIsLiveStream = true
                    }

                    else -> {
                        mMediaPlayer.prepare(ExtractorMediaSource.Factory(buildCacheDataFactory(context)).createMediaSource(uri))
                        mIsLiveStream = false
                    }
                }

                //避免URL回调时已不是播放状态
                if(state() != MetaState.PAUSED) {
                    mMediaPlayer.playWhenReady = true
                }
            }
        }
    }

    override fun pause() {
       if(state() == MetaState.PLAYING || state() == MetaState.LOADING) {
            mMediaPlayer.playWhenReady = false
            stateChange(MetaState.PAUSED)
       }
    }

    override fun resume() {
        if(state() == MetaState.PAUSED || state() == MetaState.LOADING) {
            mMediaPlayer.playWhenReady = true
            stateChange(MetaState.PLAYING)
        }
    }

    override fun release() {
        mMediaPlayer.release()
        stateChange(MetaState.IDLE)
        super.release()
    }

    override fun getDuration(): Long {
        return if(mIsLiveStream) {
            -1
        } else {
            if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.duration else 0
        }
    }

    override fun getCurrentPos(): Long {
        return if(mIsLiveStream) {
            -1
        } else {
            if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.currentPosition else 0
        }
    }

    override fun seekTo(msec: Long) {
        if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.seekTo(msec)
    }

    abstract fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback)

    companion object {
        private const val mCacheDir = "aiui_player_cache"
        private const val mMaxCacheSize:Long = 200 * 1024 * 1014
        private var mCacheDataFactory:DataSource.Factory? = null

        fun buildCacheDataFactory(context: Context): DataSource.Factory {
            if(mCacheDataFactory == null) {
                val upstreamFactory = DefaultDataSourceFactory(context, "AIUIPlayer")
                mCacheDataFactory = CacheDataSourceFactory(
                        SimpleCache(File(context.filesDir, mCacheDir), LeastRecentlyUsedCacheEvictor(mMaxCacheSize)),
                        upstreamFactory,
                        CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            }

            return mCacheDataFactory!!
        }
    }
}