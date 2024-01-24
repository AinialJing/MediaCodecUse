//
// Created by company on 2024-01-24.
//

#include "FFmpegH264Decoder.h"
#include "Log.h"

FFmpegH264Decoder::FFmpegH264Decoder(ANativeWindow *aNativeWindow) {
    mDecodeContext = nullptr;
    mDecodeFrame = nullptr;
    mNativeWindow = aNativeWindow;

    mDecodeCodec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (mDecodeCodec == nullptr) {
        LOGE("find decode codec error");
        return;
    }
    mDecodeContext = avcodec_alloc_context3(mDecodeCodec);
    if (mDecodeContext == nullptr) {
        LOGE("alloc context error");
        return;
    }

    if (avcodec_open2(mDecodeContext, mDecodeCodec, NULL) < 0) {
        LOGE("avcodec_open2 error");
        avcodec_free_context(&mDecodeContext);
        return;
    }
    mDecodeFrame = av_frame_alloc();
    if (mDecodeFrame == nullptr) {
        LOGE("av_frame_alloc error");
        avcodec_close(mDecodeContext);
        return;
    }
}

FFmpegH264Decoder::~FFmpegH264Decoder() {
    if (mDecodeContext != nullptr) {
        avcodec_close(mDecodeContext);
        mDecodeContext = nullptr;
    }
    if (mDecodeFrame != nullptr) {
        av_frame_free(&mDecodeFrame);
        mDecodeFrame = nullptr;
    }
    av_frame_free(&mDecodeFrame);
    ANativeWindow_release(mNativeWindow);
}

void FFmpegH264Decoder::decodeH264Packet(unsigned char *pBuff, int len) {
    AVPacket *mDecodePacket = av_packet_alloc();
    mDecodePacket->data = pBuff;
    mDecodePacket->size = len;

    int ret;
    // 将要解码的数据包送入解码器
    ret = avcodec_send_packet(mDecodeContext, mDecodePacket);
    if (ret < 0) {
        LOGE("Error sending a packet for decoding");
        av_packet_unref(mDecodePacket);
        return;
    }
    LOGE("decode success");
    //从解码器内部缓存中提取解码后的音视频帧
    ret = avcodec_receive_frame(mDecodeContext, mDecodeFrame);
    if (ret < 0) {
        av_packet_unref(mDecodePacket);
        LOGE("avcodec_receive_frame error");
        return;
    }
    LOGE("decode:%d", __LINE__);
    mSwsContext = sws_getContext(mDecodeContext->width, mDecodeContext->height,
                                 mDecodeContext->pix_fmt, mDecodeContext->width,
                                 mDecodeContext->height, AV_PIX_FMT_RGBA, SWS_BILINEAR,
                                 0, 0, 0);
    LOGE("decode:%d", __LINE__);
    // 设置渲染格式和大小
    ANativeWindow_setBuffersGeometry(mNativeWindow, mDecodeContext->width,
                                     mDecodeContext->height,
                                     WINDOW_FORMAT_RGBA_8888);
    // 分配渲染缓冲区
    LOGE("decode:%d", __LINE__);
    ANativeWindow_Buffer outBuffer;
    uint8_t *dst_data[4];
    int dst_linesize[4];
    av_image_alloc(dst_data, dst_linesize, mDecodeContext->width, mDecodeContext->height,
                   AV_PIX_FMT_RGBA, 1);
    // 锁定 Surface 并获取渲染缓冲区
    LOGE("decode:%d", __LINE__);
    ANativeWindow_lock(mNativeWindow, &outBuffer, NULL);
    // 将解码后的帧转换为目标格式
    LOGE("decode:%d", __LINE__);
    sws_scale(mSwsContext, mDecodeFrame->data, mDecodeFrame->linesize, 0, mDecodeFrame->height,
              dst_data, dst_linesize);

    //渲染
    LOGE("decode:%d", __LINE__);
    uint8_t *first = static_cast<uint8_t *>(outBuffer.bits);
    uint8_t *src_data = dst_data[0];
    int dstStride = outBuffer.stride * 4;
    int src_linesize = dst_linesize[0];
    for (int i = 0; i < outBuffer.height; ++i) {
        memcpy(first + i * dstStride, src_data + i * src_linesize, dstStride);
    }
    // 解锁 Surface
    LOGE("decode:%d", __LINE__);
    ANativeWindow_unlockAndPost(mNativeWindow);
    sws_freeContext(mSwsContext);
    av_packet_unref(mDecodePacket);
}

