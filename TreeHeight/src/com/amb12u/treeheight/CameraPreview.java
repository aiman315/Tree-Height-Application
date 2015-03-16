package com.amb12u.treeheight;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

	private final String TAG = "CameraPreview";
	private Camera camera;
	private SurfaceHolder holder;

	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "CameraPreview");
	}

	public CameraPreview(Context context) {
		super(context);
		Log.d(TAG, "CameraPreview");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged");
		stopPreview();
		startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		Log.d(TAG, "surfaceCreated");
		startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Log.d(TAG, "surfaceDestroyed");
		stopPreview();
	}

	/**
	 * Connects the preview to the selected camera
	 * and uses its ID to correct preview's orientation
	 * and starts preview
	 * @param cameraRef: camera to connect to
	 * @param cameraId: ID of camera to connect to
	 */
	public void connectCamera (Camera cameraRef, int cameraId) {
		Log.d(TAG, "connectCamera");
		camera = cameraRef;

		int previewOrientation = getCameraPreviewOrientation(cameraId);
		camera.setDisplayOrientation(previewOrientation);

		//TODO: set other camera params
		Camera.Parameters params = camera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		camera.setParameters(params);

		holder = getHolder();
		holder.addCallback(this);

		startPreview();
	}

	/**
	 * If the preview is connected to a camera, stop the preview and disconnect the camera  
	 */
	public void releaseCamera() {
		Log.d(TAG, "releaseCamera");
		if (camera != null) {
			stopPreview();
			camera = null;
		}
	}

	/**
	 * Verify camera is connected, and surface holder exists
	 * then starts the camera preview
	 */
	private void startPreview() {
		Log.d(TAG, "startPreview");
		if (camera != null && holder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (IOException e) {
				Log.e(TAG, "Error setting preview display: " + e.getMessage());
			}
		}
	}

	/**
	 * Verify camera is connected
	 * then stops the camera preview
	 */
	private void stopPreview() {
		Log.d(TAG, "stopPreview");
		if (camera != null) {
			try {
				camera.stopPreview();
			} catch (Exception e) {
				Log.e(TAG, "Error stopping preview display: " + e.getMessage());
			}
		}
	}

	/**
	 * Gets the correct preview orientation for a selected camera
	 * @param cameraId: ID of selected camera
	 * @return previewOrientation: The correct orientation value for preview in degrees
	 */
	private int getCameraPreviewOrientation(int cameraId) {
		Log.d(TAG, "getCameraPreviewOrientation");
		final int DEGREES_IN_CIRCLE = 360; 
		int temp = 0;
		int previewOrientation = 0;

		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, cameraInfo);

		int deviceOrientation = getDeviceOrientationDegrees();

		switch(cameraInfo.facing) {
		case Camera.CameraInfo.CAMERA_FACING_BACK:
			temp = cameraInfo.orientation - deviceOrientation + DEGREES_IN_CIRCLE;
			previewOrientation = temp % DEGREES_IN_CIRCLE;
			break;
		case Camera.CameraInfo.CAMERA_FACING_FRONT:
			temp = (cameraInfo.orientation + deviceOrientation ) % DEGREES_IN_CIRCLE;
			previewOrientation = (DEGREES_IN_CIRCLE - temp) % DEGREES_IN_CIRCLE;
			break;
		}
		return previewOrientation;
	}

	/**
	 * Gets the value of device rotation (0, 90, 180, 270)
	 * @return degrees: the current device rotation in degrees
	 */
	private int getDeviceOrientationDegrees() {
		Log.d(TAG, "getDeviceOrientationDegrees");
		int degrees = 0;
		WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		int rotation = windowManager.getDefaultDisplay().getRotation();

		switch(rotation) {
		case Surface.ROTATION_0: 
			degrees = 0; 
			break;
		case Surface.ROTATION_90: 
			degrees = 90; 
			break;
		case Surface.ROTATION_180: 
			degrees = 180; 
			break;
		case Surface.ROTATION_270: 
			degrees = 270; 
			break;

		}
		return degrees;
	}

}
