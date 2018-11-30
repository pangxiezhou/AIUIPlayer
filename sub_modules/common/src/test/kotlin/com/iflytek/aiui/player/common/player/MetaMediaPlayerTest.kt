package com.iflytek.aiui.player.common.player

import android.media.MediaPlayer
import com.nhaarman.mockitokotlin2.*
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MetaMediaPlayerTest {
    private lateinit var onCompleteListener: MediaPlayer.OnCompletionListener
    private lateinit var onPrepareListener: MediaPlayer.OnPreparedListener
    private val mediaPlayer:MediaPlayer = mock()

    @Spy
    private val mInitializer: MediaPlayerInitializer = { callback ->
        callback(mediaPlayer)
    }

    @Mock
    private lateinit var listener: MetaListener
    @InjectMocks
    private val player = MetaMediaPlayer(mock(), mock(), mock())

    private val url = "http://fake.url/test.mp3"
    private val validItem = constructItem("test", url)

    @Before
    fun setUp() {
        whenever(mediaPlayer.setOnCompletionListener(any())).then {
            onCompleteListener = it.getArgument(0)
            null
        }

        whenever(mediaPlayer.setOnPreparedListener(any())).then {
            onPrepareListener = it.getArgument(0)
            null
        }

        whenever(mediaPlayer.prepareAsync()).then {
            onPrepareListener.onPrepared(mediaPlayer)
        }

        whenever(mediaPlayer.isPlaying).thenReturn(true)

        player.initialize()
        player.addListener(listener)
    }

    @Test
    fun initialize() {
        verify(listener).onReady()
        verify(mediaPlayer).setOnCompletionListener(any())
    }

    @Test
    fun canDispose() {
        val urlItem = constructItem(
                "河马当保姆",
                "http://od.open.qingting.fm/vod/00/00/0000000000000000000025449186_24.m4a?u=786&channelId=97894&programId=2588214"
        )
        assertTrue(player.canDispose(urlItem))

        val qingtingItem = MetaItem(JSONObject(hashMapOf<String, Any>(
                "source" to "qingtingfm",
                "name" to "河马当保姆",
                "resourceId" to "123, 456"
        )), "story")
        assertFalse(player.canDispose(qingtingItem))

        val mixItem = MetaItem(JSONObject(hashMapOf<String, Any>(
                "source" to "qingtingfm",
                "name" to "河马当保姆",
                "playUrl" to "http://od.open.qingting.fm/vod/00/00/0000000000000000000025449186_24.m4a?u=786&channelId=97894&programId=2588214",
                "resourceId" to "123, 456"
        )), "story")
        assertFalse(player.canDispose(mixItem))
    }

    @Test
    fun playItem() {
        player.play(validItem)

        verify(mediaPlayer).setDataSource(eq(url))
        verify(mediaPlayer).prepareAsync()
        verify(mediaPlayer).start()

        verify(listener).onStateChange(MetaState.PLAYING)


        //second playProgram
        clearInvocations(mediaPlayer)
        clearInvocations(listener)
        val secondUrl = "http://second.fake.url/test.mp3"
        player.play(constructItem("second name", secondUrl))

        verify(mediaPlayer).reset()
        verify(mediaPlayer).setDataSource(eq(secondUrl))
        verify(mediaPlayer).prepareAsync()

        verify(listener).onStateChange(MetaState.PLAYING)
    }

    @Test
    fun pause() {
        player.play(validItem)

        player.pause()

        verify(mediaPlayer).pause()
        verify(listener).onStateChange(MetaState.PAUSED)
    }

    @Test
    fun resume() {
        player.play(validItem)
        player.pause()


        clearInvocations(mediaPlayer, listener)
        player.resume()
        verify(mediaPlayer).start()
        verify(listener).onStateChange(MetaState.PLAYING)
    }

    @Test
    fun duration() {
        //非播放状态，返回0
        whenever(mediaPlayer.duration).thenReturn(42)
        assertEquals(player.getDuration(), 0)

        player.play(validItem)
        assertEquals(player.getDuration(), 42)

        //直播流返回-1
        whenever(mediaPlayer.duration).thenReturn(-1)
        assertEquals(player.getDuration(), -1)
    }

    @Test
    fun progress() {
        //非播放状态下调用，返回0
        whenever(mediaPlayer.currentPosition).thenReturn(42)
        assertEquals(player.getCurrentPos(), 0)

        player.play(validItem)
        assertEquals(player.getCurrentPos(), 42)
    }

    @Test
    fun seek() {
        doNothing().whenever(mediaPlayer).seekTo(any())

        player.seekTo(100)
        verify(mediaPlayer, never()).seekTo(any())

        player.play(validItem)
        player.seekTo(100)
        verify(mediaPlayer).seekTo(100)
    }



    @Test
    fun complete() {
        player.play(validItem)

        onCompleteListener.onCompletion(mediaPlayer)

        verify(listener).onStateChange(MetaState.COMPLETE)
    }

    @Test
    fun release() {
        player.play(validItem)

        player.release()

        verify(mediaPlayer).release()
        verify(listener).onRelease()
    }

    private fun constructItem(name: String, url: String): MetaItem {
        val mapInfo = mutableMapOf<String, Any>(
                "name" to name,
                "playUrl" to url
        )

        return MetaItem(JSONObject(mapInfo), "story")
    }
}
