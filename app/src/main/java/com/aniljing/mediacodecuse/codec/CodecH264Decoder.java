package com.aniljing.mediacodecuse.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;


import com.aniljing.mediacodecuse.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CodecH264Decoder {
    private static final String TAG = CodecH264Decoder.class.getSimpleName();
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private long m_nCount = 0;

    private MediaCodec decoder;
    private boolean isDecoder = false;
    private HandlerThread thread;
    private Handler mHandler;

    public void initDecoder(Surface surface) {
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
            MediaFormat mediaFormatVideo = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT);
            decoder.configure(mediaFormatVideo, surface, null, 0);
            thread = new HandlerThread("videoCodec");
            thread.start();
            mHandler = new Handler(thread.getLooper());
            isDecoder = true;
            decoder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void decode(byte[] buff, int nLen) {
        if (!isDecoder) {
            LogUtils.e(TAG,"isDevoder:"+isDecoder);
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (decoder != null) {
                    try {
                        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                        int inputBufferIndex = decoder.dequeueInputBuffer(0);//之前是-1
                        if (inputBufferIndex >= 0) {
                            //获取解码芯片有效的输入Buffer通道
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(buff, 0, nLen);

                            long pts = computePresentationTime(m_nCount);
                            decoder.queueInputBuffer(inputBufferIndex, 0, nLen, pts, 0);
                            m_nCount++;
                        }
                        // 释放缓冲区
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                        while (outputBufferIndex >= 0) {
                            decoder.releaseOutputBuffer(outputBufferIndex, true);
                            outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }

    // 释放
    public void uninit() {
        isDecoder = false;
        if (mHandler == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogUtils.e(TAG, "uninit");
                if (decoder != null) {
                    try {
                        decoder.stop();
                        decoder.release();
                        decoder = null;
                        mHandler.getLooper().quitSafely();
                        mHandler = null;
                        thread.quitSafely();
                        thread = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Exception:" + e.toString());
                    }
                }
            }
        });

    }
    private long computePresentationTime(long frameIndex) {
        int frameRate = 30;
        return 132 + frameIndex * 1000000 / frameRate;
    }
}
