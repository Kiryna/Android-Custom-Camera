package tut.camera;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import tut.camera.api.ApiWrapper;
import tut.camera.api.BuildConfig;
import tut.camera.api.NewApiWrapper;
import tut.camera.api.OldApiWrapper;
import tut.camera.api.R;
import tut.camera.view.CameraTextureListener;
import tut.camera.view.CameraUi;
import tut.camera.view.CameraView;

public class CameraActivity extends AppCompatActivity implements CameraUi, View.OnClickListener {
    private CameraView cameraView;
    private Button takePhotoButton;
    private Button switchCameraButton;
    private Button switchFlashButton;
    private Button okButton;
    private Button cancelButton;

    private ApiWrapper apiWrapper;
    private CameraTextureListener textureListener;
    private ApiWrapper.OnPictureTakenCallback pictureTakenCallback =
            new ApiWrapper.OnPictureTakenCallback() {
        @Override
        public void onPictureTaken(boolean isTaken) {
            if (isTaken) {
                okButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                takePhotoButton.setEnabled(false);
            }
        }
    };;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setupButtons();
        setupCameraView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        apiWrapper.initCamera();
    }

    @Override
    public void showChangeCameraButton() {
        switchCameraButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo_btn:
                apiWrapper.takePhoto();
                break;
            case R.id.switch_cameras_btn:
                apiWrapper.switchCamera();
                break;
            case R.id.switch_flash_btn:
                int flash = apiWrapper.nextFlash();
                switchFlashUI(flash);
                break;
            case R.id.ok_btn:
                apiWrapper.usePhoto(true);
                takePhotoButton.setEnabled(true);
                okButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                break;
            case R.id.cancel_btn:
                apiWrapper.usePhoto(false);
                takePhotoButton.setEnabled(true);
                okButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                break;
            default:
                if (BuildConfig.DEBUG) {
                    throw new IllegalArgumentException("No view with such id");
                }
                break;
        }
    }

    private void switchFlashUI(int flash) {
        switch (flash) {
            case 0:
                switchFlashButton.setBackgroundResource(R.drawable.ic_flash_off);
                break;
            case 1:
                switchFlashButton.setBackgroundResource(R.drawable.ic_flash_on);
                break;
            case 2:
                switchFlashButton.setBackgroundResource(R.drawable.ic_flash_auto);
                break;
        }
    }

    private void setupCameraView() {
        cameraView = (CameraView) findViewById(R.id.camera_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            apiWrapper = new NewApiWrapper(this, cameraView, pictureTakenCallback);
        } else {
            apiWrapper = new OldApiWrapper(this, cameraView, pictureTakenCallback);
        }
        textureListener = new CameraTextureListener(apiWrapper, this);
        cameraView.setSurfaceTextureListener(textureListener);
    }

    private void setupButtons() {
        takePhotoButton = (Button) findViewById(R.id.take_photo_btn);
        switchCameraButton = (Button) findViewById(R.id.switch_cameras_btn);
        switchFlashButton = (Button) findViewById(R.id.switch_flash_btn);
        okButton = (Button) findViewById(R.id.ok_btn);
        cancelButton = (Button) findViewById(R.id.cancel_btn);
        takePhotoButton.setOnClickListener(this);
        switchFlashButton.setOnClickListener(this);
        switchCameraButton.setOnClickListener(this);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }
}
