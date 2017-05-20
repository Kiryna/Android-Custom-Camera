package tut.camera.api;

import android.hardware.Camera;

import static android.hardware.Camera.getCameraInfo;
import static android.hardware.Camera.getNumberOfCameras;


/*package*/ class CameraInfoHolder {
    private int frontCameraId = -1;
    private int backCameraId = -1;
    private int currentCameraId = -1;
    private boolean isOpenning = false;
    private int currentFlashMode;


    /*package*/ int getCameraCount() {
        if (backCameraId > 0) {
            if (frontCameraId > 0) {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    /*package*/ boolean setupCameraIds(int front, int back) {
        frontCameraId = front;
        backCameraId = back;
        if (frontCameraId < 0 && backCameraId < 0) {
            return false;
        }
        if (backCameraId >= 0) {
            currentCameraId = backCameraId;
        } else if (frontCameraId >= 0) {
            currentCameraId = frontCameraId;
        }
        return true;
    }

    /*package*/ boolean isFront() {
        return currentCameraId == frontCameraId;
    }

    /*package*/ void switchCamera() {
        currentCameraId = isFront() ? backCameraId : frontCameraId;
    }

    /*package*/ void setOppenning(boolean isOppenning) {
        this.isOpenning = isOppenning;
    }

    /*package*/ int getCurrentCameraId() {
        return currentCameraId;
    }

    /*package*/ boolean isOpenning() {
        return isOpenning;
    }

    /*package*/ int getNextFlashMode(int flashModesLength) {
        currentFlashMode = (currentFlashMode + 1) % flashModesLength;
        return currentFlashMode;
    }
}
