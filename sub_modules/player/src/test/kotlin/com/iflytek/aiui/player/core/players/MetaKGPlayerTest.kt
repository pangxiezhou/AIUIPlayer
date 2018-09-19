package com.iflytek.aiui.player.core.players

import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCCallback
import com.iflytek.aiui.player.common.rpc.storage.Storage
import com.iflytek.aiui.player.core.MetaInfo
import com.nhaarman.mockitokotlin2.*
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
    private val storageMap = hashMapOf<String, Any>()
    private val storage: Storage = mock()

    @InjectMocks
    private val player = MetaKGPlayer(mRPC, storage)

    private val fakeURL = "fake.url"
    private val fakeUserID = 112358132
    private val fakeToken = "fake.token"

    @Before
    fun setUp() {
        whenever(mKuGouAPI.login(any(), any())).thenReturn(true)
        whenever(mKuGouAPI.retrieveUrl(any(), any())).thenReturn(fakeURL)

        whenever(mRPC.request<String>(any(), any())).then {
            (it.arguments[1] as RPCCallback<String>)("$fakeUserID#$fakeToken")
        }

        whenever(storage.put(any(), any<String>())).then {
            storageMap.put(it.arguments[0] as String, it.arguments[1])
        }

        whenever(storage.put(any(), any<Int>())).then {
            storageMap.put(it.arguments[0] as String, it.arguments[1])
        }

        whenever(storage.getString(any())).then {
            storageMap[it.arguments[0]] ?: ""
        }

        whenever(storage.getInteger(any())).then {
            storageMap[it.arguments[0]] ?: 0
        }

        player.initialize()
    }

    @Test
    fun initialize() {
        verify(mKuGouAPI).init()
    }

    @Test
    fun canDispose() {
        assertTrue(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "qingting",
                "itemid" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "playUrl" to "http://fake.url/test.mp3"
        )), "story")))

        assertFalse(player.canDispose(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to ""
        )), "story")))
    }

    @Test
    fun play() {
        val itemID = "1234567889"
        player.play(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemID
        )), "story"))

        verify(mRPC).request<String>(any(), any())
        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        verify(mKuGouAPI).retrieveUrl(itemID, 0)
    }

    @Test
    fun tokenCache() {
        player.play(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        verify(mRPC).request<String>(any(), any())
        verify(mKuGouAPI).login(fakeUserID, fakeToken)

        clearInvocations(mRPC)
        player.play(MetaInfo(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        verify(mRPC, never()).request<String>(any(), any())
        //一次缓存校验 一次用于rpc后登录
        verify(mKuGouAPI, times(2)).login(fakeUserID, fakeToken)
    }

}