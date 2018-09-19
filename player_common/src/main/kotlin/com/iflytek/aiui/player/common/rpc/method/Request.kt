package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject

abstract class Request {
    companion object {
        var lastID = 0

    }


    val id: Int
    val params = JSONObject()
    val method
        get() = methodName()

    constructor(serializeData: String?) {
        id = if(serializeData != null) {
            JSONObject(serializeData).optInt("id")
        } else {
            (++lastID) % 10000
        }
    }

    abstract fun methodName(): String

    protected fun addParams(key: String, value: String) {
        params.put(key, value)
    }


    fun toJSONString():String {
        val description = JSONObject()

        description.put("jsonrpc", "2.0")
        description.put("id", id)
        description.put("method", "getAuth")
        description.put("params", params)

        return description.toString()
    }


    override fun equals(other: Any?): Boolean {
        return if(other !is Request) {
            false
        } else {
             id == other.id && method == other.method && params.toString() == other.params.toString()
        }
    }


    override fun hashCode(): Int {
        return id.hashCode() + method.hashCode() + params.hashCode()
    }
}
