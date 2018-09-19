package com.iflytek.aiui.player.common.rpc.method

import android.os.Parcel
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull


class TokenReqTest {
    @Test
    fun equal() {
        val firstReq = TokenReq.createFor(SourceType.QingTing)
        val secondReq = TokenReq.createFor(SourceType.QingTing)

        assertNotEquals(firstReq, secondReq)
    }

    @Test
    fun idGen() {
        val firstReq = TokenReq.createFor(SourceType.QingTing)
        val secondReq = TokenReq.createFor( SourceType.QingTing)
        val thirdReq = TokenReq.createFor(SourceType.QingTing)

        assertEquals(secondReq.id - firstReq.id, 1)
        assertEquals(thirdReq.id - firstReq.id, 2)
    }

    @Test
    fun serialize() {
        val tokenReq = TokenReq.createFor( SourceType.QingTing)
        val serializeData = JSONObject(tokenReq.toJSONString())

        //检测是否符合jsonrpc的输出格式
        assertEquals(serializeData.getString("jsonrpc"), "2.0")
        assertNotNull(serializeData.getInt("id"))
        assertEquals(serializeData.getString("method"), "getAuth")
        assertEquals(serializeData.getJSONObject("params").getString("source"), "qingting")
    }

    @Test
    fun parcelDeserialize() {
        //mock Parcel
        var parcelStr = ""
        val parcel = mock<Parcel>()
        whenever(parcel.writeString(any())).then {
            parcelStr = it.arguments[0] as String
            null
        }
        whenever(parcel.readString()).then {
            parcelStr
        }

        val req = TokenReq.createFor( SourceType.QingTing)
        req.writeToParcel(parcel, 0)
        val deserializeReq = TokenReq.createFromParcel(parcel)

        assertEquals(req, deserializeReq)
    }

    @Test
    fun jsonDeserialize() {
        val tokenReq = TokenReq.createFor( SourceType.QingTing)
        val forkTokenReq = TokenReq.createFromJSON(tokenReq.toJSONString())

        assertEquals(tokenReq, forkTokenReq)
    }
}