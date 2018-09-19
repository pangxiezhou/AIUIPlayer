package com.iflytek.aiui.player.common.rpc.connection.impl

import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.*

abstract class ServerConnectionListener {
    open fun onStart() {

    }
}

abstract class ClientConnectionListener {
    open fun onOpen() {

    }

    open fun onClose() {

    }
}

class WebSocketServerConnection(private val port: Int, val connectionListener: ServerConnectionListener) : DataConnection() {
    private lateinit var server: WebSocketServer

    private fun initServer() {
        server = object : WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                println("server on client connected")
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                println("server on client disconnected")
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                onData(message)
            }

            override fun onStart() {
                println("server on Start")
                connectionListener.onStart()
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
                println("server on error $ex")
            }
        }
    }

    override fun start() {
        initServer()
        server.start()
    }

    override fun stop() {
        server.stop()
    }

    override fun send(data: String) {
        server.broadcast(data)
    }

}

class WebSocketClientConnection(private val host: String, private val port: Int, val connectionListener: ClientConnectionListener) : DataConnection() {
    private val sendQueue = mutableListOf<String>()
    private lateinit var client: WebSocketClient

    private fun initClient() {
        client = object : WebSocketClient(URI("ws://$host:$port")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            println("on Open")
            connectionListener.onOpen()
            if (!sendQueue.isEmpty()) {
                sendQueue.removeAll {
                    send(it)
                    true
                }
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("On Close")
            connectionListener.onClose()
            Timer().schedule(object: TimerTask() {
                override fun run() {
                    reconnect()
                }
            }, 100)
            println("start reconnect")
        }

        override fun onMessage(message: String?) {
            onData(message)
        }

        override fun onError(ex: Exception?) {
            println("onError $ex")
        }
    }
    }

    override fun start() {
        initClient()
        client.connect()
    }

    override fun send(data: String) {
        if (!client.isOpen) {
            sendQueue.add(data)
        } else {
            client.send(data)
        }
    }

    override fun stop() {
        client.close()
    }
}

