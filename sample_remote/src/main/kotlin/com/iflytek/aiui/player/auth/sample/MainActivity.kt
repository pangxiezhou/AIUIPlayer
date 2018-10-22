package com.iflytek.aiui.player.auth.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.iflytek.aiui.player.auth.PlayerRemote

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PlayerRemote.init(this, "192.168.1.114")
    }
}
