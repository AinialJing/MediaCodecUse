//
// Created by company on 2024-01-29.
//

#include "RtmpHandler.h"
#include "Log.h"
#include "PushInterface.h"

RtmpHandler::RtmpHandler() {

}

RtmpHandler::~RtmpHandler() {
    isPushing = false;
}

void RtmpHandler::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len, const long tms) {
    int bodySize = 13 + sps_len + 3 + pps_len;
    auto *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    // type
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    // timestamp
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //version
    packet->m_body[i++] = 0x01;
    // profile
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //sps
    packet->m_body[i++] = 0xE1;
    //sps len
    packet->m_body[i++] = (sps_len >> 8) & 0xFF;
    packet->m_body[i++] = sps_len & 0xFF;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xFF;
    packet->m_body[i++] = (pps_len) & 0xFF;
    memcpy(&packet->m_body[i], pps, pps_len);

    //video
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x10;
    //sps and pps no timestamp
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    //一定要设置好时间戳，要不然解码视频的时候，就会出现卡顿
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;
    packets.push(packet);
}

void RtmpHandler::sendFrame(int type, uint8_t *payload, int i_payload, const long tms) {
    if (payload[2] == 0x00) {
        i_payload -= 4;
        payload += 4;
    } else {
        i_payload -= 3;
        payload += 3;
    }
    int i = 0;
    int bodySize = 9 + i_payload;
    auto *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, bodySize);

    if (type == 1) {
        packet->m_body[i++] = 0x17; // 1:Key frame  7:AVC
    } else {
        packet->m_body[i++] = 0x27; // 2:None key frame 7:AVC
    }
    //AVC NALU
    packet->m_body[i++] = 0x01;
    //timestamp
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //packet len
    packet->m_body[i++] = (i_payload >> 24) & 0xFF;
    packet->m_body[i++] = (i_payload >> 16) & 0xFF;
    packet->m_body[i++] = (i_payload >> 8) & 0xFF;
    packet->m_body[i++] = (i_payload) & 0xFF;

    memcpy(&packet->m_body[i], payload, static_cast<size_t>(i_payload));

    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    //一定要设置好时间戳，要不然解码视频的时候，就会出现卡顿
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;
    packets.push(packet);
}

void RtmpHandler::pushProcessor(void *args) {
    char *url = static_cast<char *>(args);
    LOGE("RtmpHandler::pushProcessor url:%s", url);
    RTMP *rtmp;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("RTMP_Alloc fail");
            break;
        }
        RTMP_Init(rtmp);
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("RTMP_SetupURL error");
            break;
        }
        //timeout
        rtmp->Link.timeout = 10;
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("RTMP_Connect error");
            m_startErrorCallBack(ERROR_RTMP_CONNECT);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("RTMP_ConnectStream error");
            m_startErrorCallBack(ERROR_RTMP_CONNECT_STREAM);
            break;
        }
        //start time
        start_time = RTMP_GetTime();
        //start pushing
        isPushing = true;
        packets.setRunning(true);
        RTMPPacket *packet = nullptr;
        while (isPushing) {
            packets.pop(packet);
            if (!isPushing) {
                break;
            }
            if (!packet) {
                continue;
            }

            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("RTMP_SendPacket fail...");
                m_startErrorCallBack(ERROR_RTMP_SEND_PACKET);
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    LOGD("%d", __LINE__);
    isPushing = false;
    packets.setRunning(false);
    packets.clear();
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete (url);
}

void RtmpHandler::releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void RtmpHandler::start(char *url) {
    LOGE("RtmpHandler::start");
    isPushing = true;
    std::thread pushThread(&RtmpHandler::pushProcessor, this, url);
    pushThread.detach();
}

void RtmpHandler::setStartErrorCallBack(StartErrorCallBack startErrorCallBack) {
    m_startErrorCallBack = startErrorCallBack;
}

void RtmpHandler::initAndPullRtmpData(ANativeWindow *nativeWindow, char *url) {
    LOGE("RtmpHandler::initAndPullRtmpData");
    isPulling = true;
    std::thread pullThread(&RtmpHandler::pullProcessor, this, nativeWindow, url);
    pullThread.detach();
}

void RtmpHandler::pullProcessor(ANativeWindow *nativeWindow, char *url) {
    AVFormatContext *fct = avformat_alloc_context();
    if (avformat_open_input(&fct, url, nullptr, nullptr) != 0) {
        LOGE("open input failed");
        return;
    }
    // 获取流信息
    if (avformat_find_stream_info(fct, nullptr) < 0) {
        LOGE("find stream info failed");
        return;
    }
    int m_videoStreamIdx = -1;
    //查找音视频流对应索引
    for (int i = 0; i < (int) fct->nb_streams; ++i) {
        //编解码参数
        AVCodecParameters *codecParameters = fct->streams[i]->codecpar;
        if (codecParameters->codec_type == AVMEDIA_TYPE_VIDEO) {//视频流
            //视频流索引
            m_videoStreamIdx = i;
            //获取视频解码器
            mDecodeCodec = avcodec_find_decoder(codecParameters->codec_id);
            if (mDecodeCodec == nullptr) {
                LOGE("Video AVCodec is NULL");
                return;
            }
            //初始化视频解码器上下文
            mDecodeContext = avcodec_alloc_context3(mDecodeCodec);
            //解码器参数设置到解码器上下文环境
            avcodec_parameters_to_context(mDecodeContext, codecParameters);
            //打开视频解码器
            if (avcodec_open2(mDecodeContext, mDecodeCodec, NULL) < 0) {
                LOGE("Could not open codec.");
                return;
            }
        }
    }
    // 创建图像转换上下文
    mSwsContext = sws_getContext(mDecodeContext->width, mDecodeContext->height,
                                 mDecodeContext->pix_fmt, mDecodeContext->width,
                                 mDecodeContext->height, AV_PIX_FMT_RGBA, SWS_BILINEAR,
                                 0, 0, 0);
    // 设置渲染格式和大小
    ANativeWindow_setBuffersGeometry(nativeWindow, mDecodeContext->width, mDecodeContext->height,
                                     WINDOW_FORMAT_RGBA_8888);
    // 分配渲染缓冲区
    ANativeWindow_Buffer outBuffer;
    AVPacket *packet = av_packet_alloc();
    if (!packet) {
        return;
    }
    while (av_read_frame(fct, packet) >= 0) {
        if (packet->stream_index == m_videoStreamIdx) {
            // 将要解码的数据包送入解码器
            avcodec_send_packet(mDecodeContext, packet);
            mDecodeFrame = av_frame_alloc();
            //从解码器内部缓存中提取解码后的音视频帧
            int ret = avcodec_receive_frame(mDecodeContext, mDecodeFrame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }
            uint8_t * dst_data[4];
            int dst_linesize[4];
            av_image_alloc(dst_data, dst_linesize, mDecodeContext->width, mDecodeContext->height,
                           AV_PIX_FMT_RGBA, 1);
            // 锁定 Surface 并获取渲染缓冲区
            ANativeWindow_lock(nativeWindow, &outBuffer, NULL);
            // 将解码后的帧转换为目标格式
            sws_scale(mSwsContext, mDecodeFrame->data, mDecodeFrame->linesize, 0,
                      mDecodeFrame->height,
                      dst_data, dst_linesize);

            //渲染
            uint8_t * first = static_cast<uint8_t *>(outBuffer.bits);
            uint8_t * src_data = dst_data[0];
            int dstStride = outBuffer.stride * 4;
            int src_linesize = dst_linesize[0];
            for (int i = 0; i < outBuffer.height; ++i) {
                memcpy(first + i * dstStride, src_data + i * src_linesize, dstStride);
            }
            // 解锁 Surface
            ANativeWindow_unlockAndPost(nativeWindow);
            av_frame_free(&mDecodeFrame);
        }
    }
    ANativeWindow_release(nativeWindow);
    sws_freeContext(mSwsContext);
    avcodec_free_context(&mDecodeContext);
    avformat_close_input(&fct);
}
