package com.iflytek.aiui.player.common.rpc.method

import org.json.JSONObject

/**
 * JSONRPC 请求对象抽象
 */
abstract class Request {
    companion object {
        var lastID = 0
    }

    val id: Int
    val params: JSONObject
    val method: String

    /**
     * 从JSON输出反序列化出请求对象
     */
    constructor(serializeData: JSONObject) {
        id = serializeData.optInt("id")
        params = serializeData.optJSONObject("params")
        method = serializeData.optString("method")
    }

    /**
     * 构造新请求对象
     */
    constructor(method: String) {
        id =  (++lastID) % 10000
        params = JSONObject()
        this.method = method
    }

    /**
     * 添加请求参数
     */
    protected fun addParam(key: String, value: String) {
        params.put(key, value)
    }

    /**
     * 获取请求参数
     */
    protected fun getStringParam(key: String): String {
        return params.optString(key) ?: ""
    }

    /**
     * 构造JSONRPC格式的表示
     */
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
