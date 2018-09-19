package com.iflytek.aiui.player.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import org.json.JSONObject

class PlayerRemoteService:Service() {
    private lateinit var clientRPC: RPC

    override fun onBind(p0: Intent?): IBinder? {
       return null
    }

    override fun onCreate() {
        super.onCreate()

        val connection = WebSocketClientConnection("localhost", 4096)
        clientRPC = RPC(connection, object: RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = JSONObject(data)
                when(req.optString("method")) {
                    "getAuth" -> {
                        val getAuthReq = TokenReq.createFromJSON(data)
                        when(getAuthReq.source) {
                            SourceType.KuGou -> {

                            }
                        }
                    }
                }
            }
        })
    }
}