package com.amb12u.treeheight;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends Activity implements SensorEventListener {


	//TODO: Move all text to strings.xml
	private final String TAG = "CameraActivity";
	private final int INVALID_ANGLE = -999;

	private final int STAGE_HEIGHT_INPUT = 0;
	private final int STAGE_TREETOP_ANGLE = 1;
	private final int STAGE_TREE_BOTTOM_ANGLE = 2;
	private final int STAGE_CALCULATE_TREE_HEIGHT = 3;

	private final String SELETED_CAMERA_ID_KEY = "selectedCameraId";
	private final int CAMERA_ID_NOT_SET = -1;

	// Determine existence of cameras
	private boolean hasCamera;
	private boolean hasFrontCamera;

	private Camera selectedCamera;
	private int currentStage;
	private boolean isInstructionEnabled;
	private boolean isDebuggingEnabled;

	// Holds ID values for cameras
	private int frontFacingCameraId;
	private int backFacingCameraId;
	private int selectedCameraId;

	// Accelerometer variables
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;

	// Camera height
	private double heightCamera;

	// Angle readings
	private double accelerometerAngle;
	private double angleTreetop;
	private double angleTreeBottom;

	//Tree height
	private double heightTree;

	private SeekBar seekBarZoom;

	//Debugging textViews
	TextView textViewCameraHeight;
	TextView textViewX;
	TextView textViewY;
	TextView textViewZ;
	TextView textViewAngle;
	TextView textViewFirstAngle;
	TextView textViewSecondAngle;
	TextView textViewFormula;
	TextView textViewTreeHeight;



	/**
	 * Read and store current angle reading
	 * @param v: The view that invoked the method
	 */
	public void onClickReadAngle(View v) {
		Log.d(TAG, "onClickReadAngle");
		if (currentStage == STAGE_TREETOP_ANGLE) {
			//Treetop angle must be positive
			if(accelerometerAngle > 0) {
				angleTreetop = accelerometerAngle;
				Toast.makeText(this, String.format("Angle at treetop = %.2fº", angleTreetop), Toast.LENGTH_SHORT).show();

				Button buttonUndoAngle = (Button) findViewById(R.id.buttonUndoAngle);
				buttonUndoAngle.setEnabled(true);

				//change programme stage
				currentStage = STAGE_TREE_BOTTOM_ANGLE;

				//show next step's instructions
				showInsturctions(isInstructionEnabled);

				//TODO:remove later
				textViewFirstAngle.setText(String.format("angle treetop = %.2f",angleTreetop));
			} else {
				Toast.makeText(this, "Angle to treetop must be positive", Toast.LENGTH_SHORT).show();
			}
		} else if (currentStage == STAGE_TREE_BOTTOM_ANGLE){
			if(accelerometerAngle < 0) {
				angleTreeBottom = accelerometerAngle;
				Toast.makeText(this, String.format("Angle at tree bottom = %.2fº", angleTreeBottom), Toast.LENGTH_SHORT).show();

				Button buttonReadAngle = (Button) v;
				buttonReadAngle.setEnabled(false);

				Button buttonCalculateHeight = (Button) findViewById(R.id.buttonCalculateHeight);
				buttonCalculateHeight.setEnabled(true);

				//change programme stage
				currentStage = STAGE_CALCULATE_TREE_HEIGHT;

				//TODO:remove later
				textViewSecondAngle.setText(String.format("angle tree bottom = %.2f", angleTreeBottom));
			} else {
				Toast.makeText(this, "Angle to tree bottom must be negative", Toast.LENGTH_SHORT).show();
			}
		}	
	}

	/**
	 * Reset angles readings
	 * @param v
	 */
	public void onClickResetAngle(View v) {
		Log.d(TAG, "onClickResetAngle");

		if (currentStage == STAGE_TREE_BOTTOM_ANGLE) {
			currentStage = STAGE_TREETOP_ANGLE;
			angleTreetop = INVALID_ANGLE;
			Toast.makeText(this, "Cleared treetop angle value", Toast.LENGTH_SHORT).show();


		} else if (currentStage == STAGE_CALCULATE_TREE_HEIGHT){
			currentStage = STAGE_TREE_BOTTOM_ANGLE;
			angleTreeBottom = INVALID_ANGLE;
			Toast.makeText(this, "Cleared tree bottom angle value", Toast.LENGTH_SHORT).show();

			Button buttonCalculateHeight = (Button) findViewById(R.id.buttonCalculateHeight);
			buttonCalculateHeight.setEnabled(false);

			Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
			buttonReadAngle.setEnabled(true);
		}
		Button buttonReadAngle = (Button) v;
		buttonReadAngle.setEnabled(false);
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

		angleTreetop = angleTreeBottom = INVALID_ANGLE;

		Button buttonCalculateHeight = (Button) v;
		buttonCalculateHeight.setEnabled(false);

		Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
		buttonReadAngle.setEnabled(true);

		Button buttonUndoAngle = (Button) findViewById(R.id.buttonUndoAngle);
		buttonUndoAngle.setEnabled(false);

		//show next step's instructions
		showInsturctions(isInstructionEnabled);

		//disable instructions
		isInstructionEnabled = false;
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

				//configure camera zoom
				seekBarZoom = (SeekBar) findViewById(R.id.seekBarZoom);
				if (selectedCamera.getParameters().isZoomSupported()) {
					seekBarZoom.setMax(selectedCamera.getParameters().getMaxZoom());
					seekBarZoom.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							Camera.Parameters params = selectedCamera.getParameters();
							params.setZoom(progress);
							selectedCamera.setParameters(params);

						}
					});
				} else {
					seekBarZoom.setVisibility(View.INVISIBLE);
				}

			} catch (Exception e) {
				message = "Unable to open camera: "+ Log.getStackTraceString(e); 
				Log.e(TAG, message);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
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

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.d(TAG, "onSensorChanged");

		//TODO: remove TextViews. Only need to capture the values

		float valueX = event.values[0];
		float valueY = event.values[1];
		float valueZ = -1 * event.values[2]; //multiply by -1 to obtain Z actual readings for device on its back

		//Log.d(TAG, ""+System.currentTimeMillis()+","+event.values[0]+","+event.values[1]+","+(event.values[2]));
		int rotation = getRotation(this);
		switch (rotation) {
		case Surface.ROTATION_0:
			//portrait
			accelerometerAngle = calculateAngle(valueY, valueZ);
			break;
		case Surface.ROTATION_90:
			//landscape
			accelerometerAngle = calculateAngle(valueX, valueZ);
			break;
		case Surface.ROTATION_180:
			//reverse portrait
			accelerometerAngle = calculateAngle(-1*valueY, valueZ);
			break;
		default:
			//reverse landscape
			accelerometerAngle = calculateAngle(-1*valueX, valueZ);
			break;
		}

		//TODO: remove if not needed
		/*if(valueY < 0) {
			Toast.makeText(this, "You have exceeded angle value", Toast.LENGTH_SHORT).show();
		}*/

		textViewX.setText("acceleration X = "+Float.toString(valueX));
		textViewY.setText("acceleration Y = "+Float.toString(valueY));
		textViewZ.setText("acceleration Z = "+Float.toString(valueZ));
		textViewAngle.setText(String.format("Current angle = %.2f",accelerometerAngle));
	}

	/**
	 * Calculate the angle between two dimensions in degrees
	 * <b>axis</b>: either x-axis or y-axis
	 * <b>axis</b>: z-axis
	 * @param axis
	 * @param depth
	 * @return the angle between dimensions in degrees 
	 */
	private double calculateAngle(float axis, float depth) {
		Log.d(TAG, "calculateAngle");
		//TODO: remove to degrees. used only for easier reading
		return Math.toDegrees(Math.atan(depth/axis));
	}

	/**
	 * Get the current rotation of device
	 * @param context
	 * @return (0: portrait, 90: landscape, 180: reverse portrait, 270: reverse landscape)
	 */
	private int getRotation(Context context){
		Log.d(TAG, "getRotation");
		return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	}

	/**
	 * Creates a dialog to input camera height
	 * The value of camera height is positive double, and can't be zero
	 * The unit for camera height is cm
	 */
	private void setupCameraHeight() {
		Log.d(TAG, "setupCameraHeight");
		//camera height input-dialog setup
		final Dialog dialogInstruction = new Dialog(this, R.style.myInstructionDialog);
		dialogInstruction.setContentView(R.layout.dialog_custom_person_height);
		dialogInstruction.setTitle("Your Height");

		Button button = (Button) dialogInstruction.findViewById(R.id.buttonConfrimHeight);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				//verify correct input
				try {
					EditText editTextHeight = (EditText) dialogInstruction.findViewById(R.id.editTextYourHeight);
					heightCamera = Double.parseDouble(editTextHeight.getText().toString());

					if (heightCamera > 0) {
						dialogInstruction.dismiss();

						//Display Height
						textViewCameraHeight.setText(String.format("Camera Height: %.2f", heightCamera));
						//Enable interface
						Button buttonReadAngle = (Button) findViewById(R.id.buttonReadAngle);
						buttonReadAngle.setEnabled(true);

						CheckBox checkBoxInstruction = (CheckBox) dialogInstruction.findViewById(R.id.checkBoxInstructions);
						if (checkBoxInstruction.isChecked()){
							isInstructionEnabled = true;
						}

						//change programme stage
						currentStage = STAGE_TREETOP_ANGLE;

						//show next step's instructions
						showInsturctions(isInstructionEnabled);
					} else {
						Toast.makeText(CameraActivity.this, "Input Must be greater than zero", Toast.LENGTH_SHORT).show();
					}
				} catch (NumberFormatException e) {
					Toast.makeText(CameraActivity.this, "Invalid Input", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "exception", e);
				}
			}
		});
		dialogInstruction.setCancelable(false);
		dialogInstruction.show();
	}

	/**
	 * Display instructions using a custom dialog.
	 * Depends on the current application stage
	 * @param isEnabled: flag to show or hide instruction dialogs
	 */
	private void showInsturctions(boolean isEnabled) {
		Log.d(TAG, "showInsturctions");

		if (!isEnabled) {
			return;
		}

		final Dialog dialogPerson = new Dialog(CameraActivity.this, R.style.myCustomDialog);
		dialogPerson.setContentView(R.layout.dialog_custom_person);
		ImageView imageViewPerson = (ImageView) dialogPerson.findViewById(R.id.imageViewPerson);
		imageViewPerson.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				dialogPerson.dismiss();

				String dialogTitle = null;
				int dialogLayoutID = 0;
				final Dialog dialogInstruction = new Dialog(CameraActivity.this, R.style.myInstructionDialog);

				switch(currentStage) {
				case STAGE_TREETOP_ANGLE:
					dialogTitle = "Highest Point!";
					dialogLayoutID = R.layout.dialog_custom_math_treetop;
					break;

				case STAGE_TREE_BOTTOM_ANGLE:
					dialogTitle = "Lowest Point!";
					dialogLayoutID = R.layout.dialog_custom_math_tree_bottom;
					break;

				case STAGE_CALCULATE_TREE_HEIGHT:
					dialogTitle = "How Did We Calculate The Tree Height?";
					dialogLayoutID = R.layout.dialog_custom_math_how;
					currentStage = STAGE_TREETOP_ANGLE;
					break;

				default:
					return false;
				}

				dialogInstruction.setContentView(dialogLayoutID);
				dialogInstruction.setTitle(dialogTitle);
				Button button = (Button) dialogInstruction.findViewById(R.id.buttonOkay);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dialogInstruction.dismiss();
					}
				});
				dialogPerson.setCancelable(true);
				dialogPerson.setCanceledOnTouchOutside(true);
				dialogInstruction.show();
				return false;
			}
		});
		WindowManager.LayoutParams dialogAttrib = dialogPerson.getWindow().getAttributes();
		Point point = new Point();
		getWindowManager().getDefaultDisplay().getSize(point);

		dialogAttrib.gravity = Gravity.BOTTOM;
		dialogAttrib.y = 1 * point.y / 4;
		dialogAttrib.x = 3 * point.x / 4;

		dialogPerson.setCancelable(true);
		dialogPerson.setCanceledOnTouchOutside(true);
		dialogPerson.show();
	}


	/**
	 * Sets the visibility of debugging textViews
	 * @param isEnabled: flag to show or hide debugging information
	 */
	private void showDebuggingInfo(boolean isEnabled) {
		Log.d(TAG, "showDebuggingInfo");
		if (isEnabled) {
			textViewCameraHeight.setVisibility(View.VISIBLE);

			textViewX.setVisibility(View.VISIBLE);
			textViewY.setVisibility(View.VISIBLE);
			textViewZ.setVisibility(View.VISIBLE);
			textViewAngle.setVisibility(View.VISIBLE);

			textViewFirstAngle.setVisibility(View.VISIBLE);
			textViewSecondAngle.setVisibility(View.VISIBLE);

			textViewFormula.setVisibility(View.VISIBLE);
			textViewTreeHeight.setVisibility(View.VISIBLE);

		} else {
			textViewCameraHeight.setVisibility(View.INVISIBLE);

			textViewX.setVisibility(View.INVISIBLE);
			textViewY.setVisibility(View.INVISIBLE);
			textViewZ.setVisibility(View.INVISIBLE);
			textViewAngle.setVisibility(View.INVISIBLE);

			textViewFirstAngle.setVisibility(View.INVISIBLE);
			textViewSecondAngle.setVisibility(View.INVISIBLE);

			textViewFormula.setVisibility(View.INVISIBLE);
			textViewTreeHeight.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Calculate height for current angles readings
	 * and display it
	 */
	private void calculateTreeHeight() {
		Log.d(TAG, "calculateTreeHeight");

		if (angleTreetop < angleTreeBottom) {
			double temp = angleTreeBottom;
			angleTreeBottom = angleTreetop;
			angleTreetop = temp;
		} 

		angleTreetop = Math.abs(angleTreetop);
		angleTreeBottom = Math.abs(angleTreeBottom);
		heightTree = heightCamera*((Math.tan(Math.toRadians(angleTreetop))/Math.tan(Math.toRadians(angleTreeBottom)))+ 1);
		heightTree = Math.abs(heightTree);
		textViewFormula.setText(String.format("%.2f*((%.2f/%.2f)+1)",heightCamera, angleTreetop, angleTreeBottom));
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
		setContentView(R.layout.activity_camera);
		//hide navigation bar
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		//initializations
		frontFacingCameraId = CAMERA_ID_NOT_SET;
		backFacingCameraId = CAMERA_ID_NOT_SET;
		selectedCameraId = CAMERA_ID_NOT_SET;

		heightCamera = 0;
		heightTree = 0;
		angleTreetop = INVALID_ANGLE;
		angleTreeBottom = INVALID_ANGLE;
		currentStage = STAGE_HEIGHT_INPUT;
		isInstructionEnabled = false;
		isDebuggingEnabled = false;


		textViewCameraHeight = (TextView)findViewById(R.id.textViewCameraHeight);

		textViewX = (TextView)findViewById(R.id.textViewX);
		textViewY = (TextView)findViewById(R.id.textViewY);
		textViewZ = (TextView)findViewById(R.id.textViewZ);
		textViewAngle = (TextView)findViewById(R.id.textViewAngle);

		textViewFirstAngle = (TextView) findViewById(R.id.textViewTreetopAngle);
		textViewSecondAngle= (TextView) findViewById(R.id.textViewTreeBottomAngle);

		textViewFormula = (TextView) findViewById(R.id.textViewFormula);
		textViewTreeHeight = (TextView) findViewById(R.id.textViewTotalHeight);


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

		//disable debugging information
		showDebuggingInfo(isDebuggingEnabled);

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
		case R.id.action_instruction:
			isInstructionEnabled = !isInstructionEnabled;
			return true;
		case R.id.action_debugging_information:
			isDebuggingEnabled = !isDebuggingEnabled;
			showDebuggingInfo(isDebuggingEnabled);
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

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}
}
