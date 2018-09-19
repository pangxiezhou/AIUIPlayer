package com.iflytek.aiui.player.common.rpc.method

import com.iflytek.aiui.player.common.rpc.method.GetToken
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull


class GetTokenTest {
    @Test
    fun equal() {
        val firstReq = GetToken.forSource("qingting")
        val secondReq = GetToken.forSource("qingting")

        assertNotEquals(firstReq, secondReq)
    }

    @Test
    fun serialize() {
        val tokenReq = GetToken.forSource("qingting")
        val serializeData = JSONObject(tokenReq.toJSONString())

        //检测是否符合jsonrpc的输出格式
        assertEquals(serializeData.getString("jsonrpc"), "2.0")
        assertNotNull(serializeData.getInt("id"))
        assertEquals(serializeData.getString("method"), "getAuth")
        assertEquals(serializeData.getJSONObject("params").getString("source"), "qingting")
    }

    @Test
    fun deserialize() {
        val tokenReq = GetToken.forSource("qingting")
        val forkTokenReq = GetToken.deserializeFrom(tokenReq.toJSONString())

        assertEquals(tokenReq, forkTokenReq)
    }
}