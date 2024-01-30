package com.aniljing.mediacodecuse.utils;

import android.util.Log;

public class LogUtils {
    public static final String TAG = "LogUtils";

    public static boolean DEBUG = true;

    public static boolean DEBUG_VIDEO = false;

    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void i(String msg) {
        if (DEBUG) {
            Log.i(TAG, "-->" + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag,  msg);
        }
    }

    public static void d(String msg) {
        if (DEBUG) {
            Log.d(TAG, "-->" + msg);
        }
    }

    public static void v(String tag, String msg) {
        if (DEBUG) {
            Log.v(tag,  msg);
        }
    }

    public static void v(String msg) {
        if (DEBUG) {
            Log.v(TAG,  msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void e(String msg) {
        if (DEBUG) {
            Log.e(TAG, "-->" + msg);
        }
    }

    public static void printStackTrace(Exception e) {
        if (DEBUG) {
            e.printStackTrace();
        }
    }

    public static void w(String tag, String msg) {
        Log.w(TAG, tag + "-->" + msg);
    }
}
