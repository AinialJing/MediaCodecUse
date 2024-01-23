package com.aniljing.mediacodecuse;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;

import com.aniljing.mediacodecuse.camera2.Camera2ProviderPreviewWithYUV;
import com.aniljing.mediacodecuse.camera2.Texture2dProgram;
import com.aniljing.mediacodecuse.databinding.ActivityX264CodecBinding;
import com.aniljing.mediacodecuse.utils.LogUtils;
import com.aniljing.mediacodecuse.utils.MediaUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.appcompat.app.AppCompatActivity;

public class X264CodecActivity extends AppCompatActivity {
    private static final String TAG = X264CodecActivity.class.getSimpleName();
    private ActivityX264CodecBinding mBinding;
    private Context mContext;
    //    private Camera2Helper mCamera2Helper;
    private Camera2ProviderPreviewWithYUV mPreviewWithYUV;
    //对讲的视频
    private Texture2dProgram texture2dProgream;
    private int videoTextureId;
    private SurfaceTexture videoTexture;
    private Surface decoderSurface;
    private final float[] mSTMatrix = new float[16];
    //构造顶点坐标、纹理坐标的buffer
    private final float[] fullVertex = new float[]{
            -1.0f, 1.0f,
            1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f
    };
    private final float[] fullTexture = new float[]{
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };
    private final FloatBuffer fullVertexBuffer = getBufferFromArray(fullVertex);
    private final FloatBuffer fullTextureBuffer = getBufferFromArray(fullTexture);
    private MediaUtil mMediaUtil;
    private LinkedBlockingDeque<byte[]> dataQu = new LinkedBlockingDeque<>();
    private boolean startEncode = false;
    private File mFile = new File(Environment.getExternalStorageDirectory(), "x264.h264");
    private BufferedOutputStream bos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityX264CodecBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        try {
            if (mFile.exists()) {
                mFile.delete();
            }
            bos = new BufferedOutputStream(new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        mPreviewWithYUV = new Camera2ProviderPreviewWithYUV(this);
        mPreviewWithYUV.initTexture(mBinding.preview);
        mPreviewWithYUV.setYUVDataCallBack((data, width, height, orientation) -> {
            if (mMediaUtil == null) {
                int result;
                mMediaUtil = new MediaUtil();
                mMediaUtil.setEncodeCallBack(encode -> {
                    LogUtils.e(TAG, "x264 encode size:" + encode.length);
                    if (bos != null) {
                        try {
                            bos.write(encode);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                if (orientation == 90) {
                    result = mMediaUtil.initX264(height, width);
                } else {
                    result = mMediaUtil.initX264(width, height);
                }
                startEncode = true;
                new Thread(new X264EncodeThread()).start();
                LogUtils.e(TAG, "initX264 state:" + result);
                addVideoData(width, height, orientation, data);
            } else {
                addVideoData(width, height, orientation, data);
            }
        });
//        mBinding.render.setEGLContextClientVersion(2);
//        mBinding.render.setRenderer(new GLSurfaceView.Renderer() {
//            @Override
//            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//                texture2dProgream = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
//                videoTextureId = texture2dProgream.createTextureObject();
//                videoTexture = new SurfaceTexture(videoTextureId);
//                decoderSurface = new Surface(videoTexture);
//                videoTexture.setOnFrameAvailableListener(surfaceTexture -> {
//                    mBinding.render.requestRender();
//                });
//            }
//
//            @Override
//            public void onSurfaceChanged(GL10 gl, int width, int height) {
//                GLES20.glViewport(0, 0, width, height);
//            }
//
//            @Override
//            public void onDrawFrame(GL10 gl) {
//                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
//                videoTexture.updateTexImage();
//                videoTexture.getTransformMatrix(mSTMatrix);
//                texture2dProgream.draw(GlUtil.IDENTITY_MATRIX, fullVertexBuffer
//                        , 0, 4, 2, 8, mSTMatrix, fullTextureBuffer, videoTextureId, 8);
//            }
//        });
//        mBinding.render.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mBinding.render.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaUtil != null) {
            startEncode = false;
            mMediaUtil.releaseX264();
        }
        if (bos != null) {
            try {
                bos.flush();
                bos.close();
                bos = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private FloatBuffer getBufferFromArray(float[] array) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(array.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(array).position(0);
        return buffer;
    }

    private void addVideoData(int width, int height, int orientation, byte[] data) {
        if (!startEncode) {
            return;
        }
        if (orientation == 90 && mMediaUtil != null && dataQu != null) {
            byte[] rotate_data = new byte[data.length];
            mMediaUtil.i420Rotate(data, width, height, rotate_data, orientation);
            LogUtils.e(TAG, "add rotate_data video");
            dataQu.add(rotate_data);
        } else {
            dataQu.add(data);
            LogUtils.e(TAG, "add video");
        }
    }

    private class X264EncodeThread implements Runnable {

        @Override
        public void run() {
            while (startEncode) {
                if (mMediaUtil != null && !dataQu.isEmpty()) {
                    LogUtils.e(TAG, "X264EncodeThread");
                    byte[] data = dataQu.poll();
                    mMediaUtil.x264Encode(data);
                }
            }

        }
    }
}