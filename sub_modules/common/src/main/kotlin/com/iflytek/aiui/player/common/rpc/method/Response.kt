package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject

class Response<T>(req_id: Int, value: T) {
    private val id: Int = req_id
    private val result: T = value

    fun toJSONString(): String {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", id)
        description.put("result", result)

        return description.toString()
    }
}