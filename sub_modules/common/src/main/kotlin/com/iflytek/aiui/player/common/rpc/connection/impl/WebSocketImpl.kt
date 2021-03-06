package com.iflytek.aiui.player.common.rpc.connection.impl

import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.URI
import java.util.*

typealias ServerInitializer = (Int) -> WebSocketServer
typealias ClientInitializer = (String, Int) -> WebSocketClient


/**
 * RPC抽象连接 WebSocketServer实现
 */
class WebSocketServerConnection(private val port: Int) : DataConnection() {
    private lateinit var server: WebSocketServer
    private var serverInitializer: ServerInitializer = { port ->
        object : WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Timber.d("webSocket server on client connected")
                if (connections.size == 1) {
                    onActive()
                }
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                Timber.d("webSocket server on client disconnected")
                if (connections.isEmpty()) {
                    onDeactivate()
                }
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                onData(message)
            }

            override fun onStart() {
                this@WebSocketServerConnection.onStart()
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
                Timber.e("webSocket server on error $ex")
            }
        }
    }

    private fun initServer() {
        server = serverInitializer(port)
        server.isReuseAddr = true
    }

    override fun start() {
        initServer()
        server.start()
    }

    override fun stop() {
        server.stop()
    }

    override fun send(data: String): Boolean {
        return try {
            server.broadcast(data)
            true
        } catch (e: Exception) {
            false
        }
    }

}

/**
 * RPC抽象连接 WebSocketClient实现
 */
class WebSocketClientConnection(private val host: String, private val port: Int) : DataConnection() {
    private lateinit var client: WebSocketClient
    private var stopped = false
    private var clientInitializer: ClientInitializer = { host, port ->
        object : WebSocketClient(URI("ws://$host:$port")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                onActive()
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                onDeactivate()
                if(!stopped) {
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            reconnect()
                        }
                    }, 1000)
                }
            }

            override fun onMessage(message: String?) {
                onData(message)
            }

            override fun onError(ex: Exception?) {
                println("onError $ex")
            }
        }
    }

    private fun initClient() {
        client = clientInitializer(host, port)
    }

    override fun start() {
        initClient()
        client.connect()
    }

    override fun send(data: String): Boolean {
        return try {
            client.send(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun stop() {
        stopped = true
        client.close()
    }
}

