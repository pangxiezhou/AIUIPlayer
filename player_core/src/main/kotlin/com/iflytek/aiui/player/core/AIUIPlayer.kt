package com.iflytek.aiui.player.core

import android.content.Context
import com.iflytek.aiui.player.core.players.*
import org.json.JSONObject

/**
 * 播放器状态
 */
enum class PlayState {
    /**
     * 未初始化状态，需要调用initialize
     */
    IDLE,
    /**
     * 就绪状态
     */
    READY,
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
     * 停止状态，调用play重新播放
     */
    STOPPED,
}

/**
 * 播放器可接受的播放项
 */
data class MetaInfo(val info: JSONObject) {
    //TODO 覆盖更多情况的title提取
    val title: String
        get() = info.optString("name")
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
    fun onMediaChange(item: MetaInfo)

    /**
     * 播放器已销毁
     */
    fun onPlayerRelease()
}

/**
 * AIUI播放器，用来解析播放AIUI平台返回可播放的信源内容列表，
 *
 * 目前支持:
 * <ul>
 *     <li>蜻蜓FM</li>
 * </ul>
 *
 * 内部根据每项内容的source字段分发到具体的播放器
 */

class AIUIPlayer(context: Context) {
    private var mPlayers = listOf(MetaQTPlayer(context), MetaMediaPlayer())
    private var mActivePlayer: MetaAbstractPlayer? = null
    private val mListeners = mutableListOf<PlayerListener>()
    //默认初始化状态
    private var mState: PlayState = PlayState.IDLE
    //播放列表内容
    private var mData = listOf<MetaInfo>()
    private var mIndex = 0
    //初始化标识，避免重复初始化
    private var mInitialized = false
    //Ready标记保存，Listener的注册可能在Ready之后，保证状态回调正常
    private var mReadyCount = 0


    //当前播放项内容
    val currentPlay: MetaInfo?
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

    /**
     * 初始化，在构造完成后立即调用
     *
     * （不放在构造函数中，是为测试用例分离需要)
     */
    fun initialize() {
        // 避免重复初始化
        if (mInitialized) return

        //初始化各子播放器
        mPlayers.forEach {
            it.initialize()
            it.addListener(object : MetaListener {
                override fun onReady() {
                    if (++mReadyCount == mPlayers.size) {
                        onStateChange(PlayState.READY)
                        mListeners.forEach { it.onPlayerReady() }
                    }
                }

                override fun onStateChange(state: MetaState) {
                    when(state) {
                        MetaState.COMPLETE -> {
                            if (!next()) {
                                onComplete()
                            }
                        }
                    }
                }

                override fun onRelease() {
                    if(--mReadyCount == 0) {
                        mListeners.forEach { it.onPlayerRelease() }
                        onStateChange(PlayState.IDLE)
                    }
                }
            })
        }

        mInitialized = true
    }

    /**
     * 播放列表内容
     *
     * @param data 播放内容的列表
     *
     * 需要在PlayerReady后调用
     */
    fun play(data: List<MetaInfo>) {
        if (data.isEmpty()) return

        mActivePlayer?.pause()
        mData = data
        mIndex = -1
        playToNextAvailable()
    }

    /**
     * 下一个
     *
     * @return 是否操作成功
     */
    fun next(): Boolean {
        return playToNextAvailable()
    }

    /**
     * 上一个
     *
     * @return 是否操作成功
     */
    fun previous(): Boolean {
        return playToNextAvailable(false)
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (mState == PlayState.PLAYING) {
            onStateChange(PlayState.PAUSED)
            mActivePlayer?.pause()
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
                playToNextAvailable(false)
            }
        }
    }

    /**
     * 播放器停止，列表清空，状态恢复到STOPPED
     */
    fun stop() {
        if (mState != PlayState.STOPPED) {
            onStateChange(PlayState.STOPPED)
            mActivePlayer?.pause()

            mData = listOf()
            mIndex = -1
        }
    }

    /**
     * 添加播放监听器
     */
    fun addListener(listener: PlayerListener) {
        mListeners.add(listener)
        if (mState != PlayState.IDLE) {
            listener.onPlayerReady()
            listener.onStateChange(PlayState.READY)
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
        stop()
        mPlayers.forEach { it.release() }
    }

    private fun onComplete() {
        mIndex = mData.size
        mActivePlayer?.pause()
        onStateChange(PlayState.COMPLETE)
    }


    private fun onStateChange(state: PlayState) {
        mState = state
        mListeners.forEach {
            it.onStateChange(state)
        }
    }

    private fun onItemChange(item: MetaInfo) {
        mListeners.forEach {
            it.onMediaChange(item)
        }
    }

    private fun playToNextAvailable(positive: Boolean = true): Boolean {
        mActivePlayer?.pause()

        var range: IntProgression = mIndex + 1 until mData.size
        if (!positive) {
            range = mIndex - 1 downTo 0
        }
        for (index in range) {
            if (index !in 0 until mData.size) continue
            //寻找能处理此item的player
            val availablePlayer = mPlayers.find {
                it.play(mData[index])
            }

            if (availablePlayer != null) {
                mIndex = index
                mActivePlayer = availablePlayer
                onItemChange(mData[mIndex])
                if (mState != PlayState.PLAYING) {
                    onStateChange(PlayState.PLAYING)
                }
                return true
            }
        }

        if (mState == PlayState.PLAYING) mActivePlayer?.resume()
        return false
    }

}
