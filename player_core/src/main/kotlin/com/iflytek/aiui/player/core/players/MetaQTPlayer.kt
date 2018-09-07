package com.iflytek.aiui.player.core.players

import fm.qingting.qtsdk.QTSDK
import fm.qingting.qtsdk.play.QTPlay
import fm.qingting.qtsdk.player.QTPlayer
import android.content.Context
import com.iflytek.aiui.player.core.MetaInfo
import com.iflytek.aiui.player.init.PlayerInitializer
import fm.qingting.qtsdk.callbacks.QTCallback

//蜻蜓FM初始化接口，方便测试需要
typealias QTInitializeCallback = (QTPlayer) -> Unit

typealias QTPlayerInitializer = (readyCallback: QTInitializeCallback) -> Unit


/**
 * Meta播放器 蜻蜓FM实现
 *
 * 内部通过调用蜻蜓FM SDK的接口实现播放和暂停继续的控制
 */
class MetaQTPlayer(context: Context) : MetaAbstractPlayer() {
    private var player: QTPlayer? = null
    private var mPlayState = QTPlayer.PlayState.NONE
    private var mQTSDKInitialized = false
    private var initializer: QTPlayerInitializer = {
        //因为蜻蜓SDK目前不提供销毁接口，重复初始化报错，故只只初始化一次
        if(!mQTSDKInitialized) {
            QTSDK.setHost("https://open.staging.qingting.fm")
            PlayerInitializer.initQTFM(context)
            QTSDK.setAuthRedirectUrl("http://qttest.qingting.fm")
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
        initializer?.invoke {
            player = it
            stateChange(MetaState.READY)
            player?.addListener(object : QTPlayer.StateChangeListener {
                override fun onPlayDurationChange(duration: Int) {
                }

                override fun onPlayProgressChange(p0: Int) {

                }

                override fun onPlayStateChange(state: Int) {
                    when (state) {
                        QTPlayer.PlayState.PLAYING -> {
                            stateChange(MetaState.PLAYING)
                        }

                        QTPlayer.PlayState.PAUSED -> {
                            stateChange(MetaState.PAUSED)
                        }

                        //播放完成
                        QTPlayer.PlayState.EOF -> {
                            stateChange(MetaState.COMPLETE)
                        }
                    }

                    mPlayState = state
                }
            })
        }

    }


    override fun play(info: MetaInfo):Boolean {
        if(!canDispose(info)) return false

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

        mPlayState = QTPlayer.PlayState.NONE
        if (programID == -1) {
            player?.prepare(channelID)
        } else {
            player?.prepare(channelID, programID)
        }

        return true
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun release() {
        player?.stop()
        stateChange(MetaState.IDLE)
        super.release()
    }

    private fun canDispose(item: MetaInfo): Boolean {
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