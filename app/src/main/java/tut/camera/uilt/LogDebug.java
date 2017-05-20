package tut.camera.uilt;

import android.util.Log;

import tut.camera.api.BuildConfig;

public class LogDebug {

    public static void debug(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void error(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void error(String tag, String message, Exception e) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, e);
        }
    }
}
