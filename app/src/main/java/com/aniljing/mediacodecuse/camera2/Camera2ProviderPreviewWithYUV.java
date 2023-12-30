package com.aniljing.mediacodecuse.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.view.Surface;
import android.view.TextureView;


import com.aniljing.mediacodecuse.utils.LogUtils;

import java.util.Arrays;

/**
 * @ClassName Camera2ProviderPreviewWithYUV
 * Camera2 两路预览：
 * 1、使用TextureView预览，直接输出。
 * 2、使用ImageReader获取数据，输出格式为ImageFormat.YUV_420_888，java端转化为NV21，再使用YuvImage生成Bitmap实现预览。
 */
public class Camera2ProviderPreviewWithYUV {
    private static final String TAG = Camera2ProviderPreviewWithYUV.class.getSimpleName();
    private Activity mContext;
    private String mCameraId;
    private HandlerThread handlerThread;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private Surface previewSurface;
    private Range<Integer> fpsRanges;
    private YUVDataCallBack mYUVDataCallBack;

    public Camera2ProviderPreviewWithYUV(Activity mContext) {
        this.mContext = mContext;
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
    }

    public void initTexture(TextureView textureView) {
        LogUtils.e(TAG, "initTexture:" + textureView);
        mTextureView = textureView;
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LogUtils.e(TAG, "onSurfaceTextureAvailable:width-"+width+"--height:"+height);
                openCamera(BaseCameraProvider.previewSize.getWidth(), BaseCameraProvider.previewSize.getHeight());
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                LogUtils.e(TAG, "onSurfaceTextureDestroyed");
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        LogUtils.e(TAG, "openCamera");
        LogUtils.e(TAG, "Width:" + width + "------Height:" + height);
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (int i = 0; i < cameraIds.length; i++) {
                //描述相机设备的属性类
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIds[i]);
                Range<Integer>[] allFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                fpsRanges = allFpsRanges[allFpsRanges.length - 1];

                //获取是前置还是后置摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //使用后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        mCameraId = cameraIds[i];
                    }
                    //maxImages 此时需要2路，一路渲染到屏幕，一路用于网络传输
                    mImageReader = ImageReader.newInstance(width, height,
                            ImageFormat.YUV_420_888, 2);
                    mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
                    cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
                }
            }
        } catch (CameraAccessException r) {
            LogUtils.e(TAG, "openCamera:" + r);
            LogUtils.e(TAG, "openCamera getMessage:" + r.getMessage());
            LogUtils.e(TAG, "openCamera getLocalizedMessage:" + r.getLocalizedMessage());
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            try {
                LogUtils.e(TAG, "onOpened");
                mCameraDevice = camera;
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(BaseCameraProvider.previewSize.getWidth(), BaseCameraProvider.previewSize.getHeight());
                //Surface负责渲染
                previewSurface = new Surface(surfaceTexture);
                //创建请求
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.addTarget(previewSurface);
                mPreviewBuilder.addTarget(mImageReader.getSurface());
                //创建会话
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), mStateCallBack, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                LogUtils.e(TAG, "onOpened:" + e.getMessage());
                LogUtils.e(TAG, "onOpened:" + e.getLocalizedMessage());
                LogUtils.e(TAG, "onOpened:" + e);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            LogUtils.e(TAG, "onDisconnected");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            LogUtils.e(TAG, "onError:" + error);
            camera.close();
            mCameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @SuppressLint("LongLogTag")
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            CameraUtil.getDataFromImage(image, CameraUtil.COLOR_FormatI420, mYUVDataCallBack);

        }
    };

    private final CameraCaptureSession.StateCallback mStateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                mCaptureSession = session;
                CaptureRequest request = mPreviewBuilder.build();
                // Finally, we start displaying the camera preview.
                session.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            LogUtils.e(TAG, "onConfigureFailed:");
        }
    };

    public void releaseCamera() {
        LogUtils.e(TAG, "releaseCamera");
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mPreviewBuilder != null) {
            mPreviewBuilder=null;
        }
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader.close();
            mImageReader = null;
        }

        if (handlerThread != null && !handlerThread.isInterrupted()) {
            handlerThread.quit();
            handlerThread.interrupt();
            handlerThread = null;
        }
        if (mCameraHandler != null) {
            mCameraHandler = null;
        }

    }


    public interface YUVDataCallBack {
        void yuvData(byte[] i420, int width, int height);
    }

    public void setYUVDataCallBack(YUVDataCallBack YUVDataCallBack) {
        mYUVDataCallBack = YUVDataCallBack;
    }

}
