package com.iflytek.aiui.player.common.rpc.connection.impl

import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI

class WebSocketServerConnection(val port: Int, val connectionListener: ServerConnectionListener) : DataConnection() {
    private var activeConnection: WebSocket? = null
    private val server: WebSocketServer = object : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            activeConnection = conn
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            if (activeConnection == conn) activeConnection = null
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            onData(message)
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

    override fun send(data: String) {
        activeConnection?.send(data)
    }

}

class WebSocketClientConnection(host: String, port: Int, val connectionListener: ClientConnectionListener) : DataConnection() {
    private val client = object : WebSocketClient(URI("ws://$host:$port")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            connectionListener.onOpen()
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
        }

        override fun onMessage(message: String?) {
            onData(message)
        }

        override fun onError(ex: Exception?) {
        }
    }

    fun connect() {
        client.connect()
    }

    override fun send(data: String) {
        client.send(data)
    }
}

interface ServerConnectionListener {
    fun onStart()
}

interface ClientConnectionListener {
    fun onOpen()
}