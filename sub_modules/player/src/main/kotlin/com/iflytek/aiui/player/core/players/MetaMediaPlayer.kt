package com.iflytek.aiui.player.core.players

import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.core.MetaItem


class MetaMediaPlayer(rpc: RPC): AbstractMediaPlayer(rpc) {
    override fun retriveURL(item: MetaItem, callback: URLRetriveCallback) {
        callback(item.url)
    }

    override fun canDispose(item: MetaItem): Boolean {
        if(item.source !in listOf("qingtingfm")) {
            val url = item.url
            if(!url.isEmpty() && url.contains(Regex("mp3|m4a"))) {
                return true
            }
        }

        return false
    }
}