package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.ConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import com.iflytek.aiui.player.common.rpc.method.GetToken
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RPCTest {
    private val port = 4096
    private lateinit var client: WebSocketClientConnection
    private lateinit var server: WebSocketServerConnection

    @Before
    fun setUp() {
        //Server和Client Connection建立
        val countStartDown = CountDownLatch(1)
        server = WebSocketServerConnection(port)
        server.registerConnectionListener(object: ConnectionListener() {
            override fun onStart() {
                countStartDown.countDown()
            }
        })
        server.start()
        countStartDown.await()

        val countConnectionDown = CountDownLatch(1)
        client = WebSocketClientConnection("localhost", port)
        client.registerConnectionListener(object: ConnectionListener() {
            override fun onActive() {
                countConnectionDown.countDown()
            }
        })
        client.start()
        countConnectionDown.await()
    }

    @After
    fun tearDown() {
        client.stop()
        server.stop()
    }


    @Test()
    fun rpc() {
        val fakeServerToken = "server_foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeClientToken)
            }
        })

        val countServerTokenRecvDown = CountDownLatch(1)
        serverRPC.request<String>(GetToken.forSource("qingting")) { token ->
            assertEquals(token, fakeClientToken)
            countServerTokenRecvDown.countDown()
        }

        val countClientTokenRecvDown = CountDownLatch(1)
        clientRPC.request<String>(GetToken.forSource("qingting")) { token ->
            assertEquals(token, fakeServerToken)
            countClientTokenRecvDown.countDown()
        }

        countServerTokenRecvDown.await()
        countClientTokenRecvDown.await()
    }

    @Test
    fun clientRetry() {
        val fakeServerToken = "server_foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeClientToken)
            }
        })

        server.stop()
        val countClientDeactivateDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countClientDeactivateDown.countDown()
            }
        })
        countClientDeactivateDown.await()
        server.start()

        val countClientTokenRecvDown = CountDownLatch(1)
        clientRPC.request<String>(GetToken.forSource("qingting")) { token ->
            assertEquals(token, fakeServerToken)
            countClientTokenRecvDown.countDown()
        }

        countClientTokenRecvDown.await()
    }

    @Test
    fun serverRetry() {
        val fakeServerToken = "server_foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeClientToken)
            }
        })

        client.stop()
        val countServerDeactivateDown = CountDownLatch(1)
        server.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countServerDeactivateDown.countDown()
            }
        })
        countServerDeactivateDown.await()
        client.start()

        val countServerTokenRecvDown = CountDownLatch(1)
        serverRPC.request<String>(GetToken.forSource("qingting")) { token ->
            assertEquals(token, fakeClientToken)
            countServerTokenRecvDown.countDown()
        }

        countServerTokenRecvDown.await()
    }
}