package com.iflytek.aiui.player.core

import android.test.mock.MockContext
import com.iflytek.aiui.player.core.players.*
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
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class AIUIPlayerTest {
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

    @Before
    fun setUp() {
        whenever(qtPlayer.play(any())).then {
            it.getArgument<MetaInfo>(0).info.optString("source") == "qingtingfm"
        }

        whenever(mediaPlayer.play(any())).then {
            !it.getArgument<MetaInfo>(0).info.optString("playUrl").isEmpty()
        }
    }

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

    private val data = listOf(
            MetaInfo(JSONObject(hashMapOf(
                    "source" to "qingtingfm",
                    "name" to "foo",
                    "resourceId" to "123,456"
            ))),
            MetaInfo(JSONObject(hashMapOf(
                    "name" to "bar",
                    "playUrl" to "http://fake.url/test.mp3"
            ))),
            MetaInfo(JSONObject(hashMapOf(
                    "source" to "qingtingfm",
                    "name" to "foobar",
                    "resourceId" to "123,456"
            )))
    )


    @Test
    fun initializeNotAllReady() {
        notAllPlayerReady()

        player.initialize()
        player.addListener(listener)

        verify(listener, never()).onPlayerReady()
        verify(listener, never()).onStateChange(PlayState.READY)
    }

    @Test
    fun initializeAllReady() {
        allPlayerReady()

        player.initialize()
        player.addListener(listener)

        verify(listener).onPlayerReady()
        verify(listener).onStateChange(PlayState.READY)
        assertNull(player.currentPlay)
        assertEquals(player.currentState, PlayState.READY)
    }


    @Test
    fun playMixType() {
        before()
        player.play(data)

        verify(qtPlayer).play(data[0])
        assertEquals(player.currentPlay, data[0])
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun playEmptyList() {
        before()

        player.play(listOf())

        assertNull(player.currentPlay)
        assertEquals(player.currentState, PlayState.READY)
    }

    @Test
    fun playMixUnablePlayType() {
        before()

        val mixTypeList = listOf(
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "UnReg type",
                        "name" to "first",
                        "resourceId" to "123,456"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "foo",
                        "resourceId" to "123,456"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "UnReg type",
                        "name" to "foobar",
                        "resourceId" to "123,456"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "name" to "bar",
                        "playUrl" to "http://fake.url/test.mp3"
                ))))
        player.play(mixTypeList)
        assertEquals(player.currentPlay, mixTypeList[1])
        assertEquals(player.currentState, PlayState.PLAYING)

        player.next()

        assertEquals(player.currentPlay, mixTypeList[3])
        assertEquals(player.currentState, PlayState.PLAYING)

        player.previous()

        assertEquals(player.currentPlay, mixTypeList[1])
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    private fun before() {
        allPlayerReady()

        player.initialize()
        player.addListener(listener)
    }

    @Test
    fun next() {
        before()
        player.play(data)
        verify(listener).onStateChange(PlayState.PLAYING)

        clearInvocations(listener)
        assertTrue(player.next())
        verify(qtPlayer).pause()
        verify(mediaPlayer).play(data[1])
        verify(listener).onMediaChange(data[1])
        verify(listener, never()).onStateChange(any())
        assertEquals(player.currentPlay, data[1])
        assertEquals(player.currentState, PlayState.PLAYING)

        assertTrue(player.next())
        verify(qtPlayer).play(data[2])
        verify(listener).onMediaChange(data[2])
        verify(listener, never()).onStateChange(any())
        assertEquals(player.currentPlay, data[2])
        assertEquals(player.currentState, PlayState.PLAYING)

        assertFalse(player.next())
        verify(qtPlayer).play(data[2])
        verify(listener).onMediaChange(data[2])
        verify(listener, never()).onStateChange(any())
        assertEquals(player.currentPlay, data[2])
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun previous() {
        before()

        player.play(data)
        assertFalse(player.previous())

        player.next()
        player.next()

        clearInvocations(listener, qtPlayer, mediaPlayer)
        //到第二首
        assertTrue(player.previous())
        verify(mediaPlayer).play(data[1])
        verify(listener).onMediaChange(data[1])
        assertEquals(player.currentPlay, data[1])
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun pause() {
        before()

        player.play(data)

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
    fun resume() {
        before()

        player.play(data)
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

        player.play(data)
        player.pause()

        clearInvocations(listener)
        player.next()

        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun autoNext() {
        before()

        player.play(data)

        clearInvocations(listener)
        qtPlayerListener.onStateChange(MetaState.COMPLETE)
        assertEquals(player.currentPlay, data[1])
        assertEquals(player.currentState, PlayState.PLAYING)

        mediaPlayerListener.onStateChange(MetaState.COMPLETE)
        assertEquals(player.currentPlay, data[2])
        assertEquals(player.currentState, PlayState.PLAYING)
    }

    @Test
    fun complete() {
        before()
        player.play(data)

        qtPlayerListener.onStateChange(MetaState.COMPLETE)
        mediaPlayerListener.onStateChange(MetaState.COMPLETE)
        mediaPlayerListener.onStateChange(MetaState.COMPLETE)

        assertEquals(player.currentPlay, null)
        assertEquals(player.currentState, PlayState.COMPLETE)
        verify(listener).onStateChange(PlayState.COMPLETE)

        clearInvocations(listener)
        player.resume()
        assertEquals(player.currentPlay, data[2])
        assertEquals(player.currentState, PlayState.PLAYING)
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun stop() {
        before()
        player.play(data)

        clearInvocations(listener)
        player.stop()
        assertEquals(player.currentPlay, null)
        assertEquals(player.currentState, PlayState.STOPPED)
        verify(listener).onStateChange(PlayState.STOPPED)

        player.play(data)
        assertEquals(player.currentPlay, data[0])
        assertEquals(player.currentState, PlayState.PLAYING)
        verify(listener).onStateChange(PlayState.PLAYING)
    }

    @Test
    fun release() {
        before()
        player.play(data)

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

        player.play(data)
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
}