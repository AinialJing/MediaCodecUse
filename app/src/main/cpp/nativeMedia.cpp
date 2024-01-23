#include <jni.h>
#include <cstdio>
#include <ctime>
#include <malloc.h>
#include <cstring>
#include <string>
#include "Log.h"
#include "YuvUtil.h"

extern "C" {


extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_yv12ToI420(JNIEnv *env, jobject thiz,
                                                           jbyteArray yv12, jbyteArray i420,
                                                           jint width, jint height) {
    jbyte *src_yv12_data = env->GetByteArrayElements(yv12, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(i420, JNI_FALSE);
    yv12ToI420(src_yv12_data, width, height, dst_i420_data);

    env->ReleaseByteArrayElements(yv12, src_yv12_data, 0);
    env->ReleaseByteArrayElements(i420, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_nv21ToI420(JNIEnv *env, jobject thiz,
                                                           jbyteArray src_nv21_array, jint width,
                                                           jint height, jbyteArray dst_i420_array) {
    jbyte *src_nv21_data = env->GetByteArrayElements(src_nv21_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);
    nv21ToI420(src_nv21_data, width, height, dst_i420_data);

    env->ReleaseByteArrayElements(src_nv21_array, src_nv21_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_i420ToNv21(JNIEnv *env, jobject thiz,
                                                           jbyteArray src_i420_array, jint width,
                                                           jint height, jbyteArray dst_nv21_array) {
    jbyte *src_i420_data = env->GetByteArrayElements(src_i420_array, JNI_FALSE);
    jbyte *dst_nv21_data = env->GetByteArrayElements(dst_nv21_array, JNI_FALSE);
    i420ToNv21(src_i420_data, width, height, dst_nv21_data);

    env->ReleaseByteArrayElements(src_i420_array, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_nv21_array, dst_nv21_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_i420Rotate(JNIEnv *env, jobject thiz,
                                                           jbyteArray src_i420_array, jint width,
                                                           jint height, jbyteArray dst_i420_array,
                                                           jint degree) {
    jbyte *src_i420_data = env->GetByteArrayElements(src_i420_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);

    rotateI420(src_i420_data, width, height, dst_i420_data, degree);

    env->ReleaseByteArrayElements(src_i420_array, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_i420Mirror(JNIEnv *env, jobject thiz,
                                                           jbyteArray src_i420_array, jint width,
                                                           jint height, jbyteArray dst_i420_array) {
    jbyte *src_i420_data = env->GetByteArrayElements(src_i420_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);

    mirrorI420(src_i420_data, width, height, dst_i420_data);

    env->ReleaseByteArrayElements(src_i420_array, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_i420Scale(JNIEnv *env, jobject thiz,
                                                          jbyteArray src_i420_array, jint width,
                                                          jint height, jbyteArray dst_i420_array,
                                                          jint dst_width, jint dst_height,
                                                          jint filter_mode) {
    jbyte *src_i420_data = env->GetByteArrayElements(src_i420_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);

    scaleI420(src_i420_data, width, height, dst_i420_data, dst_width, dst_height, filter_mode);

    env->ReleaseByteArrayElements(src_i420_array, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}

}

