//
// Created by company on 2024-01-17.
//

#include "X264Handle.h"
#include <fstream>

X264Handle::X264Handle() : m_frameLen(0),
                           videoCodec(nullptr),
                           pic_in(nullptr) {

}

X264Handle::~X264Handle() {
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = nullptr;
    }
}

int X264Handle::init(int width, int height, int fps, int bitrate) {
    LOGE("init encode:width=%d,height=%d,fps=%d,bitrate=%d", width, height, fps, bitrate);
    std::lock_guard<std::mutex> l(m_mutex);
    m_frameLen = width * height;
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = nullptr;
    }

    //setting x264 params
    x264_param_t param;
    int ret = x264_param_default_preset(&param, "ultrafast", "zerolatency");
    if (ret < 0) {
        return ret;
    }
    param.i_level_idc = 32;
    //input format
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //no B frame
    param.i_bframe = 0;
    //i_rc_method:bitrate control, CQP(constant quality), CRF(constant bitrate), ABR(average bitrate)
    param.rc.i_rc_method = X264_RC_ABR;
    //bitrate(Kbps)
    param.rc.i_bitrate = bitrate / 1024;
    //max bitrate
    param.rc.i_vbv_max_bitrate = bitrate / 1024 * 1.2;
    //unit:kbps
    param.rc.i_vbv_buffer_size = bitrate / 1024;

    //frame rate
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
    //using fps
    param.b_vfr_input = 0;
    //key frame interval(GOP)
    param.i_keyint_max = fps * 2;
    //each key frame attaches sps/pps
    param.b_repeat_headers = 1;
    //thread number
    param.i_threads = 1;

    ret = x264_param_apply_profile(&param, "baseline");
    if (ret < 0) {
        return ret;
    }
    //open encoder
    videoCodec = x264_encoder_open(&param);
    if (!videoCodec) {
        return -1;
    }
    pic_in = new x264_picture_t();
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
    return ret;
}

void X264Handle::setEncodeCallBack(EncodeCallBack callBack) {
    m_encodeCallBack = callBack;
}

void X264Handle::x264Encode(int8_t *data) {
    std::lock_guard<std::mutex> l(m_mutex);
    if (!pic_in)
        return;
    LOGE("encode %d", __LINE__);
    int offset = 0;
    memcpy(pic_in->img.plane[0], data, (size_t) m_frameLen); // y
    offset += m_frameLen;
    memcpy(pic_in->img.plane[1], data + offset, (size_t) m_frameLen / 4); // u
    offset += m_frameLen / 4;
    memcpy(pic_in->img.plane[2], data + offset, (size_t) m_frameLen / 4); // v
    x264_nal_t *pp_nal;
    LOGE("encode %d", __LINE__);
    int pi_nal;
    x264_picture_t pic_out;
    LOGE("encode %d", __LINE__);
    int ret = x264_encoder_encode(videoCodec, &pp_nal, &pi_nal, pic_in, &pic_out);
    if (ret >= 0) {
        if (ret > 0) {
            LOGD("encode finished,encode len:%d", ret);
            // 编码成功，获取编码后的数据
            const uint8_t *encodedData = pp_nal[0].p_payload;
            int dataSize = pp_nal[0].i_payload;

            if (m_encodeCallBack != nullptr) {
                m_encodeCallBack(encodedData, dataSize);
            }
        } else {
            LOGD("encode finished,but not data");
        }
    } else {
        LOGE("encode error");
    }

}
