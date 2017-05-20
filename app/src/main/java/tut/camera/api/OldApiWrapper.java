package tut.camera.api;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import tut.camera.uilt.FileSaver;
import tut.camera.uilt.LogDebug;
import tut.camera.view.CameraView;

import static android.hardware.Camera.*;

public class OldApiWrapper implements ApiWrapper {
    private static final String TAG = OldApiWrapper.class.getSimpleName();
    private static final int HALF_CIRCLE = 180;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String[] flashModes = new String[]{
            Camera.Parameters.FLASH_MODE_OFF,
            Camera.Parameters.FLASH_MODE_ON,
            Camera.Parameters.FLASH_MODE_AUTO
    };

    private Activity activity;
    private OnPictureTakenCallback pictureTakenCallback;
    private byte[] data;
    private CameraInfoHolder infoHolder = new CameraInfoHolder();
    private CameraView textureView;
    private Camera camera;
    private Camera.Size previewSize;

    private PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (pictureTakenCallback != null) {
                pictureTakenCallback.onPictureTaken(data != null);
            }
            OldApiWrapper.this.data = data;
        }
    };

    public OldApiWrapper(Activity activity, CameraView textureView,
                         OnPictureTakenCallback pictureTakenCallback) {
        this.activity = activity;
        this.textureView = textureView;
        this.pictureTakenCallback = pictureTakenCallback;
    }

    @Override
    public boolean initCamera() {
        if (camera != null) {
            return true;
        }
        if (activity == null || activity.isFinishing() || infoHolder.isOpenning()) {
            return false;
        }
        infoHolder.setOppenning(true);
        try {
            if (!setupCameraIds()) return false;
            camera = open(infoHolder.getCurrentCameraId());
            infoHolder.setOppenning(false);
            previewSize = getPreviewSize();
            setAspectRation();
            setCameraParameters();

        } catch (Exception e) {
            Toast.makeText(activity, "Cannot access a camera", Toast.LENGTH_SHORT).show();
            LogDebug.error(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    private boolean setupCameraIds() {
        int frontCameraId = -1;
        int backCameraId = -1;
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < getNumberOfCameras(); i++) {
            getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i;
            }
        }
        if (frontCameraId < 0 && backCameraId < 0) return true;
        infoHolder.setupCameraIds(frontCameraId, backCameraId);
        return true;
    }

    @Override
    public boolean startPreview() {
        if (camera == null || !textureView.isAvailable() || previewSize == null) {
            return false;
        }

        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            texture.setDefaultBufferSize(previewSize.width, previewSize.height);
        }

        try {
            camera.setPreviewTexture(texture);
            camera.startPreview();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean takePhoto() {
        if (activity == null || camera == null) {
            return false;
        }
        camera.takePicture(null, null, pictureCallback);
        return true;
    }

    @Override
    public void switchCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }

        infoHolder.switchCamera();
        camera = open(infoHolder.getCurrentCameraId());
        infoHolder.setOppenning(false);
        previewSize = getPreviewSize();

        setAspectRation();
        setCameraParameters();
        startPreview();
    }

    @Override
    public int nextFlash() {
        Camera.Parameters parameters = camera.getParameters();
        int nextFlashMode = infoHolder.getNextFlashMode(flashModes.length);
        String flashMode = flashModes[nextFlashMode];
        parameters.setFlashMode(flashMode);
        camera.setParameters(parameters);
        return nextFlashMode;
    }

    @Override
    public void usePhoto(boolean shouldUse) {
        if (shouldUse) {
            saveFileToDisk();
        }
        camera.startPreview();
    }

    @Override
    public void configureTransform(int width, int height) {
        if (activity == null || textureView == null || previewSize == null) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, previewSize.width, previewSize.height);

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) height / previewSize.height, (float) width / previewSize.width);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    @Override
    public void updatePreview(SurfaceTexture texture) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        textureView.requestLayout();
        camera.setParameters(parameters);
        camera.startPreview();
    }

    @Override
    public int getCamerasCount() {
        return infoHolder.getCameraCount();
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
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void setAspectRation() {
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.width, previewSize.height);
        } else {
            textureView.setAspectRatio(previewSize.height, previewSize.width);
        }
    }

    private Camera.Size getPreviewSize() {
        Camera.Size previewSize = camera.getParameters().getSupportedPreviewSizes().get(0);
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);

        LogDebug.debug(TAG, "screen size: [" + screenSize.x + "x" + screenSize.y + "]");
        float screenRatio = screenSize.y / (float) screenSize.x;

        for (Camera.Size size : sizes) {
            LogDebug.debug(TAG, "[" + size.width + "x" + size.height + "]");
            if (screenSize.y == size.width && screenSize.x == size.height) {
                LogDebug.debug(TAG, "preview size: [" + size.width + "x" + size.height + "]");
                return size;
            }
            float currentRatio = size.width / (float) size.height;
            float oldRatio = previewSize.width / (float) previewSize.height;
            if (Math.abs(currentRatio - screenRatio) < Math.abs(oldRatio - screenRatio)) {
                previewSize = size;
            }
        }
        LogDebug.debug(TAG, "preview size: [" + previewSize.width + "x" + previewSize.height + "]");
        return previewSize;
    }

    private void setCameraParameters() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Camera.Parameters parameters = camera.getParameters();
        if (!infoHolder.isFront()) {
            parameters.setRotation(ORIENTATIONS.get(rotation));
        } else {
            parameters.setRotation(ORIENTATIONS.get(rotation) + HALF_CIRCLE);
        }
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        Camera.Size pictureSize = getPictureSizes()[0];
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(ORIENTATIONS.get(rotation));
    }

    private Camera.Size[] getPictureSizes() {
        List<Camera.Size> sizeList = camera.getParameters().getSupportedPictureSizes();
        Camera.Size[] sizes = new Camera.Size[sizeList.size()];
        return sizeList.toArray(sizes);
    }

}
