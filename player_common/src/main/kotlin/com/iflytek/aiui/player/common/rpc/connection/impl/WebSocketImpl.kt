package com.iflytek.aiui.player.common.rpc.connection.impl

import com.iflytek.aiui.player.common.rpc.connection.DataConnection
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.*

typealias ServerInitializer = (Int) -> WebSocketServer
typealias ClientInitializer = (String, Int) -> WebSocketClient

class WebSocketServerConnection(private val port: Int) : DataConnection() {
    private lateinit var server: WebSocketServer
    private var serverInitializer: ServerInitializer = { port ->
        object : WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
//                println("server on client connected")
                if (connections.size == 1) {
                    onActive()
                }
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
//                println("server on client disconnected")
                if (connections.isEmpty()) {
                    onDeactivate()
                }
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                onData(message)
            }

            override fun onStart() {
//                println("server on Start")
                this@WebSocketServerConnection.onStart()
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
                println("server on error $ex")
            }
        }
    }

    private fun initServer() {
        server = serverInitializer(port)
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

class WebSocketClientConnection(private val host: String, private val port: Int) : DataConnection() {
    private lateinit var client: WebSocketClient
    private var stopped = false
    private var clientInitializer: ClientInitializer = { host, port ->
        object : WebSocketClient(URI("ws://$host:$port")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
//                println("on Open")
                onActive()
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
//                println("On Close")
                onDeactivate()
                if(!stopped) {
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
//                            println("start reconnect")
                            reconnect()
                        }
                    }, 100)
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

