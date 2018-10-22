package com.iflytek.aiui.player.remote

import android.content.Context
import android.content.Intent
import com.iflytek.aiui.player.remote.login.KuGouLoginActivity
import com.iflytek.aiui.player.common.rpc.RPC
import com.iflytek.aiui.player.common.rpc.RPCListener
import com.iflytek.aiui.player.common.rpc.connection.impl.WebSocketClientConnection
import com.iflytek.aiui.player.common.rpc.method.SourceType
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import org.json.JSONObject

object PlayerRemote {
    private var rpc: RPC? = null
    private var connection: WebSocketClientConnection? = null
    private var stopOnDestroy = false

    val rpcProxy
        get() = rpc

    fun init(context: Context, host: String? = null) {
        val connection = WebSocketClientConnection(host ?: "localhost", 4096)
        init(context, connection)
        stopOnDestroy = true
    }

    fun init(context: Context, connection: WebSocketClientConnection) {
        stopOnDestroy = false
        this.connection = connection
        rpc = RPC(connection!!, object : RPCListener {
            override fun onRequest(rpc: RPC, data: String) {
                val req = JSONObject(data)
                when (req.optString("method")) {
                    "getAuth" -> {
                        val getAuthReq = TokenReq.createFromJSON(data)
                        when (getAuthReq.source) {
                            SourceType.KuGou -> {
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

    fun destroy() {
        if(stopOnDestroy) {
            connection?.stop()
        }
        rpc?.reset()
    }
}