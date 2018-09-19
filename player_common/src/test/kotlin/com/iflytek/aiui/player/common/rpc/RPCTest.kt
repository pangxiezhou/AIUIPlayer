package com.iflytek.aiui.player.common.rpc

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RPCTest {
    @Test()
    fun invoke() {
        val port = 4096
        val countTokenRecvDown = CountDownLatch(1)
        var server: RPCServer? = null
        server = RPCServer(port, object : RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
            }
        }, object : ServerConnectionListener {
            override fun onStart() {
                val fakeToken = "foobar"
                val client = RPCClient(port, object : RPCListener {
                    override fun onRequest(rpc: RPC, data: String) {
                        val req = GetToken.deserializeFrom(data)
                        rpc.response(req, fakeToken)
                    }
                }, object : ClientConnectionListener {
                    override fun onOpen() {
                        val req = GetToken.forSource("qingting")
                        server?.request<String>(req) {
                            assertEquals(it, fakeToken)
                            countTokenRecvDown.countDown()
                        }
                    }
                })
                client.connect()
            }
        })
        server.start()

        countTokenRecvDown.await()
    }
}