package com.iflytek.aiui.player.core

import android.content.Context
import android.os.Handler
import com.iflytek.aiui.player.common.error.ErrorDef
import com.iflytek.aiui.player.common.player.*
import com.nhaarman.mockitokotlin2.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class AIUIPlayerTest {
    private val serviceStory = "story"

    private lateinit var qtPlayerListener: MetaListener
    private lateinit var mediaPlayerListener: MetaListener
    private val qtPlayer: MetaAbstractPlayer = mock()
    private val mediaPlayer: MetaAbstractPlayer = mock()

    @Mock
    private lateinit var mainHandler: Handler
    private val mockContext = mock<Context>()

    @Mock
    private lateinit var listener: PlayerListener

    @Spy
    private val mPlayers: List<MetaAbstractPlayer> = listOf(qtPlayer, mediaPlayer)

    @InjectMocks
    private val player = AIUIPlayer(mockContext)

    @Before
    fun setUp() {
        whenever(mainHandler.post(any())).then {
            (it.arguments[0] as Runnable).run()
            null
        }
    }

    private val data = JSONArray(listOf(
            JSONObject(hashMapOf(
                    "source" to "qingtingfm",
                    "name" to "foo",
                    "resourceId" to "123,456"
            )),
            JSONObject(hashMapOf(
                    "name" to "bar",
                    "playUrl" to "http://fake.url/test.mp3"
            )),
            JSONObject(hashMapOf(
                    "source" to "qingtingfm",
                    "name" to "foobar",
                    "resourceId" to "123,456"
            ))))

    private val invalidData = JSONArray(listOf(
            JSONObject(hashMapOf(
                    "source" to "unable canDispose",
                    "name" to "foo",
                    "resourceId" to "123,456"
            )),
            JSONObject(hashMapOf(
                    "name" to "bar",
                    "mp3Url" to "http://fake.url/test.mp3"
            ))))

    private val mixData = JSONArray(listOf(
            JSONObject(hashMapOf(
                    "source" to "UnReg type",
                    "name" to "first",
                    "resourceId" to "123,456"
            )),
            JSONObject(hashMapOf(
                    "source" to "qingtingfm",
                    "name" to "foo",
                    "resourceId" to "123,456"
            )),
            JSONObject(hashMapOf(
                    "source" to "UnReg type",
                    "name" to "foobar",
                    "resourceId" to "123,456"
            )),
            JSONObject(hashMapOf(
                    "name" to "bar",
                    "playUrl" to "http://fake.url/test.mp3"
            )))
    )

    private fun allPlayerReady() {
        whenever(qtPlayer.addListener(any())).then {
            qtPlayerListener = it.getArgument<MetaListener>(0)
            qtPlayerListener.onReady()
        }

        whenever(mediaPlayer.addListener(any())).then {
            mediaPlayerListener = it.getArgument<MetaListener>(0)
            mediaPlayerListener.onReady()
        }
    }

    private fun notAllPlayerReady() {
        whenever(qtPlayer.addListener(any())).then {
            it.getArgument<MetaListener>(0).onReady()
        }
    }


    @Test
    fun initializeNotAllReady() {
        notAllPlayerReady()

        player.addListener(listener)
        player.initialize()

        verify(listener, never()).onPlayerReady()
        verify(listener).onStateChange(PlayState.INITIALIZING)
        verify(listener, never()).onStateChange(PlayState.READY)
        assertEquals(player.currentState, PlayState.INITIALIZING)
    }

    @Test
    fun initializeAllReady() {
        allPlayerReady()

        player.addListener(listener)
        player.initialize()

        verify(listener).onPlayerReady()
        verify(listener).onStateChange(PlayState.INITIALIZING)
        verify(listener).onStateChange(PlayState.READY)
        assertNull(player.currentPlay)
        assertEquals(player.currentState, PlayState.READY)
    }


    private fun before(autoPlaying: Boolean = true) {
        whenever(qtPlayer.canDispose(any())).then {
            val canDispose = it.getArgument<MetaItem>(0).source== "qingtingfm"
            canDispose
        }

        whenever(qtPlayer.play(any())).then {
            val canDispose = it.getArgument<MetaItem>(0).source== "qingtingfm"
            if(autoPlaying && canDispose) {
                qtPlayerListener.onStateChange(MetaState.LOADING)
                qtPlayerListener.onStateChange(MetaState.PLAYING)
            }
            canDispose
        }


        whenever(mediaPlayer.canDispose(any())).then {
            val canDispose = !it.getArgument<MetaItem>(0).url.isEmpty()
            canDispose
        }

        whenever(mediaPlayer.play(any())).then {
            val canDispose = !it.getArgument<MetaItem>(0).url.isEmpty()
            if(autoPlaying && canDispose) {
                mediaPlayerListener.onStateChange(MetaState.LOADING)
                mediaPlayerListener.onStateChange(MetaState.PLAYING)
            }
            canDispose
        }


        allPlayerReady()

        player.addListener(listener)
        player.initialize()
    }


    @Test
    fun playMixType() {
        before(false)
        player.play(data, serviceStory)

        val firstItem = constructMetaInfo(data, 0)
        verify(qtPlayer).play(firstItem)
        assertEquals(player.currentPlay, firstItem)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        assertEquals(player.currentState, PlayState.LOADING)

        qtPlayerListener.onStateChange(MetaState.PLAYING)
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun playEmptyList() {
        before()

        player.play(JSONArray(), serviceStory)

        assertNull(player.currentPlay)
        assertEquals(player.currentState, PlayState.READY)
    }

    @Test
    fun playFalse() {
        before()

        assertTrue(player.play(data, serviceStory))
        //非READY状态 调用play false
        assertFalse(player.play(data, serviceStory))
        assertEquals(player.currentState, PlayState.PLAYING)
        assertEquals(player.currentPlay, constructMetaInfo(data, 0))

        //reset，可调用play
        player.reset()
        assertTrue(player.play(data, serviceStory))
        assertEquals(player.currentState, PlayState.PLAYING)
        assertEquals(player.currentPlay, constructMetaInfo(data, 0))

        //没有可播放项时 调用play false
        player.reset()
        assertFalse(player.play(invalidData, serviceStory))
        assertEquals(player.currentState, PlayState.READY)
        assertNull(player.currentPlay)

        player.reset()
        assertFalse(player.play(JSONArray(), serviceStory))
        assertEquals(player.currentState, PlayState.READY)
        assertNull(player.currentPlay)
    }

    @Test
    fun canPlay() {
        before()

        assertTrue(player.anyAvailablePlay(data, serviceStory))
        assertTrue(player.anyAvailablePlay(mixData, serviceStory))
        assertFalse(player.anyAvailablePlay(invalidData, serviceStory))
    }


    @Test
    fun next() {
        before()

        player.play(data, serviceStory)
        verify(listener).onStateChange(PlayState.PLAYING)

        val expectSecondItem = constructMetaInfo(data, 1)
        clearInvocations(listener, qtPlayer, mediaPlayer)
        assertTrue(player.next())
        verify(qtPlayer).pause()
        verify(mediaPlayer).play(expectSecondItem)
        verify(listener).onMediaChange(expectSecondItem)
        verify(listener).onStateChange(PlayState.LOADING)
        verify(listener).onStateChange(PlayState.PLAYING)
        assertEquals(player.currentPlay, expectSecondItem)
        assertEquals(player.currentState, PlayState.PLAYING)

        val expectThirdItem = constructMetaInfo(data, 2)
        clearInvocations(listener, qtPlayer, mediaPlayer)
        assertTrue(player.next())
        verify(qtPlayer).play(expectThirdItem)
        verify(listener).onMediaChange(expectThirdItem)
        verify(listener).onStateChange(PlayState.LOADING)
        verify(listener).onStateChange(PlayState.PLAYING)
        assertEquals(player.currentPlay, expectThirdItem)
        assertEquals(player.currentState, PlayState.PLAYING)

        //最后一项
        assertFalse(player.next())
        clearInvocations(listener)
        clearInvocations(qtPlayer)
        verify(qtPlayer, never()).play(expectThirdItem)
        verify(listener, never()).onMediaChange(expectThirdItem)
        verify(listener, never()).onStateChange(any())
        assertEquals(player.currentPlay, expectThirdItem)
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun previous() {
        before()

        player.play(data, serviceStory)


        //位于第一首，不能上一首操作
        val expectFirstItem = constructMetaInfo(data, 0)
        clearInvocations(listener, qtPlayer, mediaPlayer)
        assertFalse(player.previous())
        verify(qtPlayer, never()).play(expectFirstItem)
        verify(listener, never()).onMediaChange(expectFirstItem)
        verify(listener, never()).onStateChange(any())
        assertEquals(player.currentPlay, expectFirstItem)
        assertEquals(player.currentState, PlayState.PLAYING)

        player.next()
        player.next()

        val expectSecondItem = constructMetaInfo(data, 1)
        clearInvocations(listener, qtPlayer, mediaPlayer)
        assertTrue(player.previous())
        verify(mediaPlayer).play(expectSecondItem)
        verify(listener).onMediaChange(expectSecondItem)
        assertEquals(player.currentPlay, expectSecondItem)
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun playMixUnablePlayType() {
        before()

        player.play(mixData, serviceStory)

        verify(listener, never()).onMediaChange(constructMetaInfo(mixData, 0))
        assertEquals(player.currentPlay,constructMetaInfo(mixData, 1))
        assertEquals(player.currentState, PlayState.PLAYING)

        //自动跳过不能解析项
        player.next()

        verify(listener, never()).onMediaChange(constructMetaInfo(mixData, 2))
        assertEquals(player.currentPlay, constructMetaInfo(mixData, 3))
        assertEquals(player.currentState, PlayState.PLAYING)

        //自动跳过不能解析项
        player.previous()

        verify(listener, never()).onMediaChange(constructMetaInfo(mixData, 2))
        assertEquals(player.currentPlay, constructMetaInfo(mixData, 1))
        assertEquals(player.currentState, PlayState.PLAYING)
    }


    @Test
    fun pause() {
        before()

        player.play(data, serviceStory)

        clearInvocations(listener, qtPlayer, mediaPlayer)

        player.pause()
        verify(qtPlayer).pause()
        verify(mediaPlayer, never()).pause()
        verify(listener).onStateChange(PlayState.PAUSED)

        player.next()

        clearInvocations(listener, qtPlayer, mediaPlayer)

        player.pause()
        verify(mediaPlayer).pause()
        verify(qtPlayer, never()).pause()
        verify(listener).onStateChange(PlayState.PAUSED)
    }

    @Test
    fun pauseLoading() {
        before(false)

        player.play(data, serviceStory)
        qtPlayerListener.onStateChange(MetaState.LOADING)

        player.pause()

        verify(qtPlayer).pause()
        assertEquals(player.currentState, PlayState.PAUSED)

        qtPlayerListener.onStateChange(MetaState.PLAYING)
        assertEquals(player.currentState, PlayState.PAUSED)
    }


    @Test
    fun resume() {
        before()

        player.play(data, serviceStory)
        player.pause()

        clearInvocations(listener)
        player.resume()
        verify(qtPlayer).resume()
        verify(mediaPlayer, never()).resume()
        verify(listener).onStateChange(PlayState.PLAYING)

        player.next()
        player.pause()

        clearInvocations(listener, qtPlayer, mediaPlayer)
        player.resume()
        verify(mediaPlayer).resume()
        verify(qtPlayer, never()).resume()
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun autoResume() {
        before()

        player.play(data, serviceStory)
        player.pause()

        clearInvocations(listener)
        player.next()
        verify(listener).onStateChange(PlayState.PLAYING)

        player.pause()
        clearInvocations(listener)
        player.previous()

        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun autoNext() {
        before()

        player.play(data, serviceStory)

        clearInvocations(listener)
        qtPlayerListener.onStateChange(MetaState.COMPLETE)
        assertEquals(player.currentPlay, constructMetaInfo(data, 1))
        assertEquals(player.currentState, PlayState.PLAYING)

        mediaPlayerListener.onStateChange(MetaState.COMPLETE)
        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun complete() {
        before()
        player.play(data, serviceStory)

        qtPlayerListener.onStateChange(MetaState.COMPLETE)
        mediaPlayerListener.onStateChange(MetaState.COMPLETE)
        mediaPlayerListener.onStateChange(MetaState.COMPLETE)

        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.COMPLETE)
        verify(listener).onStateChange(PlayState.COMPLETE)

        clearInvocations(listener)
        player.resume()
        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.PLAYING)
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun stop() {
        before()
        player.play(data, serviceStory)

        clearInvocations(listener)
        player.reset()
        assertEquals(player.currentPlay, null)
        assertEquals(player.currentState, PlayState.READY)
        verify(listener).onStateChange(PlayState.READY)

        player.play(data, serviceStory)
        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.PLAYING)
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun release() {
        before()
        player.play(data, serviceStory)

        player.release()

        verify(qtPlayer).release()
        verify(mediaPlayer).release()

        mediaPlayerListener.onRelease()
        verify(listener, never()).onPlayerRelease()

        qtPlayerListener.onRelease()
        verify(listener).onPlayerRelease()
        verify(listener).onStateChange(PlayState.IDLE)
    }

    @Test
    fun listener() {
        before()

        player.play(data, serviceStory)
        verify(listener).onStateChange(PlayState.PLAYING)

        clearInvocations(listener)
        player.removeListener(listener)
        player.pause()
        verify(listener, never()).onStateChange(PlayState.PAUSED)

        clearInvocations(listener)
        player.addListener(listener)
        player.resume()
        verify(listener).onPlayerReady()
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun autoSkipError() {
        before(false)

        player.play(data, serviceStory)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer loading failed")

        assertEquals(player.currentPlay, constructMetaInfo(data, 1))
        assertEquals(player.currentState, PlayState.LOADING)

        mediaPlayerListener.onStateChange(MetaState.LOADING)
        mediaPlayerListener.onError(ErrorDef.ERROR_RPC_TIMEOUT, "auth RPC Timeout")

        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.LOADING)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer loading failed")

        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.ERROR)
        verify(listener).onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer loading failed")

        player.reset()
        player.play(data, serviceStory)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onStateChange(MetaState.PLAYING)

        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.PLAYING)


    }

    @Test
    fun error() {
        before(false)

        player.play(data, serviceStory, false)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer loading failed")

        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.ERROR)
        verify(listener).onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer loading failed")
    }

    private fun constructMetaInfo(source: JSONArray, index: Int): MetaItem {
        return MetaItem(source.optJSONObject(index), serviceStory)
    }
}