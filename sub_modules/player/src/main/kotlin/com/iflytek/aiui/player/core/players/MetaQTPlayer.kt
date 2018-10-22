package com.iflytek.aiui.player.core.players

import fm.qingting.qtsdk.QTSDK
import fm.qingting.qtsdk.play.QTPlay
import fm.qingting.qtsdk.player.QTPlayer
import android.content.Context
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.error.ErrorDef
import com.iflytek.aiui.player.core.MetaItem
import com.iflytek.aiui.player.init.ThirdPartyPlayers
import fm.qingting.qtsdk.callbacks.QTCallback

//蜻蜓FM初始化接口，方便测试需要
typealias QTInitializeCallback = (QTPlayer) -> Unit

typealias QTPlayerInitializer = (readyCallback: QTInitializeCallback) -> Unit


/**
 * Meta播放器 蜻蜓FM实现
 *
 * 内部通过调用蜻蜓FM SDK的接口实现播放和暂停继续的控制
 */
class MetaQTPlayer(context: Context, rpc: RPC) : MetaAbstractPlayer(rpc) {
    private var player: QTPlayer? = null
    private var mQTSDKInitialized = false
    private var mPaused = true
    private var initializer: QTPlayerInitializer = {
        //因为蜻蜓SDK目前不提供销毁接口，重复初始化报错，故只只初始化一次
        if(!mQTSDKInitialized) {
            QTSDK.Debug  = true
            QTSDK.setHost("https://api.open.qingting.fm")
            ThirdPartyPlayers.initQTFM(context)
            QTPlay.initial(QTCallback { success, _ ->
                if(success) {
                    it(QTSDK.getPlayer())
                    mQTSDKInitialized = true
                }
            })
        } else {
            it(QTSDK.getPlayer())
        }
    }

    override fun initialize() {
        super.initialize()
        initializer.invoke {
            player = it
            player?.addListener(object : QTPlayer.StateChangeListener {
                override fun onPlayDurationChange(duration: Int) {
                }

                override fun onPlayProgressChange(p0: Int) {

                }

                override fun onPlayStateChange(state: Int) {
                    when (state) {
                        QTPlayer.PlayState.PLAYING -> {
                            if(state() == MetaState.LOADING || state() == MetaState.PLAYING) {
                                stateChange(MetaState.PLAYING)
                            } else {
                                player?.pause()
                            }
                        }

                        //播放完成
                        QTPlayer.PlayState.EOF -> {
                            stateChange(MetaState.COMPLETE)
                        }

                        //播放源错误
                        QTPlayer.PlayState.SOURCEFAIL -> {
                            onError(ErrorDef.ERROR_QT_SOURCE_FAILED, "QTPlayer Loading failed")
                        }
                    }
                }
            })
            stateChange(MetaState.READY)
        }

    }


    override fun play(info: MetaItem) {
        val data = info.info
        //通过resourceId字段拆分出channelID和ProgramID
        //完整广播节目只包含ChannelID
        val resourceId = data.optString("resourceId")
        val temp = resourceId.split(",")
        val channelID = temp[0].trim().toInt()
        val programID = if (temp.size > 1) {
            temp[1].trim().toInt()
        } else {
            -1
        }

        mPaused = false
        stateChange(MetaState.LOADING)
        if (programID == -1) {
            player?.prepare(channelID)
        } else {
            player?.prepare(channelID, programID)
        }
    }

    override fun pause() {
        if(state() == MetaState.PLAYING) {
            player?.pause()
        }
        stateChange(MetaState.PAUSED)

    }

    override fun resume() {
        if(state() == MetaState.PAUSED) {
            player?.play()
        }
        stateChange(MetaState.PLAYING)
    }

    override fun release() {
        player?.stop()
        stateChange(MetaState.IDLE)
        super.release()
    }

    override fun canDispose(item: MetaItem): Boolean {
        if (item.source == "qingtingfm") {
            val resourceId = item.info.optString("resourceId", "")
            if (!resourceId.isEmpty()) {
                val resourceIDs = resourceId.split(",")
                return resourceIDs.all {
                    try {
                        it.trim().toInt()
                        true
                    } catch (e: NumberFormatException) {
                        false
                    }
                }
            }
        }

        return false
    }
}