//
// Created by PR on 2018/8/28.
//

#include <string.h>
#include <jni.h>

#include "migu_player_init.h"

char chCodeRaw[] = "dbb6d32a813c64ca";

JNIEXPORT void JNICALL Java_com_iflytek_aiui_player_players_MiGuPlayerNative_initMiGu(JNIEnv *env, jclass thisClass, jobject context){

    jstring chCode = env->NewStringUTF(chCodeRaw);
    jstring empty = env->NewStringUTF("");

    jclass miguClass = env->FindClass("com/sitech/migurun/init/MiGuRun");
    jmethodID initMethod = env->GetMethodID(miguClass , "<init>", "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    env->NewObject(miguClass, initMethod, context, empty, empty, empty, chCode);
}


