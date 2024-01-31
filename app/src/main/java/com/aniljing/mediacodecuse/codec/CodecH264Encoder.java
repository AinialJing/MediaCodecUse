package com.aniljing.mediacodecuse.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.aniljing.mediacodecuse.camera2.CameraUtil;
import com.aniljing.mediacodecuse.utils.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class CodecH264Encoder {
    private static final String TAG = CodecH264Encoder.class.getSimpleName();
    private MediaCodec mMediaCodecVideo;
    private EncodeCallBack mCallBack;
    private final LinkedBlockingQueue<byte[]> videoBuffer = new LinkedBlockingQueue<>();
    private
    byte[] m_info = null;
    private long m_nCount = 0;
    private long timeStamp = 0;
    private boolean startEncode = false;

    public void initCodec(EncodeCallBack callBack, int width, int height, int orientation) {
        try {
            mCallBack = callBack;
            timeStamp = 0;
            mMediaCodecVideo = MediaCodec.createEncoderByType("video/avc");
            MediaCodecInfo mediaCodecInfo = CameraUtil.selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
            LogUtils.e(TAG,mediaCodecInfo.getName());
            MediaFormat mediaEncodeFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, orientation == 90 ? height : width, orientation == 90 ? width : height);
            mediaEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800_000);
            mediaEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
            mediaEncodeFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            mediaEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mMediaCodecVideo.configure(mediaEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodecVideo.start();
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
            throw new RuntimeException(e);
        }
    }

    public MediaCodec getMediaCodecVideo() {
        return mMediaCodecVideo;
    }

    public void startEncode() {
        startEncode = true;
        new Thread(() -> {
            while (startEncode) {
                if (videoBuffer.isEmpty()) {
                    continue;
                }
                if (mMediaCodecVideo == null) {
                    break;
                }
                byte[] data = videoBuffer.poll();
                //获取所有编码的输入缓冲区
                ByteBuffer[] inputBuffers = mMediaCodecVideo.getInputBuffers();
                //获取可用编码输入缓冲区的索引
                int inputBufferIndex = mMediaCodecVideo.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    //放入待编码的数据
                    inputBuffer.put(data);
                    long nts = computePresentationTime(m_nCount);
                    //通知编码器可以编码
                    mMediaCodecVideo.queueInputBuffer(inputBufferIndex, 0, data.length, nts, 0);
                    m_nCount++;
                }
                //获取所有编码的输出缓冲区
                ByteBuffer[] outputBuffers = mMediaCodecVideo.getOutputBuffers();
                //获取编码好的输出缓冲区的索引
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//                if (System.currentTimeMillis() - timeStamp >= 2000) {
//                    Bundle params = new Bundle();
//                    params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//                    //dsp 芯片触发I帧
//                    mMediaCodecVideo.setParameters(params);
//                    timeStamp = System.currentTimeMillis();
//                }
                int outputIndex = mMediaCodecVideo.dequeueOutputBuffer(bufferInfo, 4000);
                if (outputIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputIndex];
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
//                    if (bufferInfo.flags==2){
//                        LogUtils.e(TAG,"sps pps");
//                    } else if (bufferInfo.flags==1) {
//                        LogUtils.e(TAG,"key frame");
//                    }else{
//                        LogUtils.e(TAG,"frame:"+bufferInfo.flags);
//                    }
                    if (mCallBack != null) {
                        mCallBack.encodeData(outData, bufferInfo);
                    }
                    mMediaCodecVideo.releaseOutputBuffer(outputIndex, false);
                }
            }
        }).start();
    }

    public void putData(byte[] data) {
        videoBuffer.add(data);
    }


    public interface EncodeCallBack {
        void encodeData(byte[] out, MediaCodec.BufferInfo bufferInfo);
    }

    public void stopEncode() {
        if (mMediaCodecVideo != null) {
            startEncode = false;
            mMediaCodecVideo.stop();
            mMediaCodecVideo.release();
            mMediaCodecVideo = null;
            timeStamp = 0;
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
