package com.iflytek.aiui.player.core.players

import android.media.MediaPlayer
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCCallback
import com.iflytek.aiui.player.common.rpc.RPCErrorCallback
import com.iflytek.aiui.player.common.rpc.error.ErrorDef
import com.iflytek.aiui.player.common.rpc.storage.Storage
import com.iflytek.aiui.player.core.MetaItem
import com.kugou.common.utils.KgInfo
import com.nhaarman.mockitokotlin2.*
import org.json.JSONObject
import org.junit.Assert.*
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


    private fun setUpKuGouAPIMock() {
        var hasLogin = false
        whenever(mKuGouAPI.login(any(), any())).then {
            hasLogin  = true
            true
        }
        whenever(mKuGouAPI.retrieveUrl(any(), any())).then {
            val kgInfo = KgInfo()
            if(hasLogin) {
                kgInfo.errorNo = 0
                kgInfo.Info = fakeURL
            } else {
                kgInfo.errorNo = -2
                kgInfo.Info = "token expire"
            }
            kgInfo
        }
    }

    private fun setUpStorageMock() {
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
    }

    private fun setUp() {
        setUpKuGouAPIMock()
        setUpStorageMock()

        whenever(mRPC.request<String>(any(), any(), any(), any())).then {
            (it.arguments[1] as RPCCallback<String>)("$fakeUserID#$fakeToken")
        }


        player.initialize()
    }


    @Test
    fun initialize() {
        setUp()

        verify(mKuGouAPI).init()
    }

    @Test
    fun canDispose() {
        setUp()

        assertTrue(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingting",
                "itemid" to "1234567889"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "playUrl" to "http://fake.url/test.mp3"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to ""
        )), "story")))
    }

    @Test
    fun play() {
        setUp()

        val itemID = "1234567889"
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemID
        )), "story"))

        verify(mRPC).request<String>(any(), any(), any(), any())
        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        verify(mKuGouAPI, times(2)).retrieveUrl(itemID, 0)
    }

    @Test
    fun rpcError() {
        setUp()

        val listener = mock<MetaListener>()
        player.addListener(listener)

        var errorCallback: RPCErrorCallback? = null
        whenever(mRPC.request<String>(any(), any(), any(), any())).then { it ->
            //只存储，不立即回调
            errorCallback = it.arguments[2] as RPCErrorCallback
            true
        }

        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        errorCallback?.invoke(ErrorDef.ERROR_RPC_RESET, "rpc peer reset")

        verify(listener).onStateChange(MetaState.ERROR)
        verify(listener).onError(ErrorDef.ERROR_RPC_RESET, "rpc peer reset")
        verify(mKuGouAPI, never()).login(fakeUserID, fakeToken)
        verify(mRPC).request<String>(any(), any(), any(), any())
    }


    @Test
    fun tokenCache() {
        setUp()

        var hasLogin = false
        whenever(mKuGouAPI.login(any(), any())).then {
            hasLogin  = true
            true
        }
        whenever(mKuGouAPI.retrieveUrl(any(), any())).then {
            val kgInfo = KgInfo()
            if(hasLogin) {
                kgInfo.errorNo = 0
                kgInfo.Info = fakeURL
            } else {
                kgInfo.errorNo = -2
                kgInfo.Info = "token expire"
            }
            kgInfo
        }

        val itemId = "112358132134"
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemId
        )), "story"))

        verify(mRPC).request<String>(any(), any(), any(), any())
        verify(mKuGouAPI).login(fakeUserID, fakeToken)

        clearInvocations(mRPC, mKuGouAPI)
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemId
        )), "story"))

        verify(mKuGouAPI).retrieveUrl(itemId, 0)
        verify(mKuGouAPI, never()).login(fakeUserID, fakeToken)
        verify(mRPC, never()).request<String>(any(), any(), any(), any())

        clearInvocations(mRPC, mKuGouAPI)
        hasLogin = false
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemId
        )), "story"))

        verify(mKuGouAPI, times(2)).retrieveUrl(itemId, 0)
        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        //使用本地存储的token缓存
        verify(mRPC, never()).request<String>(any(), any(), any(), any())

        clearInvocations(mRPC, mKuGouAPI)
        hasLogin = false
        storageMap.clear()
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to itemId
        )), "story"))

        verify(mKuGouAPI, times(2)).retrieveUrl(itemId, 0)
        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        //本地存储token清空，重新rpc请求
        verify(mRPC).request<String>(any(), any(), any(), any())

    }

    @Test
    fun onlyOneActiveRequest() {
        setUp()

        var errorCallback: RPCErrorCallback? = null
        whenever(mRPC.request<String>(any(), any(), any(), any())).then { it ->
            //只存储，不立即回调
            errorCallback = it.arguments[2] as RPCErrorCallback
            true
        }

        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        verify(mKuGouAPI, never()).login(fakeUserID, fakeToken)
        verify(mRPC).request<String>(any(), any(), any(), any())


        clearInvocations(mRPC, mKuGouAPI)
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        verify(mRPC, never()).request<String>(any(), any(), any(), any())
        verify(mKuGouAPI, never()).login(fakeUserID, fakeToken)

        clearInvocations(mRPC, mKuGouAPI)
        errorCallback?.invoke(ErrorDef.ERROR_RPC_RESET, "rpc peer reset")
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to "112358132134"
        )), "story"))

        verify(mRPC).request<String>(any(), any(), any(), any())
    }

    @Test
    fun tokenOnCurrent() {
        setUp()

        var callback: RPCCallback<String>? = null
        whenever(mRPC.request<String>(any(), any(), any(), any())).then { it ->
            //只存储，不立即回调
            callback = it.arguments[1] as RPCCallback<String>
            true
        }

        val firstItem = "112358132134"
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to firstItem
        )), "story"))

        val secondItem = "fake second hash"
        player.play(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "itemid" to secondItem
        )), "story"))

        clearInvocations(mKuGouAPI)
        callback?.invoke("$fakeUserID#$fakeToken")

        verify(mKuGouAPI).login(fakeUserID, fakeToken)
        verify(mKuGouAPI).retrieveUrl(secondItem, 0)
    }
}