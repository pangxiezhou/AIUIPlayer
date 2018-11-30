package com.iflytek.aiui.player.common.player

import android.content.Context
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.storage.Storage


class MetaMediaPlayer(context: Context, rpc: RPC, storage: Storage): AbstractMediaPlayer(context, rpc, storage) {
    override fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback) {
        callback(item.url)
    }

    override fun canDispose(item: MetaItem): Boolean {
        if(item.source !in listOf("qingtingfm")) {
            val url = item.url
            if(!url.isEmpty() && url.contains(Regex("mp3|m4a|ts|m3u8"))) {
                return true
            }
        }

        return false
    }
}