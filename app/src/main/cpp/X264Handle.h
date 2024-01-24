//
// Created by company on 2024-01-17.
//

#ifndef MEDIACODECUSE_X264HANDLE_H
#define MEDIACODECUSE_X264HANDLE_H

#include <inttypes.h>
#include <stdlib.h>
#include <mutex>
#include "Log.h"
#include <x264.h>
#include <x264_config.h>

typedef void (*EncodeCallBack)(const uint8_t* data, int dataSize);

class X264Handle {
private:
    std::mutex m_mutex;

    int m_frameLen;
    x264_t *videoCodec = 0;
    x264_picture_t *pic_in = 0;
    EncodeCallBack m_encodeCallBack;
public:
    X264Handle();

    ~X264Handle();

    int initEncode(int width, int height, int fps, int bitrate);

    void setEncodeCallBack(EncodeCallBack callBack);

    void x264Encode(int8_t *data);

};


#endif //MEDIACODECUSE_X264HANDLE_H
