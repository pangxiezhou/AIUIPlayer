package com.iflytek.aiui.player.core.players

import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCCallback
import com.iflytek.aiui.player.core.MetaInfo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MetaKGPlayerTest {
    private val mediaPlayer: MediaPlayer = mock()

    @Spy
    private val mInitializer: MediaPlayerInitializer = { callback ->
        callback(mediaPlayer)
    }
    @Mock
    private lateinit var mKuGouAPI: KuGouAPI

    private val mRPC: RPC = mock()

    @InjectMocks
    private val player = MetaKGPlayer(mRPC)

    private val fakeURL = "fake.url"
    private val fakeUserID = 112358132
    private val fakeToken = "fake.token"

    @Before
    fun setUp() {
        player.initialize()

        whenever(mKuGouAPI.login(any(), any())).thenReturn(true)
        whenever(mKuGouAPI.retriveUrl(any(), any())).thenReturn(fakeURL)

        whenever(mRPC.request<String>(any(), any())).then {
            (it.arguments[1] as RPCCallback<String>)("$fakeUserID#$fakeToken")
        }
    }

    @Test
    fun initialize() {
        verify(mKuGouAPI).init()
    }

    @Test
    fun play() {
        val itemID = "1234567889"
        player.play(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemId" to itemID
        )), "story"))

        verify(mRPC).request<String>(any(), any())
        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        verify(mKuGouAPI).retriveUrl(itemID, 0)
    }

    @Test
    fun canDispose() {
        assertTrue(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemId" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "qingting",
                "itemId" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "playUrl" to "http://fake.url/test.mp3"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemId" to ""
        )), "story")))
    }
}