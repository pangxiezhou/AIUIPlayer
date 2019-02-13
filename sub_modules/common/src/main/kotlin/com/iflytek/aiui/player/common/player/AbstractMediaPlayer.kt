package com.iflytek.aiui.player.common.player

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import com.iflytek.aiui.player.common.BuildConfig
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
                mMediaPlayer.prepare(ExtractorMediaSource.Factory(buildDataSourceFactory(context)).createMediaSource(Uri.parse(url)))
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
        return if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.duration else 0
    }

    override fun getCurrentPos(): Long {
        return if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.currentPosition else 0
    }

    override fun seekTo(msec: Long) {
        if(state() in listOf(MetaState.PLAYING, MetaState.PAUSED)) mMediaPlayer.seekTo(msec)

    }

    abstract fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback)

    companion object {
        private val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

        private var downloadDirectory: File? = null
        private var downloadCache: Cache? = null


        /** Returns a [DataSource.Factory].  */
        fun buildDataSourceFactory(context: Context): DataSource.Factory {
            val upstreamFactory = DefaultDataSourceFactory(context, buildHttpDataSourceFactory(context))
            return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context))
        }

        /** Returns a [HttpDataSource.Factory].  */
        fun buildHttpDataSourceFactory(context: Context): HttpDataSource.Factory {
            return DefaultHttpDataSourceFactory(Util.getUserAgent(context, "AIUIPlayer"))
        }

        /** Returns whether extension renderers should be used.  */
        fun useExtensionRenderers(): Boolean {
            return "withExtensions" == BuildConfig.FLAVOR
        }

        private fun getDownloadCache(context: Context): Cache {
            if (downloadCache == null) {
                val downloadContentDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
                downloadCache = SimpleCache(downloadContentDirectory, NoOpCacheEvictor())
            }
            return downloadCache!!
        }

        private fun getDownloadDirectory(context: Context): File? {
            if (downloadDirectory == null) {
                downloadDirectory = context.getExternalFilesDir(null)
                if (downloadDirectory == null) {
                    downloadDirectory = context.getFilesDir()
                }
            }
            return downloadDirectory
        }

        private fun buildReadOnlyCacheDataSource(
                upstreamFactory: DefaultDataSourceFactory, cache: Cache): CacheDataSourceFactory {
            return CacheDataSourceFactory(
                    cache,
                    upstreamFactory,
                    FileDataSourceFactory(), null,
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null)/* cacheWriteDataSinkFactory= */
        }
    }
}