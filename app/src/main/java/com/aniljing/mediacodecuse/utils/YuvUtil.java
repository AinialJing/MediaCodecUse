package com.aniljing.mediacodecuse.utils;

import android.view.Surface;

public class YuvUtil {

    /**
     * 转换类型
     */
    public interface Type {
        int YUV420P_TO_RGB24 = 0;
        int NV12_TO_RGB24 = 1;
        int NV21_TO_RGB24 = 2;
    }

    /**
     * 初始化
     *
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     **/
    public static native void init(int width, int height, int dst_width, int dst_height);

    /**
     * YUV数据的基本的处理
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     * @param degree     旋转的角度，90，180和270三种
     * @param isMirror   是否镜像，一般只有270的时候才需要镜像
     **/
    public static native void compressYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int mode, int degree, boolean isMirror);

    /**
     * yuv数据的裁剪操作
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param left       裁剪的x的开始位置，必须为偶数，否则显示会有问题
     * @param top        裁剪的y的开始位置，必须为偶数，否则显示会有问题
     **/
    public static native void cropYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int left, int top);

    /**
     * 将I420转化为NV21
     *
     * @param i420Src 原始I420数据
     * @param nv21Src 转化后的NV21数据
     * @param width   输出的宽
     * @param width   输出的高
     **/
    public static native void yuvI420ToNV21(byte[] i420Src, byte[] nv21Src, int width, int height);

    public native void yuv2rgb(String yuvPath, int type, int width, int height, Surface surface);

    public native void rotateI420(byte[] src,int width,int height,byte[] dest,int degree);

    static {
        System.loadLibrary("yuvUtil");
    }
}
