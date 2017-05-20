package tut.camera.api;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tut.camera.uilt.FileSaver;
import tut.camera.uilt.LogDebug;
import tut.camera.view.CameraView;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NewApiWrapper implements ApiWrapper {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String TAG = NewApiWrapper.class.getSimpleName();

    private CameraCaptureSession previewSession;
    private CameraDevice device;
    private CaptureRequest.Builder previewBuilder;

    private Activity activity;
    private CameraView textureView;
    private OnPictureTakenCallback pictureTakenCallback;
    private Size previewSize;
    private byte[] data = null;

    private CameraInfoHolder infoHolder = new CameraInfoHolder();

    private CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            previewSession = cameraCaptureSession;
            updatePreview(null);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            if (activity != null) {
                Toast.makeText(activity, "Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private ImageReader.OnImageAvailableListener onImageListener = new ImageReader
            .OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) return;
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            data = bytes;
        }
    };

    public NewApiWrapper(Activity activity, CameraView textureView,
                         OnPictureTakenCallback pictureTakenCallback) {
        this.activity = activity;
        this.textureView = textureView;
        this.pictureTakenCallback = pictureTakenCallback;
    }

    private CameraDevice.StateCallback stateListener = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            device = cameraDevice;
            startPreview();
            infoHolder.setOppenning(false);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            device = null;
            infoHolder.setOppenning(false);
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            device = null;
            if (activity != null) {
                activity.finish();
            }
            infoHolder.setOppenning(false);
        }
    };

    @Override
    public boolean initCamera() {
        if (activity == null || activity.isFinishing() || infoHolder.isOpenning()) {
            return false;
        }
        infoHolder.setOppenning(true);
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            String cameraId = cameraIdList[0];
            int back = (cameraIdList.length > 0) ? 0 : -1;
            int front = (cameraIdList.length > 1) ? 1 : -1;

            infoHolder.setupCameraIds(front, back);

            CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configurationMap = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = configurationMap.getOutputSizes(SurfaceTexture.class)[0];

            setAspectRatio();
            manager.openCamera(cameraId, stateListener, null);
            return true;
        } catch (SecurityException e) {
            LogDebug.error(TAG, "Camera not permitted", e);
            return false;
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access a camera", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
    }

    @Override
    public boolean startPreview() {
        if (device == null || !textureView.isAvailable() || previewSize == null) {
            return false;
        }
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            previewBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);
            device.createCaptureSession(Arrays.asList(surface), callback, null);
            return true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean takePhoto() {
        if (activity == null || device == null) {
            return false;
        }
        Size[] jpegSize = getJpegSizes(activity);
        ImageReader reader = getImageReader(jpegSize);

        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(reader.getSurface());
        outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

        CaptureRequest.Builder builder = getCaptureRequestBuilder(activity, reader);

        File file = new File(activity.getExternalCacheDir(), "pic" + System.currentTimeMillis() + ".jpg");


        HandlerThread thread = new HandlerThread("CameraPicture" + System.currentTimeMillis());
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());
        reader.setOnImageAvailableListener(onImageListener, backgroundHandler);

        CameraCaptureSession.CaptureCallback captureCallback = getCaptureCallback();
        createCaptureSession(outputSurfaces, builder, backgroundHandler, captureCallback);
        return true;
    }

    @Override
    public void switchCamera() {
        if (device != null) {
            device.close();
        }
        infoHolder.switchCamera();
        if (activity == null || activity.isFinishing() || infoHolder.isOpenning()) {
            return;
        }
        infoHolder.setOppenning(true);
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            String cameraId = cameraIdList[infoHolder.getCurrentCameraId()];
            CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configurationMap = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = configurationMap.getOutputSizes(SurfaceTexture.class)[0];

            setAspectRatio();
            manager.openCamera(cameraId, stateListener, null);
            infoHolder.setOppenning(false);
        } catch (SecurityException e) {
            LogDebug.error(TAG, "Camera not permitted", e);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access a camera", Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    }

    @Override
    public int nextFlash() {
        return 0;
    }

    @Override
    public void usePhoto(boolean shouldUse) {
        if (shouldUse) {
            saveFileToDisk();
        }
        startPreview();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void configureTransform(int width, int height) {
        if (activity == null || textureView == null || previewSize == null) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) height / previewSize.getHeight(), (float) width / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    @Override
    public String saveFileToDisk() {
        FileSaver fileSaver = new FileSaver();
        File dir = activity.getExternalCacheDir();
        if (dir == null) {
            dir = activity.getCacheDir();
        }
        return fileSaver.saveFileToDisk(dir, data);
    }

    @Override
    public void releaseCamera() {
        if (device != null) {
            device.close();
            device = null;
        }
    }

    @Override
    public void updatePreview(SurfaceTexture surface) {
        if (device == null) {
            return;
        }
        setUpCaptureRequestBuilder(previewBuilder);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder previewBuilder) {
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
    }

    @Override
    public int getCamerasCount() {
        return infoHolder.getCameraCount();
    }

    private void setAspectRatio() {
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        } else {
            textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        }
    }

    private ImageReader getImageReader(Size[] jpegSize) {
        if (jpegSize == null || jpegSize.length == 0) {
            return null;
        }
        int width = jpegSize[0].getWidth();
        int height = jpegSize[0].getHeight();
        return ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
    }

    private Size[] getJpegSizes(Activity activity) {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics;
        try {
            characteristics = manager.getCameraCharacteristics(device.getId());
        } catch (CameraAccessException e) {
            return null;
        }

        return characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(ImageFormat.JPEG);
    }

    private CaptureRequest.Builder getCaptureRequestBuilder(Activity activity, ImageReader reader) {
        try {
            CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(reader.getSurface());
            setUpCaptureRequestBuilder(builder);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            return builder;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private CameraCaptureSession.CaptureCallback getCaptureCallback() {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                           TotalCaptureResult result) {
                if (pictureTakenCallback != null) {
                    pictureTakenCallback.onPictureTaken(data != null);
                }
            }
        };
    }

    private void createCaptureSession(List<Surface> outputSurfaces,
                                      final CaptureRequest.Builder builder,
                                      final Handler backgroundHandler,
                                      final CameraCaptureSession.CaptureCallback captureCallback) {
        try {
            device.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(builder.build(), captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        LogDebug.error(TAG, "Cannot access camera", e);
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
