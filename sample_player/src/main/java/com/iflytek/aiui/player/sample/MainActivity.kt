package com.iflytek.aiui.player.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.iflytek.aiui.aiuiplayer.R
import com.iflytek.aiui.player.core.AIUIPlayer
import com.iflytek.aiui.player.core.MetaItem
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

        player = AIUIPlayer(this)
        player.addListener(object : PlayerListener {
            override fun onError(error: Int, info: String) {
                titleTxt.text = "播放错误 $error $info"
            }

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

            override fun onMediaChange(item: MetaItem) {
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
//        player.play(JSONArray(listOf(
//                JSONObject(hashMapOf(
//                        "source" to "qingtingfm",
//                        "name" to "三只骄傲的小猫",
//                        "resourceId" to "192838,9290559"
//                )),
//                JSONObject(hashMapOf(
//                        "name" to "河马当保姆",
//                        "playUrl" to "http://od.open.qingting.fm/vod/00/00/0000000000000000000025449186_24.m4a?u=786&channelId=97894&programId=2588214"
//                )),
//                JSONObject(hashMapOf(
//                        "source" to "qingtingfm",
//                        "name" to "图图的生日礼物",
//                        "resourceId" to "238643,8493379"
//                ))
//        )
//        ), "story")

//        player.play(JSONArray(listOf(
//                JSONObject(hashMapOf(
//                        "source" to "qingtingfm",
//                        "name" to "中国之声",
//                        "resourceId" to "386"
//                )),
//                JSONObject(hashMapOf(
//                        "source" to "qingtingfm",
//                        "name" to "环球资讯",
//                        "resourceId" to "1005"
//                ))
//        )
//        ), "radio")


        player.play(JSONArray(listOf(
                JSONObject(hashMapOf(
                        "songname" to "她说",
                        "singernames" to listOf("林俊杰"),
                        "audiopath" to "http://vbox.hf.openstorage.cn/ctimusic/128/2015-07-21/%E6%9E%97%E4%BF%8A%E6%9D%B0/%E5%A5%B9%E8%AF%B4%20%E6%A6%82%E5%BF%B5%E8%87%AA%E9%80%89%E8%BE%91/%E5%A5%B9%E8%AF%B4.mp3"
                )),
                JSONObject(hashMapOf(
                        "source" to "kugou",
                        "songname" to "尽头",
                        "singernames" to listOf("赵方婧"),
                        "itemid" to "73f211b375593a4332bb5e4a28602c61"
                )),
                JSONObject(hashMapOf(
                        "source" to "kugou",
                        "songname" to "广东爱情故事",
                        "singernames" to listOf("广东雨神"),
                        "itemid" to "2a25aaff4b6c84b859b4d77f944de57a"
                ))
        )
        ), "musicX")
    }
}
