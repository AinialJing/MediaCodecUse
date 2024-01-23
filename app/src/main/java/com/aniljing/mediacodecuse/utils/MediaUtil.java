package com.aniljing.mediacodecuse.utils;


public class MediaUtil {
    private static EncodeCallBack mEncodeCallBack;
    private DecodeCallback mDecodeCallback;

    public native void yv12ToI420(byte[] yv12, byte[] i420, int width, int height);

    public native void nv21ToI420(byte[] src_nv21_data, int width, int height, byte[] dst_i420_data);

    public native void i420ToNv21(byte[] src_i420_data, int width, int height, byte[] dst_nv21_data);

    public native void i420Rotate(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int degree);

    public native void i420Mirror(byte[] src_i420_data, int width, int height, byte[] dst_i420_data);

    public native void i420Scale(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int dst_width, int dst_height, int filter_mode);


    public native int initX264Encode(int width, int height);

    public native void x264Encode(byte[] src_data);

    public native void releaseX264();

    public native int intFFMpegDecode(int width,int height);

    public native void ffmpegDecode(byte[] src_data);

    public native void releaseFFMpegDecode();

    public void x264EncodeCallBack(byte[] x264) {
        if (mEncodeCallBack != null) {
            mEncodeCallBack.encodeData(x264);
        }
    }

    public void setEncodeCallBack(EncodeCallBack encodeCallBack) {
        mEncodeCallBack = encodeCallBack;
    }

    public void setDecodeCallback(DecodeCallback decodeCallback) {
        mDecodeCallback = decodeCallback;
    }

    public interface EncodeCallBack {
        void encodeData(byte[] encode);
    }

    public interface DecodeCallback {
        void decodeData(byte[] decode);
    }

    public static void jniEncodeCallBack(byte[] data) {
        if (mEncodeCallBack != null) {
            mEncodeCallBack.encodeData(data);
        }
    }

    public void jniDecodeCallBack(byte[] data) {
        if (mDecodeCallback != null) {
            mDecodeCallback.decodeData(data);
        }
    }

    static {
        System.loadLibrary("mediaUtil");
    }
}
