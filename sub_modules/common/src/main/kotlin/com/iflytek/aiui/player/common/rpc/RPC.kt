package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.ConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import com.iflytek.aiui.player.common.rpc.method.Request
import org.json.JSONObject

typealias RPCCallback<T> = (T) -> Unit

interface RPCListener {
    fun onRequest(rpc: RPC, data: String)
}

class RPC(private val dataConnection: DataConnection, private val rpcListener: RPCListener) {
    private val callbackMap = hashMapOf<Int, RPCCallback<Any>>()
    private val sendQueue = mutableListOf<String>()

    init {
        dataConnection.registerConnectionListener(object: ConnectionListener() {
            override fun onActive() {
                sendQueue.removeAll {
                    dataConnection.send(it)
                    true
                }
            }

            override fun onData(message: String) {
                val data = JSONObject(message)
                if (data.has("result")) {
                    callbackMap[data.optInt("id")]?.invoke(data.optString("result"))
                } else {
                    rpcListener.onRequest(this@RPC, message)
                }
            }

            override fun onDeactivate() {
            }
        })
    }

    fun <T> request(req: Request, callback: RPCCallback<T>) {
        callbackMap[req.id] = callback as RPCCallback<Any>
        if(!dataConnection.active || dataConnection.send(req.toJSONString())) {
            sendQueue.add(req.toJSONString())
        }
    }

    fun <T> response(req: Request, value: T) {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", req.id)
        description.put("result", value.toString())

        if(!dataConnection.active || dataConnection.send(description.toString())) {
            sendQueue.add(description.toString())
        }
    }
}