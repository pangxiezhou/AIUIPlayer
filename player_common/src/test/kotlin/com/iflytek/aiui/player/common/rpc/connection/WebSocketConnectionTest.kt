package com.iflytek.aiui.player.common.rpc.connection

import com.iflytek.aiui.player.common.rpc.connection.impl.ClientConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.ServerConnectionListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

class WebSocketConnectionTest {
    private val port = 4096
    private lateinit var server: DataConnection
    private lateinit var client: DataConnection

    private fun setUp() {
        val countStartDown = CountDownLatch(1)
        server = WebSocketServerConnection(port, object : ServerConnectionListener() {
            override fun onStart() {
                countStartDown.countDown()
            }
        })
        server.start()
        countStartDown.await()

        val countConnectionDown = CountDownLatch(1)
        client = WebSocketClientConnection("localhost", port, object : ClientConnectionListener() {
            override fun onOpen() {
                countConnectionDown.countDown()
            }
        })
        client.start()
        countConnectionDown.await()
    }

    private fun tearDown() {
        client.stop()
        server.stop()
    }

    @Test(timeout = 2000)
    fun recv() {
        setUp()

        val clientContent = "client_foobar"
        val serverContent = "server_foobar"

        val countServerRecvDown = CountDownLatch(1)
        server.registerDataCallback {
            assertEquals(it, clientContent)
            countServerRecvDown.countDown()
        }

        val countClientRecvDown = CountDownLatch(1)
        client.registerDataCallback {
            assertEquals(it, serverContent)
            countClientRecvDown.countDown()
        }

        client.send(clientContent)
        server.send(serverContent)

        countServerRecvDown.await()
        countClientRecvDown.await()

        tearDown()
    }

    @Test
    fun reconnect() {
        var countStartDown = CountDownLatch(1)
        server = WebSocketServerConnection(port, object : ServerConnectionListener() {
            override fun onStart() {
                countStartDown.countDown()
            }
        })
        server.start()
        countStartDown.await()

        val countOpenDown = CountDownLatch(1)
        val countCloseDown = CountDownLatch(1)
        client = WebSocketClientConnection("localhost", port, object : ClientConnectionListener() {
            override fun onOpen() {
                countOpenDown.countDown()
            }

            override fun onClose() {
                countCloseDown.countDown()
            }
        })
        client.start()
        countOpenDown.await()

        server.stop()
        countCloseDown.await()

        val clientContent = "client_foobar"
        val countServerRecvDown = CountDownLatch(1)
        client.send(clientContent)
        server.start()
        server.registerDataCallback {
            assertEquals(it, clientContent)
            countServerRecvDown.countDown()
        }

        countServerRecvDown.await()


        client.stop()
        server.stop()
    }
}