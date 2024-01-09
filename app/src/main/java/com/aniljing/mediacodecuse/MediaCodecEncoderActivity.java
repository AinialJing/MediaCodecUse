package com.aniljing.mediacodecuse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;

import com.aniljing.mediacodecuse.camera2.Camera2ProviderPreviewWithYUV;
import com.aniljing.mediacodecuse.camera2.GlUtil;
import com.aniljing.mediacodecuse.camera2.Texture2dProgram;
import com.aniljing.mediacodecuse.codec.CodecH264Decoder;
import com.aniljing.mediacodecuse.codec.CodecH264Encoder;
import com.aniljing.mediacodecuse.databinding.ActivityMediaCodecEncoderBinding;
import com.aniljing.mediacodecuse.utils.LogUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MediaCodecEncoderActivity extends AppCompatActivity {
    private static final String TAG = MediaCodecEncoderActivity.class.getSimpleName();
    private ActivityMediaCodecEncoderBinding mBinding;
    private Context mContext;
    //    private Camera2Helper mCamera2Helper;
    private Camera2ProviderPreviewWithYUV mPreviewWithYUV;
    private CodecH264Encoder mH264Encoder;
    private CodecH264Decoder mDecoder;
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
    private static final String[] PERMISSION = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private long start;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mBinding = ActivityMediaCodecEncoderBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        checkPermission();
        mPreviewWithYUV = new Camera2ProviderPreviewWithYUV(this);
        mPreviewWithYUV.initTexture(mBinding.preview);
        mPreviewWithYUV.setYUVDataCallBack((nv12, width, height, orientation) -> {
            if (mH264Encoder == null) {
                mH264Encoder = new CodecH264Encoder();
                mH264Encoder.initCodec((data, presentationTimeUs) -> {
                    if (mDecoder != null) {
                        mDecoder.decode(data, data.length);
                    }
                }, width, height, orientation);
                mH264Encoder.startEncode();
            }
            if (decoderSurface != null && mDecoder == null) {
                mDecoder = new CodecH264Decoder();
                mDecoder.initDecoder(decoderSurface, orientation == 90 ? height : width, orientation == 90 ? width : height);
            }
            if (mH264Encoder != null) {
                mH264Encoder.putData(nv12);
            }
        });
        mBinding.render.setEGLContextClientVersion(2);
        mBinding.render.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                texture2dProgream = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
                videoTextureId = texture2dProgream.createTextureObject();
                videoTexture = new SurfaceTexture(videoTextureId);
                decoderSurface = new Surface(videoTexture);
                videoTexture.setOnFrameAvailableListener(surfaceTexture -> {
                    mBinding.render.requestRender();
                });
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(mSTMatrix);
                texture2dProgream.draw(GlUtil.IDENTITY_MATRIX, fullVertexBuffer
                        , 0, 4, 2, 8, mSTMatrix, fullTextureBuffer, videoTextureId, 8);
            }
        });
        mBinding.render.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mBinding.startPushStream.setOnClickListener((view) -> {
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.render.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreviewWithYUV != null) {
            mPreviewWithYUV.releaseCamera();
        }
        if (mH264Encoder != null) {
            mH264Encoder.stopEncode();
        }
        if (mDecoder != null) {
            mDecoder.uninit();
        }
        if (decoderSurface != null) {
            LogUtils.e(TAG, "Release decoderSurface");
            decoderSurface.release();
            decoderSurface = null;
        }
        if (videoTexture != null) {
            LogUtils.e(TAG, "Release videoTexture");
            videoTexture.release();
            videoTexture = null;
        }
        if (texture2dProgream != null) {
            LogUtils.e(TAG, "Release texture2dProgream");
            texture2dProgream.release();
            texture2dProgream = null;
        }
    }

    private FloatBuffer getBufferFromArray(float[] array) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(array.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(array).position(0);
        return buffer;
    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //申请权限
            ActivityCompat.requestPermissions(this, PERMISSION, 2000);
            return false;
        }
        return true;
    }

}