package com.iflytek.aiui.player.core.players

import android.support.annotation.CallSuper
import com.iflytek.aiui.player.core.MetaInfo


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
     * 销毁
     */
    fun onRelease()
}

/**
 * Meta抽象播放器
 */
abstract class MetaAbstractPlayer {
    private var mListeners = mutableListOf<MetaListener>()
    private var mInitialized = false
    private var mState = MetaState.IDLE
//    private var mReady = false

    /**
     * 初始化，在使用前调用
     *
     * 初始化的结果通过MetaListener的onReady回调
     */
    @CallSuper
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

    /**
     * 播放单项内容
     */
    abstract fun play(info: MetaInfo): Boolean

    /**
     * 暂停播放
     */
    abstract fun pause()

    /**
     * 继续播放
     */
    abstract fun resume()

    /**
     * 销毁播放器，进入IDLE状态，需要重新调用initialize接口
     */
    @CallSuper
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
        }
        mListeners.forEach { it.onStateChange(mState) }
    }
}

