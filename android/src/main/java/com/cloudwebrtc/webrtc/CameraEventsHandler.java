package com.cloudwebrtc.webrtc;

import android.os.Looper;
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
        Log.d(TAG, "CameraEventsHandler.waitForCameraOpen");
        boolean isMainThread = Looper.getMainLooper().getThread() == Thread.currentThread();
        
        // Use very short wait times to avoid blocking UI while allowing camera to initialize
        long maxWaitTime = isMainThread ? 100 : 300; // 100ms on main thread, 300ms on background
        long waitInterval = 10; // Check every 10ms
        long startTime = System.currentTimeMillis();
        
        synchronized (stateLock) {
            // Quick check first - if already ready, return immediately
            if (state == CameraState.OPENED || state == CameraState.ERROR) {
                return;
            }
            
            while (state != CameraState.OPENED && state != CameraState.ERROR) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= maxWaitTime) {
                    Log.d(TAG, "waitForCameraOpen: timeout after " + elapsed + "ms, state: " + state + " (camera will continue initializing asynchronously)");
                    return;
                }
                try {
                    stateLock.wait(waitInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "waitForCameraOpen interrupted, camera will continue initializing");
                    return;
                }
            }
        }
    }

    public void waitForCameraClosed() {
        Log.d(TAG, "CameraEventsHandler.waitForCameraClosed");
        boolean isMainThread = Looper.getMainLooper().getThread() == Thread.currentThread();
        
        // Don't block main thread at all - camera closing happens asynchronously via callbacks
        // The real blocking issue is in SurfaceTextureHelper.stopListening() which is a WebRTC library call
        if (isMainThread) {
            Log.d(TAG, "Skipping wait on main thread to avoid blocking UI (camera will close asynchronously)");
            return;
        }
        
        // On background threads, allow a short wait
        long maxWaitTime = 300; // 300ms max on background threads
        long waitInterval = 10; // Check every 10ms
        long startTime = System.currentTimeMillis();
        
        synchronized (stateLock) {
            // Quick check first - if already closed, return immediately
            if (state == CameraState.CLOSED || state == CameraState.ERROR) {
                return;
            }
            
            while (state != CameraState.CLOSED && state != CameraState.ERROR) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= maxWaitTime) {
                    Log.d(TAG, "waitForCameraClosed: timeout after " + elapsed + "ms, state: " + state + " (camera will continue closing asynchronously)");
                    return;
                }
                try {
                    stateLock.wait(waitInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "waitForCameraClosed interrupted, camera will continue closing");
                    return;
                }
            }
        }
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
