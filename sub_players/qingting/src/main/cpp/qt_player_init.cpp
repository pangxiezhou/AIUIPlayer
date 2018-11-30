//
// Created by PR on 2018/8/28.
//

#include <string.h>
#include <jni.h>

#include "qt_player_init.h"

char qtKey[] = "ZYTTQF2hNNTDkYwyNNGTUUttNMmjMY1wOYSi00xzMZWmUQ31LLTTkgywMZ2mYIttMYDTAEx1NYjTNkl1MMDDAIy0MMGjFhkj";

JNIEXPORT void JNICALL Java_com_iflytek_aiui_player_players_QTPlayerNative_initQTFM(JNIEnv *env, jclass thisClass, jobject context){
    char _client_id[49], _key[49];

    int cursor = 0;
    for(int index = 0; index < strlen(qtKey); index+=2) {
        _client_id[cursor] = qtKey[index];
        _key[cursor] = qtKey[index + 1];
        cursor++;
    }

    _client_id[48] = '\0';
    _key[48] = '\0';

    jstring client_ID = env->NewStringUTF(_client_id);
    //jstring appkey = env->NewStringUTF(_key);

    jclass QTSDKClass=env->FindClass("fm/qingting/qtsdk/QTSDK");
    jmethodID initMethod = env->GetStaticMethodID(QTSDKClass, "init", "(Landroid/content/Context;Ljava/lang/String;)V");

    env->CallStaticVoidMethod(QTSDKClass, initMethod, context, client_ID);
}


