package com.iflytek.aiui.player.core

import android.test.mock.MockContext
import com.iflytek.aiui.player.core.players.*
import com.nhaarman.mockitokotlin2.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class AIUIPlayerTest {
    private val SERVICE_STORY = "story"

    private lateinit var qtPlayerListener: MetaListener
    private lateinit var mediaPlayerListener: MetaListener
    private val qtPlayer: MetaQTPlayer = mock()
    private val mediaPlayer: MetaMediaPlayer = mock()

    @Spy
    private val mPlayers: List<MetaAbstractPlayer> = listOf(qtPlayer, mediaPlayer)

    @Mock
    private lateinit var listener: PlayerListener

    @InjectMocks
    private val player = AIUIPlayer(MockContext())


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
                    "source" to "unable play",
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
        whenever(qtPlayer.play(any())).then {
            val canDispose = it.getArgument<MetaInfo>(0).source== "qingtingfm"
            if(autoPlaying && canDispose) {
                qtPlayerListener.onStateChange(MetaState.LOADING)
                qtPlayerListener.onStateChange(MetaState.PLAYING)
            }
            canDispose
        }

        whenever(mediaPlayer.play(any())).then {
            val canDispose = !it.getArgument<MetaInfo>(0).url.isEmpty()
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
        player.play(data, SERVICE_STORY)

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

        player.play(JSONArray(), SERVICE_STORY)

        assertNull(player.currentPlay)
        assertEquals(player.currentState, PlayState.READY)
    }

    @Test
    fun playEmptyNotAffect() {
        before()
        player.play(data, SERVICE_STORY)

        //play empty list
        player.play(JSONArray(), SERVICE_STORY)

        val firstItem = constructMetaInfo(data, 0)
        //not affect
        assertEquals(player.currentPlay, firstItem)
        assertEquals(player.currentState, PlayState.PLAYING)

        player.play(invalidData, SERVICE_STORY)
        //not affect
        assertEquals(player.currentPlay, firstItem)
        assertEquals(player.currentState, PlayState.PLAYING)
    }


    @Test
    fun next() {
        before()

        player.play(data, SERVICE_STORY)
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

        player.play(data, SERVICE_STORY)


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

        player.play(mixData, SERVICE_STORY)

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

        player.play(data, SERVICE_STORY)

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

        player.play(data, SERVICE_STORY)
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

        player.play(data, SERVICE_STORY)
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

        player.play(data, SERVICE_STORY)
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

        player.play(data, SERVICE_STORY)

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
        player.play(data, SERVICE_STORY)

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
        player.play(data, SERVICE_STORY)

        clearInvocations(listener)
        player.reset()
        assertEquals(player.currentPlay, null)
        assertEquals(player.currentState, PlayState.READY)
        verify(listener).onStateChange(PlayState.READY)

        player.play(data, SERVICE_STORY)
        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.PLAYING)
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun release() {
        before()
        player.play(data, SERVICE_STORY)

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

        player.play(data, SERVICE_STORY)
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

        player.play(data, SERVICE_STORY)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onStateChange(MetaState.ERROR)

        assertEquals(player.currentPlay, constructMetaInfo(data, 1))
        assertEquals(player.currentState, PlayState.LOADING)

        mediaPlayerListener.onStateChange(MetaState.LOADING)
        mediaPlayerListener.onStateChange(MetaState.ERROR)

        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.LOADING)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onStateChange(MetaState.ERROR)
        assertEquals(player.currentPlay, constructMetaInfo(data, 2))
        assertEquals(player.currentState, PlayState.ERROR)

        player.reset()
        player.play(data, SERVICE_STORY)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onStateChange(MetaState.PLAYING)

        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.PLAYING)


    }

    @Test
    fun error() {
        before(false)

        player.play(data, SERVICE_STORY, false)

        qtPlayerListener.onStateChange(MetaState.LOADING)
        qtPlayerListener.onStateChange(MetaState.ERROR)

        assertEquals(player.currentPlay, constructMetaInfo(data, 0))
        assertEquals(player.currentState, PlayState.ERROR)
    }

    private fun constructMetaInfo(source: JSONArray, index: Int): MetaInfo {
        return MetaInfo(source.optJSONObject(index), SERVICE_STORY)
    }
}