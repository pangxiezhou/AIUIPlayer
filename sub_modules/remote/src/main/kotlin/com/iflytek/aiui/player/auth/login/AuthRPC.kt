package com.iflytek.aiui.player.auth.login

import android.content.Context
import android.content.Intent
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.method.GetToken
import org.json.JSONObject

object AuthRPC {
    private var rpc: RPC? = null

    val rpcProxy
        get() = rpc

    fun init(context: Context) {
        val connection = WebSocketClientConnection("localhost", 4096)
        rpc = RPC(connection, object : RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = JSONObject(data)
                when (req.optString("method")) {
                    "getAuth" -> {
                        val getAuthReq = GetToken.deserializeFrom(data)
                        when (getAuthReq.source) {
                            "kugou" -> {
                                val intent = Intent(context, KuGouLoginActivity::class.java)
                                intent.putExtra("request", getAuthReq)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        })
        connection.start()
    }
}