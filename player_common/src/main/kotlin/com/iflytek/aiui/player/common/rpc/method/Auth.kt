package com.iflytek.aiui.player.common.rpc.method

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject


class GetToken(from: String?, serializeData: String? = null): Request(serializeData), Parcelable{
    val source: String?

    init {
        source = from ?:  JSONObject(serializeData).optJSONObject("params")?.optString("source")
        addParams("source", source!!)
    }

    override fun methodName(): String {
        return "getAuth"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(source)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GetToken> {
        override fun createFromParcel(parcel: Parcel): GetToken {
            val description = JSONObject()

            description.put("jsonrpc", "2.0")
            description.put("id", parcel.readInt())
            description.put("method", "getAuth")
            description.put("params", JSONObject(hashMapOf(
                    "source" to parcel.readString()
            )))

            return deserializeFrom(description.toString())
        }

        override fun newArray(size: Int): Array<GetToken?> {
            return arrayOfNulls(size)
        }

        fun forSource(source: String): GetToken {
            return GetToken(source)
        }

        fun deserializeFrom(serializeData: String): GetToken {
            return GetToken(null, serializeData)
        }
    }
}