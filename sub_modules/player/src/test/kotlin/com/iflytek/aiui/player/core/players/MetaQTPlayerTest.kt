package com.iflytek.aiui.player.core.players

import android.test.mock.MockContext
import com.iflytek.aiui.player.core.MetaItem
import com.nhaarman.mockitokotlin2.*
import fm.qingting.qtsdk.player.QTPlayer
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class MetaQTPlayerTest {
    private lateinit var stateListener: QTPlayer.StateChangeListener
    private var qtPlayer: QTPlayer = mock()

    @Spy
    private val initializer: QTPlayerInitializer = {
        it(qtPlayer)
    }

    @Mock
    private lateinit var listener: MetaListener

    @InjectMocks
    private val player: MetaQTPlayer = MetaQTPlayer(MockContext(), mock())

    private val validItem = MetaItem(JSONObject(hashMapOf(
            "source" to "qingtingfm",
            "resourceId" to "1234,5678"
    )), "story")

    @Before
    fun setUp() {
        whenever(qtPlayer.addListener(any())).then {
            stateListener = it.getArgument<QTPlayer.StateChangeListener>(0)
            null
        }
        whenever(qtPlayer.prepare(any(), any<Int>())).then {
            stateListener.onPlayStateChange(QTPlayer.PlayState.LOADING)
            stateListener.onPlayStateChange(QTPlayer.PlayState.PLAYING)
        }

        whenever(qtPlayer.prepare(any())).then {
            stateListener.onPlayStateChange(QTPlayer.PlayState.LOADING)
            stateListener.onPlayStateChange(QTPlayer.PlayState.PLAYING)
        }

        whenever(qtPlayer.pause()).then {
            stateListener.onPlayStateChange(QTPlayer.PlayState.PAUSED)
        }

        whenever(qtPlayer.play()).then {
            stateListener.onPlayStateChange(QTPlayer.PlayState.PLAYING)
        }

        player.initialize()
        player.addListener(listener)
    }

    @Test
    fun initialize() {
        verify(qtPlayer).addListener(any())

        verify(listener).onReady()
    }

    @Test
    fun canDispose() {
        assertTrue(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "resourceId" to "123, 456"
        )), "story")))

        assertTrue(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "resourceId" to "123"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "resourceId" to "0.1, 0.54"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "playUrl" to "http://fake.url/test.mp3"
        )), "story")))

        assertFalse(player.canDispose(MetaItem(JSONObject(hashMapOf(
                "source" to "kugou",
                "resourceId" to "123, 456"
        )), "story")))
    }

    @Test
    fun playProgram() {
        val channelId = 123
        val programId = 456
        val item = MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "resourceId" to "$channelId, $programId"
        )), "story")
        player.play(item)

        verify(qtPlayer).prepare(eq(channelId), eq(programId))
        verify(listener).onStateChange(MetaState.PLAYING)
    }

    @Test
    fun playChannel() {
        val channelId = 123
        val item = MetaItem(JSONObject(hashMapOf(
                "source" to "qingtingfm",
                "resourceId" to "$channelId"
        )), "radio")
        player.play(item)

        verify(qtPlayer).prepare(eq(channelId))
        verify(listener).onStateChange(MetaState.PLAYING)
    }

    @Test
    fun pause() {
        player.play(validItem)
        player.pause()

        verify(qtPlayer).pause()
        verify(listener).onStateChange(MetaState.PAUSED)
    }

    @Test
    fun resume() {
        player.play(validItem)
        qtPlayer.pause()

        clearInvocations(qtPlayer, listener)
        qtPlayer.play()

        verify(qtPlayer).play()
        verify(listener).onStateChange(MetaState.PLAYING)
    }

    @Test
    fun complete() {
        player.play(validItem)
        stateListener.onPlayStateChange(QTPlayer.PlayState.EOF)

        verify(listener).onStateChange(MetaState.COMPLETE)
    }

    @Test
    fun release() {
        player.release()

        verify(qtPlayer).stop()
        verify(listener).onRelease()
    }
}