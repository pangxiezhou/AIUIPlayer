package com.iflytek.aiui.player.common.player

import android.content.Context
import android.text.TextUtils
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.storage.Storage
import org.json.JSONObject

/**
 * 播放器可接受的播放项
 */
data class MetaItem(val info: JSONObject, val service: String) {
    val source: String = info.optString("source")
    val title: String
    val author: String
    val url: String

    init {
        val contentType = info.optInt("type", -1)
        when (contentType) {
            1 -> {
                url = info.optString("url")
                var tempTitle = info.optString("title")
                if (TextUtils.isEmpty(tempTitle)) {
                    tempTitle = info.optString("name")
                }
                title = tempTitle
                author = ""
            }

            -1 -> {
                when (service) {
                    "joke" -> {
                        title = info.optString("title")
                        url = info.optString("mp3Url")
                        author = ""
                    }

                    "radio" -> {
                        title = info.optString("name")
                        url = info.optString("url")
                        author = ""
                    }

                    "musicX" -> {
                        title = info.optString("songname")
                        url = info.optString("audiopath")
                        val singers = mutableListOf<String>()
                        val singersJSONArray = info.optJSONArray("singernames")
                        for (i in 0 until singersJSONArray.length()) {
                            singers.add(singersJSONArray.optString(i))
                        }
                        author = singers.joinToString(",")
                    }

                    "story" -> {
                        title = info.optString("name")
                        url = info.optString("playUrl")
                        author = ""
                    }

                    else -> {
                        title = ""
                        author = ""
                        url = ""
                    }
                }

            }

            else -> {
                title = ""
                author = ""
                url = ""
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        return if (other is MetaItem) {
            info == other.info && service == other.service
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return info.hashCode() + service.hashCode();
    }
}


enum class MetaState {
    /**
     * 空闲状态，未初始化
     */
    IDLE,
    /**
     * 初始化完成
     */
    READY,
    /**
     * 缓冲状态
     */
    LOADING,
    /**
     * 正在播放
     */
    PLAYING,
    /**
     * 暂停
     */
    PAUSED,
    /**
     * 播放完成
     */
    COMPLETE,
    /**
     * 错误
     */
    ERROR,
}

/**
 * Meta播放器监听器
 */
interface MetaListener {
    /**
     * 初始化完成
     */
    fun onReady()

    /**
     * 播放状态变化
     */
    fun onStateChange(state: MetaState)

    /**
     * 错误回调
     */
    fun onError(error: Int, description: String)

    /**
     * 销毁
     */
    fun onRelease()
}

/**
 * Meta抽象播放器
 */
abstract class MetaAbstractPlayer(protected val context: Context, protected val rpc: RPC, protected val storage: Storage) {
    private var mListeners = mutableListOf<MetaListener>()
    private var mInitialized = false
    private var mState = MetaState.IDLE
//    private var mReady = false

    /**
     * 初始化，在使用前调用
     *
     * 初始化的结果通过MetaListener的onReady回调
     */
    open fun initialize() {
        if (mInitialized) return
        mInitialized = true
    }

    /**
     * 添加监听器, 可在任何时刻调用
     *
     */
    fun addListener(listener: MetaListener) {
        mListeners.add(listener)
        if (mState != MetaState.IDLE) listener.onReady()
    }

    abstract fun canDispose(item: MetaItem): Boolean
    /**
     * 播放单项内容
     */
    abstract fun play(item: MetaItem)

    /**
     * 暂停播放
     */
    abstract fun pause()

    /**
     * 继续播放
     */
    abstract fun resume()

    /**
     * 当前播放项时长
     * (直播流返回 -1）
     * @return 播放项时长（单位：毫秒 milliseconds）
     */
    abstract fun getDuration(): Long

    /**
     * 当前播放进度
     * @return 播放项进度信息（单位：毫秒 milliseconds）
     */
    abstract fun getCurrentPos(): Long

    /**
     * 播放位置选择
     */
    abstract fun seekTo(msec: Long)

    /**
     * 销毁播放器，进入IDLE状态，需要重新调用initialize接口
     */
    open fun release() {
        mInitialized = false
    }

    protected fun stateChange(state: MetaState) {
        mState = state
        when (mState) {
            MetaState.READY -> {
                mListeners.forEach { it.onReady() }
            }
            MetaState.IDLE -> {
                mListeners.forEach { it.onRelease() }
            }

            else -> {}
        }
        mListeners.forEach { it.onStateChange(mState) }
    }

    protected fun onError(error: Int, description: String) {
        stateChange(MetaState.ERROR)
        mListeners.forEach { it.onError(error, description) }
    }

    protected fun state(): MetaState {
        return mState
    }
}


