//
// Created by company on 2024-01-29.
//

#ifndef MEDIACODECUSE_RTMPHANDLER_H
#define MEDIACODECUSE_RTMPHANDLER_H

#include "PacketQueue.h"
#include "librtmp/rtmp.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}

typedef void (*StartErrorCallBack)(int error);

class RtmpHandler {
public:
    RtmpHandler();

    ~RtmpHandler();

    void start(char *);

    void setStartErrorCallBack(StartErrorCallBack startErrorCallBack);

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len, const long tms);

    void sendFrame(int type, uint8_t *payload, int i_payload, const long tms);

    void releasePackets(RTMPPacket *&packet);

    void pushProcessor(void *args);

    void initAndPullRtmpData(ANativeWindow *, char *);

    void pullProcessor(ANativeWindow *nativeWindow, char *url);

private:

    PacketQueue<RTMPPacket *> packets;

    std::atomic<bool> isPushing;
    std::atomic<bool> isPulling;
    uint32_t start_time;

    StartErrorCallBack m_startErrorCallBack;

    AVCodec *mDecodeCodec;
    AVCodecContext *mDecodeContext;
    AVFrame *mDecodeFrame;
    SwsContext *mSwsContext;

};


#endif //MEDIACODECUSE_RTMPHANDLER_H
