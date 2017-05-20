package tut.camera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

import tut.camera.uilt.LogDebug;

public class CameraView extends TextureView {
    private static final String TAG = CameraView.class.getSimpleName();
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(int width, int height) {
        LogDebug.debug(TAG, "width = " + width);
        LogDebug.debug(TAG, "height = " + height);
        validateSize(width, height);
        ratioWidth = width;
        ratioHeight = height;
        invalidate();
    }

    private void validateSize(int width, int height) {
        if (width < 0) {
            throw new IllegalStateException("Incorrect width: " + width +
                    ". Width should be greater than 0");
        } else if (height < 0) {
            throw new IllegalStateException("Incorrect height: " + height +
                    ". Height should be greater than 0");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }
}
