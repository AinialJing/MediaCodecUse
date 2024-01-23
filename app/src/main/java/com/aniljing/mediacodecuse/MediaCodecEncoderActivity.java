package com.aniljing.mediacodecuse;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;

import com.aniljing.mediacodecuse.camera2.Camera2ProviderPreviewWithYUV;
import com.aniljing.mediacodecuse.camera2.GlUtil;
import com.aniljing.mediacodecuse.camera2.Texture2dProgram;
import com.aniljing.mediacodecuse.codec.CodecH264Decoder;
import com.aniljing.mediacodecuse.codec.CodecH264Encoder;
import com.aniljing.mediacodecuse.databinding.ActivityMediaCodecEncoderBinding;
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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.appcompat.app.AppCompatActivity;

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

    private File encodeFile = new File(Environment.getExternalStorageDirectory(), "encode.h264");
    private BufferedOutputStream bosEncode;
    private MediaUtil mMediaUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mBinding = ActivityMediaCodecEncoderBinding.inflate(getLayoutInflater());
        mMediaUtil = new MediaUtil();
        setContentView(mBinding.getRoot());
        if (encodeFile.exists()) {
            encodeFile.delete();
        }
        if (bosEncode == null) {
            try {
                bosEncode = new BufferedOutputStream(new FileOutputStream(encodeFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        mPreviewWithYUV = new Camera2ProviderPreviewWithYUV(this);
        mPreviewWithYUV.initTexture(mBinding.preview);
        mPreviewWithYUV.setYUVDataCallBack((i420, width, height, orientation) -> {
            if (mH264Encoder == null) {
                mH264Encoder = new CodecH264Encoder();
                mH264Encoder.initCodec((data, presentationTimeUs) -> {
                    try {
                        if (bosEncode != null) {
                            bosEncode.write(data);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
                int format = mH264Encoder.getMediaCodecVideo().getInputFormat().getInteger(MediaFormat.KEY_COLOR_FORMAT);
                switch (format) {
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://420p 公司
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible://google
                        if (orientation == 90) {
                            byte[] dst_rotate = new byte[i420.length];
                            mMediaUtil.i420Rotate(i420, width, height, dst_rotate, orientation);
                            mH264Encoder.putData(dst_rotate);
                        } else {
                            mH264Encoder.putData(i420);
                        }
                        break;
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar://420sp vivo
                        byte[] dst_rotate = new byte[i420.length];
                        byte[] nv21 = new byte[i420.length];
                        byte[] nv12 = new byte[i420.length];
                        if (orientation == 90) {
                            mMediaUtil.i420Rotate(i420, width, height, dst_rotate, orientation);
                            mMediaUtil.i420ToNv21(dst_rotate, height, width, nv21);
                        } else {
                            mMediaUtil.i420ToNv21(i420, width, height, nv21);
                        }
                        nv21toNV12(nv21, nv12);
                        mH264Encoder.putData(nv12);
                        break;
                }

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
        if (bosEncode != null) {
            try {
                bosEncode.flush();
                bosEncode.close();
                bosEncode = null;
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

    private void nv21toNV12(byte[] nv21, byte[] nv12) {
        int size = nv21.length;
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
    }

}