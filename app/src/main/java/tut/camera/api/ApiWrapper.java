package tut.camera.api;

import android.graphics.SurfaceTexture;

public interface ApiWrapper {

    boolean initCamera();

    boolean startPreview();

    boolean takePhoto();

    void switchCamera();

    int nextFlash();

    void usePhoto(boolean shouldUse);

    void configureTransform(int width, int height);

    String saveFileToDisk();

    void releaseCamera();

    void updatePreview(SurfaceTexture surface);

    int getCamerasCount();

    interface OnPictureTakenCallback {
        void onPictureTaken(boolean isTaken);
    }

}
