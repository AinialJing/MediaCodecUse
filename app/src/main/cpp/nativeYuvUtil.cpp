#include <jni.h>
#include <cstdio>
#include <ctime>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <malloc.h>
#include <cstring>
#include <libyuv.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string>
#include "ConvertNV.h"
#include "Log.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_YuvUtil_nv21ToNV12Rotate(JNIEnv *env, jobject clazz,
                                                               jbyteArray nv21_, jbyteArray nv12_,
                                                               jint w, jint h, jint orientation) {
    jbyte *nv21 = env->GetByteArrayElements(nv21_, JNI_FALSE);
    jbyte *nv12 = env->GetByteArrayElements(nv12_, JNI_FALSE);

    int ySize = w * h;
    const uint8_t *nv21Y = reinterpret_cast<const uint8_t *>(nv21);
    const uint8_t *nv21VU = nv21Y + ySize;
    uint8_t *nv12Y = reinterpret_cast<uint8_t *>(nv12);
    uint8_t *nv12UV = nv12Y + ySize;
    LOGD("nv21Rotate %d", orientation);
    switch (orientation) {
        case libyuv::kRotate0:
            NV21ToNV12Rotate(nv21Y, w, nv21VU, w, nv12Y, w, nv12UV, w, w, h, libyuv::kRotate0);
            break;
        case libyuv::kRotate90:
            NV21ToNV12Rotate(nv21Y, w, nv21VU, w, nv12Y, h, nv12UV, h, w, h, libyuv::kRotate90);
            break;
        case libyuv::kRotate180:
            NV21ToNV12Rotate(nv21Y, w, nv21VU, w, nv12Y, w, nv12UV, w, w, h, libyuv::kRotate180);
            break;
        case libyuv::kRotate270:
            NV21ToNV12Rotate(nv21Y, w, nv21VU, w, nv12Y, h, nv12UV, h, w, h, libyuv::kRotate270);
            break;
    }

    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    env->ReleaseByteArrayElements(nv12_, nv12, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_YuvUtil_nv21Rotate(JNIEnv *env, jclass clazz, jbyteArray nv21_,
                                                         jbyteArray outs_, jint w, jint h,
                                                         jint orientation) {
    jbyte *nv21 = env->GetByteArrayElements(nv21_, JNI_FALSE);
    jbyte *outs = env->GetByteArrayElements(outs_, JNI_FALSE);

    int ySize = w * h;
    const uint8_t *nv21Y = reinterpret_cast<const uint8_t *>(nv21);
    const uint8_t *nv21VU = nv21Y + ySize;

    uint8_t *outsY = reinterpret_cast<uint8_t *>(outs);
    uint8_t *outsVU = outsY + ySize;

    LOGD("nv21Rotate %d", orientation);
    switch (orientation) {
        case libyuv::kRotate0:
            memcpy(outsY, nv21Y, env->GetArrayLength(nv21_));//直接拷贝内存
            break;
        case libyuv::kRotate90:
            NV21Rotate(nv21Y, w, nv21VU, w, outsY, h, outsVU, h, w, h, libyuv::kRotate90);
            break;
        case libyuv::kRotate180:
            NV21Rotate(nv21Y, w, nv21VU, w, outsY, w, outsVU, w, w, h, libyuv::kRotate180);
            break;
        case libyuv::kRotate270:
            NV21Rotate(nv21Y, w, nv21VU, w, outsY, h, outsVU, h, w, h, libyuv::kRotate270);
            break;
    }

    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    env->ReleaseByteArrayElements(outs_, outs, 0);
}