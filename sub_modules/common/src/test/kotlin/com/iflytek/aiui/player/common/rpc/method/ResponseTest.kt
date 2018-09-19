package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

class ResponseTest {

    @Test
    fun valueString() {
        val reqID = 1
        val result = "fake.result"
        val response = Response(reqID, result)
        val jsonResp = JSONObject(response.toJSONString())

        assertEquals(jsonResp.optString("jsonrpc"), "2.0")
        assertEquals(jsonResp.optInt("id"), reqID)
        assertEquals(jsonResp.optString("result"), result)
    }

    @Test
    fun valueInteger() {
        val reqID = 1
        val result = 666
        val response = Response(reqID, result)
        val jsonResp = JSONObject(response.toJSONString())

        assertEquals(jsonResp.optString("jsonrpc"), "2.0")
        assertEquals(jsonResp.optInt("id"), reqID)
        assertEquals(jsonResp.optInt("result"), result)
    }
}