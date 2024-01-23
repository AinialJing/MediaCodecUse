#include <jni.h>
#include <cstdio>
#include <ctime>
#include <malloc.h>
#include <cstring>
#include <string>
#include "Log.h"
#include "YuvUtil.h"
#include "X264Handle.h"

JavaVM *gJavaVM;
jclass mObjectClass = NULL;
static jmethodID jniEncodeCallBackId = nullptr;

void jniEncodeCallBack(const uint8_t *data, int dataSize);

extern "C" {
X264Handle *x264Handle = nullptr;


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

extern "C"
JNIEXPORT jint JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_initX264Encode(JNIEnv *env, jobject thiz,
                                                               jint width,
                                                               jint height) {
    env->GetJavaVM(&gJavaVM);
    mObjectClass = (jclass) env->NewGlobalRef((jobject) env->GetObjectClass(thiz));
    jniEncodeCallBackId = env->GetStaticMethodID(mObjectClass, "jniEncodeCallBack",
                                                 "([B)V");
    x264Handle = new X264Handle();
    int ret = x264Handle->initEncode(width, height, 10, 800000);
    x264Handle->setEncodeCallBack(jniEncodeCallBack);
    LOGE("x264 init finished");
    return ret;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_x264Encode(JNIEnv *env, jobject thiz,
                                                           jbyteArray src_data) {
    LOGE("x264Encode start");
    if (!x264Handle) {
        LOGE("x264Handle is null");
        return;
    }
    jbyte *data = env->GetByteArrayElements(src_data, JNI_FALSE);
    x264Handle->x264Encode(data);
    env->ReleaseByteArrayElements(src_data, data, 0);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_releaseX264(JNIEnv *env, jobject thiz) {
    delete x264Handle;
    x264Handle = nullptr;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_intFFMpegDecode(JNIEnv *env, jobject thiz,
                                                                jint width, jint height) {


}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_ffmpegDecode(JNIEnv *env, jobject thiz,
                                                             jbyteArray src_data) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_mediacodecuse_utils_MediaUtil_releaseFFMpegDecode(JNIEnv *env, jobject thiz) {

}

}

void jniEncodeCallBack(const uint8_t *data, int len) {
    LOGD("x264 encode callBack size:%d", len);
    JNIEnv *env;
    //从全局的JavaVM中获取到环境变量
    gJavaVM->AttachCurrentThread(&env, NULL);
    jbyteArray jbarray = env->NewByteArray(len);
    jbyte *jy = reinterpret_cast<jbyte *>(const_cast<uint8_t *>(data));
    env->SetByteArrayRegion(jbarray, 0, len, jy);
    env->CallStaticVoidMethod(mObjectClass, jniEncodeCallBackId, jbarray);
    env->DeleteLocalRef(jbarray);
//    gJavaVM->DetachCurrentThread();
}
