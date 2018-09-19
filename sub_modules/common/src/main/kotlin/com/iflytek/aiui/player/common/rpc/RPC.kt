package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.ConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import com.iflytek.aiui.player.common.rpc.method.Request
import com.iflytek.aiui.player.common.rpc.method.Response
import org.json.JSONObject

typealias RPCCallback<T> = (T) -> Unit

interface RPCListener {
    fun onRequest(rpc: RPC, data: String)
}

/**
 * JSONRPC 远程调用接口
 */
class RPC(private val dataConnection: DataConnection, private val rpcListener: RPCListener) {
    private val callbackMap = hashMapOf<Int, RPCCallback<Any>>()
    private val sendQueue = mutableListOf<String>()

    init {
        dataConnection.registerConnectionListener(object: ConnectionListener() {
            override fun onActive() {
                sendQueue.removeAll {
                    dataConnection.send(it)
                }
            }

            override fun onData(message: String) {
                val data = JSONObject(message)
                if (data.has("result")) {
                    callbackMap[data.optInt("id")]?.invoke(data.opt("result"))
                } else {
                    rpcListener.onRequest(this@RPC, message)
                }
            }

            override fun onDeactivate() {
            }
        })
    }


    fun <T> request(request: Request, callback: RPCCallback<T>) {
        callbackMap[request.id] = callback as RPCCallback<Any>
        send(request.toJSONString())
    }

    fun <T> response(req: Request, value: T) {
        val response = Response(req.id, value!!)
        send(response.toJSONString())
    }

    private fun send(description: String) {
        println("active ${dataConnection.active} $dataConnection send $description")
        if (!dataConnection.active || dataConnection.send(description)) {
            sendQueue.add(description)
        }
    }

    fun reset() {
        sendQueue.clear()
        callbackMap.clear()
    }
}