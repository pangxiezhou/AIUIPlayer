package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject


class GetToken(from: String?, serializeData: String? = null): Request(serializeData){
    companion object {
        fun forSource(source: String): GetToken {
           return GetToken(source)
        }

        fun deserializeFrom(serializeData: String): GetToken {
            return GetToken(null, serializeData)
        }
    }


    val source: String?

    init {
        source = from ?:  JSONObject(serializeData).optJSONObject("params")?.optString("source")
        addParams("source", source!!)
    }

    override fun methodName(): String {
        return "getAuth"
    }
}