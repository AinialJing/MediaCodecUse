#include <jni.h>
#include <cstdio>
#include <ctime>
#include <malloc.h>
#include <cstring>
#include <string>
#include "Log.h"
#include "YuvUtil.h"
#include "RtmpHandler.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

JavaVM *gJavaVM;
jclass mObjectClass = nullptr;
jmethodID errorCallBackId = nullptr;

RtmpHandler *rtmpHandler;

ANativeWindow *m_nativeWindow;

void throwErrToJava(int error_code);

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


extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_connectRtmp(JNIEnv *env, jobject thiz,
                                                            jstring url_) {
    env->GetJavaVM(&gJavaVM);
    mObjectClass = (jclass) env->NewGlobalRef((jobject) env->GetObjectClass(thiz));
    errorCallBackId = env->GetStaticMethodID(mObjectClass, "connectErrorCallBack", "(I)V");
    const char *path = env->GetStringUTFChars(url_, JNI_FALSE);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);
    rtmpHandler = new RtmpHandler();
    rtmpHandler->setStartErrorCallBack(throwErrToJava);
    rtmpHandler->start(url);
    env->ReleaseStringUTFChars(url_, path);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_sendRtmpData(JNIEnv *env, jobject thiz,
                                                             jbyteArray data, jint len, jlong tms,
                                                             jint type) {
    jbyte *encodeData = env->GetByteArrayElements(data, JNI_FALSE);
    if (type == 1) {//key frame
        rtmpHandler->sendFrame(type, reinterpret_cast<uint8_t *>(encodeData), len, tms);
    } else if (type == 2) {//sps pps
        int pps_len, sps_len = 0;
        uint8_t sps[100];
        uint8_t pps[100];
        for (int i = 0; i < len; i++) {
            //防止越界
            if (i + 4 < len) {
                if (encodeData[i] == 0x00 && encodeData[i + 1] == 0x00
                    && encodeData[i + 2] == 0x00
                    && encodeData[i + 3] == 0x01) {
                    if (encodeData[i + 4] == 0x68) {
                        sps_len = i - 4;
                        //sps解析
                        memcpy(sps, encodeData + 4, sps_len);
                        //解析pps
                        pps_len = len - (4 + sps_len) - 4;
                        memcpy(pps, encodeData + 4 + sps_len + 4, pps_len);
                        LOGI("sps:%d pps:%d", sps_len, pps_len);
                        rtmpHandler->sendSpsPps(sps, pps, sps_len, pps_len, tms);
                        break;
                    }
                }

            }
        }
    } else {//frame
        rtmpHandler->sendFrame(type, reinterpret_cast<uint8_t *>(encodeData), len, tms);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_releaseRtmp(JNIEnv *env, jobject thiz) {
    delete rtmpHandler;
    rtmpHandler = nullptr;
}

void throwErrToJava(int error_code) {
    JNIEnv *env;
    //从全局的JavaVM中获取到环境变量
    gJavaVM->AttachCurrentThread(&env, NULL);
    LOGD("nativeMedia:%d,error_code:%d", __LINE__, error_code);
    env->CallStaticVoidMethod(mObjectClass, errorCallBackId, error_code);
    //要解绑当前线程
    gJavaVM->DetachCurrentThread();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_initAndPullRtmpData(JNIEnv *env, jobject thiz,
                                                                    jobject surface, jstring url_) {
    m_nativeWindow = ANativeWindow_fromSurface(env, surface);
    const char *path = env->GetStringUTFChars(url_, JNI_FALSE);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);
    rtmpHandler->initAndPullRtmpData(m_nativeWindow, url);
    env->ReleaseStringUTFChars(url_, path);
}