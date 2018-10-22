package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject

class Error(val id: Int, private val code: Int, val message: String) {

    fun toJSONString(): String {
        val error = JSONObject()
        error.put("code", code)
        error.put("message", message)

        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", id)
        description.put("error", error)

        return description.toString()
    }
}