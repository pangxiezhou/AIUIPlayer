package com.iflytek.aiui.player.common.player

import android.content.Context
import android.text.TextUtils
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.storage.Storage


class MetaMediaPlayer(context: Context, rpc: RPC, storage: Storage): AbstractMediaPlayer(context, rpc, storage) {
    override fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback) {
        callback(item.url)
    }

    override fun canDispose(item: MetaItem): Boolean {
        val url = item.url
        if(!TextUtils.isEmpty(url)) {
            return true
        }

        return false
    }
}