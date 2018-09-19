package com.iflytek.aiui.player.common.rpc

import org.json.JSONObject


class GetToken {
    companion object {
        var lastID = 0

        fun forSource(source: String): GetToken {
           return GetToken(source)
        }

        fun deserializeFrom(serializeData: String): GetToken {
            return GetToken(null, serializeData)
        }
    }


    val id: Int
    val source: String?

    private constructor(source: String?, serializeData: String? = null) {
        if(source != null) {
            this.source = source
            id = (++lastID) % 10000
        } else {
            val data = JSONObject(serializeData)
            this.source = data.optJSONObject("params")?.optString("source")
            this.id = data.optInt("id")
        }
    }


    fun toJSONString():String {
       val description = JSONObject()

        val params = JSONObject()
        params.put("source", source)

        description.put("jsonrpc", "2.0")
        description.put("id", id)
        description.put("method", "getAuth")
        description.put("params", params)

        return description.toString()
    }

    override fun equals(other: Any?): Boolean {
        return if(other !is GetToken) {
           false
        } else {
           source == other.source && id == other.id
        }
    }

    override fun hashCode(): Int {
        return id.hashCode() + (source?.hashCode() ?: 0)
    }
}