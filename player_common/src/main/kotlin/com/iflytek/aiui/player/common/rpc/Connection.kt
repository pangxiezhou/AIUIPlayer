package com.iflytek.aiui.player.common.rpc

import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.URI
import java.util.*

typealias RPCCallback<T> = (T) -> Unit

interface RPCListener {
    fun onRequest(rpc: RPC, data: String)
}

interface ServerConnectionListener {
    fun onStart()
}

interface ClientConnectionListener {
    fun onOpen()
}

interface RPC {
    fun <T> request(req: GetToken, callback: RPCCallback<T>)
    fun <T> response(req: GetToken, value: T)
}

class RPCServer(val port: Int, val rpcListener: RPCListener, val connectionListener: ServerConnectionListener):RPC {
    private val callbackMap = HashMap<Int, RPCCallback<Any>>()
    private var activeConnection: WebSocket? = null
    private val server: WebSocketServer = object: WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                activeConnection = conn
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                if(activeConnection == conn) activeConnection = null
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                if(message == null) return
                val data = JSONObject(message)
                if(data.has("result")) {
                    callbackMap.get(data.optInt("id"))?.invoke(data.optString("result"))
                } else {
                    rpcListener.onRequest(this@RPCServer, message)
                }
            }

            override fun onStart() {
                connectionListener.onStart()
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
            }

        }

    fun start() {
        server.start()
    }


    override fun <T> request(req: GetToken, callback: RPCCallback<T>) {
        callbackMap.put(req.id, callback as RPCCallback<Any>)
        activeConnection?.send(req.toJSONString())
    }

    override fun <T> response(req: GetToken, value: T) {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", req.id)
        description.put("result", value.toString())

        activeConnection?.send(description.toString())
    }

}

class RPCClient(val port: Int, val rpcListener: RPCListener, val connectionListener: ClientConnectionListener):RPC {
    private val callbackMap = HashMap<Int, RPCCallback<Any>>()
    private val client = object: WebSocketClient(URI("ws://localhost:$port")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            connectionListener.onOpen()
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
        }

        override fun onMessage(message: String?) {
            if(message == null) return
            val data = JSONObject(message)
            if(data.has("result")) {
                callbackMap.get(data.optInt("id"))?.invoke(data.optString("result"))
            } else {
                rpcListener.onRequest(this@RPCClient, message)
            }
        }

        override fun onError(ex: Exception?) {
        }

    }

    fun connect() {
        client.connect()
    }

    override fun <T> request(req: GetToken, callback: RPCCallback<T>) {
        callbackMap.put(req.id, callback as RPCCallback<Any>)
        client?.send(req.toJSONString())
    }

    override fun <T> response(req: GetToken, value: T) {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", req.id)
        description.put("result", value.toString())

        client?.send(description.toString())
    }
}
