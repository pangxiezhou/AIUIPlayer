//
// Created by PR on 2018/8/28.
//

#ifndef AIUIPLAYER_PLAYER_AUTH_H
#define AIUIPLAYER_PLAYER_AUTH_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_iflytek_aiui_player_init_ThirdPartyPlayers_initQTFM(JNIEnv *, jclass, jobject);
JNIEXPORT void JNICALL Java_com_iflytek_aiui_player_init_ThirdPartyPlayers_initKuGouMusic(JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif //AIUIPLAYER_PLAYER_AUTH_H
