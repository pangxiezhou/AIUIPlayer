package com.iflytek.aiui.player.common.rpc

import com.iflytek.aiui.player.common.rpc.connection.ConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import com.iflytek.aiui.player.common.rpc.error.ErrorDef
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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


    @Test(timeout = 2000)
    fun rpc() {
        val fakeServerToken = "server_foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
                rpc.response(req, fakeClientToken)
            }
        })

        val countServerTokenRecvDown = CountDownLatch(1)
        serverRPC.request<String>(TokenReq.createFor(SourceType.QingTing), { token ->
            assertEquals(token, fakeClientToken)
            countServerTokenRecvDown.countDown()
        })

        val countClientTokenRecvDown = CountDownLatch(1)
        clientRPC.request<String>(TokenReq.createFor(SourceType.QingTing), { token ->
            assertEquals(token, fakeServerToken)
            countClientTokenRecvDown.countDown()
        })

        countServerTokenRecvDown.await()
        countClientTokenRecvDown.await()
    }

    @Test()
    fun clientRetry() {
        val fakeServerToken = "server_foobar"
        RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
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

        //after server restart
        val countClientTokenRecvDown = CountDownLatch(1)
        clientRPC.request<String>(TokenReq.createFor(SourceType.QingTing), { token ->
            assertEquals(token, fakeServerToken)
            countClientTokenRecvDown.countDown()
        })

        countClientTokenRecvDown.await()
    }

    @Test(timeout = 2000)
    fun serverRetry() {
        val fakeServerToken = "server_foobar"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
                rpc.response(req, fakeServerToken)
            }
        })

        val fakeClientToken = "client_foobar"
        RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = TokenReq.createFromJSON(data)
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

        //after client restart
        val countServerTokenRecvDown = CountDownLatch(1)
        serverRPC.request<String>(TokenReq.createFor(SourceType.QingTing), { token ->
            assertEquals(token, fakeClientToken)
            countServerTokenRecvDown.countDown()
        })

        countServerTokenRecvDown.await()
    }

    @Test(timeout = 2000)
    fun reset() {
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
            }
        })

        val fakeResult = "fake_result"
        val serverRPC = RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                clientRPC.reset()
                rpc.response(TokenReq.createFromJSON(data), fakeResult)
            }
        })

        val clientRecvDown = CountDownLatch(1)
        //client rpc request never callback after reset
        clientRPC.request<String>(TokenReq.createFor(SourceType.QingTing), {
            clientRecvDown.countDown()
        })

        assertFalse(clientRecvDown.await(1, TimeUnit.SECONDS))
        serverRPC.request<String>(TokenReq.createFor(SourceType.QingTing), {})
//        verify(server, never()).send(any())
    }

    @Test(timeout = 2000)
    fun error() {
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
            }
        })

        RPC(server, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                //server rpc reset
                rpc.reset()
            }
        })

        val countErrorAfterResetDown = CountDownLatch(1)
        clientRPC.request<String>(TokenReq.createFor(SourceType.QingTing), {

        }, { error, _ ->
            assertEquals(error, ErrorDef.ERROR_RPC_RESET)
            countErrorAfterResetDown.countDown()
        })

        countErrorAfterResetDown.await()
    }

    @Test(timeout = 2000)
    fun timeout() {
        val clientRPC = RPC(client, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
            }
        })

        val countErrorAfterResetDown = CountDownLatch(1)
        clientRPC.request<String>(TokenReq.createFor(SourceType.QingTing), { }, { error, _ ->
            assertEquals(error, ErrorDef.ERROR_RPC_TIMEOUT)
            countErrorAfterResetDown.countDown()
        }, 1000)
        countErrorAfterResetDown.await()
    }
}