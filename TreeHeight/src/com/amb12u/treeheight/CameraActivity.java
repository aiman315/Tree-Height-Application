package com.amb12u.treeheight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends Activity implements SensorEventListener {

	//TODO: organize variables (private final, private, public, initialization is on create)
	//TODO: Instantiate required layout components, and initialize them in functions
	//TODO: Move all text to strings.xml

	private final String TAG = "CameraActivity";
	private final int INVALID_ANGLE = -999;

	private final String SELETED_CAMERA_ID_KEY = "selectedCameraId";
	private final int CAMERA_ID_NOT_SET = -1;

	// Determine existence of cameras
	private boolean hasCamera = false;
	private boolean hasFrontCamera = false;

	private Camera selectedCamera;

	// Holds ID values for cameras
	private int frontFacingCameraId = CAMERA_ID_NOT_SET;
	private int backFacingCameraId = CAMERA_ID_NOT_SET;
	private int selectedCameraId = CAMERA_ID_NOT_SET;

	// Accelerometer variables
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;

	// Camera height
	private double heightCamera;
	
	// Angle readings
	private double tempAngle;
	private int angle1;
	private int angle2;
	
	//Tree height
	private double heightTree;



	/**
	 * Read and store current angle reading
	 * @param v: The view that invoked the method
	 */
	public void onClickReadAngle(View v) {
		Log.d(TAG, "onClickReadAngle");
		if (angle1 == INVALID_ANGLE) {
			angle1 = (int) tempAngle;
			Toast.makeText(this, String.format("1st angle reading: %dº", angle1), Toast.LENGTH_SHORT).show();
			
			Button buttonUndoAngle = (Button) findViewById(R.id.buttonUndoAngle);
			buttonUndoAngle.setEnabled(true);
		} else {
			angle2 = (int) tempAngle;
			Toast.makeText(this, String.format("2nd angle reading: %dº", angle2), Toast.LENGTH_SHORT).show();
			
			Button buttonReadAngle = (Button) v;
			buttonReadAngle.setEnabled(false);
			 
			Button buttonCalculateHeight = (Button) findViewById(R.id.buttonCalculateHeight);
			buttonCalculateHeight.setEnabled(true);
		}	
	}
	
	/**
	 * Reset angles readings
	 * @param v
	 */
	public void onClickUndoAngle(View v) {
		Log.d(TAG, "onClickResetAngles");
		
		if (angle2 != INVALID_ANGLE) {
			angle2 = INVALID_ANGLE;
			Toast.makeText(this, "Cleared 2nd angle reading", Toast.LENGTH_SHORT).show();
			
			Button buttonCalculateHeight = (Button) findViewById(R.id.buttonCalculateHeight);
			buttonCalculateHeight.setEnabled(false);
			
			Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
			buttonReadAngle.setEnabled(true);
		} else {
			angle1 = INVALID_ANGLE;
			Toast.makeText(this, "Cleared 1st angle reading", Toast.LENGTH_SHORT).show();
			
			Button buttonUndoAngle = (Button) v;
			buttonUndoAngle.setEnabled(false);
		}
	}
	
	/**
	 * Calculate the total height of tree
	 * and reset angles readings
	 * @param v
	 */
	public void onClickCalculateHeight(View v) {
		Log.d(TAG, "onClickCalculateHeight");
		calculateTreeHeight();
		//FIXME: what is the reasonable accuracy %.2f m or cm?
		Toast.makeText(this, String.format("Total Tree Height: %.2f cm", heightTree), Toast.LENGTH_SHORT).show();
		
		angle1 = angle2 = INVALID_ANGLE;
		
		Button buttonCalculateHeight = (Button) v;
		buttonCalculateHeight.setEnabled(false);
		
		Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
		buttonReadAngle.setEnabled(true);
		
		Button buttonUndoAngle = (Button) findViewById(R.id.buttonUndoAngle);
		buttonUndoAngle.setEnabled(false);
	}

	/**
	 * Gets the ID of requested camera
	 * @param facing: whether facing front or back
	 * @return cameraId: ID of camera requested
	 */
	private int getFacingCameraId(int facing) {
		Log.d(TAG, "getFacingCameraId");
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
		Log.d(TAG, "getFrontFacingCameraId");
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
		Log.d(TAG, "getBackFacingCameraId");
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
		Log.d(TAG, "openSelectedCamera");
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
	 * Swaps the selected camera, between front facing and back facing
	 */
	public void swapCamera() {
		Log.d(TAG, "swapCamera");
		if (selectedCameraId == frontFacingCameraId) {
			selectedCameraId = getBackFacingCameraId();	
		} else {
			selectedCameraId = getFrontFacingCameraId();
		}
		openSelectedCamera();
	}

	/**
	 * Releases the selected camera object, and resets it
	 */
	private void releaseSelectedCamera() {
		Log.d(TAG, "releaseSelectedCamera");
		if(selectedCamera != null) {
			CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
			cameraPreview.releaseCamera();
			selectedCamera.release();
			selectedCamera = null;
		}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onAccuracyChanged");
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.d(TAG, "onSensorChanged");

		//TODO: remove TextViews. Only need to capture the values
		TextView textX = (TextView)findViewById(R.id.textViewX);
		TextView textY = (TextView)findViewById(R.id.textViewY);
		TextView textZ = (TextView)findViewById(R.id.textViewZ);
		TextView textAngle = (TextView)findViewById(R.id.textViewAngle);

		float valueX = event.values[0];
		float valueY = event.values[1];
		float valueZ = -1 * event.values[2]; //multiply by -1 to obtain Z actual readings for device on its back

		int rotation = getRotation(this);
		switch (rotation) {
		case Surface.ROTATION_0:
			//portrait
			tempAngle = calculateAngle(valueY, valueZ);
			break;
		case Surface.ROTATION_90:
			//landscape
			tempAngle = calculateAngle(valueX, valueZ);
			break;
		case Surface.ROTATION_180:
			//reverse portrait
			tempAngle = calculateAngle(-1*valueY, valueZ);
			break;
		default:
			//reverse landscape
			tempAngle = calculateAngle(-1*valueX, valueZ);
			break;
		}

		//TODO: remove if not needed
		/*if(valueY < 0) {
			Toast.makeText(this, "You have exceeded angle value", Toast.LENGTH_SHORT).show();
		}*/

		textX.setText(Float.toString(valueX));
		textY.setText(Float.toString(valueY));
		textZ.setText(Float.toString(valueZ));
		textAngle.setText(Double.toString(tempAngle));

	}

	private double calculateAngle(float axis, float depth) {
		//TODO: remove to degrees. used only for easier reading
		return Math.toDegrees(Math.atan(depth/axis));
	}

	/**
	 * Get the current rotation of device
	 * @param context
	 * @return (0: portrait, 90: landscape, 180: reverse portrait, 270: reverse landscape)
	 */
	private int getRotation(Context context){
		return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	}

	private void setupCameraHeight() {
		// EditText to allow user input
		final EditText input = new EditText(this);
		input.setHint(R.string.height_dialog_text);
		input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

		//camera height input-dialog setup
		final AlertDialog heightDialog = new AlertDialog.Builder(this)
		.setView(input)
		.setTitle(R.string.height_dialog_title)
		.setPositiveButton(android.R.string.ok, null)
		.create();

		heightDialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {

				Button buttonOk = heightDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				buttonOk.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						//verify correct input
						try {
							heightCamera = Double.parseDouble(input.getText().toString());
							
							if (heightCamera > 0) {
								heightDialog.dismiss();
								//Display Height
								TextView textViewCameraHeight = (TextView) findViewById(R.id.textViewCameraHeight);
								textViewCameraHeight.setText(String.format(
										"Camera Height: %f", heightCamera));
								//Enable interface
								Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
								buttonReadAngle.setEnabled(true);
							} else {
								Toast.makeText(CameraActivity.this, "Input Must be greater than zero", Toast.LENGTH_SHORT).show();
							}
						} catch (NumberFormatException e) {
							Toast.makeText(CameraActivity.this, "Invalid Input", Toast.LENGTH_SHORT).show();
							Log.e(TAG, "exception", e);
						}

					}
				});
			}
		});
		heightDialog.setCanceledOnTouchOutside(false);
		heightDialog.show();
	}
	
	/**
	 * Calculate height for current angles readings
	 * and display it
	 */
	private void calculateTreeHeight() {
		TextView textViewTreeHeight = (TextView) findViewById(R.id.textViewTotalHeight);
		
		if (angle1 < angle2) {
			heightTree = heightCamera*((Math.tan(Math.toRadians(angle2))/Math.tan(Math.toRadians(angle1)))+ 1);	
		} else {
			heightTree = heightCamera*((Math.tan(Math.toRadians(angle1))/Math.tan(Math.toRadians(angle2)))+ 1);
		}
		
		heightTree = Math.abs(heightTree);
		textViewTreeHeight.setText(String.format("Tree Height = %.2f",heightTree));
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
		
		//initializations
		heightCamera = 0;
		heightTree = 0;
		angle1 = INVALID_ANGLE;
		angle2 = INVALID_ANGLE;

		//check for camera feature
		PackageManager pm = getPackageManager();
		hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		hasFrontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

		if (!(hasCamera || hasFrontCamera)) {
			Toast.makeText(this, "Camera Feature is not supported by this device", Toast.LENGTH_SHORT).show();
		} else {
			//sets back facing camera as default for selected camera
			selectedCameraId = getBackFacingCameraId();
			//checks if the program has a saved state
			if (savedInstanceState != null) {
				selectedCameraId = savedInstanceState.getInt(SELETED_CAMERA_ID_KEY);
			}
			openSelectedCamera();
		}

		//accelerometer setup
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

		//camera height setup
		setupCameraHeight();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.math, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		
		int id = item.getItemId();
		
		switch(id) {
		case R.id.action_camera_height:
			setupCameraHeight();
			return true;
		case R.id.action_camera_swap:
			swapCamera();
			return true;
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
		openSelectedCamera();
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
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		openSelectedCamera(); //to restore control of selected camera resource
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
