package com.iflytek.aiui.player.common.rpc.connection


/**
 * RPC连接监听器
 */
abstract class ConnectionListener {
    /**
     * 连接启动
     */
    open fun onStart() {}

    /**
     * 连接成功
     */
    open fun onActive() {}

    /**
     * 连接数据回调
     */
    open fun onData(data: String) {}

    /**
     * 连接断开
     */
    open fun onDeactivate() {}
}

/**
 * RPC 抽象连接
 */
abstract class DataConnection {
    private var mActive = false
    private val mListeners = mutableListOf<ConnectionListener>()

    val active
        get() = mActive

    fun registerConnectionListener(listener: ConnectionListener) {
        mListeners.add(listener)
        if (mActive) {
            listener.onActive()
        } else {
            listener.onDeactivate()
        }
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        mListeners.remove(listener)
    }

    protected fun onStart() {
        mListeners.forEach { it.onStart() }
    }

    protected fun onActive() {
        mActive = true
        mListeners.forEach { it.onActive() }
    }

    protected fun onData(data: String?) {
        if (data != null) {
            mListeners.forEach { it.onData(data) }
        }
    }

    protected fun onDeactivate() {
        mActive = false
        mListeners.forEach { it.onDeactivate() }
    }

    abstract fun start()
    abstract fun stop()
    abstract fun send(data: String): Boolean
}

