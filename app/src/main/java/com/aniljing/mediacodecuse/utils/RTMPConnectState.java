package com.aniljing.mediacodecuse.utils;

import android.content.Context;

import com.aniljing.mediacodecuse.R;

public class RTMPConnectState {
    private final static int ERROR_VIDEO_ENCODER_OPEN = 0x01;
    private final static int ERROR_VIDEO_ENCODER_ENCODE = 0x02;
    private final static int ERROR_AUDIO_ENCODER_OPEN = 0x03;
    private final static int ERROR_AUDIO_ENCODER_ENCODE = 0x04;
    private final static int ERROR_RTMP_CONNECT_SERVER = 0x05;
    private final static int ERROR_RTMP_CONNECT_STREAM = 0x06;
    private final static int ERROR_RTMP_SEND_PACKET = 0x07;

    public static String getErrorMsg(Context context, int errorCode) {
        String msg = "";
        switch (errorCode) {
            case ERROR_VIDEO_ENCODER_OPEN:
                msg = context.getString(R.string.error_video_encoder);
                break;
            case ERROR_VIDEO_ENCODER_ENCODE:
                msg = context.getString(R.string.error_video_encode);
                break;
            case ERROR_AUDIO_ENCODER_OPEN:
                msg = context.getString(R.string.error_audio_encoder);
                break;
            case ERROR_AUDIO_ENCODER_ENCODE:
                msg = context.getString(R.string.error_audio_encode);
                break;
            case ERROR_RTMP_CONNECT_SERVER:
                msg = context.getString(R.string.error_rtmp_connect);
                break;
            case ERROR_RTMP_CONNECT_STREAM:
                msg = context.getString(R.string.error_rtmp_connect_strem);
                break;
            case ERROR_RTMP_SEND_PACKET:
                msg = context.getString(R.string.error_rtmp_send_packet);
                break;
            default:
                break;
        }
        return msg;
    }
}
