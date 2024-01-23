package com.aniljing.mediacodecuse.utils;


public class MediaUtil {

    public native void yv12ToI420(byte[] yv12, byte[] i420, int width, int height);

    public native void nv21ToI420(byte[] src_nv21_data, int width, int height, byte[] dst_i420_data);

    public native void i420ToNv21(byte[] src_i420_data, int width, int height, byte[] dst_nv21_data);

    public native void i420Rotate(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int degree);

    public native void i420Mirror(byte[] src_i420_data, int width, int height, byte[] dst_i420_data);

    public native void i420Scale(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int dst_width, int dst_height, int filter_mode);


    static {
        System.loadLibrary("mediaUtil");
    }
}
