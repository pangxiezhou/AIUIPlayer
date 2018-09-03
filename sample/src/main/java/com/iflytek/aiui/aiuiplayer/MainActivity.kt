package com.iflytek.aiui.aiuiplayer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.iflytek.aiui.player.core.AIUIPlayer
import com.iflytek.aiui.player.core.MetaInfo
import com.iflytek.aiui.player.core.PlayState
import com.iflytek.aiui.player.core.PlayerListener
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val player: AIUIPlayer = AIUIPlayer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player.initialize()
        player.addListener(object : PlayerListener {
            override fun onPlayerReady() {
                startPlaySamples()
            }

            override fun onStateChange(state: PlayState) {
                when (state) {
                    PlayState.PLAYING -> ToggleBtn.text = "暂停"
                    PlayState.PAUSED -> ToggleBtn.text = "继续"
                }
            }

            override fun onMediaChange(item: MetaInfo) {
                //根据播放项变化回调修改title内容
                titleTxt.text = item.title
            }

            override fun onPlayerRelease() {
            }

        })

        PreBtn.setOnClickListener {
            if (!player.previous()) {
                Toast.makeText(this, "当前已是第一首", Toast.LENGTH_LONG).show()
            }
        }

        NextBtn.setOnClickListener {
            if (!player.next()) {
                Toast.makeText(this, "当前已是最后一首", Toast.LENGTH_LONG).show()
            }
        }

        ToggleBtn.setOnClickListener {
            if (player.currentState == PlayState.PLAYING) {
                player.pause()
            } else {
                player.resume()
            }
        }
    }

    private fun startPlaySamples() {
        player.play(listOf(
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "樊梨花_1",
                        "resourceId" to "5142784,1434432"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "name" to "河马当保姆",
                        "playUrl" to "http://od.open.qingting.fm/vod/00/00/0000000000000000000025449186_24.m4a?u=786&channelId=97894&programId=2588214"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "樊梨花_5",
                        "resourceId" to "5142784,1434436"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "中国之声",
                        "resourceId" to "386"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "环球资讯",
                        "resourceId" to "1005"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "左手右手",
                        "resourceId" to "53408,2121237"
                ))),
                MetaInfo(JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "我爱北京天安门",
                        "resourceId" to "53408,2013374"
                )))
        ))
    }
}
