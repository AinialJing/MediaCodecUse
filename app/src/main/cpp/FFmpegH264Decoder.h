//
// Created by company on 2024-01-24.
//

#ifndef MEDIACODECUSE_FFMPEGH264DECODER_H
#define MEDIACODECUSE_FFMPEGH264DECODER_H
#include <android/native_window.h>
#include <android/native_window_jni.h>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include "libavutil/imgutils.h"
}

class FFmpegH264Decoder {
public:
    FFmpegH264Decoder(ANativeWindow *aNativeWindow);

    ~FFmpegH264Decoder();

    void decodeH264Packet(unsigned char *pBUff, int len);

private:
    AVCodec *mDecodeCodec;
    AVCodecContext *mDecodeContext;
    AVFrame *mDecodeFrame;
    ANativeWindow *mNativeWindow;
    SwsContext *mSwsContext;
};


#endif //MEDIACODECUSE_FFMPEGH264DECODER_H
