package com.iflytek.aiui.player.players

import android.content.Context
import com.iflytek.aiui.player.common.player.AbstractMediaPlayer
import com.iflytek.aiui.player.common.player.MetaItem
import com.iflytek.aiui.player.common.player.URLRetrieveCallback
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.storage.Storage
import com.sitech.migurun.bean.MusicInfo
import com.sitech.migurun.init.MiGuRun
import com.sitech.migurun.net.MusicInfoProvider
import java.lang.Exception


class MiGuAPI {
    private lateinit var mContext: Context
    private lateinit var mMusicProvider: MusicInfoProvider

    fun init(context: Context) {
        mContext = context
        try{
            MiGuRun(context, "", "", "", "dbb6d32a813c64ca")
            mMusicProvider = MusicInfoProvider(context)

//            val intentService = Intent(context, AudioPlayService::class.java)
//            //播放模式，默认顺序播放
//            intentService.putExtra(Keys.CURRENT_PLAY_MODE, 1)
//            //初始化要播放的音乐起始位置（第一首）
//            intentService.putExtra(Keys. songPosition, 0)
//            context.startService(intentService);
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun queryMusicByID(id: String, callback: (MusicInfo) -> Unit) {
        mMusicProvider.queryMusicByID(mContext, id) { musicInfo ->
            callback(musicInfo)
        }
    }
}

class MetaMiGuPlayer(context: Context, rpc: RPC, storage: Storage): AbstractMediaPlayer(context, rpc, storage) {
    private var mMiGuAPI = MiGuAPI()

    override fun initialize() {
        super.initialize()
        mMiGuAPI.init(context)
    }

    override fun retrieveURL(item: MetaItem, callback: URLRetrieveCallback) {
        val itemID = item.info.optString("itemid", "")
        mMiGuAPI.queryMusicByID(itemID) { info ->
            callback(info.listenUrl)
        }
    }

    override fun canDispose(item: MetaItem): Boolean {
        if (item.source == "migu") {
            val itemID = item.info.optString("itemid", "")
            if (!itemID.isEmpty()) {
                return true
            }
        }

        return false
    }

}