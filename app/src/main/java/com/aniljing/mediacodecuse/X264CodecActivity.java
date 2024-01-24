package com.aniljing.mediacodecuse;

import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceHolder;

import com.aniljing.mediacodecuse.camera2.Camera2ProviderPreviewWithYUV;
import com.aniljing.mediacodecuse.databinding.ActivityX264CodecBinding;
import com.aniljing.mediacodecuse.utils.MediaUtil;

import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class X264CodecActivity extends AppCompatActivity {
    private static final String TAG = X264CodecActivity.class.getSimpleName();
    private ActivityX264CodecBinding mBinding;
    private Context mContext;
    //    private Camera2Helper mCamera2Helper;
    private Camera2ProviderPreviewWithYUV mPreviewWithYUV;
    private MediaUtil mMediaUtil;
    private LinkedBlockingDeque<byte[]> dataQu = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<byte[]> decodeQue = new LinkedBlockingDeque<>();
    private boolean startEncode = false;
    private boolean initEncode = false;
    private boolean initDecode = false;
    private boolean startDecode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityX264CodecBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mMediaUtil = new MediaUtil();
        mPreviewWithYUV = new Camera2ProviderPreviewWithYUV(this);
        mPreviewWithYUV.initTexture(mBinding.preview);
        mBinding.render.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(@NonNull SurfaceHolder holder) {

            }

            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mMediaUtil != null) {
                    mMediaUtil.initFFmpegDecode(holder.getSurface());
                    initDecode = true;
                    startDecode = true;
                    new Thread(new FFmpegDecodeThread()).start();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
        mPreviewWithYUV.setYUVDataCallBack((data, width, height, orientation) -> {
            if (!initEncode) {
                mMediaUtil.setEncodeCallBack(encode -> {
                    if (mMediaUtil != null && initDecode) {
                        decodeQue.add(encode);
                    }
                });
                if (orientation == 90) {
                    mMediaUtil.initX264Encode(height, width);
                } else {
                    mMediaUtil.initX264Encode(width, height);
                }
                startEncode = true;
                initEncode = true;
                new Thread(new X264EncodeThread()).start();
                addVideoData(width, height, orientation, data);
            } else {
                addVideoData(width, height, orientation, data);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaUtil != null) {
            startEncode = false;
            startDecode = false;
            mMediaUtil.releaseX264();
            mMediaUtil.releaseFFmpegDecode();
        }
    }


    private void addVideoData(int width, int height, int orientation, byte[] data) {
        if (!startEncode) {
            return;
        }
        if (orientation == 90 && mMediaUtil != null && dataQu != null) {
            byte[] rotate_data = new byte[data.length];
            mMediaUtil.i420Rotate(data, width, height, rotate_data, orientation);
            dataQu.add(rotate_data);
        } else {
            dataQu.add(data);
        }
    }

    private class X264EncodeThread implements Runnable {

        @Override
        public void run() {
            while (startEncode) {
                if (mMediaUtil != null && !dataQu.isEmpty()) {
                    byte[] data = dataQu.poll();
                    mMediaUtil.x264Encode(data);
                }
            }

        }
    }

    private class FFmpegDecodeThread implements Runnable {
        @Override
        public void run() {
            while (startDecode) {
                if (mMediaUtil != null && !decodeQue.isEmpty()) {
                    byte[] decode = decodeQue.poll();
                    mMediaUtil.ffmpegDecode(decode, decode.length);
                }
            }
        }
    }
}