package com.aniljing.mediacodecuse.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;


import com.aniljing.mediacodecuse.utils.LogUtils;
import com.aniljing.mediacodecuse.utils.Orientation;
import com.aniljing.mediacodecuse.utils.YuvUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class CodecH264Encoder {
    private static final String TAG = CodecH264Encoder.class.getSimpleName();
    private MediaCodec mMediaCodecVideo;
    private EncodeCallBack mCallBack;
    private static final int BUFFER_SIZE=30;
    private final ArrayBlockingQueue<byte[]> videoBuffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
    private
    byte[] m_info = null;
    private long m_nCount = 0;
    private static final int m_width = 1280;
    private static final int m_height = 720;
    private byte[] yuv420;
    private byte[] output;

    private int pos = 0;
    private YuvUtil yuvUtil;


    public void initCodec(EncodeCallBack callBack) {
        try {
            yuv420 = new byte[m_width * m_height * 3 / 2];
            output = new byte[m_width * m_height * 3 / 2];
            mCallBack = callBack;
            yuvUtil=new YuvUtil();
            mMediaCodecVideo = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormatVideo = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, m_width, m_height);
            mediaFormatVideo.setInteger(MediaFormat.KEY_BIT_RATE, m_width * m_height * 5);
            mediaFormatVideo.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormatVideo.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            //COLOR_FormatYUV420Planar、COLOR_FormatYUV411Planar、COLOR_FormatYUV420PackedPlanar、COLOR_FormatYUV420SemiPlanar弃用
            mediaFormatVideo.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormatVideo.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            //异步方式编码，该方法必须放在configure之前
            mMediaCodecVideo.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    pos = 0;
                    byte[] data = videoBuffer.poll();
                    //获取可用的输入缓冲区
                    if (data != null) {
                        yuv420 = data;
                        byte[] yuv420sp = new byte[m_width * m_height * 3 / 2];
                        YV12toNV12(yuv420, yuv420sp, m_width, m_height);
                        yuv420=yuv420sp;
//                        yuv420 = yuv420sp;
//                        yuvUtil.nv21ToNV12Rotate(yuv420,yuv420sp,m_width,m_height, Orientation.ROTATE90);
//                        yuv420=yuv420sp;
                        ByteBuffer inputBuffer = codec.getInputBuffer(index);
                        inputBuffer.clear();
                        inputBuffer.put(yuv420);
                        long nts = computePresentationTime(m_nCount);
                        //把数据放入可用的缓冲区，通知编码器编码
                        codec.queueInputBuffer(index, 0, yuv420.length, nts, 0);
                        m_nCount++;

                    } else {
                        //有没有数据都要放入缓存队列里面
                        codec.queueInputBuffer(index, 0, 0, 0, 0);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    //取出编码好的数据
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    byte[] outData = new byte[info.size];
                    outputBuffer.get(outData);

                    if (m_info != null) {
                        System.arraycopy(outData, 0, output, pos, outData.length);
                        pos += outData.length;

                    } else {
                        ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                        if (spsPpsBuffer.getInt() == 0x00000001) {
                            m_info = new byte[outData.length];
                            System.arraycopy(outData, 0, m_info, 0, outData.length);
                        }
                    }
                    codec.releaseOutputBuffer(index, false);

                    if ((output[4] & 0x1F) == 5) { //key frame
                        System.arraycopy(output, 0, yuv420, 0, pos);
                        System.arraycopy(m_info, 0, output, 0, m_info.length);
                        System.arraycopy(yuv420, 0, output, m_info.length, pos);
                        pos += m_info.length;
                    }
                    if (mCallBack != null) {
                        mCallBack.encodeData(output);
                    }
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    LogUtils.e(TAG, "onError:" + e.getMessage());
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            mMediaCodecVideo.configure(mediaFormatVideo, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodecVideo.start();
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
            throw new RuntimeException(e);
        }
    }

    public void putData(byte[] data) {
        if (videoBuffer.size() >= BUFFER_SIZE) {
            videoBuffer.poll();
            LogUtils.e(TAG,"Drop data");
        }
        videoBuffer.add(data);
    }


    public interface EncodeCallBack {
        void encodeData(byte[] out);
    }

    public void stopEncode() {
        if (mMediaCodecVideo != null) {
            mMediaCodecVideo.stop();
            mMediaCodecVideo.release();
            mMediaCodecVideo = null;
        }
    }

    private void YV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {

        int nLenY = width * height;
        int nLenU = nLenY / 4;


        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + nLenU + i];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        int frameRate = 30;
        return 132 + frameIndex * 1000000 / frameRate;
    }

}
