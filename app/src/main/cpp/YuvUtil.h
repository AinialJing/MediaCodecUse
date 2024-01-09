//
// Created by company on 2024-01-09.
//

#ifndef MEDIACODECUSE_YUVUTIL_H
#define MEDIACODECUSE_YUVUTIL_H
#include <jni.h>
#include <libyuv.h>
/**
 * yv12 -> i420(yu12)
 * @param src_yv12_data
 * @param width
 * @param height
 * @param dst_i420_data
 */
void yv12ToI420(jbyte *src_yv12_data,jint width,jint height,jbyte* dst_i420_data);

void nv21ToI420(jbyte *src_nv21_data,jint width,jint height,jbyte* dst_i420_data);

void i420ToNv21(jbyte *src_i420_data,jint width,jint height,jbyte* dst_nv21_data);
/**
 * i420缩放
 * @param src_i420_data 原始数据
 * @param width 原始宽
 * @param height 原始高
 * @param dst_i420_data 目标数据
 * @param dst_width 目标宽
 * @param dst_height 目标高
 * @param mode  一般传0，速度最快
 */
void scaleI420(jbyte* src_i420_data,jint width,jint height,jbyte* dst_i420_data,jint dst_width,jint dst_height,jint mode);
/**
 * i420旋转
 * @param src_i420_data
 * @param width
 * @param height
 * @param dst_i420_data
 * @param degree
 */
void rotateI420(jbyte*src_i420_data,jint width,jint height,jbyte*dst_i420_data,jint degree);

void mirrorI420(jbyte*src_i420_data,jint width,jint height,jbyte*dst_i420_data);

#endif //MEDIACODECUSE_YUVUTIL_H
