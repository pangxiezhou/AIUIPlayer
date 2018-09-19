package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject

class Response<T> {
    private val id: Int
    private val result: T

    constructor(req_id: Int, value: T) {
        id = req_id
        result = value
    }

    fun toJSONString(): String {
        val description = JSONObject()
        description.put("jsonrpc", "2.0")
        description.put("id", id)
        description.put("result", result)

        return description.toString()
    }
}