package com.iflytek.aiui.player.auth.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.iflytek.aiui.player.auth.login.AuthRPC
import com.merry.sample.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AuthRPC.init(this)
    }
}
