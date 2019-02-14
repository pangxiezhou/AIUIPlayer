package com.iflytek.aiui.player.players

import android.content.Context
import com.sitech.migurun.init.MiGuRun

class MiGuPlayerNative {
    companion object {
        private lateinit var mUID: String
        private lateinit var mDeviceID: String
        private lateinit var mPhone: String
        private lateinit var mChannelCode: String

        fun initWith(uid: String, deviceID: String, phone: String, channelCode: String) {
            mUID = uid
            mDeviceID = deviceID
            mPhone  = phone
            mChannelCode = channelCode

        }

        fun initWith( deviceID: String,  channelCode: String) {
            mUID = ""
            mDeviceID = deviceID
            mPhone  = ""
            mChannelCode = channelCode

        }

        fun initMiGu(context: Context) {
            MiGuRun(context, mUID, mDeviceID, mPhone, mChannelCode)
        }
    }
}