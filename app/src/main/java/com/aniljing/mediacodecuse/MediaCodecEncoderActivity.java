package com.aniljing.mediacodecuse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.aniljing.mediacodecuse.camera2.Camera2ProviderPreviewWithYUV;
import com.aniljing.mediacodecuse.camera2.GlUtil;
import com.aniljing.mediacodecuse.camera2.Texture2dProgram;
import com.aniljing.mediacodecuse.codec.CodecH264Decoder;
import com.aniljing.mediacodecuse.codec.CodecH264Encoder;
import com.aniljing.mediacodecuse.databinding.ActivityMediaCodecEncoderBinding;
import com.aniljing.mediacodecuse.utils.LogUtils;
import com.aniljing.mediacodecuse.utils.Orientation;
import com.aniljing.mediacodecuse.utils.YUVTools;
import com.aniljing.mediacodecuse.utils.YuvUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MediaCodecEncoderActivity extends AppCompatActivity {
    private static final String TAG = MediaCodecEncoderActivity.class.getSimpleName();
    private ActivityMediaCodecEncoderBinding mBinding;
    private Context mContext;
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
    private float[] fullVertex = new float[]{
            -1.0f, 1.0f,
            1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f
    };
    private float[] fullTexture = new float[]{
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };
    private FloatBuffer fullVertexBuffer = getBufferFromArray(fullVertex);
    private FloatBuffer fullTextureBuffer = getBufferFromArray(fullTexture);
    private static final String[] PERMISSION={Manifest.permission.CAMERA};
    private YuvUtil yuvUtil;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mBinding = ActivityMediaCodecEncoderBinding.inflate(getLayoutInflater());
        yuvUtil=new YuvUtil();
        setContentView(mBinding.getRoot());
        checkPermission();
        mPreviewWithYUV = new Camera2ProviderPreviewWithYUV(MediaCodecEncoderActivity.this);
        mPreviewWithYUV.initTexture(mBinding.preview);
        mH264Encoder = new CodecH264Encoder();
        mH264Encoder.initCodec(data -> {
            if (mDecoder != null) {
                mDecoder.decode(data, data.length);
            }
        });
        mPreviewWithYUV.setYUVDataCallBack((i420, width, height) -> {
            if (mH264Encoder != null) {
//                byte[] rotate=new byte[i420.length];
//                YUVTools.rotateP90(i420,rotate,width,height);
                byte[] outs=new byte[i420.length];

                yuvUtil.nv21Rotate(i420,outs,width,height, Orientation.ROTATE90);
                mH264Encoder.putData(outs);
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
                mDecoder = new CodecH264Decoder();
                mDecoder.initDecoder(decoderSurface);
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

    private boolean checkPermission(){
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            //申请权限
            ActivityCompat.requestPermissions(this, PERMISSION, 2000);
            return false;
        }
        return true;
    }
}