
package com.kugou.common.utils;

import android.util.Log;


public class MusicJNI
{
    private final static String TAG = "MusicJNI";


    static public int KgHwsdkLogin(String inputToken, Object info, int userId) {
        Log.i(TAG, "===========KgHwsdkLogin==" + inputToken);
        return kg_hw_sdk_login(inputToken, info, userId);
    }

    static public int KgNativeInit() {
        Log.i(TAG, "===========setUnInit");
        return kg_native_init();
    }


    static public int KgHwSdkLogout() {
        Log.i(TAG, "KgHwSdkLogout=======");
        return  kg_hw_sdk_logout();
    }

    static public int KgHwsdkGetUrl(String hash, Object info, int albumId, int max_length) {
        Log.i(TAG, "KgHwsdkGetUrl=======");
        return  kg_hw_sdk_get_song_url(hash, info, albumId, max_length);
    }

    static public int KgHwsdkGetInfo(String hash, Object info, int max_length) {
        Log.i(TAG, "KgHwsdkGetInfo=======");
        return  kg_hw_sdk_get_song_info(hash, info, max_length);
    }

    static public int KgHwsdkGetRadio(Object info,int radioId, int radioType, int offset, int count, int max_length) {
        Log.i(TAG, "KgHwsdkGetRadio=======");
        return  kg_hw_sdk_get_radio_songs(info, radioId, radioType, offset,count, max_length);
    }

    static public int KgHwsdkGetCollection(Object info, int collectionId, int max_length) {
        Log.i(TAG, "KgHwsdkGetCollection=======");
        return  kg_hw_sdk_get_collection_songs(info, collectionId, max_length);
    }
    // maxLength for reserved, default value is 0
    static  native int kg_native_init();
    static  native int kg_hw_sdk_login(String inputToken, Object Info, int userId);
    static  native int kg_hw_sdk_logout();
    static  native int kg_hw_sdk_get_song_url(String input,Object Info, int albumId, int maxLength);
    static  native int kg_hw_sdk_get_song_info(String input,Object Info, int maxLength);
    static  native int kg_hw_sdk_get_radio_songs(Object Info, int radioId, int radioType, int offset, int count, int max_length);
    static  native int kg_hw_sdk_get_collection_songs(Object Info, int collectionId, int maxLength);

}
