//
// Created by PR on 2018/8/28.
//

#include <string.h>
#include <jni.h>

#include "kugou_player_init.h"

char kuGouKey[] = "29UqRDzSCIZqOkgXIEjXsnsZ3EmPuXXmYP06";


JNIEXPORT void JNICALL Java_com_iflytek_aiui_player_players_KuGouPlayerNative_initKuGouMusic(JNIEnv *env, jclass thisClass){
    char _app_id[5], _key[33];

    _app_id[0] = kuGouKey[0];
    _app_id[1] = kuGouKey[1];
    _app_id[2] = kuGouKey[strlen(kuGouKey) - 2];
    _app_id[3] = kuGouKey[strlen(kuGouKey) - 1];
    _app_id[4] = '\0';

    int cursor = 0;
    for(int index = 2; index < strlen(kuGouKey) - 2; index+=1) {
        _key[cursor++] = kuGouKey[index];
    }
    _key[32] = '\0';


    jstring appId = env->NewStringUTF(_app_id);
    jstring appKey = env->NewStringUTF(_key);

    jclass kgSDKClass=env->FindClass("com/kugou/kgmusicsdk/KGMusicSDK");
    jmethodID kgGetInstance = env->GetStaticMethodID(kgSDKClass, "sharedInstance", "()Lcom/kugou/kgmusicsdk/KGMusicSDK;");
    jobject kgSDKInstance = env->CallStaticObjectMethod(kgSDKClass, kgGetInstance);

    jclass kgMusicSDK = env->GetObjectClass(kgSDKInstance);
    jmethodID registerWithAppid = env->GetMethodID(kgMusicSDK, "registerWithAppID", "(Ljava/lang/String;Ljava/lang/String;)V");

    (env)->CallVoidMethod(kgSDKInstance, registerWithAppid, appId, appKey);
}

