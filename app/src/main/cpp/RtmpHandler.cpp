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
    //一定要设置好时间戳，要不然解码视频的时候，就会出现卡顿的感觉
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
    //一定要设置好时间戳，要不然解码视频的时候，就会出现卡顿的感觉
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
