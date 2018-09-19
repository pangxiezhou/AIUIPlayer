package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

class ErrorTest {

    @Test
    fun toJSONString() {
        val reqID = 1
        val code = 6553
        val message = "fake error message"
        val response = Error(reqID, code, message)
        val jsonResp = JSONObject(response.toJSONString())

        assertEquals(jsonResp.optString("jsonrpc"), "2.0")
        assertEquals(jsonResp.optInt("id"), reqID)
        assertTrue(jsonResp.has("error"))

        val error = jsonResp.optJSONObject("error")
        assertEquals(error.optInt("code"), code)
        assertEquals(error.optString("message"), message)

    }
}