package com.iflytek.aiui.player.common.rpc.connection

import com.iflytek.aiui.player.common.rpc.connection.impl.ClientInitializer
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketServerConnection
import com.iflytek.aiui.player.common.rpc.connection.impl.ServerInitializer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.java_websocket.client.WebSocketClient
import org.java_websocket.server.WebSocketServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class WebSocketConnectionTest {
    private val port = 4096

    private val server = WebSocketServerConnection(port)
    private val client = WebSocketClientConnection("localhost", port)


    @Before
    fun setUp() {
        val countStartDown = CountDownLatch(1)
        server.registerConnectionListener(object: ConnectionListener() {
            override fun onStart() {
                countStartDown.countDown()
            }
        })
        server.start()
        countStartDown.await()

        val countConnectionDown = CountDownLatch(1)
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
    fun start() {
        assertTrue(server.active)
        assertTrue(client.active)
    }

    @Test(timeout = 2000)
    fun clientClose() {
        client.stop()

        val countClientDeActivateDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countClientDeActivateDown.countDown()
            }
        })

        val countServerDeactivateDown = CountDownLatch(1)
        server.registerConnectionListener(object: ConnectionListener() {
            override fun onDeactivate() {
                countServerDeactivateDown.countDown()
            }
        })

        countClientDeActivateDown.await()
        countServerDeactivateDown.await()

        assertFalse(server.active)
        assertFalse(client.active)
    }

    @Test(timeout = 2000)
    fun serverClose() {
        server.stop()

        val countClientDeActivateDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countClientDeActivateDown.countDown()
            }
        })

        val countServerDeactivateDown = CountDownLatch(1)
        server.registerConnectionListener(object: ConnectionListener() {
            override fun onDeactivate() {
                countServerDeactivateDown.countDown()
            }
        })

        countClientDeActivateDown.await()
        countServerDeactivateDown.await()

        assertFalse(server.active)
        assertFalse(client.active)
    }

    @Test(timeout = 2000)
    fun communicate() {
        val clientContent = "client_foobar"
        val serverContent = "server_foobar"

        val countServerRecvDown = CountDownLatch(1)
        server.registerConnectionListener(object: ConnectionListener() {
            override fun onData(data: String) {
                assertEquals(data, clientContent)
                countServerRecvDown.countDown()
            }
        })

        val countClientRecvDown = CountDownLatch(1)
        client.registerConnectionListener(object: ConnectionListener() {
            override fun onData(data: String) {
                assertEquals(data, serverContent)
                countClientRecvDown.countDown()
            }
        })

        client.send(clientContent)
        server.send(serverContent)

        countServerRecvDown.await()
        countClientRecvDown.await()

        tearDown()
    }

    @Test(timeout = 2000)
    fun reconnect() {
        server.stop()
        val countClientDeactivateDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countClientDeactivateDown.countDown()
            }
        })
        countClientDeactivateDown.await()
        assertFalse(server.active)
        assertFalse(client.active)

        server.start()
        val countClientActiveDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onActive() {
                countClientActiveDown.countDown()
            }
        })
        countClientActiveDown.await()
        assertTrue(server.active)
        assertTrue(client.active)
    }

    @Ignore
    @Test(timeout = 2000)
    fun noReconnectWhenStop() {
        client.stop()

        val countClientDeactivateDown = CountDownLatch(1)
        client.registerConnectionListener(object : ConnectionListener() {
            override fun onDeactivate() {
                countClientDeactivateDown.countDown()
            }
        })
        countClientDeactivateDown.await()
    }
}