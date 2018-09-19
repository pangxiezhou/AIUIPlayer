package com.iflytek.aiui.player.common.rpc.connection


typealias DataCallback = (String?) -> Unit

abstract class DataConnection {
    private val callbacks = mutableListOf<DataCallback>()

    fun registerDataCallback(callback: DataCallback) {
       callbacks.add(callback)
    }

    protected fun onData(data: String?) {
        callbacks.forEach { it.invoke(data) }
    }

    abstract fun start()
    abstract fun stop()
    abstract fun send(data: String)
}

