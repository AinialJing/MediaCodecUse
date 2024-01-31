package com.aniljing.mediacodecuse.utils;


import android.view.Surface;

public class MediaUtil {
    private static ConnectErrorCallBack mCallBack;

    public native void yv12ToI420(byte[] yv12, byte[] i420, int width, int height);

    public native void nv21ToI420(byte[] src_nv21_data, int width, int height, byte[] dst_i420_data);

    public native void i420ToNv21(byte[] src_i420_data, int width, int height, byte[] dst_nv21_data);

    public native void i420Rotate(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int degree);

    public native void i420Mirror(byte[] src_i420_data, int width, int height, byte[] dst_i420_data);

    public native void i420Scale(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int dst_width, int dst_height, int filter_mode);

    public native void connectRtmp(String url);

    public native void sendRtmpData(byte[] data, int len, long tms, int type);

    public native void initAndPullRtmpData(Surface surface,String url);

    public native void releaseRtmp();

    public static void connectErrorCallBack(int error) {
        if (mCallBack != null) {
            mCallBack.connectState(error);
        }
    }

    public interface ConnectErrorCallBack {
        void connectState(int state);
    }

    public void setCallBack(ConnectErrorCallBack callBack) {
        mCallBack = callBack;
    }

    static {
        System.loadLibrary("mediaUtil");
    }
}
