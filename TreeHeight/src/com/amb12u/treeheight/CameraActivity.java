package com.amb12u.treeheight;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class CameraActivity extends Activity {

	private final String TAG = "CameraActivity";
	
	private final String SELETED_CAMERA_ID_KEY = "selectedCameraId";
	private final int CAMERA_ID_NOT_SET = -1;
	
	private final int PHOTO_INTENT = 1000;
	
	private Uri photoFileUri;
	
	// Determine existence of cameras
	private boolean hasCamera = false;
	private boolean hasFrontCamera = false;
	
	private Camera selectedCamera;
	
	// Holds ID values for cameras
	private int frontFacingCameraId = CAMERA_ID_NOT_SET;
	private int backFacingCameraId = CAMERA_ID_NOT_SET;
	private int selectedCameraId = CAMERA_ID_NOT_SET;

	
	/**
	 * Starts Camera application to capture an image with timeStamp name
	 * @param v: The view that invoked the method
	 */
	public void takePicture(View v) {
		Log.d(TAG, "takePicture");
		photoFileUri = generateTimeStampPhotoFileUri();
		if (photoFileUri != null) {
			Intent intent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
			startActivityForResult(intent, PHOTO_INTENT);
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent resultIntent) {
		Log.d(TAG, "onActivityResult");
		Log.d(TAG, String.format("requestCode: %d | resultCode: %d", requestCode, resultCode));
		
		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "User Canceled", Toast.LENGTH_SHORT).show();
			return;
		}

		switch (requestCode) {
		case PHOTO_INTENT:
			//obtain image from application images directory and display it in activity
			Bitmap imageBitmap = BitmapFactory.decodeFile(photoFileUri.getPath());
			if (imageBitmap != null) {
				//ImageView imageView = (ImageView) findViewById(R.id.imageView);
				//imageView.setImageBitmap(imageBitmap);
			}
			break;
		}
	}

	/**
	 * Swaps the selected camera, between front facing and back facing
	 * @param v: The view that invoked the method
	 */
	public void onClickSwapCamera(View v) {
		if (selectedCameraId == frontFacingCameraId) {
			selectedCameraId = getBackFacingCameraId();	
		} else {
			selectedCameraId = getFrontFacingCameraId();
		}
		openSelectedCamera();
	}
	
	/**
	 * Stops the camera preview
	 * @param v: The view that invoked the method
	 */
	public void onClickCloseCamera(View v) {
		releaseSelectedCamera();
		selectedCameraId = CAMERA_ID_NOT_SET;
	}
	
	/** 
	 * Get the photo directory for application, and creates it if necessary
	 * @return File outputDir
	 */
	private File getPhotoDirectory() {
		Log.d(TAG, "getPhotoDirectory");
		
		File outputDir = null;
		String externalStorageState = Environment.getExternalStorageState();
		if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
			File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			outputDir = new File (pictureDir, "TreeHeightApp");
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					Toast.makeText(this,  "failed to create directory: " + outputDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
					outputDir = null;
				}
			}
		}
		return outputDir;
	}
	
	/**
	 * Creates a time stamp for captured image and converts it to full Uri
	 * @return photoFileUri
	 */
	private Uri generateTimeStampPhotoFileUri() {
		Log.d(TAG, "generateTimeStampPhotoFileUri");
		
		Uri photoFileUri = null;
		File outputDir = getPhotoDirectory();
		
		if (outputDir != null) {
			String timeStamp = 	new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.UK).format(new Date()); 
			Toast.makeText(this, timeStamp, Toast.LENGTH_SHORT).show();
			String photoFileName = "IMG_" + timeStamp + ".jpg";
			
			File photoFile = new File (outputDir, photoFileName);
			photoFileUri = Uri.fromFile(photoFile);	
		}
		
		return photoFileUri;
	}

	/**
	 * Creates a Dialog to inform there is no camera functionality in device
	 */
	private void showNoCameraDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("No Camera");
		builder.setMessage("Device does not have required camera supprt. " +
		"Some features will not be available.");
		builder.setPositiveButton("continue", null);
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	/**
	 * Gets the ID of requested camera
	 * @param facing: whether facing front or back
	 * @return cameraId: ID of camera requested
	 */
	private int getFacingCameraId(int facing) {
		int cameraId = CAMERA_ID_NOT_SET;
		
		int nCameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		
		for(int cameraInfoId = 0 ; cameraInfoId < nCameras ; cameraInfoId++) {
			Camera.getCameraInfo(cameraInfoId, cameraInfo);
			// is camera in list of cameras in device the same as the one requested?
			if(cameraInfo.facing == facing) {
				cameraId = cameraInfoId;
				break;
			}
		}
		return cameraId;
	}

	/**
	 * Gets ID of front facing camera
	 * @return frontFacingCameraId
	 */
	private int getFrontFacingCameraId() {
		if(frontFacingCameraId == CAMERA_ID_NOT_SET) {
			frontFacingCameraId = getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
		}
		return frontFacingCameraId;
	}

	/**
	 * Gets ID of back facing camera
	 * @return backFacingCameraId
	 */
	private int getBackFacingCameraId() {
		
		if(backFacingCameraId == CAMERA_ID_NOT_SET) {
			backFacingCameraId = getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
		}
		return backFacingCameraId;
	}

	/**
	 * Opens the selected camera
	 * and displays its preview on the activity
	 */
	private void openSelectedCamera() {
		releaseSelectedCamera(); //to ensure no cameras are open
		
		if(selectedCameraId != CAMERA_ID_NOT_SET) {
			String message;
			try {
				selectedCamera = Camera.open(selectedCameraId);
				message = "Opened Camera ID:" + selectedCameraId;
				
				CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
				cameraPreview.connectCamera(selectedCamera, selectedCameraId);
			} catch (Exception e) {
				message = "Unable to open camera: "+ e.getMessage(); 
				Log.e(TAG, message);
			}
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Releases the selected camera object, and resets it
	 */
	private void releaseSelectedCamera() {
		if(selectedCamera != null) {
			CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
			cameraPreview.releaseCamera();
			selectedCamera.release();
			selectedCamera = null;
		}
	}


//	---------------- Activity Methods ---------------- //
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//gets the selected camera ID from previous state
		outState.putInt(SELETED_CAMERA_ID_KEY, selectedCameraId);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_camera);
		
		//check for camera feature
		PackageManager pm = getPackageManager();
		hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		hasFrontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		
		if (!(hasCamera || hasFrontCamera)) {
			showNoCameraDialog();
		} else {
			//sets back facing camera as default for selected camera
			selectedCameraId = getBackFacingCameraId();
			//checks if the program has a saved state
			if (savedInstanceState != null) {
				selectedCameraId = savedInstanceState.getInt(SELETED_CAMERA_ID_KEY);
			}
			openSelectedCamera();
		}
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		releaseSelectedCamera(); //to allow other applications to use selected camera
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		openSelectedCamera(); //to restore control of selected camera resource
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
}
