package com.aniljing.mediacodecuse.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.aniljing.mediacodecuse.utils.LogUtils;
import com.aniljing.mediacodecuse.utils.MediaUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static com.aniljing.mediacodecuse.camera2.CameraUtil.COLOR_FormatI420;
import static com.aniljing.mediacodecuse.camera2.CameraUtil.COLOR_FormatNV21;

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
    private Size mPreviewSize;
    private final Point previewViewSize;
    private Range<Integer> fpsRanges;
    private byte[] i420;
    private YUVDataCallBack mYUVDataCallBack;
    private int orientation;
    private int frameIndex = 0;

    public Camera2ProviderPreviewWithYUV(Activity mContext) {
        this.mContext = mContext;
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        previewViewSize = new Point();
        previewViewSize.x = 640;
        previewViewSize.y = 480;
    }

    public void initTexture(TextureView textureView) {
        LogUtils.e(TAG, "initTexture:" + textureView);
        mTextureView = textureView;
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
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
                    //寻找一个 最合适的尺寸     ---》 一模一样
                    mPreviewSize = getBestSupportedSize(new ArrayList<Size>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
                    if (map != null) {
                        mCameraId = cameraIds[i];
                    }
                    orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    //maxImages 此时需要2路，一路渲染到屏幕，一路用于网络传输
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
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
                mCameraDevice = camera;
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(BaseCameraProvider.previewSize.getWidth(), BaseCameraProvider.previewSize.getHeight());
                //Surface负责渲染
                Surface previewSurface = new Surface(surfaceTexture);
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
            Image image = reader.acquireNextImage();
            // Y:U:V == 4:2:2
            if (mYUVDataCallBack != null && image.getFormat() == ImageFormat.YUV_420_888) {
                Rect crop = image.getCropRect();
                int format = image.getFormat();
                int width = crop.width();
                int height = crop.height();
                if (i420 == null) {
                    i420 = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
                }
//                i420 = getI420DataByOnePlane(image,COLOR_FormatI420);
                i420 = getI420(image);
                if (mYUVDataCallBack != null) {
                    mYUVDataCallBack.yuvData(i420, width, height, orientation);
                }
            }
            frameIndex++;
            image.close();
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
            mPreviewBuilder = null;
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
        void yuvData(byte[] data, int width, int height, int orientation);
    }

    public void setYUVDataCallBack(YUVDataCallBack YUVDataCallBack) {
        mYUVDataCallBack = YUVDataCallBack;
    }

    /**
     * 将Y:U:V == 4:2:2的数据转换为nv21
     *
     * @param y      Y 数据
     * @param u      U 数据
     * @param v      V 数据
     * @param nv21   生成的nv21，需要预先分配内存
     * @param stride 步长
     * @param height 图像高度
     */
    private void yuv422ToYuv420sp(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        LogUtils.e(TAG, "yuv422ToYuv420sp");
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }

    /**
     * 将Y:U:V == 4:1:1的数据转换为nv21
     *
     * @param y      Y 数据
     * @param u      U 数据
     * @param v      V 数据
     * @param nv21   生成的nv21，需要预先分配内存
     * @param stride 步长
     * @param height 图像高度
     */
    private void yuv420ToYuv420sp(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        LogUtils.e(TAG, "yuv420ToYuv420sp");
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length + v.length;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i++) {
            nv21[i] = v[vIndex++];
            nv21[i + 1] = u[uIndex++];
        }
    }


    private void yuvToNv21(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }

    private void nv21_rotate_to_90(byte[] nv21_data, byte[] nv21_rotated, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;

        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++) {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
    }

    //3/2    2   1
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

    private Size getBestSupportedSize(List<Size> sizes) {
        Point maxPreviewSize = new Point(640, 480);
        Point minPreviewSize = new Point(480, 320);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, (o1, o2) -> {
            if (o1.getWidth() > o2.getWidth()) {
                return -1;
            } else if (o1.getWidth() == o2.getWidth()) {
                return o1.getHeight() > o2.getHeight() ? -1 : 1;
            } else {
                return 1;
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    private byte[] getI420DataByOnePlane(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        try {
            Rect crop = image.getCropRect();
            int format = image.getFormat();
            int width = crop.width();
            int height = crop.height();
            Image.Plane[] planes = image.getPlanes();
            byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
            byte[] rowData = new byte[planes[0].getRowStride()];
            int channelOffset = 0;
            int outputStride = 1;
            for (int i = 0; i < planes.length; i++) {
                switch (i) {
                    case 0:
                        channelOffset = 0;
                        outputStride = 1;
                        break;
                    case 1:
                        if (colorFormat == COLOR_FormatI420) {
                            channelOffset = width * height;
                            outputStride = 1;
                        } else if (colorFormat == COLOR_FormatNV21) {
                            channelOffset = width * height + 1;
                            outputStride = 2;
                        }
                        break;
                    case 2:
                        if (colorFormat == COLOR_FormatI420) {
                            channelOffset = (int) (width * height * 1.25);
                            outputStride = 1;
                        } else if (colorFormat == COLOR_FormatNV21) {
                            channelOffset = width * height;
                            outputStride = 2;
                        }
                        break;
                    default:
                }
                ByteBuffer buffer = planes[i].getBuffer();
                int rowStride = planes[i].getRowStride();
                int pixelStride = planes[i].getPixelStride();
                int shift = (i == 0) ? 0 : 1;
                int w = width >> shift;
                int h = height >> shift;
                buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
                for (int row = 0; row < h; row++) {
                    int length;
                    if (pixelStride == 1 && outputStride == 1) {
                        length = w;
                        buffer.get(data, channelOffset, length);
                        channelOffset += length;
                    } else {
                        length = (w - 1) * pixelStride + 1;
                        buffer.get(rowData, 0, length);
                        for (int col = 0; col < w; col++) {
                            data[channelOffset] = rowData[col * pixelStride];
                            channelOffset += outputStride;
                        }
                    }
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                }
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getI420DataByTwoPlane(Image image) {
        MediaUtil mediaUtil = new MediaUtil();
        ReentrantLock lock = new ReentrantLock();
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            Image.Plane[] planes = image.getPlanes();
            // 加锁确保y、u、v来源于同一个Image
            lock.lock();
            Rect crop = image.getCropRect();
            int format = image.getFormat();
            int width = crop.width();
            int height = crop.height();
            byte[] y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            byte[] u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            byte[] v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            byte[] nv21 = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
            byte[] i420 = new byte[nv21.length];
            if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(u);
                planes[2].getBuffer().get(v);
            }
            yuv422ToYuv420sp(y, u, v, nv21, width, height);
            mediaUtil.nv21ToI420(nv21, width, height, i420);
            return i420;
        }
        return null;
    }

    private byte[] getI420(Image image) {
        try {
            int w = image.getWidth(), h = image.getHeight();
            // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
            int i420Size = w * h * 3 / 2;

            Image.Plane[] planes = image.getPlanes();
            //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
            int remaining0 = planes[0].getBuffer().remaining();
            int remaining1 = planes[1].getBuffer().remaining();
            //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
            int remaining2 = planes[2].getBuffer().remaining();
            //获取pixelStride，可能跟width相等，可能不相等
            int pixelStride = planes[2].getPixelStride();
            int rowOffest = planes[2].getRowStride();
            byte[] nv21 = new byte[i420Size];
            //分别准备三个数组接收YUV分量。
            byte[] yRawSrcBytes = new byte[remaining0];
            byte[] uRawSrcBytes = new byte[remaining1];
            byte[] vRawSrcBytes = new byte[remaining2];
            planes[0].getBuffer().get(yRawSrcBytes);
            planes[1].getBuffer().get(uRawSrcBytes);
            planes[2].getBuffer().get(vRawSrcBytes);
            if (pixelStride == image.getWidth()) {
                //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
                System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
            } else {
                //根据每个分量的size先生成byte数组
                byte[] ySrcBytes = new byte[w * h];
                byte[] uSrcBytes = new byte[w * h / 2 - 1];
                byte[] vSrcBytes = new byte[w * h / 2 - 1];
                for (int row = 0; row < h; row++) {
                    //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                    System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                    //y执行两次，uv执行一次
                    if (row % 2 == 0) {
                        //最后一行需要减一
                        if (row == h - 2) {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                        } else {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                        }
                    }
                }
                //yuv拷贝到一个数组里面
                System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
                System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
                byte[] i420 = new byte[nv21.length];
                MediaUtil mediaUtil = new MediaUtil();
                mediaUtil.nv21ToI420(nv21, w, h, i420);
                return i420;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
