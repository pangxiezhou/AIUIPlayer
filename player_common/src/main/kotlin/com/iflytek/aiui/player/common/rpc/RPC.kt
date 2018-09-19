package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import com.iflytek.aiui.player.common.rpc.method.GetToken
import org.json.JSONObject

typealias RPCCallback<T> = (T) -> Unit

interface RPCListener {
    fun onRequest(rpc: RPC, data: String)
}

class RPC(private val dataConnection: DataConnection, private val rpcListener: RPCListener) {
    private val callbackMap = hashMapOf<Int, RPCCallback<Any>>()

    init {
        dataConnection.registerDataCallback  { message ->
            if (message != null)  {
                val data = JSONObject(message)
                if (data.has("result")) {
                    callbackMap.get(data.optInt("id"))?.invoke(data.optString("result"))
                } else {
                    rpcListener.onRequest(this@RPC, message)
                }
            }
        }
    }

    fun <T> request(req: GetToken, callback: RPCCallback<T>) {
        callbackMap.put(req.id, callback as RPCCallback<Any>)
        dataConnection.send(req.toJSONString())
    }

    fun <T> response(req: GetToken, value: T) {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", req.id)
        description.put("result", value.toString())

        dataConnection.send(description.toString())
    }

}