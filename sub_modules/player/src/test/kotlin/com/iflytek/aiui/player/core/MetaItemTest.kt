package com.iflytek.aiui.player.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

class MetaItemTest {

    @Test
    fun parse() {
        val songName = "演员"
        val songPath = "http://vbox.hf.openstorage.cn/ctimusic/128/2015-06-12/%E8%96%9B%E4%B9%8B%E8%B0%A6/%E7%BB%85%E5%A3%AB/%E6%BC%94%E5%91%98.mp3"
        val singer = "薛之谦"

        val songItem = MetaItem(JSONObject(hashMapOf(
                "songname" to songName,
                "audiopath" to songPath,
                "singernames" to JSONArray(listOf(singer))
        )), "musicX")

        assertEquals(songItem.title, songName)
        assertEquals(songItem.author, singer)
        assertEquals(songItem.url, songPath)

        val storyName = "田忌赛马"
        val storyUrl = "http://od.open.qingting.fm/m4a/59df8eeb7cb8914775a2d36c_8056708_64.m4a?u=786&channelId=227528&programId=7886648"

        val storyItem = MetaItem(JSONObject(hashMapOf(
                "name" to storyName,
                "playUrl" to storyUrl
        )), "story")
        assertEquals(storyItem.title, storyName)
        assertEquals(storyItem.url, storyUrl)
        assertTrue(storyItem.author.isEmpty())


        val jokeName = "邯郸学步"
        val jokeUrl = "http://od.open.qingting.fm/m4a/59df8eeb7cb8914775a2d36c_8056708_64.m4a?u=786&channelId=227528&programId=7886648"

        val jokeItem = MetaItem(JSONObject(hashMapOf(
                "title" to jokeName,
                "mp3Url" to jokeUrl
        )), "joke")
        assertEquals(jokeItem.title, jokeName)
        assertEquals(jokeItem.url, jokeUrl)
        assertTrue(storyItem.author.isEmpty())

        val radioName = "中央人民广播电台中国之声"
        val radioUrl = "http://http.open.qingting.fm/786/386.mp3?deviceid=12312&clientid=ZTQ2NTkwNGUtNmM1OS0xMWU3LTkyM2YtMDAxNjNlMDAyMGFk"

        val radioItem = MetaItem(JSONObject(hashMapOf(
                "name" to radioName,
                "url" to radioUrl
        )), "radio")
        assertEquals(radioItem.title, radioName)
        assertEquals(radioItem.url, radioUrl)
        assertTrue(radioItem.author.isEmpty())
    }

    @Test
    fun equals() {
        val info = JSONObject(hashMapOf(
                "name" to "foo",
                "playUrl" to "http://fake.url/test.mp3"
        ))

        assertEquals(MetaItem(info, "story"), MetaItem(info, "story"))

        val radioItem =JSONObject(hashMapOf(
                "name" to "广播",
                "url" to "http://http.open.qingting.fm/786/386.mp3?deviceid=12312&clientid=ZTQ2NTkwNGUtNmM1OS0xMWU3LTkyM2YtMDAxNjNlMDAyMGFk"
        ))
        assertNotEquals(MetaItem(radioItem, "story"), MetaItem(radioItem, "radio"))
    }
}