package com.iflytek.aiui.aiuiplayer

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.iflytek.aiui.player.auth.login.AuthRPC
import com.iflytek.aiui.player.auth.login.KuGouLoginActivity
import com.iflytek.aiui.player.core.AIUIPlayer
import com.iflytek.aiui.player.core.MetaInfo
import com.iflytek.aiui.player.core.PlayState
import com.iflytek.aiui.player.core.PlayerListener
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var player: AIUIPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AuthRPC.init(this)

        player = AIUIPlayer(this)
        player.addListener(object : PlayerListener {
            override fun onPlayerReady() {
                titleTxt.text = "初始化成功"
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
                titleTxt.text = "未初始化"
            }

        })

        initializeBtn.setOnClickListener {
            player.initialize()
        }

        releaseBtn.setOnClickListener {
            player.release()
        }


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
        player.play(JSONArray( listOf(
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "樊梨花_1",
                        "resourceId" to "5142784,1434432"
                )),
                JSONObject(hashMapOf(
                        "name" to "河马当保姆",
                        "playUrl" to "http://od.open.qingting.fm/vod/00/00/0000000000000000000025449186_24.m4a?u=786&channelId=97894&programId=2588214"
                )),
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "樊梨花_5",
                        "resourceId" to "5142784,1434436"
                )),
                JSONObject(hashMapOf(
                        "source" to "kugou",
                        "name" to "尽头",
                        "itemid" to "73f211b375593a4332bb5e4a28602c61"
                )),
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "中国之声",
                        "resourceId" to "386"
                )),
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "环球资讯",
                        "resourceId" to "1005"
                )),
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "左手右手",
                        "resourceId" to "53408,2121237"
                )),
                JSONObject(hashMapOf(
                        "source" to "qingtingfm",
                        "name" to "我爱北京天安门",
                        "resourceId" to "53408,2013374"
                )))
        //因为第二项playUrl是story技能下才会出现的播放字段
        ), "story")
    }
}
