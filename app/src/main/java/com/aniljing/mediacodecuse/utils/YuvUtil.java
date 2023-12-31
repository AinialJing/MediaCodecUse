package com.aniljing.mediacodecuse.utils;


public class YuvUtil {

    /**
     * 将NV21数据旋转, 不剪切
     * @param nv21 camera datas
     * @param outs out datas
     * @param w
     * @param h
     * @param orientation
     */
    public  native void nv21Rotate(byte[] nv21,
                                         byte[] outs,
                                         int w, int h,
                                         @Orientation.OrientationMode int orientation);

    /**
     * 同时将NV21数据转换成NV12并旋转, 不剪切
     * @param nv21 camera datas
     * @param nv12 out datas
     * @param w
     * @param h
     * @param orientation
     */
    public native void nv21ToNV12Rotate(byte[] nv21,
                                               byte[] nv12,
                                               int w, int h,
                                               @Orientation.OrientationMode int orientation);

    static {
        System.loadLibrary("yuvUtil");
    }
}
