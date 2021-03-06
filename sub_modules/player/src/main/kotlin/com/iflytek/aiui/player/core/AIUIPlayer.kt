package com.iflytek.aiui.player.core

import android.content.Context
import android.os.Handler
import com.iflytek.aiui.player.common.player.*
import com.iflytek.aiui.player.common.rpc.*
import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import com.iflytek.aiui.player.common.storage.Storage
import org.json.JSONArray
import kotlin.reflect.full.primaryConstructor

/**
 * 播放器状态
 */
enum class PlayState {
    /**
     * 未初始化状态，需要调用initialize
     */
    IDLE,
    /**
     * 正在初始化
     */
    INITIALIZING,
    /**
     * 就绪状态
     */
    READY,
    /**
     * 资源装在加载
     */
    LOADING,
    /**
     * 正在播放状态
     */
    PLAYING,
    /**
     * 暂停状态
     */
    PAUSED,
    /**
     * 列表播放完成状态，可调用resume或play，重新进入PLAYING
     */
    COMPLETE,
    /**
     * 错误状态，需调用reset，重新进入READY状态
     */
    ERROR;
}


/**
 * AIUIPlayer 播放监听器
 */
interface PlayerListener {
    /**
     * 播放器已就绪，调用AIUIPlayer的initialize后异步回调此接口通知已就绪
     */
    fun onPlayerReady()

    /**
     * 播放器状态回调，在播放状态变化时回调
     */
    fun onStateChange(state: PlayState)

    /**
     * 播放内容变化回调，直接调用next，previous或播放结束自动切换到下一首
     */
    fun onMediaChange(item: MetaItem)

    /**
     * 错误回调
     */
    fun onError(error: Int, info: String)

    /**
     * 播放器已销毁
     */
    fun onPlayerRelease()
}

/**
 * AIUI播放器，用来解析播放AIUI平台返回可播放的信源内容列表，
 *
 * 内部根据每项内容的source字段分发到具体的播放器
 */

class AIUIPlayer(context: Context) {
    private var mainHandler = Handler(context.mainLooper)
    private var serverConnection:DataConnection = WebSocketServerConnection(4096)
    private var rpcServer = RPC(serverConnection, object: RPCListener {
        override fun onRequest(rpc: RPC, data: String) {

        }
    })
    private var mStorage = Storage(context)
    private var mPlayers: MutableList<MetaAbstractPlayer>
    private var mActivePlayer: MetaAbstractPlayer? = null
    private val mListeners = mutableListOf<PlayerListener>()
    //默认初始化状态
    private var mState: PlayState = PlayState.IDLE
    //播放列表内容
    private var mData = mutableListOf<MetaItem>()
    private var mIndex = 0
    private var positiveDirection = true
    //初始化标识，避免重复初始化
    private var mInitialized = false
    //Ready标记保存，Listener的注册可能在Ready之后，保证状态回调正常
    private var mReadyCount = 0
    private var mAutoSkipError = true

    init {
        mPlayers = mutableListOf(MetaMediaPlayer(context, rpcServer, mStorage))
        //反射初始化可能配置的子播放器
        val prefix = "com.iflytek.aiui.player.players"
        val players = listOf("$prefix.MetaKGPlayer", "$prefix.MetaQTPlayer", "$prefix.MetaMiGuPlayer")
        players.forEach {
            try {
                val playerClass = Class.forName(it).kotlin
                val player = playerClass.primaryConstructor?.call(context, rpcServer, mStorage)
                player?.let { mPlayers.add(player as MetaAbstractPlayer) }
            } catch (exception: ClassNotFoundException) {
                //player dependency not configure just ignore
            }
        }
    }


    //当前播放项内容
    val currentPlay: MetaItem?
        get() {
            return if (mIndex !in 0 until mData.size) {
                null
            } else {
                mData[mIndex]
            }
        }

    //当前播放状态
    val currentState
        get() = mState


    val duration: Long
        get() = mActivePlayer?.getDuration() ?: 0


    val currentPosition
        get() = mActivePlayer?.getCurrentPos() ?: 0

    /**
     * 初始化，在构造完成后立即调用
     *
     * （不放在构造函数中，是为测试用例分离需要)
     */
    fun initialize() {
        // 避免重复初始化
        if (mInitialized) return

        serverConnection.start()
        onStateChange(PlayState.INITIALIZING)
        //初始化各子播放器
        mPlayers.forEach {
            it.initialize()
            it.addListener(object : MetaListener {
                override fun onError(error: Int, description: String) {
                    if (mAutoSkipError) {
                        if (positiveDirection) {
                            if (!next()) {
                                onStateError(error, description)
                            }
                        } else {
                            if (!previous()) {
                                onStateError(error, description)
                            }
                        }
                    } else {
                        onStateError(error, description)
                    }
                }

                override fun onReady() {
                    if (++mReadyCount == mPlayers.size) {
                        onStateChange(PlayState.READY)
                        runMain {
                            mListeners.forEach {listener -> listener.onPlayerReady() }
                        }
                    }
                }

                override fun onStateChange(state: MetaState) {
                    when (state) {
                        MetaState.LOADING -> {
                            onStateChange(PlayState.LOADING)
                        }

                        MetaState.PLAYING -> {
                            if (mState == PlayState.LOADING) {
                                onStateChange(PlayState.PLAYING)
                            }
                        }

                        MetaState.COMPLETE -> {
                            if (!next()) {
                                onComplete()
                            }
                        }

                        else -> {}

                    }
                }

                override fun onRelease() {
                    if (--mReadyCount == 0) {
                        runMain {
                            mListeners.forEach {listener -> listener.onPlayerRelease() }
                        }
                        onStateChange(PlayState.IDLE)
                    }
                }
            })
        }

        mInitialized = true
    }


    /**
     * 信源列表是否用可播放项
     * 需要在PlayerReady后调用
     * @param data 信源内容列表
     * @param service 技能名称
     *
     * @return 列表是否有可播放项
     *
     */
    fun anyAvailablePlay(data: JSONArray, service: String): Boolean {
        for (i in 0 until data.length()) {
            if (mPlayers.any {
                        it.canDispose(MetaItem(data.optJSONObject(i), service))
                    }) {
                return true
            }
        }

        return false
    }

    /**
     * 播放信源内容列表
     * 需要在PlayerReady后调用
     * @param data 信源内容列表
     * @param service 技能名称
     *
     * @return 列表是否播放成功
     *
     */
    fun play(data: JSONArray, service: String, autoSkipError: Boolean = true): Boolean {
        if (mState != PlayState.READY || !anyAvailablePlay(data, service)) return false

        mActivePlayer?.pause()
        mData = mutableListOf()
        for (i in 0 until data.length()) {
            mData.add(MetaItem(data.optJSONObject(i), service))
        }
        mIndex = -1
        mAutoSkipError = autoSkipError
        return playToNextAvailable()
    }

    /**
     * 下一个
     *
     * @return 是否操作成功
     */
    fun next(): Boolean {
        positiveDirection = true
        return playToNextAvailable()
    }

    /**
     * 上一个
     *
     * @return 是否操作成功
     */
    fun previous(): Boolean {
        positiveDirection = false
        return playToNextAvailable(false)
    }

    /**
     * 暂停播放
     */
    fun pause() {
        when (mState) {
            PlayState.PLAYING, PlayState.LOADING -> {
                onStateChange(PlayState.PAUSED)
                mActivePlayer?.pause()
            }
            else -> {}
        }
    }

    /**
     * 继续播放
     */
    fun resume() {
        when (mState) {
            PlayState.PAUSED -> {
                onStateChange(PlayState.PLAYING)
                mActivePlayer?.resume()
            }

            PlayState.COMPLETE -> {
                mIndex++
                playToNextAvailable(false)
            }

            else -> {}
        }
    }


    fun seekTo(msec: Long) {
        mActivePlayer?.seekTo(msec)
    }

    /**
     * 播放器停止，列表清空，状态恢复到STOPPED
     */
    fun reset() {
        onStateChange(PlayState.READY)
        mActivePlayer?.pause()

        mData.clear()
        mIndex = -1
    }

    /**
     * 添加播放监听器
     */
    fun addListener(listener: PlayerListener) {
        mListeners.add(listener)
        if (mState != PlayState.IDLE && mState != PlayState.INITIALIZING) {
            runMain {
                listener.onPlayerReady()
                listener.onStateChange(PlayState.READY)
            }
        }
    }

    /**
     * 监听器移除
     */
    fun removeListener(listener: PlayerListener) {
        mListeners.remove(listener)
    }

    /**
     * 播放器销毁，END状态
     */
    fun release() {
        mInitialized = false
        reset()
        mPlayers.forEach { it.release() }
    }

    private fun runMain(action: () -> Unit) {
        mainHandler.post {
            action()
        }
    }

    private fun onComplete() {
        mIndex = mData.size - 1
        mActivePlayer?.pause()
        onStateChange(PlayState.COMPLETE)
    }


    private fun onStateChange(state: PlayState) {
        mState = state
        runMain {
            mListeners.forEach {
                it.onStateChange(state)
            }
        }
    }

    private fun onStateError(error: Int, description: String) {
        onStateChange(PlayState.ERROR)
        runMain {
            mListeners.forEach {
                it.onError(error, description)
            }
        }
    }

    private fun onItemChange(item: MetaItem) {
        runMain {
            mListeners.forEach {
                it.onMediaChange(item)
            }
        }
    }

    private fun playToNextAvailable(positive: Boolean = true): Boolean {
        var range: IntProgression = mIndex + 1 until mData.size
        if (!positive) {
            range = mIndex - 1 downTo 0
        }
        for (index in range) {
            if (index !in 0 until mData.size) continue
            //寻找能处理此item的player
            val availablePlayer = mPlayers.find {
                it.canDispose(mData[index])
            }

            if (availablePlayer != null) {
                mActivePlayer?.pause()

                mIndex = index
                mActivePlayer = availablePlayer
                mActivePlayer?.play(mData[index])
                onItemChange(mData[mIndex])
                return true
            }
        }

        return false
    }

}

