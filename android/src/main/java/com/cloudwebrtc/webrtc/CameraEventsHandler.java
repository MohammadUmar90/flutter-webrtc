package com.cloudwebrtc.webrtc;

import android.util.Log;

import org.webrtc.CameraVideoCapturer;

class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {
    public enum CameraState {
        NEW,
        OPENING,
        OPENED,
        CLOSED,
        DISCONNECTED,
        ERROR,
        FREEZED
    }
    private final static String TAG = FlutterWebRTCPlugin.TAG;
    private CameraState state = CameraState.NEW;
    private final Object stateLock = new Object();

    public void waitForCameraOpen() {
        Log.d(TAG, "CameraEventsHandler.waitForCameraOpen - non-blocking, camera will initialize asynchronously");
        // Completely non-blocking - camera initialization happens asynchronously via callbacks
        // The camera will be ready when onFirstFrameAvailable() or onCameraError() is called
        // No need to block any thread
    }

    public void waitForCameraClosed() {
        Log.d(TAG, "CameraEventsHandler.waitForCameraClosed - non-blocking, camera will close asynchronously");
        // Completely non-blocking - camera closing happens asynchronously via callbacks
        // The camera will be closed when onCameraClosed() is called
        // No need to block any thread
    }

    private void setState(CameraState newState) {
        synchronized (stateLock) {
            state = newState;
            stateLock.notifyAll();
        }
    }

    // Camera error handler - invoked when camera can not be opened
    // or any camera exception happens on camera thread.
    @Override
    public void onCameraError(String errorDescription) {
        Log.d(TAG, String.format("CameraEventsHandler.onCameraError: errorDescription=%s", errorDescription));
        setState(CameraState.ERROR);
    }

    // Called when camera is disconnected.
    @Override
    public void onCameraDisconnected() {
        Log.d(TAG, "CameraEventsHandler.onCameraDisconnected");
        setState(CameraState.DISCONNECTED);
    }

    // Invoked when camera stops receiving frames
    @Override
    public void onCameraFreezed(String errorDescription) {
        Log.d(TAG, String.format("CameraEventsHandler.onCameraFreezed: errorDescription=%s", errorDescription));
        setState(CameraState.FREEZED);
    }

    // Callback invoked when camera is opening.
    @Override
    public void onCameraOpening(String cameraName) {
        Log.d(TAG, String.format("CameraEventsHandler.onCameraOpening: cameraName=%s", cameraName));
        setState(CameraState.OPENING);
    }

    // Callback invoked when first camera frame is available after camera is opened.
    @Override
    public void onFirstFrameAvailable() {
        Log.d(TAG, "CameraEventsHandler.onFirstFrameAvailable");
        setState(CameraState.OPENED);
    }

    // Callback invoked when camera closed.
    @Override
    public void onCameraClosed() {
        Log.d(TAG, "CameraEventsHandler.onCameraClosed");
        setState(CameraState.CLOSED);
    }
}
