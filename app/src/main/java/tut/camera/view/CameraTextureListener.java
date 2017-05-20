package tut.camera.view;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import tut.camera.api.ApiWrapper;


public class CameraTextureListener implements TextureView.SurfaceTextureListener {
    private ApiWrapper apiWrapper;
    private CameraUi cameraUi;

    public CameraTextureListener(ApiWrapper apiWrapper, CameraUi cameraUi) {
        this.apiWrapper = apiWrapper;
        this.cameraUi = cameraUi;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        apiWrapper.configureTransform(width, height);
        apiWrapper.startPreview();
        if (apiWrapper.getCamerasCount() > 1) {
            cameraUi.showChangeCameraButton();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        apiWrapper.configureTransform(width, height);
        apiWrapper.updatePreview(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        apiWrapper.releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
