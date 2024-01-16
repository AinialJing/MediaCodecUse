package com.aniljing.mediacodecuse.utils;


public class MediaUtil {
    private EncodeCallBack mEncodeCallBack;
    private DecodeCallback mDecodeCallback;

    public native void yv12ToI420(byte[] yv12, byte[] i420, int width, int height);

    public native void nv21ToI420(byte[] src_nv21_data, int width, int height, byte[] dst_i420_data);

    public native void i420ToNv21(byte[] src_i420_data, int width, int height, byte[] dst_nv21_data);

    public native void i420Rotate(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int degree);

    public native void i420Mirror(byte[] src_i420_data, int width, int height, byte[] dst_i420_data);

    public native void i420Scale(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int dst_width, int dst_height,int filter_mode);

    public native boolean initEncode(int width, int height, int bite);

    public native void unEncode();

    public native void encode(byte[] data);

    public native boolean initDecode(int width, int height);

    public native void decode(byte[] data);

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

    public void jniEncodeCallBack(byte[] data) {
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
