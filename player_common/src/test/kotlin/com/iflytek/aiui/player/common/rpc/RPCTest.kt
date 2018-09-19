package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.impl.ClientConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.ServerConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import com.iflytek.aiui.player.common.rpc.method.GetToken
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
        server = WebSocketServerConnection(port, object : ServerConnectionListener {
            override fun onStart() {
                countStartDown.countDown()
            }
        })
        server.start()
        countStartDown.await()

        val countConnectionDown = CountDownLatch(1)
        client = WebSocketClientConnection("localhost", port, object : ClientConnectionListener {
            override fun onOpen() {
                countConnectionDown.countDown()
            }
        })
        client.connect()
        countConnectionDown.await()
    }


    @Test()
    fun invoke() {
        val countTokenRecvDown = CountDownLatch(1)
        val fakeToken = "foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
            }
        })

        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = GetToken.deserializeFrom(data)
                rpc.response(req, fakeToken)
            }

        })

        serverRPC.request<String>(GetToken.forSource("qingting")) { token ->
            countTokenRecvDown.countDown()
            assertEquals(token, fakeToken)
        }

        countTokenRecvDown.await()
    }
}