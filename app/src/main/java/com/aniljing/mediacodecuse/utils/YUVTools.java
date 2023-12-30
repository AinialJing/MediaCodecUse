package com.aniljing.mediacodecuse.utils;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;

import java.nio.ByteBuffer;

public class YUVTools {

    /******************************* YUV420旋转算法 *******************************/

    // I420或YV12顺时针旋转
    public static void rotateP(byte[] src, byte[] dest, int w, int h, int rotation) {
        switch (rotation) {
            case 0:
                System.arraycopy(src, 0, dest, 0, src.length);
                break;
            case 90:
                rotateP90(src, dest, w, h);
                break;
            case 180:
                rotateP180(src, dest, w, h);
                break;
            case 270:
                rotateP270(src, dest, w, h);
                break;
        }
    }

    // NV21或NV12顺时针旋转
    public static void rotateSP(byte[] src, byte[] dest, int w, int h, int rotation) {
        switch (rotation) {
            case 0:
                System.arraycopy(src, 0, dest, 0, src.length);
                break;
            case 90:
                rotateSP90(src, dest, w, h);
                break;
            case 180:
                rotateSP180(src, dest, w, h);
                break;
            case 270:
                rotateSP270(src, dest, w, h);
                break;
        }
    }

    // NV21或NV12顺时针旋转90度
    public static void rotateSP90(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = 0;
        for (int i = 0; i <= w - 1; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }

        pos = w * h;
        for (int i = 0; i <= w - 2; i += 2) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w + i];
                dest[k++] = src[pos + j * w + i + 1];
            }
        }
    }

    // NV21或NV12顺时针旋转270度
    public static void rotateSP270(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = 0;
        for (int i = w - 1; i >= 0; i--) {
            for (int j = 0; j <= h - 1; j++) {
                dest[k++] = src[j * w + i];
            }
        }

        pos = w * h;
        for (int i = w - 2; i >= 0; i -= 2) {
            for (int j = 0; j <= h / 2 - 1; j++) {
                dest[k++] = src[pos + j * w + i];
                dest[k++] = src[pos + j * w + i + 1];
            }
        }
    }

    // NV21或NV12顺时针旋转180度
    public static void rotateSP180(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = w * h - 1;
        while (k >= 0) {
            dest[pos++] = src[k--];
        }

        k = src.length - 2;
        while (pos < dest.length) {
            dest[pos++] = src[k];
            dest[pos++] = src[k + 1];
            k -= 2;
        }
    }

    // I420或YV12顺时针旋转90度
    public static void rotateP90(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        //旋转Y
        int k = 0;
        for (int i = 0; i < w; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        pos = w * h;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }

        //旋转V
        pos = w * h * 5 / 4;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
    }

    // I420或YV12顺时针旋转270度
    public static void rotateP270(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        //旋转Y
        int k = 0;
        for (int i = w - 1; i >= 0; i--) {
            for (int j = 0; j < h; j++) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        pos = w * h;
        for (int i = w / 2 - 1; i >= 0; i--) {
            for (int j = 0; j < h / 2; j++) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }

        //旋转V
        pos = w * h * 5 / 4;
        for (int i = w / 2 - 1; i >= 0; i--) {
            for (int j = 0; j < h / 2; j++) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
    }

    // I420或YV12顺时针旋转180度
    public static void rotateP180(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = w * h - 1;
        while (k >= 0) {
            dest[pos++] = src[k--];
        }

        k = w * h * 5 / 4;
        while (k >= w * h) {
            dest[pos++] = src[k--];
        }

        k = src.length - 1;
        while (pos < dest.length) {
            dest[pos++] = src[k--];
        }
    }

    /******************************* YUV420格式相互转换算法 *******************************/

    // i420 -> nv12, yv12 -> nv21
    public static void pToSP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[pos++] = src[u++];
            dest[pos++] = src[v++];
        }
    }

    // i420 -> nv21, yv12 -> nv12
    public static void pToSPx(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[pos++] = src[v++];
            dest[pos++] = src[u++];
        }
    }

    // nv12 -> i420, nv21 -> yv12
    public static void spToP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[u++] = src[pos++];
            dest[v++] = src[pos++];
        }
    }

    // nv12 -> yv12, nv21 -> i420
    public static void spToPx(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[v++] = src[pos++];
            dest[u++] = src[pos++];
        }
    }

    // i420 <-> yv12
    public static void pToP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int off = pos >> 2;
        System.arraycopy(src, 0, dest, 0, pos);
        System.arraycopy(src, pos, dest, pos + off, off);
        System.arraycopy(src, pos + off, dest, pos, off);
    }

    // nv12 <-> nv21
    public static void spToSP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        System.arraycopy(src, 0, dest, 0, pos);
        for (; pos < src.length; pos += 2) {
            dest[pos] = src[pos + 1];
            dest[pos + 1] = src[pos];
        }
    }

}
