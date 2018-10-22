package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.ConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import com.iflytek.aiui.player.common.error.ErrorDef
import com.iflytek.aiui.player.common.rpc.method.Request
import com.iflytek.aiui.player.common.rpc.method.Response
import com.iflytek.aiui.player.common.rpc.method.Error
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

typealias RPCCallback<T> = (T) -> Unit
typealias RPCErrorCallback = (Int, String) -> Unit

interface RPCListener {
    fun onRequest(rpc: RPC, data: String)
}

data class RPCCall<T>(var timeout: Int, val request: Request, val callback: RPCCallback<T>, val errorCallback: RPCErrorCallback?)

/**
 * JSONRPC 远程调用接口
 */
class RPC(private val dataConnection: DataConnection, private val rpcListener: RPCListener) {
    private val calls = mutableListOf<RPCCall<Any>>()
    private val sendQueue = mutableListOf<String>()
    private val pendingRequest = mutableListOf<Int>()

    init {
        Timer().scheduleAtFixedRate(0, 100) {
            calls.removeAll {
                it.timeout -= 100
                if(it.timeout < 0) {
                    it.errorCallback?.invoke(ErrorDef.ERROR_RPC_TIMEOUT, "rpc timeout")
                    true
                } else {
                    false
                }
            }
        }

        dataConnection.registerConnectionListener(object : ConnectionListener() {
            override fun onActive() {
                sendQueue.removeAll {
                    dataConnection.send(it)
                }
            }

            override fun onData(message: String) {
                val data = JSONObject(message)
                when {
                    data.has("error") -> {
                        val error = data.optJSONObject("error")
                        calls.find {
                            it.request.id == data.optInt("id")
                        }?.errorCallback?.invoke(error.optInt("code"), error.optString("message"))
                    }
                    data.has("result") -> {
                        calls.find {
                            it.request.id == data.optInt("id")
                        }?.callback?.invoke(data.opt("result"))
                    }
                    else -> {
                        val req = JSONObject(message)
                        pendingRequest.add(req.optInt("id"))
                        rpcListener.onRequest(this@RPC, message)
                    }
                }
            }

            override fun onDeactivate() {
            }
        })
    }

    fun <T> request(request: Request, callback: RPCCallback<T>, timeout: Int = 10 * 60 * 1000) {
        request(request, callback, { _, _ -> }, timeout)
    }

    fun <T> request(request: Request,  callback: RPCCallback<T>, error: RPCErrorCallback, timeout: Int = 10 * 60 * 1000) {
        calls.add(RPCCall(timeout, request, callback, error) as RPCCall<Any>)
        send(request.toJSONString())
    }

    fun <T> response(req: Request, value: T) {
        val response = Response(req.id, value!!)
        send(response.toJSONString())
        pendingRequest.remove(req.id)
    }

    private fun send(description: String) {
        if (!dataConnection.active || dataConnection.send(description)) {
            sendQueue.add(description)
        }
    }

    fun reset() {
        sendQueue.clear()
        calls.removeAll {
            it.errorCallback?.invoke(ErrorDef.ERROR_RPC_TIMEOUT, "rpc reset")
            true
        }

        pendingRequest.removeAll {
            send(Error(it, ErrorDef.ERROR_RPC_RESET, "rpc peer reset").toJSONString())
            true
        }
    }
}