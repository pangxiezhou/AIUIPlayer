package com.iflytek.aiui.player.common.rpc.method

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

enum class SourceType {
    QingTing,
    KuGou;

    override fun toString(): String {
        return when (this) {
            SourceType.QingTing -> "QingTing"
            SourceType.KuGou -> "KuGou"
        }
    }

    companion object {
        fun createFromString(description: String): SourceType? {
            return when (description) {
                "QingTing" -> SourceType.QingTing
                "KuGou" -> SourceType.KuGou
                else -> null
            }
        }
    }
}

class TokenReq : Request, Parcelable {
    val source: SourceType

    constructor(source: SourceType) : super("getAuth") {
        this.source = source
        addParam("source", source.toString())
    }

    constructor(serializeData: JSONObject) : super(serializeData) {
        source = SourceType.createFromString(getStringParam("source"))!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(toJSONString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TokenReq> {
        override fun createFromParcel(parcel: Parcel): TokenReq {
            return createFromJSON(parcel.readString())
        }

        override fun newArray(size: Int): Array<TokenReq?> {
            return arrayOfNulls(size)
        }

        fun createFor(source: SourceType): TokenReq {
            return TokenReq(source)
        }

        fun createFromJSON(serializeData: String): TokenReq {
            return TokenReq(JSONObject(serializeData))
        }
    }
}