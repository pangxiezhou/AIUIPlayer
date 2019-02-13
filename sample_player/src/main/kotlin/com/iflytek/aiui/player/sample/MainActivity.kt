package com.iflytek.aiui.player.sample

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import com.iflytek.aiui.aiuiplayer.R
import com.iflytek.aiui.player.common.player.MetaItem
import com.iflytek.aiui.player.core.AIUIPlayer
import com.iflytek.aiui.player.core.PlayState
import com.iflytek.aiui.player.core.PlayerListener
import com.iflytek.aiui.player.players.MiGuPlayerNative
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.concurrent.schedule

const val RC_PERMISSION_STORAGE_PHONE = 1

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks{

    private lateinit var player: AIUIPlayer
    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(EasyPermissions.hasPermissions(this, *permissions)) {
            initializePlayer()
        } else {
            EasyPermissions.requestPermissions(this, "在设备上播放咪咕音乐，需要允许存储和电话权限", RC_PERMISSION_STORAGE_PHONE, *permissions)
        }
    }


    private fun initializePlayer() {
        MiGuPlayerNative.initWith("", "", "", "dbb6d32a813c64ca")

        player = AIUIPlayer(this)
        player.addListener(object : PlayerListener {
            override fun onPlayerReady() {
                titleTxt.text = "初始化成功"
                startPlaySamples()
            }

            override fun onStateChange(state: PlayState) {
                playState.text = state.name
                when (state) {
                    PlayState.PLAYING -> ToggleBtn.text = "暂停"
                    PlayState.PAUSED, PlayState.COMPLETE -> ToggleBtn.text = "继续"
                    PlayState.LOADING -> {
                        playSeek.progress = 0
                        playProgress.text = "--/--"
                    }
                    else -> {
                    }
                }
            }

            override fun onMediaChange(item: MetaItem) {
                //根据播放项变化回调修改title内容
                titleTxt.text = item.title
            }

            override fun onError(error: Int, info: String) {
                titleTxt.text = "播放错误 $error $info"
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

        playSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, process: Int, user: Boolean) {
                if (user) {
                    player.seekTo((player.duration * (process / 100.0)).toLong())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        Timer().schedule(0, 100) {
            if (player.currentState == PlayState.PLAYING) {
                runOnUiThread {
                    if (player.duration == -1L) {
                        playProgress.text = "直播流"
                    } else {
                        playSeek.progress = (player.currentPosition * 1.0 / player.duration * 100).toInt()
                        playProgress.text = "${toHumanRead(player.currentPosition)}/${toHumanRead(player.duration)}"
                    }
                }
            }
        }
    }

    private fun toHumanRead(msec: Long): String {
        val minute = msec / 1000 / 60
        val seconds = msec / 1000 % 60
        return "${minute.toInt().format(2)}:${seconds.toInt().format(2)}"
    }

    fun Int.format(digits: Int) = String.format("%0${digits}d", this)

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
                        "songname" to "美人鱼",
                        "singernames" to listOf("林俊杰"),
                        "audiopath" to "http://aiui.storage.iflyresearch.com/ctimusic/128/2016-01-17/%E6%9E%97%E4%BF%8A%E6%9D%B0/%E3%80%8A%E7%AC%AC%E4%BA%8C%E5%A4%A9%E5%A0%82%28%E6%B1%9F%E5%8D%97%29%E3%80%8B/%E7%BE%8E%E4%BA%BA%E9%B1%BC1452984342.mp3"
                )),
                JSONObject(hashMapOf(
                        "songname" to "中国人民广播电台",
                        "singernames" to listOf("中国人民广播电台"),
                        "audiopath" to "http://open.ls.qingting.fm/live/386/24k.m3u8"
                )),
                JSONObject(hashMapOf(
                        "source" to "migu",
                        "songname" to "屋顶(2018中国好声音澳门演唱会)",
                        "singernames" to listOf("周杰伦"),
                        "itemid" to "6990539Z1T4"
                )),
                JSONObject(hashMapOf(
                        "source" to "migu",
                        "songname" to "夜曲",
                        "singernames" to listOf("周杰伦"),
                        "itemid" to "60054701947"
                )),
                JSONObject(hashMapOf(
                        "source" to "migu",
                        "songname" to "七里香",
                        "singernames" to listOf("周杰伦"),
                        "itemid" to "60054701934"
                ))
        )
        ), "musicX")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.hasPermissions(this, *permissions)) {
            initializePlayer()
        } else {
            Toast.makeText(this, "请允许咪咕音乐运行时需要的权限", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.hasPermissions(this, *permissions)) {
            initializePlayer()
        } else {
            Toast.makeText(this, "请允许咪咕音乐运行时需要的权限", Toast.LENGTH_LONG).show()
        }
    }
}
