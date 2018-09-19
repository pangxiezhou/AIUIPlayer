package com.iflytek.aiui.player.common.rpc.connection


abstract class ConnectionListener {
    open fun onStart() {}
    open fun onActive() {}
    open fun onData(data: String) {}
    open fun onDeactivate() {}
}

typealias DataCallback = (String?) -> Unit

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

